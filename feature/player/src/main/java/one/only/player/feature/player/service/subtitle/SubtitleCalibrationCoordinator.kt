package one.only.player.feature.player.service.subtitle

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import one.only.player.core.data.repository.MediaRepository
import one.only.player.core.model.SubtitleCalibration
import one.only.player.feature.player.extensions.externalSubtitleId
import one.only.player.feature.player.extensions.externalSubtitleIds
import one.only.player.feature.player.extensions.withoutTrackPeriodPrefix

// 同一视频的各条字幕独立保存，读写按调用顺序执行
internal class SubtitleCalibrationCoordinator(
    private val mediaRepository: MediaRepository,
    private val resolvePlaybackStateUri: suspend (MediaItem) -> String,
) {
    private val mutex = Mutex()

    data class SubtitleTrack(
        val key: String,
        val index: Int,
    )

    fun selectedTrack(player: Player): SubtitleTrack? {
        val tracks = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
        val index = tracks.indexOfFirst { it.isSelected }
        val track = tracks.getOrNull(index) ?: return null
        val selectedIndex = (0 until track.length).firstOrNull(track::isTrackSelected) ?: return null
        val formatId = track.getTrackFormat(selectedIndex).id?.withoutTrackPeriodPrefix()
        val key = track.externalSubtitleId(player.externalSubtitleIds())
            ?: formatId?.let { "embedded:id:$it" }
            ?: "embedded:index:$index:$selectedIndex"
        return SubtitleTrack(key, index)
    }

    suspend fun resolveCalibration(
        mediaItem: MediaItem,
        track: SubtitleTrack,
    ): SubtitleCalibration = mutex.withLock {
        mediaRepository.getOrCreateSubtitleCalibration(
            uri = resolvePlaybackStateUri(mediaItem),
            subtitleKey = track.key,
            trackIndex = track.index,
        )
    }

    suspend fun save(
        mediaItem: MediaItem,
        subtitleKey: String,
        calibration: SubtitleCalibration,
    ) = mutex.withLock {
        mediaRepository.saveSubtitleCalibration(
            uri = resolvePlaybackStateUri(mediaItem),
            subtitleKey = subtitleKey,
            calibration = calibration,
        )
    }
}
