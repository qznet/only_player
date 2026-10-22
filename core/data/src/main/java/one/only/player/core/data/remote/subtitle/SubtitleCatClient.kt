package one.only.player.core.data.remote.subtitle

import android.net.Uri
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import one.only.player.core.common.Logger
import one.only.player.core.model.OnlineSubtitleLanguage
import one.only.player.core.model.OnlineSubtitlePayload
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.model.OnlineSubtitleResult

// SubtitleCat 没有开放接口，只能解析 HTML：搜索页拿条目，条目页拿各语言直链
@Singleton
class SubtitleCatClient @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun searchByQuery(
        query: String,
        languageCode: String?,
    ): List<OnlineSubtitleResult> {
        val searchUrl = buildUrl(SEARCH_PATH).newBuilder().addQueryParameter("search", query).build()
        val searchHtml = fetchText(searchUrl)
        val entries = parseEntries(searchHtml).take(MAX_ENTRIES)
        if (entries.isEmpty()) return emptyList()

        // 每个条目页都要单独抓一次，限量并发，避免给站点压力
        return coroutineScope {
            val outcomes = entries
                .map { entry -> async { fetchVariants(entry, languageCode) } }
                .awaitAll()
            if (outcomes.all { it.isFailure }) {
                throw SubtitleSearchFailedException("All subtitlecat entries failed", outcomes.first().exceptionOrNull())
            }
            outcomes
                .mapNotNull { it.getOrNull() }
                .flatten()
                .distinctBy { result -> result.providerId }
                .take(MAX_RESULTS)
        }
    }

    suspend fun fetchSubtitle(result: OnlineSubtitleResult): OnlineSubtitlePayload {
        val url = buildUrl(result.providerId)
        return SubtitlePayloadDecoder.decode(fetchBytes(url), result.format)
    }

    private suspend fun fetchVariants(
        entry: SubtitleCatEntry,
        languageCode: String?,
    ): Result<List<OnlineSubtitleResult>> = try {
        val entryUrl = buildUrl(entry.path)
        val html = fetchText(entryUrl)
        Result.success(
            parseVariants(html, entry)
                .filter { variant -> languageCode == null || variant.languageCode == languageCode },
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        // 单个条目页失败不影响其它条目
        Logger.error(TAG, "Failed to load subtitlecat entry", exception)
        Result.failure(exception)
    }

    private suspend fun fetchText(url: HttpUrl): String = fetchBytes(url).decodeToString()

    private suspend fun fetchBytes(url: HttpUrl): ByteArray = httpClient.newCall(buildRequest(url)).awaitBody { body ->
        SubtitlePayloadDecoder.readCapped { buffer -> body.byteStream().read(buffer) }
    }

    private fun buildRequest(url: HttpUrl): Request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .build()

    private fun parseEntries(html: String): List<SubtitleCatEntry> = ENTRY_REGEX.findAll(html)
        .mapNotNull { match ->
            val id = match.groupValues[1]
            val rawName = match.groupValues[2]
            val title = match.groupValues[3].cleanHtml()
            if (rawName.isEmpty()) return@mapNotNull null

            SubtitleCatEntry(
                path = "/subs/$id/$rawName.html",
                stem = Uri.decode(rawName),
                title = title.ifEmpty { Uri.decode(rawName).substringBeforeLast('.') },
            )
        }
        .distinctBy { entry -> entry.path }
        .toList()

    private fun parseVariants(
        html: String,
        entry: SubtitleCatEntry,
    ): List<OnlineSubtitleResult> = VARIANT_REGEX.findAll(html)
        .mapNotNull { match ->
            val path = match.groupValues[1]
            val fileName = Uri.decode(path.substringAfterLast('/'))
            val variantStem = fileName.substringBeforeLast('.')
            // 条目页会混入其它条目的链接，只保留「条目名 + 语言后缀」的形式
            val languageSuffix = variantStem.removePrefix(entry.stem).takeIf { it != variantStem }
                ?: return@mapNotNull null

            val rawTag = languageSuffix.trimStart('-', '.').toLanguageTag()
            if (rawTag.isEmpty()) return@mapNotNull null

            OnlineSubtitleResult(
                provider = OnlineSubtitleProvider.SUBTITLE_CAT,
                providerId = path,
                languageCode = resolveLanguageCode(rawTag, entry.stem),
                languageName = rawTag,
                title = entry.title,
                format = "srt",
            )
        }
        .toList()

    // 后缀形如 -en、-zh-CN、-eng-zh-CN、-zh-Hant-pt-BR，取最后一段语言标签为目标语言
    private fun String.toLanguageTag(): String {
        val parts = split('-').filter { segment -> segment.isNotEmpty() }
        if (parts.isEmpty()) return ""

        val lastPart = parts.last()
        val isRegionPart = lastPart.length in 2..4 &&
            (lastPart.all(Char::isDigit) || lastPart.first().isUpperCase())
        return if (isRegionPart && parts.size >= 2) {
            "${parts[parts.size - 2]}-$lastPart"
        } else {
            lastPart
        }
    }

    // orig 表示条目原始字幕，语言只能从条目名推断
    private fun resolveLanguageCode(
        rawTag: String,
        entryStem: String,
    ): String {
        if (!rawTag.equals(ORIG_TAG, ignoreCase = true)) return OnlineSubtitleLanguage.normalize(rawTag)

        val lowerStem = entryStem.lowercase()
        return when {
            "zh-hant" in lowerStem || "cht" in lowerStem || "zh-tw" in lowerStem ->
                OnlineSubtitleLanguage.TRADITIONAL_CHINESE
            "zh-hans" in lowerStem || "chs" in lowerStem || "zh-cn" in lowerStem ->
                OnlineSubtitleLanguage.SIMPLIFIED_CHINESE
            else -> ""
        }
    }

    private fun buildUrl(path: String): HttpUrl {
        val builder = BASE_URL.toHttpUrl().newBuilder()
        path.removePrefix("/").split('/')
            .filter { segment -> segment.isNotEmpty() }
            .forEach { segment -> builder.addPathSegment(Uri.decode(segment)) }
        return builder.build()
    }

    private fun String.cleanHtml(): String = replace(TAG_REGEX, " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

    private data class SubtitleCatEntry(
        val path: String,
        val stem: String,
        val title: String,
    )

    private companion object {
        const val TAG = "SubtitleCatClient"
        const val BASE_URL = "https://subtitlecat.com"
        const val SEARCH_PATH = "/index.php"
        const val USER_AGENT = "OnlyPlayer/1.0 (Android)"
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 20L
        const val MAX_ENTRIES = 8
        const val MAX_RESULTS = 40
        const val ORIG_TAG = "orig"

        val ENTRY_REGEX = Regex("<a[^>]+href=\"subs/(\\d+)/([^\"]+)\\.html\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
        val VARIANT_REGEX = Regex("href=\"(/subs/\\d+/[^\"]+\\.srt)\"")
        val TAG_REGEX = Regex("<[^>]+>")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
