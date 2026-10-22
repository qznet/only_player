package one.only.player.feature.player.service.audio

import one.only.player.core.model.PlayerPreferences

// 传给音频处理器的均衡器参数快照，跨线程传递时按值比较
internal data class AudioEqualizerSettings(
    val isEnabled: Boolean = false,
    val bandLevelsDb: List<Int> = PlayerPreferences.DEFAULT_AUDIO_EQUALIZER_BAND_LEVELS,
) {

    // 全零曲线等价于直通，跳过滤波省下 CPU
    val shouldBypass: Boolean
        get() = !isEnabled || bandLevelsDb.all { it == PlayerPreferences.DEFAULT_AUDIO_EQUALIZER_GAIN_DB }
}

internal fun PlayerPreferences.toAudioEqualizerSettings(): AudioEqualizerSettings = AudioEqualizerSettings(
    isEnabled = shouldApplyAudioEqualizer,
    bandLevelsDb = audioEqualizerBandLevels,
)
