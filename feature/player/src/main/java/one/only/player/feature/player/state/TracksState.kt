package one.only.player.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import one.only.player.feature.player.extensions.addedSubtitleIds
import one.only.player.feature.player.extensions.switchTrack

@UnstableApi
@Composable
fun rememberTracksState(
    player: Player,
    trackType: @C.TrackType Int,
): TracksState {
    val tracksState = remember(player, trackType) { TracksState(player, trackType) }
    LaunchedEffect(tracksState) { tracksState.observe() }
    return tracksState
}

class TracksState(
    private val player: Player,
    private val trackType: @C.TrackType Int,
) {
    var tracks: List<Tracks.Group> by mutableStateOf(emptyList())
        private set

    var addedSubtitleIds: List<String> by mutableStateOf(emptyList())
        private set

    fun switchTrack(index: Int) {
        player.switchTrack(trackType, index)
    }

    suspend fun observe() {
        updateTracks()

        player.listen { events ->
            if (events.containsAny(
                    Player.EVENT_TRACKS_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                )
            ) {
                updateTracks()
            }
        }
    }

    private fun updateTracks() {
        tracks = player.currentTracks.groups.filter { it.type == trackType && it.isSupported }
        addedSubtitleIds = player.currentMediaItem?.mediaMetadata?.addedSubtitleIds.orEmpty()
    }
}
