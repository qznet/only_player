package one.only.player.feature.player.subtitle

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import one.only.player.core.common.Dispatcher
import one.only.player.core.common.DispatcherType
import one.only.player.core.common.Logger
import one.only.player.core.model.OnlineSubtitleLanguage
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.model.OnlineSubtitleResult
import org.json.JSONObject

@Singleton
class OnlineSubtitleRepository(
    private val cacheRoot: File,
    private val filesRoot: File,
    private val downloader: suspend (String) -> DownloadStream,
    private val ioDispatcher: CoroutineDispatcher,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @Dispatcher(DispatcherType.IO) ioDispatcher: CoroutineDispatcher,
    ) : this(
        cacheRoot = context.cacheDir,
        filesRoot = context.filesDir,
        downloader = { url -> downloadWithOkHttp(url) },
        ioDispatcher = ioDispatcher,
    )

    private val subtitleDirectory = File(filesRoot, ONLINE_SUBTITLE_DIR_NAME)
    private val legacyDirectory = File(cacheRoot, ONLINE_SUBTITLE_DIR_NAME)
    private val migrationMutex = Mutex()

    suspend fun downloadSubtitle(url: String): DownloadedOnlineSubtitle = withContext(ioDispatcher) {
        val parsedUrl = ParsedSubtitleUrl.from(url)
        subtitleDirectory.mkdirs()

        val baseName = url.hashCode().toUInt().toString(16)
        val targetFile = File(subtitleDirectory, "$baseName.${parsedUrl.extension}")
        val tempFile = File.createTempFile(baseName, ".${parsedUrl.extension}.part", subtitleDirectory)
        Logger.debug(TAG, "Download online subtitle start: extension=${parsedUrl.extension}, target=${targetFile.name}")

        try {
            downloader(url).inputStream.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    copyCapped(inputStream, outputStream)
                }
            }
            promoteTempFile(tempFile, targetFile)
            saveMetadata(targetFile, SavedOnlineSubtitle(title = Uri.parse(url).lastPathSegment.orEmpty()))
        } catch (exception: OnlineSubtitleException) {
            tempFile.delete()
            throw exception
        } catch (exception: IOException) {
            Logger.error(TAG, "Download online subtitle failed", exception)
            tempFile.delete()
            throw OnlineSubtitleDownloadFailedException(exception)
        }

        Logger.debug(TAG, "Download online subtitle cached: file=${targetFile.name}, bytes=${targetFile.length()}")
        DownloadedOnlineSubtitle(file = targetFile)
    }

    // 复制时检查大小，避免超限字幕占满缓存。
    private fun copyCapped(
        inputStream: InputStream,
        outputStream: OutputStream,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val readCount = inputStream.read(buffer)
            if (readCount == -1) break

            totalBytes += readCount
            if (totalBytes > MAX_SUBTITLE_BYTES) throw OnlineSubtitleTooLargeException()
            outputStream.write(buffer, 0, readCount)
        }
        if (totalBytes == 0L) throw EmptyOnlineSubtitleException()
    }

    // 内容按哈希去重，展示信息独立保存，不把内部文件名暴露给用户。
    suspend fun importSubtitle(
        bytes: ByteArray,
        extension: String,
        result: OnlineSubtitleResult,
    ): DownloadedOnlineSubtitle = withContext(ioDispatcher) {
        subtitleDirectory.mkdirs()
        val baseName = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val targetFile = File(subtitleDirectory, "$baseName.$extension")
        val tempFile = File.createTempFile(baseName, ".$extension.part", subtitleDirectory)
        Logger.debug(TAG, "Import online subtitle start: extension=$extension, bytes=${bytes.size}")

        try {
            tempFile.writeBytes(bytes)
            promoteTempFile(tempFile, targetFile)
            saveMetadata(
                targetFile,
                SavedOnlineSubtitle(
                    title = result.title,
                    languageCode = OnlineSubtitleLanguage.normalize(result.languageCode),
                    provider = result.provider,
                    providerId = result.providerId,
                ),
            )
        } catch (exception: IOException) {
            Logger.error(TAG, "Import online subtitle failed", exception)
            tempFile.delete()
            throw OnlineSubtitleDownloadFailedException(exception)
        }

        Logger.debug(TAG, "Import online subtitle cached: file=${targetFile.name}")
        DownloadedOnlineSubtitle(file = targetFile)
    }

    private fun promoteTempFile(
        tempFile: File,
        targetFile: File,
    ) {
        if (!tempFile.renameTo(targetFile)) {
            throw IOException("Unable to move subtitle file")
        }
    }

    suspend fun migrateCachedSubtitles() = withContext(ioDispatcher) {
        migrationMutex.withLock {
            legacyDirectory.listFiles().orEmpty()
                .filter { it.isFile && it.extension in SUPPORTED_EXTENSIONS }
                .forEach { migrateFile(it) }
        }
    }

    suspend fun resolveSubtitle(uri: Uri): Uri = withContext(ioDispatcher) {
        if (uri.scheme != "file") return@withContext uri
        val file = uri.path?.let(::File) ?: return@withContext uri
        if (file.parentFile?.canonicalFile != legacyDirectory.canonicalFile) return@withContext uri
        migrationMutex.withLock { Uri.fromFile(migrateFile(file)) }
    }

    private fun migrateFile(file: File): File {
        val target = File(subtitleDirectory, file.name)
        if (target.isFile) return target
        if (!file.isFile) return file
        var temporary: File? = null
        return try {
            subtitleDirectory.mkdirs()
            temporary = File.createTempFile("migration", ".part", subtitleDirectory)
            file.copyTo(temporary, overwrite = true)
            promoteTempFile(temporary, target)
            target
        } catch (exception: IOException) {
            Logger.error(TAG, "Failed to migrate saved subtitle", exception)
            temporary?.delete()
            file
        }
    }

    suspend fun getMetadata(uri: Uri): SavedOnlineSubtitle? = withContext(ioDispatcher) {
        if (uri.scheme != "file") return@withContext null
        val file = uri.path?.let(::File) ?: return@withContext null
        if (file.parentFile?.canonicalFile != subtitleDirectory.canonicalFile) return@withContext null
        val metadata = AtomicFile(File(subtitleDirectory, "${file.name}.json"))
        if (!metadata.baseFile.isFile) return@withContext null
        try {
            val json = JSONObject(metadata.readFully().toString(Charsets.UTF_8))
            SavedOnlineSubtitle(
                title = json.getString("title"),
                languageCode = json.optString("languageCode"),
                provider = json.optString("provider").takeIf { it.isNotEmpty() }?.let(OnlineSubtitleProvider::valueOf),
                providerId = json.optString("providerId"),
            )
        } catch (exception: Exception) {
            Logger.error(TAG, "Failed to read saved subtitle metadata", exception)
            null
        }
    }

    private fun saveMetadata(
        file: File,
        subtitle: SavedOnlineSubtitle,
    ) {
        val json = JSONObject()
            .put("title", subtitle.title)
            .put("languageCode", subtitle.languageCode)
            .put("provider", subtitle.provider?.name.orEmpty())
            .put("providerId", subtitle.providerId)
        val metadata = AtomicFile(File(subtitleDirectory, "${file.name}.json"))
        val output = metadata.startWrite()
        try {
            output.write(json.toString().toByteArray(Charsets.UTF_8))
            metadata.finishWrite(output)
        } catch (exception: IOException) {
            metadata.failWrite(output)
            throw exception
        }
    }

    private data class ParsedSubtitleUrl(
        val extension: String,
    ) {
        companion object {
            fun from(url: String): ParsedSubtitleUrl {
                val uri = runCatching { URI(url.trim()) }
                    .getOrElse { throw InvalidOnlineSubtitleUrlException() }
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme !in SUPPORTED_SCHEMES) {
                    throw InvalidOnlineSubtitleSchemeException(scheme)
                }
                if (uri.host.isNullOrBlank()) {
                    throw InvalidOnlineSubtitleUrlException()
                }

                val extension = uri.path.orEmpty().substringAfterLast('.', "").lowercase()
                if (extension !in SUPPORTED_EXTENSIONS) {
                    throw InvalidOnlineSubtitleExtensionException(extension)
                }

                return ParsedSubtitleUrl(extension = extension)
            }
        }
    }

    private companion object {
        const val TAG = "OnlineSubtitleRepository"
        const val ONLINE_SUBTITLE_DIR_NAME = "online_subtitles"
        const val MAX_SUBTITLE_BYTES = 10L * 1024 * 1024
        val SUPPORTED_SCHEMES = setOf("http", "https")
        val SUPPORTED_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "webvtt", "smi", "sami")

        fun downloadWithOkHttp(url: String): DownloadStream {
            val request = Request.Builder()
                .url(url)
                .build()
            val response = OkHttpClient.Builder().build().newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                throw IOException("Subtitle download failed with code ${response.code}")
            }

            val body = response.body
            if (body == null) {
                response.close()
                throw IOException("Subtitle response body is empty")
            }

            return DownloadStream(ResponseInputStream(body.byteStream(), response::close))
        }
    }
}

