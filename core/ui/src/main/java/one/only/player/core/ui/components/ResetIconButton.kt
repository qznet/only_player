package one.only.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import one.only.player.core.ui.designsystem.AppIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 段内 trailing 位置的重置按钮，仅保留图标本体
@Composable
fun ResetIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    minHeight: Dp = IconButtonDefaults.MinHeight,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        minWidth = minWidth,
        minHeight = minHeight,
    ) {
        Icon(
            imageVector = AppIcons.History,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}
