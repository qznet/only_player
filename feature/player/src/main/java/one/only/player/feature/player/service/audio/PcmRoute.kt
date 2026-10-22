package one.only.player.feature.player.service.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build

/**
 * How many channels the *decoded* path can actually carry.
 *
 * Deliberately not `AudioCapabilities.getMaxChannelCount()`: on API < 29 that asks
 * `getAudioProfiles({PCM_16BIT}, 10)` and falls back to ten when the HDMI plug broadcast never
 * arrives — which on a television, an HDMI source that never receives that broadcast, is always.
 * Ten is a number no television has, and believing it turns the fold off on exactly the device
 * that needs it.
 *
 * So the output devices are asked directly: every sink that offers PCM, widest channel count
 * wins; a present HDMI device overrides, because that is the wire the sound leaves by. A receiver
 * behind ARC that offers no PCM at all contributes nothing — correct, since it folds whatever PCM
 * reaches it. Zero means nothing could be asked, and the caller treats that as "narrow".
 */
@SuppressLint("InlinedApi")
internal object PcmRoute {

    fun channels(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 0
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0
        var widest = 0
        var hdmiPresent = false
        var hdmiPcm = 0
        for (device in audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (!device.isSink) continue
            val type = device.type
            val hdmi = type == AudioDeviceInfo.TYPE_HDMI ||
                type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_HDMI_EARC)
            if (hdmi) hdmiPresent = true
            if (!device.offersPcm()) continue
            for (channels in device.channelCounts) {
                widest = maxOf(widest, channels)
                if (hdmi) hdmiPcm = maxOf(hdmiPcm, channels)
            }
        }
        return if (hdmiPresent) hdmiPcm else widest
    }

    private fun AudioDeviceInfo.offersPcm(): Boolean = encodings.any {
        it == AudioFormat.ENCODING_PCM_16BIT || it == AudioFormat.ENCODING_PCM_FLOAT
    }
}
