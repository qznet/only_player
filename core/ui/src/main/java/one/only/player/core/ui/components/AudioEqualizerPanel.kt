package one.only.player.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import one.only.player.core.model.AudioEqualizerBand
import one.only.player.core.model.PlayerPreferences
import one.only.player.core.model.equalizerBandLevel
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 十段增益都按整数分贝保存，滑杆刻度与存储值一一对应
private val AUDIO_EQUALIZER_GAIN_INT_RANGE =
    PlayerPreferences.MIN_AUDIO_EQUALIZER_GAIN_DB..PlayerPreferences.MAX_AUDIO_EQUALIZER_GAIN_DB
private val AUDIO_EQUALIZER_GAIN_RANGE =
    AUDIO_EQUALIZER_GAIN_INT_RANGE.first.toFloat()..AUDIO_EQUALIZER_GAIN_INT_RANGE.last.toFloat()

@Composable
fun AudioEqualizerPanel(
    preferences: PlayerPreferences,
    onEnabledChange: (Boolean) -> Unit,
    onBandLevelChange: (AudioEqualizerBand, Int) -> Unit,
    onShowPresets: () -> Unit,
    onSavePreset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        AudioEqualizerControls(
            preferences = preferences,
            onEnabledChange = onEnabledChange,
            onBandLevelChange = onBandLevelChange,
            onShowPresets = onShowPresets,
            onSavePreset = onSavePreset,
        )
    }
}

@Composable
fun AudioEqualizerControls(
    preferences: PlayerPreferences,
    onEnabledChange: (Boolean) -> Unit,
    onBandLevelChange: (AudioEqualizerBand, Int) -> Unit,
    onShowPresets: () -> Unit,
    onSavePreset: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "audio_equalizer",
) {
    var isAdvancedExpanded by rememberSaveable { mutableStateOf(false) }

    PreferenceGroup(modifier = modifier) {
        PreferenceSwitch(
            modifier = Modifier.testTag("switch_$testTagPrefix"),
            title = stringResource(R.string.enable_audio_equalizer),
            description = stringResource(R.string.enable_audio_equalizer_description),
            icon = AppIcons.Equalizer,
            isChecked = preferences.shouldApplyAudioEqualizer,
            onClick = { onEnabledChange(!preferences.shouldApplyAudioEqualizer) },
        )
        ClickablePreferenceItem(
            modifier = Modifier.testTag("item_${testTagPrefix}_presets"),
            title = stringResource(R.string.audio_equalizer_presets),
            description = preferences.audioEqualizerPresetLabel(),
            icon = AppIcons.Audio,
            onClick = onShowPresets,
        )
        PreferenceItem(
            modifier = Modifier.testTag("item_${testTagPrefix}_advanced"),
            title = stringResource(
                if (isAdvancedExpanded) R.string.collapse_audio_equalizer_advanced else R.string.audio_equalizer_advanced,
            ),
            description = stringResource(R.string.audio_equalizer_advanced_description),
            icon = AppIcons.Equalizer,
            isEnabled = true,
            onClick = { isAdvancedExpanded = !isAdvancedExpanded },
            trailingContent = {
                Icon(
                    modifier = Modifier.rotate(if (isAdvancedExpanded) 180f else 0f),
                    imageVector = AppIcons.ExpandMore,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            },
        )
        if (isAdvancedExpanded) {
            ClickablePreferenceItem(
                modifier = Modifier.testTag("item_save_${testTagPrefix}_preset"),
                title = stringResource(R.string.save_current_as_audio_equalizer_preset),
                icon = AppIcons.Save,
                onClick = onSavePreset,
            )
            AudioEqualizerBandSliders(
                preferences = preferences,
                onBandLevelChange = onBandLevelChange,
                sliderTestTagPrefix = "slider_${testTagPrefix}_band",
                resetTestTagPrefix = "btn_reset_${testTagPrefix}_band",
            )
        }
    }
}

@Composable
private fun AudioEqualizerBandSliders(
    preferences: PlayerPreferences,
    onBandLevelChange: (AudioEqualizerBand, Int) -> Unit,
    sliderTestTagPrefix: String,
    resetTestTagPrefix: String,
) {
    AudioEqualizerBand.entries.groupBy { it.rangeLabelRes() }.forEach { (labelRes, bands) ->
        Text(
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        bands.forEach { band ->
            AudioEqualizerBandSlider(
                band = band,
                levelDb = preferences.equalizerBandLevel(band),
                isEnabled = preferences.shouldApplyAudioEqualizer,
                onCommit = { onBandLevelChange(band, it) },
                sliderTestTag = "${sliderTestTagPrefix}_${band.ordinal}",
                resetTestTag = "${resetTestTagPrefix}_${band.ordinal}",
            )
        }
    }
}

@StringRes
private fun AudioEqualizerBand.rangeLabelRes(): Int = when (this) {
    AudioEqualizerBand.HZ_31,
    AudioEqualizerBand.HZ_62,
    AudioEqualizerBand.HZ_125,
    AudioEqualizerBand.HZ_250,
    -> R.string.audio_equalizer_bass
    AudioEqualizerBand.HZ_500,
    AudioEqualizerBand.HZ_1000,
    AudioEqualizerBand.HZ_2000,
    -> R.string.audio_equalizer_midrange
    AudioEqualizerBand.HZ_4000,
    AudioEqualizerBand.HZ_8000,
    AudioEqualizerBand.HZ_16000,
    -> R.string.audio_equalizer_treble
}

@Composable
private fun AudioEqualizerBandSlider(
    band: AudioEqualizerBand,
    levelDb: Int,
    isEnabled: Boolean,
    onCommit: (Int) -> Unit,
    sliderTestTag: String,
    resetTestTag: String,
) {
    // 只保留当前频段的拖动草稿，其他偏好更新不打断手势
    var draftLevel by remember(levelDb, isEnabled) { mutableIntStateOf(levelDb) }
    val frequencyLabel = band.frequencyLabel()
    PreferenceSlider(
        modifier = Modifier.testTag(sliderTestTag),
        title = frequencyLabel,
        description = stringResource(R.string.decibel_value, signedDecibels(draftLevel)),
        isEnabled = isEnabled,
        value = draftLevel.toFloat(),
        valueRange = AUDIO_EQUALIZER_GAIN_RANGE,
        steps = AUDIO_EQUALIZER_GAIN_INT_RANGE.sliderStepCount(),
        onValueChange = { draftLevel = it.roundToInt() },
        onValueChangeFinished = { onCommit(draftLevel) },
        trailingContent = {
            ResetIconButton(
                modifier = Modifier.testTag(resetTestTag),
                enabled = isEnabled,
                onClick = {
                    draftLevel = PlayerPreferences.DEFAULT_AUDIO_EQUALIZER_GAIN_DB
                    onCommit(draftLevel)
                },
                contentDescription = stringResource(R.string.reset_audio_equalizer_band, frequencyLabel),
            )
        },
    )
}

// 频段标题与重置说明共用同一份中心频率文案
@Composable
private fun AudioEqualizerBand.frequencyLabel(): String = if (centerFrequencyHz >= KILOHERTZ_THRESHOLD_HZ) {
    stringResource(R.string.frequency_khz, centerFrequencyHz / KILOHERTZ_THRESHOLD_HZ)
} else {
    stringResource(R.string.frequency_hz, centerFrequencyHz)
}

// 面板与设置页共用的增益文案，正值显式带加号
private fun signedDecibels(valueDb: Int): String = if (valueDb > 0) "+$valueDb" else "$valueDb"

private const val KILOHERTZ_THRESHOLD_HZ = 1000
