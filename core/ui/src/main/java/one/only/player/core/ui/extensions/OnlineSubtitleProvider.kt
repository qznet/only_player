package one.only.player.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import one.only.player.core.model.OnlineSubtitleProvider
import one.only.player.core.ui.R

@Composable
fun OnlineSubtitleProvider.label(): String = stringResource(labelResource)

val OnlineSubtitleProvider.labelResource: Int
    get() = when (this) {
        OnlineSubtitleProvider.OPEN_SUBTITLES -> R.string.online_subtitle_source_opensubtitles
        OnlineSubtitleProvider.OPEN_SUBTITLES_XML_RPC -> R.string.online_subtitle_source_opensubtitles_xml_rpc
        OnlineSubtitleProvider.SUBTITLE_CAT -> R.string.online_subtitle_source_subtitlecat
    }
