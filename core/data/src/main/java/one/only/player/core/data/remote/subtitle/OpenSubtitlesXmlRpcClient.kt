package one.only.player.core.data.remote.subtitle

import android.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import one.only.player.core.common.Logger
import one.only.player.core.model.OnlineSubtitleLanguage
import one.only.player.core.model.OnlineSubtitlePayload
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.model.OnlineSubtitleResult

// OpenSubtitles.org 的 XML-RPC 接口：同样免 key，但下载走 base64 内联数据，不依赖签名直链
@Singleton
class OpenSubtitlesXmlRpcClient @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val tokenMutex = Mutex()

    @Volatile
    private var cachedToken: String? = null

    suspend fun searchByQuery(
        query: String,
        languageCode: String?,
    ): List<OnlineSubtitleResult> {
        val response = callWithToken(methodName = METHOD_SEARCH_SUBTITLES) { token ->
            buildSearchParams(
                token = token,
                query = query,
                languageCode = languageCode,
            )
        }
        val subtitles = response.dataItems()
        Logger.debug(TAG, "XML-RPC search returned ${subtitles.size} items, language=$languageCode")
        return subtitles
            .mapNotNull { item -> item.toOnlineSubtitleResult() }
            .distinctBy { result -> result.providerId }
            .sortedByDescending { result -> result.downloadCount ?: 0 }
    }

    suspend fun fetchSubtitle(result: OnlineSubtitleResult): OnlineSubtitlePayload {
        val response = callWithToken(methodName = METHOD_DOWNLOAD_SUBTITLES) { token ->
            buildDownloadParams(token = token, subtitleFileId = result.providerId)
        }
        val encodedData = response.dataItems()
            .firstNotNullOfOrNull { item -> item["data"]?.toString()?.takeIf(String::isNotEmpty) }
            ?: throw SubtitleSearchFailedException("XML-RPC download returned no data")

        val decoded = runCatching { Base64.decode(encodedData, Base64.DEFAULT) }
            .getOrElse { throwable -> throw SubtitleSearchFailedException("Malformed base64 payload", throwable) }
        return SubtitlePayloadDecoder.decode(decoded, result.format)
    }

    private suspend fun callWithToken(
        methodName: String,
        buildParams: (String) -> String,
    ): Map<String, Any?> {
        val response = execute(methodName, buildParams(ensureToken()))
        if (response.status() != SESSION_EXPIRED_STATUS) return response.requireSuccess()

        Logger.debug(TAG, "XML-RPC token expired, logging in again")
        resetToken()
        return execute(methodName, buildParams(ensureToken())).requireSuccess()
    }

    private suspend fun ensureToken(): String {
        cachedToken?.let { token -> return token }
        return tokenMutex.withLock {
            cachedToken?.let { token -> return@withLock token }

            val response = execute(
                methodName = METHOD_LOG_IN,
                paramsXml = buildString {
                    append(emptyStringParam())
                    append(emptyStringParam())
                    append(stringParam(DEFAULT_LANGUAGE))
                    append(stringParam(USER_AGENT))
                },
            )
            val token = response["token"]?.toString().orEmpty()
            if (token.isEmpty()) {
                throw SubtitleSearchFailedException(
                    "XML-RPC login failed: status=${response.status()}, fault=${response["faultString"] ?: response["faultCode"]}",
                )
            }
            cachedToken = token
            Logger.debug(TAG, "XML-RPC login ok")
            token
        }
    }

    private fun resetToken() {
        cachedToken = null
    }

    private suspend fun execute(
        methodName: String,
        paramsXml: String,
    ): Map<String, Any?> {
        val requestBody = buildString {
            append("<?xml version=\"1.0\"?>")
            append("<methodCall><methodName>").append(methodName).append("</methodName>")
            append("<params>").append(paramsXml).append("</params>")
            append("</methodCall>")
        }
        val request = Request.Builder()
            .url(ENDPOINT_URL)
            .header("User-Agent", USER_AGENT)
            .post(requestBody.toRequestBody(XML_MEDIA_TYPE))
            .build()

        return httpClient.newCall(request).awaitBody { body ->
            XmlRpcParser.parseResponse(SubtitlePayloadDecoder.readCapped { body.byteStream().read(it) }.decodeToString())
        }
    }

    private fun buildSearchParams(
        token: String,
        query: String,
        languageCode: String?,
    ): String = buildString {
        append(stringParam(token))
        append("<param><value><array><data><value><struct>")
        append(member("query", query))
        if (languageCode != null) {
            append(member("sublanguageid", languageCode.toOpenSubtitlesLanguageCode()))
        }
        append("</struct></value></data></array></value></param>")
    }

    private fun buildDownloadParams(
        token: String,
        subtitleFileId: String,
    ): String = buildString {
        append(stringParam(token))
        append("<param><value><array><data><value><string>")
        append(subtitleFileId.escapeXml())
        append("</string></value></data></array></value></param>")
    }

    private fun Map<String, Any?>.status(): String = this["status"]?.toString().orEmpty()

    private fun Map<String, Any?>.requireSuccess(): Map<String, Any?> {
        if (status() != "200 OK") throw SubtitleSearchFailedException("XML-RPC request failed: ${status()}")
        return this
    }

    private fun Map<String, Any?>.dataItems(): List<Map<String, Any?>> = (this["data"] as? List<*>)
        ?.mapNotNull { item -> (item as? Map<*, *>)?.let(::toStringMap) }
        .orEmpty()

    private fun toStringMap(raw: Map<*, *>): Map<String, Any?> = raw.entries.associate { (key, value) -> key.toString() to value }

    private fun Map<String, Any?>.toOnlineSubtitleResult(): OnlineSubtitleResult? {
        val subtitleFileId = this["IDSubtitleFile"]?.toString().orEmpty()
        if (subtitleFileId.isEmpty()) return null

        val rawLanguageCode = this["SubLanguageID"]?.toString().orEmpty()
        val languageName = this["LanguageName"]?.toString().orEmpty()
        val fileName = this["SubFileName"]?.toString().orEmpty()
        val releaseName = this["MovieReleaseName"]?.toString().orEmpty()

        return OnlineSubtitleResult(
            provider = OnlineSubtitleProvider.OPEN_SUBTITLES_XML_RPC,
            providerId = subtitleFileId,
            languageCode = OnlineSubtitleLanguage.normalize(rawLanguageCode),
            languageName = languageName.ifEmpty { rawLanguageCode },
            title = fileName.substringBeforeLast('.').ifEmpty { releaseName },
            format = this["SubFormat"]?.toString().orEmpty(),
            downloadCount = this["SubDownloadsCnt"]?.toString()?.toDoubleOrNull()?.toInt(),
        )
    }

    private fun member(
        name: String,
        value: String,
    ): String = "<member><name>$name</name><value><string>${value.escapeXml()}</string></value></member>"

    private fun stringParam(value: String): String = "<param><value><string>${value.escapeXml()}</string></value></param>"

    private fun emptyStringParam(): String = stringParam("")

    private fun String.escapeXml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private companion object {
        const val TAG = "OpenSubtitlesXmlRpcClient"
        const val ENDPOINT_URL = "https://api.opensubtitles.org/xml-rpc"
        const val METHOD_LOG_IN = "LogIn"
        const val METHOD_SEARCH_SUBTITLES = "SearchSubtitles"
        const val METHOD_DOWNLOAD_SUBTITLES = "DownloadSubtitles"
        const val USER_AGENT = "OnlyPlayer/1.0 (Android)"
        const val DEFAULT_LANGUAGE = "en"
        const val SESSION_EXPIRED_STATUS = "401 Unauthorized"
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 20L
        val XML_MEDIA_TYPE = "text/xml".toMediaType()
    }
}
