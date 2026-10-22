package one.only.player.feature.player.ui.controls

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import one.only.player.core.model.PlayerControl
import one.only.player.core.ui.components.tvFocusRing
import one.only.player.feature.player.LocalControlsVisibilityState
import one.only.player.feature.player.ui.MenuRoute
import one.only.player.feature.player.ui.PlayerControlAction
import one.only.player.feature.player.ui.PlayerControlBinding
import one.only.player.feature.player.ui.resolve
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

// 顶栏和底栏共用：按编排顺序摆出角落按钮
@Composable
internal fun PlayerCornerControls(
    controls: List<PlayerControl>,
    bindings: Map<PlayerControl, PlayerControlBinding>,
    maxVisibleControls: Int,
    onOpenPanel: (MenuRoute) -> Unit,
) {
    val resolvedBindings = bindings.resolve(controls)
    if (resolvedBindings.isEmpty()) return

    val scrollState = rememberScrollState()
    val controlsVisibilityState = LocalControlsVisibilityState.current
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            controlsVisibilityState?.showControls(duration = Duration.INFINITE)
        } else {
            controlsVisibilityState?.showControls()
        }
    }

    Row(
        modifier = Modifier
            .widthIn(max = PlayerCornerControlSize * maxVisibleControls)
            .horizontalScroll(scrollState),
    ) {
        resolvedBindings.forEach { binding ->
            PlayerCornerControlButton(
                binding = binding,
                onClick = {
                    when (val action = binding.action) {
                        is PlayerControlAction.OpenPanel -> onOpenPanel(action.route)
                        is PlayerControlAction.Execute -> action.onExecute()
                    }
                },
            )
        }
    }
}

@Composable
internal fun PlayerCornerControlButton(
    binding: PlayerControlBinding,
    onClick: () -> Unit,
) {
    MiuixIconButton(
        modifier = Modifier
            .size(PlayerCornerControlSize)
            .tvFocusRing(shape = CircleShape, color = Color.White)
            .testTag(binding.cornerTestTag),
        onClick = onClick,
        enabled = binding.isEnabled,
    ) {
        MiuixIcon(
            modifier = Modifier.size(24.dp),
            imageVector = binding.icon,
            contentDescription = binding.label,
            tint = if (binding.isEnabled) Color.White else Color.White.copy(alpha = 0.4f),
        )
    }
}

private val PlayerCornerControlSize = 40.dp
