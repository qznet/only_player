package one.only.player.feature.player.extensions

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import one.only.player.core.common.Logger
import one.only.player.feature.player.service.preciseSeekTo
import one.only.player.feature.player.service.setMediaControllerIsScrubbingModeEnabled
import one.only.player.feature.player.service.setMediaControllerIsSeekPreviewEnabled

fun Player.switchTrack(
    trackType: @C.TrackType Int,
    trackIndex: Int,
) {
    val trackTypeText = when (trackType) {
        C.TRACK_TYPE_AUDIO -> "audio"
        C.TRACK_TYPE_TEXT -> "subtitle"
        else -> throw IllegalArgumentException("Invalid track type: $trackType")
    }

    if (trackIndex < 0) {
        Logger.debug("Player", "Disabling $trackTypeText")
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    } else {
        val tracks = currentTracks.groups.filter { it.type == trackType && it.isSupported }

        if (tracks.isEmpty() || trackIndex >= tracks.size) {
            Logger.error("Player", "Operation failed: Invalid track index: $trackIndex")
            return
        }

        Logger.debug("Player", "Setting $trackTypeText track: $trackIndex")
        val selectedGroup = tracks[trackIndex]
        val format = selectedGroup.mediaTrackGroup.getFormat(0)
        Logger.debug(
            "Player",
            "Track format: mime=${format.sampleMimeType}",
        )
        val trackSelectionOverride = TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)

        // 覆盖轨道选择参数，强制切到目标轨道
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(trackSelectionOverride)
            .build()
    }
}

// 元数据中的新增列表不包含同目录自动加载的字幕，需与媒体配置合并
@UnstableApi
internal fun Player.externalSubtitleIds(): Set<String> {
    val configurationIds = currentMediaItem
        ?.localConfiguration
        ?.subtitleConfigurations
        ?.mapNotNull { it.id }
        .orEmpty()
    return (configurationIds + currentMediaItem?.mediaMetadata?.addedSubtitleIds.orEmpty()).toSet()
}

@UnstableApi
fun Player.getManuallySelectedTrackIndex(trackType: @C.TrackType Int): Int? {
    val isDisabled = trackSelectionParameters.disabledTrackTypes.contains(trackType)
    if (isDisabled) return -1

    val trackOverrides = trackSelectionParameters.overrides.values.map { it.mediaTrackGroup }
    val trackOverride = trackOverrides.firstOrNull { it.type == trackType } ?: return null
    val tracks = currentTracks.groups.filter { it.type == trackType && it.isSupported }

    return tracks.indexOfFirst { it.mediaTrackGroup == trackOverride }.takeIf { it != -1 }
}

fun Player.addAdditionalSubtitleConfiguration(subtitle: MediaItem.SubtitleConfiguration) {
    val currentMediaItemLocal = currentMediaItem ?: return
    val existingSubConfigurations = currentMediaItemLocal.localConfiguration?.subtitleConfigurations ?: emptyList()
    val subtitleId = requireNotNull(subtitle.id)
    val addedSubtitleIds = (currentMediaItemLocal.mediaMetadata.addedSubtitleIds + subtitleId).distinct()

    if (existingSubConfigurations.any { it.id == subtitle.id }) {
        replaceMediaItem(currentMediaItemIndex, currentMediaItemLocal.copy(addedSubtitleIds = addedSubtitleIds))
        return
    }

    val updateMediaItem = currentMediaItemLocal
        .copy(
            positionMs = currentPosition,
            addedSubtitleIds = addedSubtitleIds,
        )
        .buildUpon()
        .setSubtitleConfigurations(existingSubConfigurations + listOf(subtitle))
        .build()

    replaceCurrentSubtitleItem(updateMediaItem)
}

