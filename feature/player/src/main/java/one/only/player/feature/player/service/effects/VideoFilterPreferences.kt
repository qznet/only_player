package one.only.player.feature.player.service.effects

import one.only.player.core.model.PlayerPreferences

data class VideoFilterPreferences(
    val shouldApply: Boolean,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val hue: Float,
    val gamma: Float,
    val sharpening: Float,
) {
    fun interpolateTo(
        target: VideoFilterPreferences,
        fraction: Float,
    ): VideoFilterPreferences {
        if (fraction >= 1f) return target

        return VideoFilterPreferences(
            shouldApply = shouldApply || target.shouldApply,
            brightness = brightness.interpolate(target.brightness, fraction),
            contrast = contrast.interpolate(target.contrast, fraction),
            saturation = saturation.interpolate(target.saturation, fraction),
            hue = hue.interpolate(target.hue, fraction),
            gamma = gamma.interpolate(target.gamma, fraction),
            sharpening = sharpening.interpolate(target.sharpening, fraction),
        )
    }

    fun shouldCreateEffect(): Boolean = shouldApply

    companion object {
        fun default(): VideoFilterPreferences = VideoFilterPreferences(
            shouldApply = false,
            brightness = PlayerPreferences.DEFAULT_VIDEO_BRIGHTNESS,
            contrast = PlayerPreferences.DEFAULT_VIDEO_CONTRAST,
            saturation = PlayerPreferences.DEFAULT_VIDEO_SATURATION,
            hue = PlayerPreferences.DEFAULT_VIDEO_HUE,
            gamma = PlayerPreferences.DEFAULT_VIDEO_GAMMA,
            sharpening = PlayerPreferences.DEFAULT_VIDEO_SHARPENING,
        )
    }
}

internal fun Float.interpolate(
    target: Float,
    fraction: Float,
): Float = this + (target - this) * fraction
