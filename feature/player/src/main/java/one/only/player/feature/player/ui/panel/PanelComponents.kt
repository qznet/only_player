package one.only.player.feature.player.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import one.only.player.core.ui.components.AppSwitch
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PanelOptionList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = rememberPlayerPanelTokens()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.itemSpacing),
        content = content,
    )
}

@Composable
fun PanelOptionRow(
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    isEnabled: Boolean = true,
    description: String? = null,
    maxTextLines: Int = if (description == null) 2 else 1,
) {
    val tokens = rememberPlayerPanelTokens()
    val shape = tokens.optionShape
    val containerColor = if (isSelected) tokens.itemSelectedColor else tokens.itemColor
    val contentColor = if (isSelected) tokens.itemSelectedContentColor else tokens.itemContentColor
    val checkTint = if (tokens.itemSelectedColor == tokens.accentColor) tokens.onAccentColor else tokens.accentColor
    Row(
        modifier = modifier
            .then(
                if (testTag != null) {
                    Modifier
                        .testTag(testTag)
                        .semantics { contentDescription = testTag }
                } else {
                    Modifier
                },
            )
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = tokens.itemVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MiuixText(
                text = text,
                color = contentColor,
                style = MiuixTheme.textStyles.body1,
                maxLines = maxTextLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                MiuixText(
                    text = description,
                    color = contentColor.copy(alpha = 0.7f),
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isSelected) {
            MiuixIcon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun PanelActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProminent: Boolean = false,
    isEnabled: Boolean = true,
) {
    MiuixTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = isEnabled,
        colors = if (isProminent) {
            MiuixButtonDefaults.textButtonColorsPrimary()
        } else {
            MiuixButtonDefaults.textButtonColors()
        },
    )
}

@Composable
fun PanelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val tokens = rememberPlayerPanelTokens()
    MiuixSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.sliderColors(
            foregroundColor = tokens.accentColor,
            backgroundColor = tokens.contentColor.copy(alpha = 0.14f),
        ),
    )
}

@Composable
fun PanelSwitchRow(
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val tokens = rememberPlayerPanelTokens()
    val shape = tokens.optionShape
    Row(
        modifier = modifier
            .then(
                if (testTag != null) {
                    Modifier
                        .testTag(testTag)
                        .semantics { contentDescription = testTag }
                } else {
                    Modifier
                },
            )
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.itemColor)
            .toggleable(
                value = isChecked,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = tokens.itemVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MiuixText(
            text = text,
            color = tokens.itemContentColor,
            style = MiuixTheme.textStyles.body1,
            modifier = Modifier.weight(1f),
        )
        AppSwitch(
            isChecked = isChecked,
            onCheckedChange = null,
        )
    }
}

@Composable
fun PanelChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val tokens = rememberPlayerPanelTokens()
    val containerColor = if (isSelected) tokens.accentColor else tokens.itemColor
    val contentColor = if (isSelected) tokens.onAccentColor else tokens.itemContentColor
    Row(
        modifier = modifier
            .then(
                if (testTag != null) {
                    Modifier
                        .testTag(testTag)
                        .semantics { contentDescription = testTag }
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(containerColor)
            .selectable(
                selected = isSelected,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = text,
            color = contentColor,
            style = MiuixTheme.textStyles.body2,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
