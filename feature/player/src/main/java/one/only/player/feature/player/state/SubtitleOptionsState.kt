package one.only.player.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.only.player.feature.player.service.getSubtitleDelayMilliseconds
import one.only.player.feature.player.service.getSubtitleSpeed
import one.only.player.feature.player.service.resetSubtitleCalibration
import one.only.player.feature.player.service.setSubtitleDelayMilliseconds
import one.only.player.feature.player.service.setSubtitleSpeed

@UnstableApi
@Composable
fun rememberSubtitleOptionsState(player: Player): SubtitleOptionsState {
    val scope = rememberCoroutineScope()
    val subtitleOptionsState = remember(player, scope) { SubtitleOptionsState(player, scope) }
    LaunchedEffect(player) { subtitleOptionsState.observe() }
    return subtitleOptionsState
}

@Stable
class SubtitleOptionsState(
    val player: Player,
    val scope: CoroutineScope,
) {

    var delayMilliseconds: Long by mutableLongStateOf(0L)
        private set

    var speedMultiplier: Float by mutableFloatStateOf(1f)
        private set

    fun setDelay(delayMillis: Long) {
        scope.launch {
            when (player) {
                is MediaController -> player.setSubtitleDelayMilliseconds(delayMillis)
                is ExoPlayer -> player.subtitleDelayMilliseconds = delayMillis
                else -> return@launch
            }
            updateSubtitleDelayMilliseconds()
        }
    }

    fun setSpeed(speed: Float) {
        scope.launch {
            when (player) {
                is MediaController -> player.setSubtitleSpeed(speed)
                is ExoPlayer -> player.subtitleSpeed = speed
                else -> return@launch
            }
            updateSubtitleSpeed()
        }
    }

    val isCalibrated: Boolean
        get() = delayMilliseconds != 0L || speedMultiplier != DEFAULT_SPEED

    fun reset() {
        scope.launch {
            when (player) {
                is MediaController -> player.resetSubtitleCalibration()
                is ExoPlayer -> {
                    player.subtitleDelayMilliseconds = 0L
                    player.subtitleSpeed = DEFAULT_SPEED
                }
                else -> return@launch
            }
            updateSubtitleDelayMilliseconds()
            updateSubtitleSpeed()
        }
    }

    suspend fun observe() {
        updateSubtitleDelayMilliseconds()
        updateSubtitleSpeed()
        player.listen { events ->
            if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                scope.launch {
                    updateSubtitleDelayMilliseconds()
                    updateSubtitleSpeed()
                }
            }
        }
    }

    private suspend fun updateSubtitleDelayMilliseconds() {
        delayMilliseconds = when (player) {
            is MediaController -> player.getSubtitleDelayMilliseconds()
            is ExoPlayer -> player.subtitleDelayMilliseconds
            else -> return
        }
    }

    private suspend fun updateSubtitleSpeed() {
        speedMultiplier = when (player) {
            is MediaController -> player.getSubtitleSpeed()
            is ExoPlayer -> player.subtitleSpeed
            else -> return
        }
    }

    private companion object {
        private const val DEFAULT_SPEED = 1f
    }
}
