package one.only.player.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.only.player.core.model.PlayerPreferences
import one.only.player.core.model.VideoFilterPreset
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun VideoFilterPresetListContent(
    preferences: PlayerPreferences,
    onApplyPreset: (VideoFilterPreset) -> Unit,
    onDeletePreset: (VideoFilterPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 列表本身不滚动，由外层容器或 LazyColumn 提供
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (preferences.videoFilterPresets.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_video_filter_presets_empty"),
                text = stringResource(R.string.video_filter_presets_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        preferences.videoFilterPresets.forEach { preset ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioTextButton(
                    text = preset.name,
                    isSelected = preferences.matchesVideoFilterPreset(preset),
                    onClick = { onApplyPreset(preset) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_filter_preset_${preset.id}"),
                )
                IconButton(
                    modifier = Modifier.testTag("btn_delete_filter_preset_${preset.id}"),
                    onClick = { onDeletePreset(preset) },
                ) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = stringResource(R.string.delete_video_filter_preset),
                    )
                }
            }
        }
    }
}

@Composable
fun VideoFilterPresetPickerDialog(
    preferences: PlayerPreferences,
    onApplyPreset: (VideoFilterPreset) -> Unit,
    onDeletePreset: (VideoFilterPreset) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionsDialog(
        modifier = modifier.testTag("dialog_video_filter_presets"),
        title = stringResource(R.string.video_filter_presets),
        onDismissClick = onDismissRequest,
    ) {
        item {
            VideoFilterPresetListContent(
                preferences = preferences,
                onApplyPreset = onApplyPreset,
                onDeletePreset = onDeletePreset,
            )
        }
    }
}

private fun PlayerPreferences.matchesVideoFilterPreset(preset: VideoFilterPreset): Boolean = videoBrightness == preset.brightness &&
    videoContrast == preset.contrast &&
    videoSaturation == preset.saturation &&
    videoHue == preset.hue &&
    videoGamma == preset.gamma &&
    videoSharpening == preset.sharpening
