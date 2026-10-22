package one.only.player.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import one.only.player.core.model.PlayerPreferences
import one.only.player.core.model.withVideoSharpening
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons

@Composable
fun VideoFiltersPanel(
    preferences: PlayerPreferences,
    onPreferencesChange: (PlayerPreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 拖动过程中只更新本地草稿，滑动结束才提交，避免逐帧写入
    var draftPreferences by remember(preferences) { mutableStateOf(preferences) }
    val commitDraft = {
        onPreferencesChange(draftPreferences)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VideoFiltersSwitch(
            isEnabled = draftPreferences.shouldApplyVideoFilters,
            onToggle = {
                draftPreferences = draftPreferences.copy(shouldApplyVideoFilters = !draftPreferences.shouldApplyVideoFilters)
                commitDraft()
            },
        )
        PreferenceGroup {
            videoFilterSliderSpecs(
                preferences = draftPreferences,
                onAdjust = { draftPreferences = it },
                onCommit = commitDraft,
            ).forEach { spec ->
                VideoFilterSliderRow(
                    spec = spec,
                    onValueChangeFinished = commitDraft,
                )
            }
        }
    }
}

@Composable
private fun VideoFiltersSwitch(
    isEnabled: Boolean,
    onToggle: () -> Unit,
) {
    PreferenceSwitch(
        modifier = Modifier.testTag("switch_video_filters"),
        title = stringResource(R.string.enable_video_filters),
        description = stringResource(R.string.enable_video_filters_description),
        icon = AppIcons.Sensitivity,
        isChecked = isEnabled,
        onClick = onToggle,
    )
}

// UI 层用整数刻度驱动滑杆，内部存储仍为浮点，由各 spec 负责换算。
private data class VideoFilterSliderSpec(
    val title: String,
    val icon: ImageVector,
    val intValue: Int,
    val intRange: IntRange,
    val valueText: String,
    val testTag: String,
    val resetTestTag: String,
    val resetContentDescription: String,
    val isEnabled: Boolean,
    val onReset: () -> Unit,
    val onIntValueChange: (Int) -> Unit,
)

// 面板与设置页共享的整数刻度区间，设置页据此派生浮点滑杆的吸附步数
val VIDEO_BRIGHTNESS_INT_RANGE =
    (PlayerPreferences.MIN_VIDEO_BRIGHTNESS * 100).roundToInt()..(PlayerPreferences.MAX_VIDEO_BRIGHTNESS * 100).roundToInt()
val VIDEO_CONTRAST_INT_RANGE =
    (PlayerPreferences.MIN_VIDEO_CONTRAST * 100).roundToInt()..(PlayerPreferences.MAX_VIDEO_CONTRAST * 100).roundToInt()
val VIDEO_SATURATION_INT_RANGE =
    PlayerPreferences.MIN_VIDEO_SATURATION.toInt()..PlayerPreferences.MAX_VIDEO_SATURATION.toInt()
val VIDEO_HUE_INT_RANGE =
    PlayerPreferences.MIN_VIDEO_HUE.toInt()..PlayerPreferences.MAX_VIDEO_HUE.toInt()
val VIDEO_GAMMA_INT_RANGE =
    (PlayerPreferences.MIN_VIDEO_GAMMA * 100).roundToInt()..(PlayerPreferences.MAX_VIDEO_GAMMA * 100).roundToInt()
val VIDEO_SHARPENING_INT_RANGE =
    PlayerPreferences.DEFAULT_VIDEO_SHARPENING.roundToInt()..(PlayerPreferences.MAX_VIDEO_SHARPENING * 100).roundToInt()

fun IntRange.sliderStepCount(): Int = (last - first - 1).coerceAtLeast(0)

@Composable
private fun videoFilterSliderSpecs(
    preferences: PlayerPreferences,
    onAdjust: (PlayerPreferences) -> Unit,
    onCommit: () -> Unit,
): List<VideoFilterSliderSpec> = listOf(
    VideoFilterSliderSpec(
        title = stringResource(R.string.video_brightness),
        icon = AppIcons.Brightness,
        intValue = (preferences.videoBrightness * 100).roundToInt(),
        intRange = VIDEO_BRIGHTNESS_INT_RANGE,
        valueText = signedPercent((preferences.videoBrightness * 100).roundToInt()),
        testTag = "slider_video_brightness",
        resetTestTag = "btn_reset_video_brightness",
        resetContentDescription = stringResource(R.string.reset_video_brightness),
        isEnabled = preferences.shouldApplyVideoFilters,
        onReset = {
            onAdjust(preferences.copy(videoBrightness = PlayerPreferences.DEFAULT_VIDEO_BRIGHTNESS))
            onCommit()
        },
        onIntValueChange = { onAdjust(preferences.copy(videoBrightness = it / 100f)) },
    ),
    VideoFilterSliderSpec(
        title = stringResource(R.string.video_contrast),
        icon = AppIcons.Exposure,
        intValue = (preferences.videoContrast * 100).roundToInt(),
        intRange = VIDEO_CONTRAST_INT_RANGE,
        valueText = signedPercent((preferences.videoContrast * 100).roundToInt()),
        testTag = "slider_video_contrast",
        resetTestTag = "btn_reset_video_contrast",
        resetContentDescription = stringResource(R.string.reset_video_contrast),
        isEnabled = preferences.shouldApplyVideoFilters,
        onReset = {
            onAdjust(preferences.copy(videoContrast = PlayerPreferences.DEFAULT_VIDEO_CONTRAST))
            onCommit()
        },
        onIntValueChange = { onAdjust(preferences.copy(videoContrast = it / 100f)) },
    ),
    VideoFilterSliderSpec(
        title = stringResource(R.string.video_saturation),
        icon = AppIcons.Drop,
        intValue = preferences.videoSaturation.roundToInt(),
        intRange = VIDEO_SATURATION_INT_RANGE,
        valueText = signedInteger(preferences.videoSaturation.roundToInt()),
        testTag = "slider_video_saturation",
        resetTestTag = "btn_reset_video_saturation",
        resetContentDescription = stringResource(R.string.reset_video_saturation),
        isEnabled = preferences.shouldApplyVideoFilters,
        onReset = {
            onAdjust(preferences.copy(videoSaturation = PlayerPreferences.DEFAULT_VIDEO_SATURATION))
            onCommit()
        },
        onIntValueChange = { onAdjust(preferences.copy(videoSaturation = it.toFloat())) },
    ),
    VideoFilterSliderSpec(
        title = stringResource(R.string.video_hue),
        icon = AppIcons.Rainbow,
        intValue = preferences.videoHue.roundToInt(),
        intRange = VIDEO_HUE_INT_RANGE,
        valueText = stringResource(R.string.degrees, preferences.videoHue.roundToInt()),
        testTag = "slider_video_hue",
        resetTestTag = "btn_reset_video_hue",
        resetContentDescription = stringResource(R.string.reset_video_hue),
        isEnabled = preferences.shouldApplyVideoFilters,
        onReset = {
            onAdjust(preferences.copy(videoHue = PlayerPreferences.DEFAULT_VIDEO_HUE))
            onCommit()
        },
        onIntValueChange = { onAdjust(preferences.copy(videoHue = it.toFloat())) },
    ),
    VideoFilterSliderSpec(
        title = stringResource(R.string.video_gamma),
        icon = AppIcons.ChartLine,
        intValue = (preferences.videoGamma * 100).roundToInt(),
        intRange = VIDEO_GAMMA_INT_RANGE,
        valueText = stringResource(R.string.percent, (preferences.videoGamma * 100).roundToInt()),
        testTag = "slider_video_gamma",
        resetTestTag = "btn_reset_video_gamma",
        resetContentDescription = stringResource(R.string.reset_video_gamma),
        isEnabled = preferences.shouldApplyVideoFilters,
        onReset = {
            onAdjust(preferences.copy(videoGamma = PlayerPreferences.DEFAULT_VIDEO_GAMMA))
            onCommit()
        },
        onIntValueChange = { onAdjust(preferences.copy(videoGamma = it / 100f)) },
    ),
    VideoFilterSliderSpec(
        title = stringResource(R.string.video_sharpening),
        icon = AppIcons.Focus,
        intValue = (preferences.videoSharpening * 100).roundToInt(),
        intRange = VIDEO_SHARPENING_INT_RANGE,
        valueText = stringResource(R.string.percent, (preferences.videoSharpening * 100).roundToInt()),
        testTag = "slider_video_sharpening",
        resetTestTag = "btn_reset_video_sharpening",
        resetContentDescription = stringResource(R.string.reset_video_sharpening),
        isEnabled = preferences.shouldApplyVideoFilters,
        onReset = {
            onAdjust(preferences.withVideoSharpening(PlayerPreferences.DEFAULT_VIDEO_SHARPENING))
            onCommit()
        },
        onIntValueChange = { onAdjust(preferences.withVideoSharpening(it / 100f)) },
    ),
)

@Composable
private fun VideoFilterSliderRow(
    spec: VideoFilterSliderSpec,
    onValueChangeFinished: () -> Unit,
) {
    PreferenceSlider(
        modifier = Modifier.testTag(spec.testTag),
        title = spec.title,
        description = spec.valueText,
        icon = spec.icon,
        isEnabled = spec.isEnabled,
        value = spec.intValue.toFloat(),
        valueRange = spec.intRange.first.toFloat()..spec.intRange.last.toFloat(),
        steps = spec.intRange.sliderStepCount(),
        onValueChange = { raw ->
            val newValue = raw.roundToInt()
            if (newValue != spec.intValue) spec.onIntValueChange(newValue)
        },
        onValueChangeFinished = onValueChangeFinished,
        trailingContent = {
            ResetIconButton(
                modifier = Modifier.testTag(spec.resetTestTag),
                enabled = spec.isEnabled,
                onClick = spec.onReset,
                contentDescription = spec.resetContentDescription,
            )
        },
    )
}

private fun signedPercent(value: Int): String = if (value > 0) "+$value%" else "$value%"

private fun signedInteger(value: Int): String = if (value > 0) "+$value" else "$value"
