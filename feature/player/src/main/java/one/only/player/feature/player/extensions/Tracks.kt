package one.only.player.feature.player.extensions

import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi

// 轨道组对应的外挂字幕 id；内嵌轨道返回 null
@UnstableApi
internal fun Tracks.Group.externalSubtitleId(externalSubtitleIds: Collection<String>): String? = getTrackFormat(0).id.toExternalSubtitleId(externalSubtitleIds)
