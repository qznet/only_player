package one.only.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import one.only.player.core.ui.designsystem.AppIcons
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun PreferenceSwitch(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    isEnabled: Boolean = true,
    isChecked: Boolean = true,
    onClick: (() -> Unit) = {},
) {
    PreferenceContainer(modifier = modifier) {
        SwitchPreference(
            modifier = Modifier.tvFocusRing(),
            title = title,
            summary = description,
            startAction = icon?.let { { PreferenceIcon(it, isEnabled) } },
            checked = isChecked,
            onCheckedChange = { onClick() },
            enabled = isEnabled,
        )
    }
}

@Preview
@Composable
fun PreferenceSwitchPreview() {
    PreferenceSwitch(
        title = "Title",
        description = "Description of the preference item goes here.",
        icon = AppIcons.DoubleTap,
        onClick = {},
    )
}
