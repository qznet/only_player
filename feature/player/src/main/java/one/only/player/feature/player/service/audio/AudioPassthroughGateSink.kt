@file:OptIn(UnstableApi::class)

package one.only.player.feature.player.service.audio

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink

/**
 * The gate between a bitstream and the receiver.
 *
 * [forceDecode] set = every compressed format is refused, so the renderer can only decode and
 * hand over PCM. A bitstream cannot be time-stretched, so a rate other than 1x has to decode;
 * the flag is switched while the sink is already built (see AudioModeController).
 *
 * Volatile rather than final: written from the app thread, read on the playback thread by
 * [refused] the next time the renderer configures the sink.
 */
internal class AudioPassthroughGateSink(sink: AudioSink) : ForwardingAudioSink(sink) {

    @Volatile
    var forceDecode: Boolean = false
        private set

    /** True while the sink bitstreams: set only when a compressed format is configured. */
    @Volatile
    var isPassthrough: Boolean = false
        private set

    /**
     * Whether the renderer has configured this sink at all yet. A file opened at 1x has not: there
     * is no configuration to tear down, and asking for a rebuild would cost one for nothing.
     */
    @Volatile
    var hasConfigured: Boolean = false
        private set

    /**
     * Only the flag moves here. The renderer asks its questions when it configures the sink,
     * which is why a bare write is useless on its own — the caller has to make it configure again.
     *
     * @return whether the answer changed, so a rate that stays on one side of 1x costs nothing.
     */
    fun setForceDecode(force: Boolean): Boolean {
        if (forceDecode == force) return false
        forceDecode = force
        return true
    }

    private fun refused(format: Format): Boolean =
        forceDecode && !MimeTypes.AUDIO_RAW.equals(format.sampleMimeType)

    override fun configure(config: AudioSink.AudioSinkConfig) {
        isPassthrough = !MimeTypes.AUDIO_RAW.equals(config.format.sampleMimeType)
        hasConfigured = true
        super.configure(config)
    }

    override fun getFormatSupport(format: Format): Int =
        if (refused(format)) AudioSink.SINK_FORMAT_UNSUPPORTED else super.getFormatSupport(format)

    override fun supportsFormat(format: Format): Boolean =
        !refused(format) && super.supportsFormat(format)
}
