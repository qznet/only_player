package one.only.player.feature.player.extensions

import one.only.player.core.data.remote.subtitle.SubtitlePayloadEmptyException
import one.only.player.core.data.remote.subtitle.SubtitlePayloadTooLargeException
import one.only.player.core.data.remote.subtitle.SubtitleVipRequiredException
import one.only.player.core.ui.R
import one.only.player.feature.player.subtitle.EmptyOnlineSubtitleException
import one.only.player.feature.player.subtitle.OnlineSubtitleTooLargeException

// 下载失败按原因给出可读提示
internal fun Throwable.toOnlineSubtitleMessageResId(): Int = when (this) {
    is SubtitlePayloadTooLargeException, is OnlineSubtitleTooLargeException -> R.string.online_subtitle_too_large
    is SubtitlePayloadEmptyException, is EmptyOnlineSubtitleException -> R.string.online_subtitle_empty
    is SubtitleVipRequiredException -> R.string.online_subtitle_vip_required
    else -> R.string.online_subtitle_download_failed
}
