package one.only.player.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import one.only.player.core.common.Logger
import one.only.player.core.ui.R

internal const val CRASH_EXCEPTION_EXTRA = "exception"
internal const val CRASH_REPORT_SAVED_EXTRA = "report_saved"
internal const val CRASH_PROCESS_SUFFIX = ":crash"

internal enum class StartupStage {
    APPLICATION,
    MAIN_ACTIVITY,
}

internal object StartupRecovery {

    private const val TAG = "StartupRecovery"
    private const val STARTUP_TIMEOUT_MILLIS = 15_000L
    private const val MAX_EXCEPTION_EXTRA_LENGTH = 8 * 1024

    private val lock = Any()
    private val watchdogExecutor = ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, "only-player-startup-watchdog").apply {
            isDaemon = true
        }
    }.apply {
        removeOnCancelPolicy = true
    }

    private var activeStartup: Startup? = null
    private var watchdog: ScheduledFuture<*>? = null
    private var isCrashPageRequested = false

    fun begin(
        context: Context,
        stage: StartupStage,
    ) {
        synchronized(lock) {
            if (activeStartup != null || isCrashPageRequested) return
            val startup = Startup(stage)
            activeStartup = startup
            watchdog = watchdogExecutor.schedule(
                { onStartupTimeout(context, startup) },
                STARTUP_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
            )
        }
    }

    fun markReady() {
        synchronized(lock) {
            cancelWatchdog()
        }
    }

    fun launchCrashPage(
        context: Context,
        exception: String,
    ) {
        val shouldLaunch = synchronized(lock) {
            claimCrashPage()
        }
        if (!shouldLaunch) return
        openCrashPage(context, exception, shouldIncludeThreads = false)
    }

    private fun openCrashPage(
        context: Context,
        exception: String,
        shouldIncludeThreads: Boolean,
    ) {
        // 报告独立保存，避免等待可能被卡死线程占用的日志锁。
        val isReportSaved = CrashReportStore(context).writeSnapshot(exception, shouldIncludeThreads)
        val intent = Intent(context, CrashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(CRASH_EXCEPTION_EXTRA, Logger.sanitize(exception).take(MAX_EXCEPTION_EXTRA_LENGTH))
            putExtra(CRASH_REPORT_SAVED_EXTRA, isReportSaved)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure { launchException ->
            Logger.error(TAG, "Failed to launch crash page", launchException, shouldWriteToFile = false)
        }
    }

    private fun onStartupTimeout(
        context: Context,
        startup: Startup,
    ) {
        val shouldHandleTimeout = synchronized(lock) {
            activeStartup === startup && claimCrashPage()
        }

        if (!shouldHandleTimeout) return
        try {
            openCrashPage(
                context = context,
                exception = context.getString(
                    R.string.crash_screen_startup_timeout,
                    STARTUP_TIMEOUT_MILLIS / 1_000L,
                ) + "\nStartup stage: ${startup.stage}",
                shouldIncludeThreads = true,
            )
        } finally {
            Process.killProcess(Process.myPid())
        }
    }

    private fun claimCrashPage(): Boolean {
        if (isCrashPageRequested) return false
        isCrashPageRequested = true
        cancelWatchdog()
        return true
    }

    private fun cancelWatchdog() {
        activeStartup = null
        watchdog?.cancel(false)
        watchdog = null
    }

    private class Startup(val stage: StartupStage)
}
