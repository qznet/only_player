package one.only.player.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 当前设备是否为 Android TV（或处于电视 UI 模式）。
 * 用于把"请求初始焦点 / 焦点反显 / 焦点导航"等 TV 专属行为与手机触摸体验隔离，
 * 避免手机端首个设置项被强制高亮聚焦。
 */
@Composable
fun isTvDevice(): Boolean {
    val uiMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * 遥控器焦点反显（TV 专属）。
 *
 * MIUIX 的偏好组件 / 图标按钮走 `Modifier.clickable`，在 Compose 焦点系统里可聚焦，
 * 但它们**只画按压反馈、不画焦点态**，所以在电视上"焦点到哪了"完全看不出来。
 * 这里用 `onFocusChanged` 观察焦点，焦点进入时叠一圈高亮描边 + 淡色底，
 * 让遥控器操作有明确的视觉反馈。
 *
 * 用法要求：必须把本 Modifier 放在组件的 `clickable` / `combinedClickable` **之前**
 * （即作为组件自身的 `modifier` 传入），否则收不到焦点事件。
 *
 * 非 TV 设备直接返回原 Modifier，手机端触摸体验零改动。
 */
@Composable
fun Modifier.tvFocusRing(
    enabled: Boolean? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    borderWidth: Dp = 3.dp,
    color: Color? = null,
): Modifier {
    val active = enabled ?: isTvDevice()
    if (!active) return this

    var isFocused by remember { mutableStateOf(false) }
    val ringColor = color ?: MiuixTheme.colorScheme.primary
    val fillColor = ringColor.copy(alpha = 0.16f)

    return this
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (isFocused) {
                Modifier
                    .background(color = fillColor, shape = shape)
                    .border(width = borderWidth, color = ringColor, shape = shape)
            } else {
                Modifier
            },
        )
}

/**
 * 设置页等列表容器：在 TV 上进入页面时把焦点落到首个可聚焦子项，
 * 之后遥控器 DPAD 上下即可遍历列表（`verticalScroll` 自身负责 bringIntoView）。
 *
 * 实现说明（重要）：不能直接给 Column 加 `focusable()` 再 `requestFocus()`——
 * 那样焦点会停在**容器**上，DPAD 上下会按几何搜索跑到别处（顶部栏/底部栏），
 * 列表行根本收不到焦点。这里改为在内容最前面放一个 1dp 的隐形"锚点"：
 * 先聚焦锚点，再朝下 moveFocus 一次落到第一行，随后立刻移除锚点，
 * 这样不会留下"向上按到看不见的节点"的副作用。
 *
 * 手机端不加任何焦点修饰符、也不请求焦点，触摸体验完全不受影响。
 */
@Composable
fun TvInitialFocusColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isTv = isTvDevice()
    val anchorRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var anchorAlive by remember { mutableStateOf(true) }

    if (isTv) {
        LaunchedEffect(Unit) {
            // 等首帧布局完成，FocusRequester 才真正绑定到节点上
            withFrameNanos { }
            val anchorFocused = runCatching { anchorRequester.requestFocus() }.isSuccess
            if (anchorFocused) {
                withFrameNanos { }
                focusManager.moveFocus(FocusDirection.Down)
                anchorAlive = false
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        if (isTv && anchorAlive) {
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(anchorRequester)
                    .focusable(),
            )
        }
        content()
    }
}