data class SavedOnlineSubtitle(
    val title: String,
    val languageCode: String = "",
    val provider: OnlineSubtitleProvider? = null,
    val providerId: String = "",
) {

    // 与 OnlineSubtitleResult.key 同构，用于把已保存字幕对回搜索结果；URL 添加的字幕没有来源编号
    val searchResultKey: String?
        get() = provider?.takeIf { providerId.isNotEmpty() }?.let { "${it.name}:$providerId" }
}

data class DownloadedOnlineSubtitle(
    val file: File,
) {
    val uriString: String = file.toURI().toString()
    val uri: Uri get() = Uri.parse(uriString)
}

class DownloadStream(
    val inputStream: InputStream,
)

open class OnlineSubtitleException(message: String) : IllegalStateException(message)

open class InvalidOnlineSubtitleException(message: String) : IllegalArgumentException(message)

class InvalidOnlineSubtitleSchemeException(
    val scheme: String,
) : InvalidOnlineSubtitleException("Unsupported subtitle scheme: $scheme")

class InvalidOnlineSubtitleUrlException : InvalidOnlineSubtitleException("Unsupported subtitle URL")

class InvalidOnlineSubtitleExtensionException(
    val extension: String,
) : InvalidOnlineSubtitleException("Unsupported subtitle extension: $extension")

class EmptyOnlineSubtitleException : OnlineSubtitleException("Online subtitle is empty")

class OnlineSubtitleTooLargeException : OnlineSubtitleException("Online subtitle exceeds 10 MB")

class OnlineSubtitleDownloadFailedException(
    cause: IOException,
) : IOException("Online subtitle download failed", cause)

private class ResponseInputStream(
    inputStream: InputStream,
    private val onClose: () -> Unit,
) : FilterInputStream(inputStream) {

    override fun close() {
        try {
            super.close()
        } finally {
            // 关闭响应，避免真实下载场景泄漏连接。
            onClose()
        }
    }
}
