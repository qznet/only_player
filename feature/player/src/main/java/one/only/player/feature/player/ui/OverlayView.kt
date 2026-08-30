package one.only.player.feature.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import one.only.player.core.ui.theme.OnlyPlayerTheme
import one.only.player.feature.player.ui.panel.FloatingPlayerPanel
import one.only.player.feature.player.ui.panel.LocalFloatingPlayerPanelState
import one.only.player.feature.player.ui.panel.rememberFloatingPlayerPanelState
import one.only.player.feature.player.ui.panel.rememberPlayerPanelTokens
import top.yukonga.miuix.kmp.basic.Text as MiuixText

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    shouldShow: Boolean,
    title: String,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val panelState = LocalFloatingPlayerPanelState.current ?: rememberFloatingPlayerPanelState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(shouldShow) {
        if (shouldShow) focusRequester.requestFocus()
    }
    FloatingPlayerPanel(
        modifier = modifier,
        shouldShow = shouldShow,
        title = title,
        panelState = panelState,
        testTag = testTag,
        contentPadding = contentPadding,
        focusRequester = focusRequester,
        content = content,
    )
}

@Preview
@Composable
private fun PreviewOverlayView() {
    OnlyPlayerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OverlayView(title = "Selector view", shouldShow = true) {
                val tokens = rememberPlayerPanelTokens()
                MiuixText(
                    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum",
                    color = tokens.contentColor,
                )
            }
        }
    }
}
