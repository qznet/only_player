package one.only.player.crash

import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.Process
import java.io.File
import java.time.Instant
import one.only.player.BuildConfig
import one.only.player.core.common.FileLogStore
import one.only.player.core.common.Logger

internal class CrashReportStore(private val context: Context) {

    fun writeSnapshot(
        exception: String,
        shouldIncludeThreads: Boolean,
    ): Boolean = runCatching {
        val report = buildString {
            appendLine(exception)
            appendLine()
            appendLine("Time: ${Instant.now()}")
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Process: ${Process.myPid()}")
            if (shouldIncludeThreads) appendThreadDump()
        }
        snapshotFile().writeText(Logger.sanitize(report))
    }.onFailure { exception ->
        Logger.error(TAG, "Failed to save crash snapshot", exception, shouldWriteToFile = false)
    }.isSuccess

    fun readReport(
        isSnapshotSaved: Boolean,
        exception: String,
    ): String {
        val snapshot = if (isSnapshotSaved) {
            runCatching { snapshotFile().readText() }
                .getOrElse { "$exception\nSnapshot read failed: ${it.javaClass.simpleName}" }
        } else {
            exception
        }
        // 恢复进程仅阅读日志，不初始化主进程的 Logger 或业务依赖。
        val logs = runCatching { FileLogStore(context).read() }
            .getOrElse { "Log read failed: ${it.javaClass.simpleName}" }
        return Logger.sanitize("$snapshot\n\nApp logs:\n$logs")
    }

    fun exportFile(report: String): File {
        val directory = File(context.cacheDir, "crash_reports")
        directory.mkdirs()
        return File(directory, "only_player_crash.txt").apply { writeText(report) }
    }

    private fun snapshotFile() = File(context.noBackupFilesDir, "crash_report.txt")

    private fun StringBuilder.appendThreadDump() {
        val mainThread = Looper.getMainLooper().thread
        val traces = Thread.getAllStackTraces()
        appendLine()
        appendLine("Threads: ${traces.size}")
        traces.entries
            .sortedWith(compareBy({ it.key !== mainThread }, { it.key.name }))
            .forEach { (thread, trace) ->
                appendLine()
                appendLine("Thread: ${thread.name} [${thread.state}]")
                trace.forEach { appendLine("    at $it") }
            }
    }

    private companion object {
        const val TAG = "CrashReportStore"
    }
}
