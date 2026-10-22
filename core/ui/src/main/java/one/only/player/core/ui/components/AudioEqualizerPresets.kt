package one.only.player.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.only.player.core.model.AudioEqualizerBuiltInPreset
import one.only.player.core.model.AudioEqualizerPreset
import one.only.player.core.model.PlayerPreferences
import one.only.player.core.model.isAudioEqualizerPresetSelected
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AudioEqualizerPresetListContent(
    preferences: PlayerPreferences,
    onApplyBuiltInPreset: (AudioEqualizerBuiltInPreset) -> Unit,
    onApplyPreset: (AudioEqualizerPreset) -> Unit,
    onDeletePreset: (AudioEqualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 列表本身不滚动，由外层容器或 LazyColumn 提供
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PresetSectionTitle(
            text = stringResource(R.string.audio_equalizer_built_in_presets),
            testTag = "text_audio_equalizer_built_in_presets",
        )
        AudioEqualizerBuiltInPreset.entries.forEach { preset ->
            RadioTextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("option_equalizer_built_in_${preset.name.lowercase()}"),
                text = stringResource(preset.labelRes()),
                isSelected = preferences.isAudioEqualizerPresetSelected(preset),
                onClick = { onApplyBuiltInPreset(preset) },
            )
        }

        PresetSectionTitle(
            text = stringResource(R.string.audio_equalizer_custom_presets),
            testTag = "text_audio_equalizer_presets",
            modifier = Modifier.padding(top = 8.dp),
        )
        if (preferences.audioEqualizerPresets.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_audio_equalizer_presets_empty"),
                text = stringResource(R.string.audio_equalizer_presets_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        preferences.audioEqualizerPresets.forEach { preset ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioTextButton(
                    text = preset.name,
                    isSelected = preferences.isAudioEqualizerPresetSelected(preset),
                    onClick = { onApplyPreset(preset) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_equalizer_preset_${preset.id}"),
                )
                IconButton(
                    modifier = Modifier.testTag("btn_delete_equalizer_preset_${preset.id}"),
                    onClick = { onDeletePreset(preset) },
                ) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = stringResource(R.string.delete_audio_equalizer_preset),
                    )
                }
            }
        }
    }
}

@Composable
fun AudioEqualizerPresetPickerDialog(
    preferences: PlayerPreferences,
    onApplyBuiltInPreset: (AudioEqualizerBuiltInPreset) -> Unit,
    onApplyPreset: (AudioEqualizerPreset) -> Unit,
    onDeletePreset: (AudioEqualizerPreset) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionsDialog(
        modifier = modifier.testTag("dialog_audio_equalizer_presets"),
        title = stringResource(R.string.audio_equalizer_presets),
        onDismissClick = onDismissRequest,
    ) {
        item {
            AudioEqualizerPresetListContent(
                preferences = preferences,
                onApplyBuiltInPreset = onApplyBuiltInPreset,
                onApplyPreset = onApplyPreset,
                onDeletePreset = onDeletePreset,
            )
        }
    }
}

@Composable
private fun PresetSectionTitle(
    text: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
internal fun PlayerPreferences.audioEqualizerPresetLabel(): String {
    val levels = audioEqualizerBandLevels
    val builtInPreset = AudioEqualizerBuiltInPreset.entries.firstOrNull { it.bandLevels == levels }
    if (builtInPreset != null) return stringResource(builtInPreset.labelRes())

    return audioEqualizerPresets.firstOrNull { it.bandLevels == levels }?.name
        ?: stringResource(R.string.audio_equalizer_custom)
}

@StringRes
private fun AudioEqualizerBuiltInPreset.labelRes(): Int = when (this) {
    AudioEqualizerBuiltInPreset.FLAT -> R.string.equalizer_preset_flat
    AudioEqualizerBuiltInPreset.VOCAL -> R.string.equalizer_preset_vocal
    AudioEqualizerBuiltInPreset.BASS_BOOST -> R.string.equalizer_preset_bass_boost
}
