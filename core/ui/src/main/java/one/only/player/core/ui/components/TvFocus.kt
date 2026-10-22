package one.only.player.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocusApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 当前设备是否为 Android TV（或处于电视 UI 模式）。
 * 用于把"请求初始焦点 / 焦点导航"等 TV 专属行为与手机触摸体验隔离，
 * 避免手机端首个设置项被强制高亮聚焦。
 */
@Composable
fun isTvDevice(): Boolean {
    val uiMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * 设置页等列表容器：在 TV 上进入页面时把焦点落到首个可聚焦子项，
 * 使遥控器 DPAD 上下可遍历、滚动自动跟随（verticalScroll 自身负责 bringIntoView）。
 *
 * 关键前提：MIUIX 偏好组件（ArrowPreference / SwitchPreference → BasicComponent）走
 * Modifier.clickable，在 Compose 焦点系统里是可聚焦节点，因此这里无需逐行包 focusable()，
 * 只需在容器层请求"进入时落到首个子项"即可。
 *
 * 手机端不加任何焦点修饰符、也不请求焦点，触摸体验完全不受影响。
 */
@OptIn(ExperimentalFocusApi::class)
@Composable
fun TvInitialFocusColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isTv = isTvDevice()

    if (isTv) {
        // 进入页面即把焦点交给首个可聚焦子项，遥控器 DPAD 才能开始导航。
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    val tvModifier = if (isTv) {
        Modifier
            .focusRequester(focusRequester)
            .focusProperties { enter = { FocusRequester.First } }
            .focusable()
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(tvModifier),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}
