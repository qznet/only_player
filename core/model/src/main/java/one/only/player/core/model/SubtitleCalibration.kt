package one.only.player.core.model

// 单条字幕的校准值；delayMilliseconds 为 0 且 speed 为 1 表示未校准
data class SubtitleCalibration(
    val delayMilliseconds: Long,
    val speed: Float,
) {
    companion object {
        const val DEFAULT_SPEED = 1f

        val Default = SubtitleCalibration(delayMilliseconds = 0L, speed = DEFAULT_SPEED)
    }
}
