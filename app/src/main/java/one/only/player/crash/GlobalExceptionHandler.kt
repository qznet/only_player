package one.only.player.crash

import android.content.Context
import kotlin.system.exitProcess

class GlobalExceptionHandler(
    private val context: Context,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            StartupRecovery.launchCrashPage(
                context = context,
                exception = "Uncaught exception on ${t.name}\n${e.stackTraceToString()}",
            )
        } finally {
            exitProcess(0)
        }
    }
}
