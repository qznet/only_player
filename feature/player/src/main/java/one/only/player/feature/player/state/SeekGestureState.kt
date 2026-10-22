package one.only.player.feature.player.state

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionResult
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import one.only.player.feature.player.extensions.availableDurationMs
import one.only.player.feature.player.extensions.canSeekCurrentMediaItem
import one.only.player.feature.player.extensions.formatted
import one.only.player.feature.player.extensions.isApproximateSeekEnabled
import one.only.player.feature.player.extensions.requestSeekToRequestedPosition
import one.only.player.feature.player.extensions.setIsScrubbingModeEnabled
import one.only.player.feature.player.extensions.setIsSeekPreviewEnabled
import one.only.player.feature.player.service.CustomCommands

@UnstableApi
@Composable
fun rememberSeekGestureState(
    player: Player,
    sensitivity: Float = 0.5f,
    isSeekGestureEnabled: Boolean,
    isSeekPreviewFrameEnabled: Boolean = false,
): SeekGestureState {
    val coroutineScope = rememberCoroutineScope()
    val seekGestureState = remember(player, sensitivity, isSeekGestureEnabled, isSeekPreviewFrameEnabled) {
        SeekGestureState(
            player = player,
            sensitivity = sensitivity,
            isSeekGestureEnabled = isSeekGestureEnabled,
            isSeekPreviewFrameEnabled = isSeekPreviewFrameEnabled,
            coroutineScope = coroutineScope,
        )
    }
    DisposableEffect(player, seekGestureState) {
        player.addListener(seekGestureState)
        onDispose {
            player.removeListener(seekGestureState)
            seekGestureState.dispose()
        }
    }
    return seekGestureState
}

