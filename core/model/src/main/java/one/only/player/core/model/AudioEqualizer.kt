package one.only.player.core.model

import kotlinx.serialization.Serializable

// 均衡器固定 10 段，中心频率是 UI 与 DSP 共用的唯一来源
enum class AudioEqualizerBand(
    val centerFrequencyHz: Int,
) {
    HZ_31(31),
    HZ_62(62),
    HZ_125(125),
    HZ_250(250),
    HZ_500(500),
    HZ_1000(1000),
    HZ_2000(2000),
    HZ_4000(4000),
    HZ_8000(8000),
    HZ_16000(16000),
}

// 内置预设只提供曲线，名称留给 UI 层本地化
enum class AudioEqualizerBuiltInPreset(
    val bandLevels: List<Int>,
) {
    FLAT(listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    VOCAL(listOf(-2, -1, 0, 2, 4, 4, 3, 1, 0, 0)),
    BASS_BOOST(listOf(6, 5, 4, 2, 0, 0, 0, 0, 0, 0)),
}

@Serializable
data class AudioEqualizerPreset(
    val id: Long,
    val name: String,
    val bandLevels: List<Int>,
)
