package one.only.player.settings.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import one.only.player.core.model.UpdateChannel
import one.only.player.core.ui.R

@Composable
fun UpdateChannel.name(): String {
    val stringRes = when (this) {
        UpdateChannel.STABLE -> R.string.update_channel_stable
        UpdateChannel.TEST -> R.string.update_channel_test
    }

    return stringResource(id = stringRes)
}