fun Player.removeAdditionalSubtitleConfiguration(
    subtitleId: String,
    selectedTrackIndex: Int,
) {
    val mediaItem = currentMediaItem ?: return
    val configurations = mediaItem.localConfiguration?.subtitleConfigurations.orEmpty()
    val updatedMediaItem = mediaItem.copy(
        positionMs = currentPosition,
        subtitleTrackIndex = selectedTrackIndex,
        addedSubtitleIds = mediaItem.mediaMetadata.addedSubtitleIds - subtitleId,
    ).buildUpon()
        .setSubtitleConfigurations(configurations.filterNot { it.id == subtitleId })
        .build()
    replaceCurrentSubtitleItem(updatedMediaItem)
}

private fun Player.replaceCurrentSubtitleItem(mediaItem: MediaItem) {
    val index = currentMediaItemIndex
    val position = currentPosition
    val shouldPlayWhenReady = playWhenReady
    addMediaItem(index + 1, mediaItem)
    seekTo(index + 1, position)
    playWhenReady = shouldPlayWhenReady
    removeMediaItem(index)
}

fun Player.availableDurationMs(): Long {
    val playerDuration = duration
    if (playerDuration != C.TIME_UNSET && playerDuration > 0L) {
        return playerDuration
    }

    // currentMediaItem 的 metadata 和 player 顶层 metadata 可能不同步
    return currentMediaItem?.mediaMetadata?.durationMs?.takeIf { it > 0L }
        ?: mediaMetadata.durationMs?.takeIf { it > 0L }
        ?: C.TIME_UNSET
}

fun Player.isCurrentMediaItemLast(): Boolean = mediaItemCount > 0 && currentMediaItemIndex == mediaItemCount - 1

fun Player.isAtEndOfCurrentMediaItem(toleranceMs: Long = 500L): Boolean {
    val durationMs = availableDurationMs()
    if (durationMs == C.TIME_UNSET) return playbackState == Player.STATE_ENDED

    val endThresholdMs = if (durationMs > toleranceMs) durationMs - toleranceMs else durationMs
    return currentPosition >= endThresholdMs
}

fun Player.canSeekCurrentMediaItem(): Boolean {
    if (availableDurationMs() == C.TIME_UNSET) return false

    return when (this) {
        is MediaController -> {
            isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                currentMediaItem?.mediaMetadata?.isApproximateSeekEnabled == true
        }

        else -> true
    }
}

fun Player.seekToRequestedPosition(positionMs: Long) {
    requestSeekToRequestedPosition(positionMs)
}

fun Player.requestSeekToRequestedPosition(positionMs: Long): ListenableFuture<SessionResult>? {
    val duration = availableDurationMs()
    if (duration == C.TIME_UNSET) return null

    val targetPosition = positionMs.coerceIn(0L, duration)
    if (this is MediaController) {
        // approximate source 的 seekTo 会从头顺序读取，必须走 preciseSeekTo 触发 source 升级
        if (currentMediaItem?.mediaMetadata?.isApproximateSeekEnabled == true) {
            return preciseSeekTo(targetPosition)
        }
    }

    seekTo(targetPosition)
    return null
}

fun Player.seekByRequestedOffset(offsetMs: Long) {
    val currentPosition = currentPosition.takeIf { it != C.TIME_UNSET } ?: 0L
    seekToRequestedPosition(currentPosition + offsetMs)
}

@OptIn(UnstableApi::class)
fun Player.setIsScrubbingModeEnabled(isEnabled: Boolean) {
    when (this) {
        is MediaController -> this.setMediaControllerIsScrubbingModeEnabled(isEnabled)
        is ExoPlayer -> this.isScrubbingModeEnabled = isEnabled
    }
}

// 预览期放宽 seek 精度到最近关键帧，长视频才能跟上手指；关闭时恢复精确 seek
@OptIn(UnstableApi::class)
fun Player.setIsSeekPreviewEnabled(isEnabled: Boolean) {
    when (this) {
        is MediaController -> this.setMediaControllerIsSeekPreviewEnabled(isEnabled)
        is ExoPlayer -> this.setSeekParameters(
            if (isEnabled) SeekParameters.CLOSEST_SYNC else SeekParameters.DEFAULT,
        )
    }
}
