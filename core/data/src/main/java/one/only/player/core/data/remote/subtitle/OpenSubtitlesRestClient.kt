package one.only.player.core.data.remote.subtitle

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import one.only.player.core.common.Logger
import one.only.player.core.model.OnlineSubtitleLanguage
import one.only.player.core.model.OnlineSubtitlePayload
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.model.OnlineSubtitleResult
import org.json.JSONArray
import org.json.JSONObject

// OpenSubtitles.org REST 接口使用 ISO639-2 语言码。
@Singleton
class OpenSubtitlesRestClient @Inject constructor() {

    // 该接口把非规范路径 302 到不可用主机，搜索请求一律不跟随重定向
    private val searchClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun searchByQuery(
        query: String,
        languageCode: String?,
    ): List<OnlineSubtitleResult> {
        val normalizedQuery = query.normalizeAsQueryToken()
        // 参数必须按字母序拼接，否则接口会重定向到坏地址
        val searchParams = buildList {
            add("query-$normalizedQuery")
            if (languageCode != null) {
                add("sublanguageid-${languageCode.toOpenSubtitlesLanguageCode()}")
            }
        }
        val url = buildSearchUrl(searchParams)
        Logger.debug(TAG, "OpenSubtitles REST search: language=$languageCode")
        val body = executeSearch(url)
        return parseResults(body)
    }

    suspend fun fetchSubtitle(result: OnlineSubtitleResult): OnlineSubtitlePayload {
        val url = result.providerId.toHttpUrlOrNull()
            ?: throw SubtitleSearchFailedException("Invalid download url")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        return downloadClient.newCall(request).awaitBody { body ->
            val bytes = SubtitlePayloadDecoder.readCapped { buffer -> body.byteStream().read(buffer) }
            SubtitlePayloadDecoder.decode(bytes, result.format)
        }
    }

    private fun buildSearchUrl(searchParams: List<String>): HttpUrl {
        val baseUrl = "$BASE_URL/search".toHttpUrl()
        val builder = baseUrl.newBuilder()
        searchParams.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    private suspend fun executeSearch(url: HttpUrl): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        return searchClient.newCall(request).awaitBody { body ->
            SubtitlePayloadDecoder.readCapped { body.byteStream().read(it) }.decodeToString()
        }
    }

    private fun parseResults(body: String): List<OnlineSubtitleResult> {
        val jsonArray = runCatching { JSONArray(body) }.getOrElse { throwable ->
            Logger.error(TAG, "Failed to parse search response", throwable)
            throw SubtitleSearchFailedException("Malformed search response", throwable)
        }

        return (0 until jsonArray.length())
            .mapNotNull { index -> jsonArray.optJSONObject(index)?.toOnlineSubtitleResult() }
            .distinctBy { it.providerId }
    }

    private fun JSONObject.toOnlineSubtitleResult(): OnlineSubtitleResult? {
        val downloadUrl = optString("SubDownloadLink", "")
        if (downloadUrl.isEmpty()) return null

        val rawLanguageCode = optString("SubLanguageID", optString("ISO639", ""))
        val languageName = optString("LanguageName", "")
        val fileName = optString("SubFileName", "")
        val releaseName = optString("MovieReleaseName", "")
        val title = fileName.ifEmpty { releaseName }.substringBeforeLast('.')

        return OnlineSubtitleResult(
            provider = OnlineSubtitleProvider.OPEN_SUBTITLES,
            providerId = downloadUrl,
            languageCode = OnlineSubtitleLanguage.normalize(rawLanguageCode),
            languageName = languageName.ifEmpty { rawLanguageCode },
            title = title.ifEmpty { releaseName },
            format = optString("SubFormat", ""),
            downloadCount = optString("SubDownloadsCnt", "").toIntOrNull(),
        )
    }

    private fun String.normalizeAsQueryToken(): String = lowercase().trim().replace(WHITESPACE_REGEX, "+").trim('+')

    private companion object {
        const val TAG = "OpenSubtitlesRestClient"
        const val BASE_URL = "https://rest.opensubtitles.org"
        const val USER_AGENT = "OnlyPlayer/1.0 (Android)"
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 15L
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
