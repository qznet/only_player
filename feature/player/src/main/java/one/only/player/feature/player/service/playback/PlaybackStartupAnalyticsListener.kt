package one.only.player.feature.player.service.playback

import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException
import one.only.player.core.common.Logger
import one.only.player.feature.player.extensions.diagnostics
import one.only.player.feature.player.extensions.toLogString
import one.only.player.feature.player.service.effects.VideoEffectsCoordinator

@UnstableApi
internal class PlaybackStartupAnalyticsListener(
    private val tag: String,
    private val currentPlayerProvider: () -> ExoPlayer?,
    private val videoEffectsCoordinator: VideoEffectsCoordinator,
) : AnalyticsListener {

    private var startupTimestamp = 0L
    private var bufferingStartedAt = 0L
    private var stallCount = 0
    private var totalStallDurationMs = 0L
    private var hasReachedReady = false
    private var isCurrentBufferingStall = false

    override fun onMediaItemTransition(
        eventTime: AnalyticsListener.EventTime,
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        startupTimestamp = System.currentTimeMillis()
        bufferingStartedAt = 0L
        stallCount = 0
        totalStallDurationMs = 0L
        hasReachedReady = false
        isCurrentBufferingStall = false
        Logger.info(tag, "startup mediaItem reason=$reason")
    }

    override fun onPlaybackStateChanged(
        eventTime: AnalyticsListener.EventTime,
        state: Int,
    ) {
        if (state == Player.STATE_BUFFERING) {
            if (startupTimestamp == 0L) startupTimestamp = System.currentTimeMillis()
            if (bufferingStartedAt == 0L) {
                bufferingStartedAt = System.currentTimeMillis()
                isCurrentBufferingStall = hasReachedReady && currentPlayerProvider()?.playWhenReady == true
            }
        } else if (bufferingStartedAt != 0L) {
            val bufferingDuration = System.currentTimeMillis() - bufferingStartedAt
            if (isCurrentBufferingStall) {
                stallCount++
                totalStallDurationMs += bufferingDuration
            }
            Logger.info(
                tag,
                "startup bufferingEnd durationMs=$bufferingDuration isStall=$isCurrentBufferingStall stallCount=$stallCount " +
                    "totalStallDurationMs=$totalStallDurationMs ${diagnosticSummary()}",
            )
            bufferingStartedAt = 0L
            isCurrentBufferingStall = false
        }
        if (state == Player.STATE_READY) hasReachedReady = true
        val label = when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN($state)"
        }
        Logger.info(tag, "startup state=$label t=${elapsed()}ms ${diagnosticSummary()}")
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        retryCount: Int,
    ) {
        Logger.info(
            tag,
            "startup loadStart t=${elapsed()}ms type=${mediaLoadData.dataType} ${diagnosticSummary()}",
        )
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        Logger.info(
            tag,
            "startup loadDone t=${elapsed()}ms type=${mediaLoadData.dataType} bytes=${loadEventInfo.bytesLoaded} ${diagnosticSummary()}",
        )
    }

    override fun onLoadCanceled(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        Logger.info(
            tag,
            "startup loadCanceled t=${elapsed()}ms type=${mediaLoadData.dataType} bytes=${loadEventInfo.bytesLoaded} ${diagnosticSummary()}",
        )
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean,
    ) {
        Logger.error(
            tag,
            "startup loadError t=${elapsed()}ms type=${mediaLoadData.dataType} bytes=${loadEventInfo.bytesLoaded} " +
                "wasCanceled=$wasCanceled ${diagnosticSummary()}",
            error,
        )
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long,
    ) {
        Logger.info(tag, "startup firstFrame t=${elapsed()}ms ${diagnosticSummary()}")
    }

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long,
    ) {
        Logger.info(
            tag,
            "startup audioUnderrun bufferSize=$bufferSize bufferSizeMs=$bufferSizeMs " +
                "elapsedSinceLastFeedMs=$elapsedSinceLastFeedMs ${diagnosticSummary()}",
        )
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        Logger.info(
            tag,
            "startup droppedVideoFrames count=$droppedFrames elapsedMs=$elapsedMs ${diagnosticSummary()}",
        )
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        videoEffectsCoordinator.setDecoderName(decoderName)
        Logger.info(tag, "startup decoderInit=$decoderName dur=${initializationDurationMs}ms t=${elapsed()}ms")
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        videoEffectsCoordinator.onVideoInputFormatChanged(
            player = currentPlayerProvider(),
            format = format,
        )
        Logger.info(
            tag,
            "startup videoFormat mime=${format.sampleMimeType} codecs=${format.codecs} bitrate=${format.bitrate} " +
                "transfer=${format.colorInfo?.colorTransfer} " +
                "standard=${format.colorInfo?.colorSpace} range=${format.colorInfo?.colorRange}",
        )
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        Logger.info(tag, "startup audioFormat bitrate=${format.bitrate} mime=${format.sampleMimeType}")
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        Logger.info(tag, "startup audioDecoder=$decoderName dur=${initializationDurationMs}ms t=${elapsed()}ms")
    }

    override fun onTracksChanged(
        eventTime: AnalyticsListener.EventTime,
        tracks: Tracks,
    ) {
        val player = currentPlayerProvider()
        Logger.info(
            tag,
            "startup tracksChanged t=${elapsed()}ms groups=${tracks.groups.size} seekable=${player?.isCurrentMediaItemSeekable} " +
                "duration=${player?.duration} ${diagnosticSummary()}",
        )
    }

    private fun elapsed(): Long = startupTimestamp
        .takeIf { it != 0L }
        ?.let { System.currentTimeMillis() - it }
        ?: 0L

    private fun diagnosticSummary(): String = currentPlayerProvider()?.diagnostics()?.toLogString().orEmpty()

    fun currentStallMetrics(): StallMetrics {
        val currentDurationMs = if (isCurrentBufferingStall) {
            System.currentTimeMillis() - bufferingStartedAt
        } else {
            0L
        }
        return StallMetrics(
            count = stallCount,
            currentDurationMs = currentDurationMs,
            totalDurationMs = totalStallDurationMs,
        )
    }
}

internal data class StallMetrics(
    val count: Int,
    val currentDurationMs: Long,
    val totalDurationMs: Long,
)
