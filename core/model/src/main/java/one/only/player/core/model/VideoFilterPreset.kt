package one.only.player.core.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoFilterPreset(
    val id: Long = 0L,
    val name: String,
    val brightness: Float = PlayerPreferences.DEFAULT_VIDEO_BRIGHTNESS,
    val contrast: Float = PlayerPreferences.DEFAULT_VIDEO_CONTRAST,
    val saturation: Float = PlayerPreferences.DEFAULT_VIDEO_SATURATION,
    val hue: Float = PlayerPreferences.DEFAULT_VIDEO_HUE,
    val gamma: Float = PlayerPreferences.DEFAULT_VIDEO_GAMMA,
    val sharpening: Float = PlayerPreferences.DEFAULT_VIDEO_SHARPENING,
)
