package one.only.player.feature.player.service.audio

import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import one.only.player.core.common.Logger
import one.only.player.core.model.PlayerPreferences

@OptIn(UnstableApi::class)
internal class AudioEffectsCoordinator {

    private var volumeNormalizationAudioProcessor: VolumeNormalizationAudioProcessor? = null
    private var equalizerAudioProcessor: EqualizerAudioProcessor? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null
    var requestedVolumeGain: Int = 0
        private set

    val isLoudnessGainSupported: Boolean
        get() = loudnessEnhancer != null

    fun createAudioProcessors(preferences: PlayerPreferences): Array<AudioProcessor> {
        // 切换解码器时新旧播放器短暂共存，处理器状态不能共用
        val normalization = VolumeNormalizationAudioProcessor().apply {
            isEnabled = preferences.isVolumeNormalizationEnabled
        }
        val equalizer = EqualizerAudioProcessor().apply {
            applySettings(preferences.toAudioEqualizerSettings())
        }
        volumeNormalizationAudioProcessor = normalization
        equalizerAudioProcessor = equalizer
        return arrayOf(normalization, equalizer)
    }

    fun setEnhancerTargetGain(
        gain: Int,
        preferences: PlayerPreferences,
        audioSessionId: Int,
    ) {
        requestedVolumeGain = gain.coerceAtLeast(0)
        if (loudnessEnhancer == null && preferences.isVolumeBoostEnabled) {
            initializeLoudnessEnhancer(
                audioSessionId = audioSessionId,
                preferences = preferences,
            )
        }
        applyLoudnessEnhancerGain()
    }

    fun initializeLoudnessEnhancer(
        audioSessionId: Int,
        preferences: PlayerPreferences,
    ) {
        if (!preferences.isVolumeBoostEnabled) return
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            releaseLoudnessEnhancer()
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            Logger.debug(TAG, "Loudness enhancer initialized: boost=true")
            applyLoudnessEnhancerGain()
        } catch (exception: Exception) {
            Logger.error(TAG, "Failed to initialize loudness enhancer", exception)
            loudnessEnhancer = null
        }
    }

    fun releaseLoudnessEnhancer() {
        val enhancer = loudnessEnhancer ?: return
        try {
            enhancer.enabled = false
        } catch (exception: Exception) {
            Logger.error(TAG, "Failed to disable loudness enhancer", exception)
        }
        try {
            enhancer.release()
        } catch (exception: Exception) {
            Logger.error(TAG, "Failed to release loudness enhancer", exception)
        } finally {
            loudnessEnhancer = null
        }
    }

    fun applyVolumeNormalization(isEnabled: Boolean) {
        volumeNormalizationAudioProcessor?.isEnabled = isEnabled
        Logger.debug(TAG, "Apply volume normalization: enabled=$isEnabled")
    }

    fun applyEqualizer(settings: AudioEqualizerSettings) {
        equalizerAudioProcessor?.applySettings(settings)
        Logger.debug(TAG, "Apply audio equalizer: enabled=${settings.isEnabled} levels=${settings.bandLevelsDb}")
    }

    private fun applyLoudnessEnhancerGain() {
        val enhancer = loudnessEnhancer ?: return
        val gain = requestedVolumeGain

        try {
            enhancer.setTargetGain(gain)
            enhancer.enabled = gain > 0
            Logger.debug(TAG, "Apply loudness gain: requested=$requestedVolumeGain, enabled=${gain > 0}")
        } catch (exception: Exception) {
            Logger.error(TAG, "Failed to apply loudness enhancer gain", exception)
        }
    }

    private companion object {
        private const val TAG = "AudioEffectsCoordinator"
    }
}