@Stable
class SeekGestureState(
    private val player: Player,
    private val isSeekGestureEnabled: Boolean = true,
    private val isSeekPreviewFrameEnabled: Boolean = false,
    private val sensitivity: Float = 0.5f,
    private val coroutineScope: CoroutineScope,
) : Player.Listener {
    var isSeeking: Boolean by mutableStateOf(false)
        private set

    var seekStartPosition: Long? by mutableStateOf(null)
        private set

    var seekAmount: Long? by mutableStateOf(null)
        private set

    var pendingSeekPosition: Long? by mutableStateOf(null)
        private set

    private var seekStartX = 0f
    private var seekMediaId: String? = null
    private var seekRequestId = 0L
    private var seekRequestJob: Job? = null
    private var previewSeekJob: Job? = null
    private var previewSeekTargets: Channel<Long>? = null

    fun onSeek(value: Long) {
        if (!player.canSeekCurrentMediaItem()) return
        val duration = player.availableDurationMs()
        if (duration == C.TIME_UNSET || duration <= 0L) return
        val currentPosition = player.currentPosition.takeIf { it != C.TIME_UNSET } ?: 0L

        if (!isSeeking) {
            startSeek(currentPosition)
        }

        val newPosition = value.coerceIn(0L, duration)
        pendingSeekPosition = newPosition
        seekAmount = (newPosition - seekStartPosition!!).coerceIn(
            minimumValue = 0 - seekStartPosition!!,
            maximumValue = duration - seekStartPosition!!,
        )
        requestPreviewSeek(newPosition)
    }

    fun onSeekEnd() {
        finishSeek()
    }

    fun onDragStart(offset: Offset) {
        if (!isSeekGestureEnabled) return
        if (!player.canSeekCurrentMediaItem()) return
        val duration = player.availableDurationMs()
        if (duration == C.TIME_UNSET || duration <= 0L) return
        val currentPosition = player.currentPosition.takeIf { it != C.TIME_UNSET } ?: 0L

        startSeek(currentPosition)
        seekStartX = offset.x
    }

    @OptIn(UnstableApi::class)
    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val seekStartPosition = seekStartPosition ?: return
        val duration = player.availableDurationMs()
        if (duration == C.TIME_UNSET || duration <= 0L) return
        if (change.isConsumed) return

        val currentPreviewPosition = pendingSeekPosition ?: seekStartPosition
        if (currentPreviewPosition <= 0L && dragAmount < 0) return
        if (currentPreviewPosition >= duration && dragAmount > 0) return

        val newPosition = (seekStartPosition + ((change.position.x - seekStartX) * (sensitivity * 100)).toInt())
            .coerceIn(0L, duration)
        pendingSeekPosition = newPosition
        seekAmount = (newPosition - seekStartPosition).coerceIn(
            minimumValue = 0 - seekStartPosition,
            maximumValue = duration - seekStartPosition,
        )
        requestPreviewSeek(newPosition)
    }

    fun onDragEnd() {
        finishSeek()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK) return
        if (isSeeking) return
        val pendingSeekPosition = pendingSeekPosition ?: return
        val mediaId = newPosition.mediaItem?.mediaId ?: player.currentMediaItem?.mediaId
        if (mediaId != seekMediaId) return
        if (abs(newPosition.positionMs - pendingSeekPosition) > SEEK_CONFIRMATION_TOLERANCE_MS) return

        clearPendingSeek()
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pendingSeekPosition == null) return
        if (mediaItem?.mediaId == seekMediaId) return
        reset()
    }

    override fun onPlayerError(error: PlaybackException) {
        if (pendingSeekPosition == null) return
        reset()
    }

    fun dispose() {
        reset()
    }

    private fun startSeek(currentPosition: Long) {
        seekRequestId++
        seekRequestJob?.cancel()
        seekRequestJob = null
        seekMediaId = player.currentMediaItem?.mediaId
        isSeeking = true
        seekStartPosition = currentPosition
        pendingSeekPosition = currentPosition
        player.setIsScrubbingModeEnabled(true)
        startPreviewSeek()
    }

    private fun finishSeek() {
        val pendingSeekPosition = pendingSeekPosition ?: return
        val currentPosition = player.currentPosition.takeIf { it != C.TIME_UNSET }
        isSeeking = false
        seekStartPosition = null
        seekAmount = null
        seekStartX = 0f
        stopPreviewSeek()
        player.setIsScrubbingModeEnabled(false)

        if (currentPosition == pendingSeekPosition) {
            clearPendingSeek()
            return
        }
        if (!player.canSeekCurrentMediaItem()) {
            clearPendingSeek()
            return
        }

        val requestId = seekRequestId
        val mediaId = seekMediaId
        val request = player.requestSeekToRequestedPosition(pendingSeekPosition) ?: return
        if (this.pendingSeekPosition != pendingSeekPosition || seekMediaId != mediaId) return
        seekRequestJob = coroutineScope.launch {
            try {
                val result = runCatching { request.await() }.getOrNull()
                if (requestId != seekRequestId || isSeeking) return@launch
                if (this@SeekGestureState.pendingSeekPosition != pendingSeekPosition || seekMediaId != mediaId) return@launch
                if (result?.resultCode != SessionResult.RESULT_SUCCESS) {
                    clearPendingSeek(requestId)
                    return@launch
                }
                if (!result.extras.getBoolean(CustomCommands.SEEK_WAS_APPLIED_KEY, true)) {
                    clearPendingSeek(requestId)
                }
            } finally {
                if (requestId == seekRequestId) seekRequestJob = null
            }
        }
    }

    private fun clearPendingSeek(requestId: Long = seekRequestId) {
        if (requestId != seekRequestId) return
        pendingSeekPosition = null
        seekMediaId = null
    }

    // 预览只在能快速定位的媒体上开启；approximate source 的 seek 会顺序读取，反而更卡
    private fun canPreviewSeek(): Boolean {
        if (!isSeekPreviewFrameEnabled) return false
        return player.currentMediaItem?.mediaMetadata?.isApproximateSeekEnabled != true
    }

    private fun startPreviewSeek() {
        stopPreviewSeek()
        if (!canPreviewSeek()) return

        // CONFLATED 只保留最新目标，拖动比解码快时自动丢弃中间位置，画面更新变慢但不会堆积，松手后仍精确落位
        val targets = Channel<Long>(Channel.CONFLATED)
        previewSeekTargets = targets
        player.setIsSeekPreviewEnabled(true)
        previewSeekJob = coroutineScope.launch {
            for (target in targets) {
                player.seekTo(target)
                delay(PREVIEW_SEEK_INTERVAL_MS)
            }
        }
    }

    private fun requestPreviewSeek(positionMs: Long) {
        previewSeekTargets?.trySend(positionMs)
    }

    private fun stopPreviewSeek() {
        val hasPreviewSeek = previewSeekTargets != null
        previewSeekTargets?.close()
        previewSeekTargets = null
        previewSeekJob?.cancel()
        previewSeekJob = null
        if (hasPreviewSeek) player.setIsSeekPreviewEnabled(false)
    }

    private fun reset() {
        seekRequestId++
        seekRequestJob?.cancel()
        seekRequestJob = null
        stopPreviewSeek()
        player.setIsScrubbingModeEnabled(false)
        isSeeking = false
        seekStartPosition = null
        seekAmount = null
        pendingSeekPosition = null
        seekStartX = 0f
        seekMediaId = null
    }

    private companion object {
        private const val SEEK_CONFIRMATION_TOLERANCE_MS = 1_000L
        private const val PREVIEW_SEEK_INTERVAL_MS = 120L
    }
}

val SeekGestureState.seekAmountFormatted: String
    get() {
        val seekAmount = seekAmount ?: return ""
        val sign = if (seekAmount < 0) "-" else "+"
        return sign + abs(seekAmount).milliseconds.formatted()
    }

val SeekGestureState.seekToPositionFormated: String
    get() {
        val position = seekStartPosition ?: return ""
        val seekAmount = seekAmount ?: return ""
        return (position + seekAmount).milliseconds.formatted()
    }
