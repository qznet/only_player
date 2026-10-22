package one.only.player.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlin.math.roundToLong
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.only.player.core.model.PlayerPreferences
import one.only.player.core.ui.R
import one.only.player.core.ui.components.AppDialog
import one.only.player.core.ui.components.ListSectionTitle
import one.only.player.core.ui.components.SubtitleStylePanel
import one.only.player.core.ui.designsystem.AppIcons
import one.only.player.feature.player.extensions.externalSubtitleId
import one.only.player.feature.player.extensions.getName
import one.only.player.feature.player.state.rememberSubtitleOptionsState
import one.only.player.feature.player.state.rememberTracksState
import one.only.player.feature.player.ui.panel.PanelActionButton
import one.only.player.feature.player.ui.panel.PanelOptionList
import one.only.player.feature.player.ui.panel.PanelOptionRow
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(UnstableApi::class)
@Composable
fun SubtitleSelectorContent(
    player: Player,
    onSelectSubtitleClick: () -> Unit,
    onAddOnlineSubtitleClick: (String) -> Unit,
    onRemoveSubtitleClick: (String) -> Unit,
    onShowSubtitleSearch: () -> Unit,
    preferences: PlayerPreferences,
    onPreferencesChange: (PlayerPreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    val subtitleTracksState = rememberTracksState(player, C.TRACK_TYPE_TEXT)
    val subtitleOptionsState = rememberSubtitleOptionsState(player)
    var isOnlineSubtitleDialogVisible by remember { mutableStateOf(false) }
    var onlineSubtitleUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .padding(horizontal = 16.dp),
    ) {
        PanelOptionList(modifier = Modifier.selectableGroup()) {
            subtitleTracksState.tracks.forEachIndexed { index, track ->
                val subtitleId = track.externalSubtitleId(subtitleTracksState.addedSubtitleIds)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PanelOptionRow(
                        modifier = Modifier.weight(1f),
                        isSelected = track.isSelected,
                        text = track.mediaTrackGroup.getName(C.TRACK_TYPE_TEXT, index),
                        maxTextLines = Int.MAX_VALUE,
                        testTag = "item_subtitle_$index",
                        onClick = {
                            subtitleTracksState.switchTrack(index)
                            onDismiss()
                        },
                    )
                    if (subtitleId != null && subtitleId in subtitleTracksState.addedSubtitleIds) {
                        MiuixIconButton(
                            modifier = Modifier.testTag("btn_remove_subtitle_$index"),
                            onClick = { onRemoveSubtitleClick(subtitleId) },
                        ) {
                            MiuixIcon(
                                imageVector = AppIcons.Delete,
                                contentDescription = stringResource(R.string.remove_subtitle),
                            )
                        }
                    }
                }
            }
            PanelOptionRow(
                isSelected = subtitleTracksState.tracks.none { it.isSelected },
                text = stringResource(R.string.disable),
                testTag = "item_subtitle_disable",
                onClick = {
                    subtitleTracksState.switchTrack(-1)
                    onDismiss()
                },
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        PanelActionButton(
            modifier = Modifier.testTag("btn_open_subtitle_picker"),
            text = stringResource(R.string.open_subtitle),
            onClick = {
                onSelectSubtitleClick()
                onDismiss()
            },
        )
        Spacer(modifier = Modifier.size(12.dp))
        PanelActionButton(
            modifier = Modifier.testTag("btn_add_online_subtitle"),
            text = stringResource(R.string.add_online_subtitle),
            onClick = {
                isOnlineSubtitleDialogVisible = true
            },
        )
        Spacer(modifier = Modifier.size(12.dp))
        PanelActionButton(
            modifier = Modifier.testTag("btn_search_online_subtitle"),
            text = stringResource(R.string.online_subtitle_search),
            onClick = onShowSubtitleSearch,
        )
        Spacer(modifier = Modifier.size(16.dp))
        DelayInput(
            value = subtitleOptionsState.delayMilliseconds,
            onValueChange = { subtitleOptionsState.setDelay(it) },
        )
        Spacer(modifier = Modifier.size(16.dp))
        SpeedInput(
            value = subtitleOptionsState.speedMultiplier,
            onValueChange = { subtitleOptionsState.setSpeed(it) },
        )
        if (subtitleOptionsState.isCalibrated) {
            Spacer(modifier = Modifier.size(12.dp))
            PanelActionButton(
                modifier = Modifier.testTag("btn_reset_subtitle_calibration"),
                text = stringResource(R.string.subtitle_calibration_reset),
                onClick = { subtitleOptionsState.reset() },
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        ListSectionTitle(text = stringResource(id = R.string.subtitle_appearance))
        SubtitleStylePanel(
            preferences = preferences,
            onPreferencesChange = onPreferencesChange,
        )
    }

    if (isOnlineSubtitleDialogVisible) {
        AppDialog(
            modifier = Modifier.testTag("dialog_online_subtitle"),
            onDismissRequest = { isOnlineSubtitleDialogVisible = false },
            title = stringResource(R.string.add_online_subtitle),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = onlineSubtitleUrl,
                        onValueChange = { onlineSubtitleUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_online_subtitle_url"),
                        singleLine = true,
                        label = stringResource(R.string.online_subtitle_url),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                        ),
                    )
                    MiuixText(
                        text = stringResource(R.string.online_subtitle_url_example),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            },
            confirmButton = {
                MiuixTextButton(
                    modifier = Modifier.testTag("btn_confirm_online_subtitle"),
                    enabled = onlineSubtitleUrl.isNotBlank(),
                    text = stringResource(R.string.online_subtitle_add),
                    colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        onAddOnlineSubtitleClick(onlineSubtitleUrl.trim())
                        onlineSubtitleUrl = ""
                        isOnlineSubtitleDialogVisible = false
                        onDismiss()
                    },
                )
            },
            dismissButton = {
                MiuixTextButton(
                    modifier = Modifier.testTag("btn_cancel_online_subtitle"),
                    text = stringResource(R.string.cancel),
                    onClick = { isOnlineSubtitleDialogVisible = false },
                )
            },
        )
    }
}

@Composable
private fun DelayInput(
    value: Long,
    onValueChange: (Long) -> Unit,
) {
    var valueString by remember {
        mutableStateOf(if (value == 0L) "0" else "%.2f".format(value / 1000.0))
    }

    LaunchedEffect(value) {
        val currentValue = valueString.toDoubleOrNull() ?: 0.0
        if (currentValue == (value / 1000.0)) return@LaunchedEffect
        valueString = if (value == 0L) "0" else "%.2f".format(value / 1000.0)
    }

    NumberChooserInput(
        title = stringResource(R.string.subtitle_delay_seconds),
        value = valueString,
        textFieldTestTag = "input_subtitle_delay",
        decrementButtonTestTag = "btn_subtitle_delay_decrement",
        incrementButtonTestTag = "btn_subtitle_delay_increment",
        onValueChange = { newValue ->
            if (newValue.isBlank()) {
                valueString = ""
                onValueChange(0)
                return@NumberChooserInput
            }

            val cleanedValue = newValue.trimStart()

            if (cleanedValue == "-" || cleanedValue == ".") {
                valueString = cleanedValue
                return@NumberChooserInput
            }

            val decimalPattern = "^-?\\d*\\.?\\d{0,2}$".toRegex()
            if (!cleanedValue.matches(decimalPattern)) {
                return@NumberChooserInput
            }

            valueString = cleanedValue

            runCatching {
                val doubleValue = cleanedValue.toDoubleOrNull() ?: 0.0
                val milliseconds = (doubleValue * 1000).roundToLong()
                onValueChange(milliseconds)
            }
        },
        onIncrement = { onValueChange(value + 100) },
        onDecrement = { onValueChange(value - 100) },
    )
}

@Composable
private fun SpeedInput(
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    var valueString by remember {
        mutableStateOf(if (value == 1f) "1" else "%.2f".format(value))
    }

    LaunchedEffect(value) {
        val currentValue = valueString.toFloatOrNull() ?: 0.0
        if (currentValue == value) return@LaunchedEffect
        valueString = if (value == 1f) "1" else "%.2f".format(value)
    }

    NumberChooserInput(
        title = stringResource(R.string.subtitle_speed_multiplier),
        value = valueString,
        textFieldTestTag = "input_subtitle_speed",
        decrementButtonTestTag = "btn_subtitle_speed_decrement",
        incrementButtonTestTag = "btn_subtitle_speed_increment",
        onValueChange = { newValue ->
            if (newValue.isBlank()) {
                valueString = ""
                onValueChange(1f)
                return@NumberChooserInput
            }

            val cleanedValue = newValue.trimStart()

            if (cleanedValue == ".") {
                valueString = cleanedValue
                return@NumberChooserInput
            }

            val decimalPattern = "^\\d*\\.?\\d{0,2}$".toRegex()
            if (!cleanedValue.matches(decimalPattern)) {
                return@NumberChooserInput
            }

            valueString = cleanedValue

            runCatching {
                val floatValue = cleanedValue.toFloatOrNull() ?: 1f
                onValueChange(floatValue)
            }
        },
        onIncrement = { onValueChange(value + 0.1f) },
        onDecrement = { onValueChange(value - 0.1f) },
    )
}

@Composable
private fun NumberChooserInput(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    textFieldTestTag: String? = null,
    decrementButtonTestTag: String? = null,
    incrementButtonTestTag: String? = null,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MiuixIconButton(
            onClick = { },
            modifier = Modifier
                .then(decrementButtonTestTag?.let(Modifier::testTag) ?: Modifier)
                .repeatingClickable(onClick = onDecrement),
        ) {
            MiuixIcon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = null,
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .then(textFieldTestTag?.let(Modifier::testTag) ?: Modifier),
            singleLine = true,
            label = title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
        )
        MiuixIconButton(
            onClick = { },
            modifier = Modifier
                .then(incrementButtonTestTag?.let(Modifier::testTag) ?: Modifier)
                .repeatingClickable(onClick = onIncrement),
        ) {
            MiuixIcon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
            )
        }
    }
}

private fun Modifier.repeatingClickable(
    isEnabled: Boolean = true,
    maxDelayMillis: Long = 200,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .20f,
    onClick: () -> Unit,
): Modifier = composed {
    val updatedOnClick by rememberUpdatedState(onClick)

    this.pointerInput(isEnabled) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val heldButtonJob = launch {
                    var currentDelayMillis = maxDelayMillis
                    while (isEnabled && down.pressed) {
                        updatedOnClick()
                        delay(currentDelayMillis)
                        val nextMillis = currentDelayMillis - (currentDelayMillis * delayDecayFactor)
                        currentDelayMillis = nextMillis.toLong().coerceAtLeast(minDelayMillis)
                    }
                }
                waitForUpOrCancellation()
                heldButtonJob.cancel()
            }
        }
    }
}
