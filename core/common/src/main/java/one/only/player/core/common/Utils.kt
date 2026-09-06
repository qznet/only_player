package one.only.player.core.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

val storagePermission = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_VIDEO
    else -> Manifest.permission.READ_EXTERNAL_STORAGE
}

const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

@ChecksSdkIntAtLeast(api = 37)
fun needsLocalNetworkPermission(): Boolean = Build.VERSION.SDK_INT >= 37

fun hasLocalNetworkPermission(context: Context): Boolean {
    if (!needsLocalNetworkPermission()) return true
    return ContextCompat.checkSelfPermission(context, LOCAL_NETWORK_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED
}

// API < 30 无 MANAGE_EXTERNAL_STORAGE 概念，传统存储下读写权限即全量访问，
// 必须返回 true，否则 UI 会误显示"授权所有文件"入口，点击后在低版本直接崩
fun hasManageExternalStorageAccess(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

fun createManageExternalStorageAccessIntent(context: Context): Intent {
    // API < 30 没有"所有文件访问"设置页，回退到应用详情页（始终可解析，不会 ActivityNotFoundException）
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    val manageAppIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return manageAppIntent.takeIf {
        it.resolveActivity(context.packageManager) != null
    } ?: Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

object Utils {

    fun pxToDp(px: Float): Float = px / Resources.getSystem().displayMetrics.density

    fun formatDurationMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(minutes) -
            TimeUnit.HOURS.toSeconds(hours)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationMillisSign(millis: Long): String = if (millis >= 0) {
        "+${formatDurationMillis(millis)}"
    } else {
        "-${formatDurationMillis(abs(millis))}"
    }

    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size < kb -> "$size B"
            size < mb -> "%.2f KB".format(size / kb.toDouble())
            size < gb -> "%.2f MB".format(size / mb.toDouble())
            else -> "%.2f GB".format(size / gb.toDouble())
        }
    }

    fun formatBitrate(bitrate: Long): String? {
        if (bitrate <= 0) {
            return null
        }

        val kiloBitrate = bitrate.toDouble() / 1000.0
        val megaBitrate = kiloBitrate / 1000.0
        val gigaBitrate = megaBitrate / 1000.0

        return when {
            gigaBitrate >= 1.0 -> String.format("%.1f Gbps", gigaBitrate)
            megaBitrate >= 1.0 -> String.format("%.1f Mbps", megaBitrate)
            kiloBitrate >= 1.0 -> String.format("%.1f kbps", kiloBitrate)
            else -> String.format("%d bps", bitrate)
        }
    }

    fun formatFrameRate(frameRate: Double): String = String.format(Locale.US, "%.3f fps", frameRate)
        .replace(Regex("0+ fps$"), " fps")
        .replace(". fps", " fps")

    fun formatLanguage(language: String?): String? = language?.let { lang -> Locale.forLanguageTag(lang).displayLanguage.takeIf { it.isNotEmpty() } }
}
