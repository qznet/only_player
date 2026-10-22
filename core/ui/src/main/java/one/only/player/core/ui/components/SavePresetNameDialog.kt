package one.only.player.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import one.only.player.core.ui.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField

// 输入预设名称的通用弹窗，由调用方提供文案与测试标识
@Composable
fun SavePresetNameDialog(
    title: String,
    presetNameLabel: String,
    dialogTestTag: String,
    inputTestTag: String,
    confirmTestTag: String,
    onDismissRequest: () -> Unit,
    onSavePreset: (String) -> Unit,
    modifier: Modifier = Modifier,
    shouldKeepSystemBarsHidden: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AppDialog(
        modifier = modifier.testTag(dialogTestTag),
        onDismissRequest = onDismissRequest,
        title = title,
        content = {
            if (shouldKeepSystemBarsHidden) {
                KeepSystemBarsHidden()
            }
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(inputTestTag),
                singleLine = true,
                label = presetNameLabel,
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(confirmTestTag),
                text = stringResource(R.string.save),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { onSavePreset(name.trim()) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismissRequest) },
    )
}

// 弹窗获得焦点时保持系统栏隐藏，避免沉浸模式被打断导致底层面板高度变化
@Composable
private fun KeepSystemBarsHidden() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
