package one.only.player.feature.player.service.decoder

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import one.only.player.feature.player.service.audio.AudioModeController
import one.only.player.feature.player.service.audio.AudioPassthroughGateSink
import one.only.player.feature.player.service.audio.DownmixAudioProcessor
import one.only.player.feature.player.service.audio.PcmRoute
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegAudioRenderer
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegVideoRenderer
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@OptIn(UnstableApi::class)
class NormalizingRenderersFactory(
    context: Context,
    private val audioProcessors: Array<AudioProcessor>,
    private val shouldUseAudioExtensionFallback: Boolean,
    private val audioModeController: AudioModeController,
    private val downmixNarrowRoute: Boolean,
) : NextRenderersFactory(context) {

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) {
        fun newRendererBuilder() = MediaCodecVideoRenderer.Builder(context)
            .setCodecAdapterFactory(codecAdapterFactory)
            .setMediaCodecSelector(mediaCodecSelector)
            .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
            .setEnableDecoderFallback(enableDecoderFallback)
            .setEventHandler(eventHandler)
            .setEventListener(eventListener)
            .setMaxDroppedFramesToNotify(MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY)

        val firstVideoRendererIndex = out.size
        val hardwareRenderer = VideoEffectsRenderer(
            builder = newRendererBuilder(),
            eventHandler = eventHandler,
            eventListener = eventListener,
        )
        out.add(hardwareRenderer)
        out.add(DolbyVisionVideoRenderer(newRendererBuilder()))
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) return

        val softwareRenderer = FfmpegVideoRenderer(
            allowedVideoJoiningTimeMs,
            eventHandler,
            eventListener,
            MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
        )
        val extensionRendererIndex = if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            firstVideoRendererIndex
        } else {
            out.size
        }
        out.add(extensionRendererIndex, softwareRenderer)
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        val routePcmChannels = PcmRoute.channels(context)
        val downmix = DownmixAudioProcessor(downmixNarrowRoute, routePcmChannels)
        val sink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
            // The fold goes first, so the normalization below sees two channels — which is what
            // will actually leave the box.
            .setAudioProcessors(arrayOf(downmix, *audioProcessors))
            .build()
        return AudioPassthroughGateSink(sink).also { audioModeController.attach(it) }
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>,
    ) {
        super.buildAudioRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            audioSink,
            eventHandler,
            eventListener,
            out,
        )
        if (!shouldUseAudioExtensionFallback || extensionRendererMode != EXTENSION_RENDERER_MODE_OFF) return

        out.add(FfmpegAudioRenderer(eventHandler, eventListener, audioSink))
        Log.i(TAG, "Loaded FfmpegAudioRenderer as audio fallback.")
    }

    private companion object {
        private const val TAG = "NormalizingRenderersFactory"
    }
}
