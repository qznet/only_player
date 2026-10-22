package one.only.player.feature.player.ui.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import one.only.player.core.model.PlayerControl
import one.only.player.core.ui.components.tvFocusRing
import one.only.player.core.ui.R
import one.only.player.core.ui.designsystem.AppIcons
import one.only.player.core.ui.extensions.copy
import one.only.player.feature.player.ui.MenuRoute
import one.only.player.feature.player.ui.PlayerControlBinding
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ControlsTopModernView(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    topRightControls: List<PlayerControl>,
    bindings: Map<PlayerControl, PlayerControlBinding>,
    maxVisibleControls: Int,
    onOpenPanel: (MenuRoute) -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixIconButton(
            modifier = Modifier
                .tvFocusRing(shape = CircleShape, color = Color.White)
                .testTag("btn_back"),
            onClick = onBackClick,
        ) {
            MiuixIcon(
                modifier = Modifier.size(24.dp),
                imageVector = AppIcons.ArrowBack,
                contentDescription = stringResource(R.string.navigate_up),
                tint = Color.White,
            )
        }
        MiuixText(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = title,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PlayerCornerControls(
            controls = topRightControls,
            bindings = bindings,
            maxVisibleControls = maxVisibleControls,
            onOpenPanel = onOpenPanel,
        )
        MiuixIconButton(
            modifier = Modifier
                .tvFocusRing(shape = CircleShape, color = Color.White)
                .testTag("btn_menu"),
            onClick = onMenuClick,
        ) {
            MiuixIcon(
                modifier = Modifier.size(24.dp),
                imageVector = AppIcons.Menu,
                contentDescription = stringResource(R.string.menu),
                tint = Color.White,
            )
        }
    }
}
