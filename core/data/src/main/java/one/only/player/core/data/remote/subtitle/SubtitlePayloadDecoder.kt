package one.only.player.core.data.remote.subtitle

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import one.only.player.core.model.OnlineSubtitlePayload

// 各来源的字幕统一解包，缓存前拒绝错误页面与不支持的格式。
internal object SubtitlePayloadDecoder {

    const val MAX_PAYLOAD_BYTES = 10L * 1024 * 1024

    private val supportedExtensions = setOf("srt", "ass", "ssa", "vtt", "webvtt", "smi", "sami")

    fun decode(
        rawBytes: ByteArray,
        declaredFormat: String,
    ): OnlineSubtitlePayload {
        if (rawBytes.isEmpty()) throw SubtitlePayloadEmptyException()
        if (rawBytes.size.toLong() > MAX_PAYLOAD_BYTES) throw SubtitlePayloadTooLargeException(rawBytes.size.toLong())

        val bytes = if (rawBytes.isGzip()) gunzip(rawBytes) else rawBytes
        if (bytes.isEmpty()) throw SubtitlePayloadEmptyException()
        // 免费下载部分条目只会拿到 VIP 广告占位，不能当字幕落盘
        if (bytes.isVipPlaceholder()) throw SubtitleVipRequiredException()
        val extension = resolveExtension(declaredFormat, bytes)

        return OnlineSubtitlePayload(
            bytes = if (extension == "srt") bytes.normalizeSubripIndices() else bytes,
            extension = extension,
        )
    }

    // 仅修正时间轴前的 ASCII 序号行；单字节映射保证其他编码的正文原样保留。
    private fun ByteArray.normalizeSubripIndices(): ByteArray = toString(Charsets.ISO_8859_1)
        .replace(SRT_INDEX_REGEX) { it.groupValues[1] }
        .toByteArray(Charsets.ISO_8859_1)

    fun readCapped(readBytes: (ByteArray) -> Int): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        var totalBytes = 0L

        while (true) {
            val readCount = readBytes(buffer)
            if (readCount == -1) break

            totalBytes += readCount
            // 超限立即中断，避免把超大字幕读进内存
            if (totalBytes > MAX_PAYLOAD_BYTES) throw SubtitlePayloadTooLargeException(totalBytes)
            output.write(buffer, 0, readCount)
        }

        return output.toByteArray()
    }

    private fun ByteArray.isGzip(): Boolean = size >= 2 && this[0] == 0x1F.toByte() && this[1] == 0x8B.toByte()

    // 占位内容形如 "Become OpenSubtitles.org VIP Member to get subtitles -> osdb.link/vip"
    private fun ByteArray.isVipPlaceholder(): Boolean = size <= VIP_PLACEHOLDER_MAX_BYTES && decodeToString().contains(VIP_PLACEHOLDER_MARKER, ignoreCase = true)

    private fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { inputStream ->
        readCapped { buffer -> inputStream.read(buffer) }
    }

    private fun resolveExtension(
        declaredFormat: String,
        bytes: ByteArray,
    ): String {
        val head = bytes.copyOfRange(0, minOf(bytes.size, SNIFF_BYTES)).decodeToString().trimStart('\uFEFF', ' ', '\n', '\r')
        if (head.contains("<html", ignoreCase = true) || head.startsWith("<!DOCTYPE html", ignoreCase = true)) {
            throw SubtitleSearchFailedException("Subtitle download returned HTML")
        }
        val declared = declaredFormat.trim().lowercase()
        if (declared in supportedExtensions) return declared
        if (head.contains("[Script Info]", ignoreCase = true) || head.contains("Dialogue:", ignoreCase = true)) {
            return "ass"
        }
        if (head.contains("WEBVTT", ignoreCase = true)) return "vtt"
        if (head.contains("<SAMI", ignoreCase = true)) return "smi"
        if (SRT_TIMESTAMP_REGEX.containsMatchIn(head)) return "srt"
        throw SubtitleSearchFailedException("Unsupported subtitle format: $declaredFormat")
    }

    private const val SNIFF_BYTES = 1024
    private const val VIP_PLACEHOLDER_MARKER = "osdb.link/vip"
    private const val VIP_PLACEHOLDER_MAX_BYTES = 2048
    private val SRT_TIMESTAMP_REGEX = Regex("\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{3}\\s*-->")
    private val SRT_INDEX_REGEX = Regex("(?m)^[ \\t]*(\\d+)[ \\t]*(?=\\r?\\n\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{3}\\s*-->)")
}

class SubtitlePayloadEmptyException : IllegalStateException("Subtitle payload is empty")

class SubtitlePayloadTooLargeException(
    val bytes: Long,
) : IllegalStateException("Subtitle payload exceeds 10 MB: $bytes")

class SubtitleVipRequiredException : IllegalStateException("Subtitle requires OpenSubtitles VIP download")

class SubtitleSearchFailedException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
