package one.only.player.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.window.WindowDialog

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    additionalButton: @Composable (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current

    WindowDialog(
        show = true,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .widthIn(max = configuration.screenWidthDp.dp - AppDialogDefaults.dialogMargin * 2),
        title = title,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                content()
            }
            DialogButtonRow(
                confirmButton = confirmButton,
                dismissButton = dismissButton,
                additionalButton = additionalButton,
            )
        }
    }
}

@Composable
fun DoneCancelDialog(
    title: String,
    onDoneClick: () -> Unit,
    onDismissClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppDialog(
        title = title,
        confirmButton = { DoneButton(onClick = onDoneClick) },
        dismissButton = { CancelButton(onClick = onDismissClick) },
        onDismissRequest = onDismissClick,
        content = content,
    )
}

@Composable
private fun DialogButtonRow(
    confirmButton: @Composable (() -> Unit)?,
    dismissButton: @Composable (() -> Unit)?,
    additionalButton: @Composable (() -> Unit)?,
) {
    val buttons = listOfNotNull(additionalButton, dismissButton, confirmButton)
    if (buttons.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buttons.forEach { button ->
            Box(
                modifier = Modifier.weight(1f),
                propagateMinConstraints = true,
            ) {
                button()
            }
        }
    }
}

object AppDialogDefaults {
    val dialogMargin: Dp = 16.dp
    val contentMaxHeight: Dp = 420.dp
}
