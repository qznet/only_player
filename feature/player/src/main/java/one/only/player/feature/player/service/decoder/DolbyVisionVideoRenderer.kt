package one.only.player.feature.player.service.decoder

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import one.only.player.core.common.Logger
import one.only.player.feature.player.service.effects.isDolbyVisionVideoFormat

@OptIn(UnstableApi::class)
internal class DolbyVisionVideoRenderer(builder: Builder) : MediaCodecVideoRenderer(builder) {

    override fun getName(): String = "DolbyVisionVideoRenderer"

    override fun onEnabled(
        joining: Boolean,
        mayRenderStartOfStream: Boolean,
    ) {
        super.onEnabled(joining, mayRenderStartOfStream)
        Logger.info(name, "Video output path=direct effectsSupported=false")
    }

    override fun supportsFormat(
        mediaCodecSelector: MediaCodecSelector,
        format: Format,
    ): Int {
        if (!format.isDolbyVisionVideoFormat()) return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE)
        return super.supportsFormat(mediaCodecSelector, format)
    }

    override fun handleMessage(
        messageType: Int,
        message: Any?,
    ) {
        // 空效果列表也会创建中间 Surface，杜比视界必须忽略整个效果指令。
        if (messageType == MSG_SET_VIDEO_EFFECTS) return
        super.handleMessage(messageType, message)
    }
}
