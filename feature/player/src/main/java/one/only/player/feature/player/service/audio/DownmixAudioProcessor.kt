@file:OptIn(UnstableApi::class)

package one.only.player.feature.player.service.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Folds a decoded surround mix down to stereo, ahead of a route that cannot carry the channels
 * and would otherwise do it itself — with no headroom, which is heard as a crackle.
 *
 * Only decoded PCM is touched. A bitstream never reaches this class (the passthrough path does
 * not run the processor chain), so 1x is completely unaffected by it.
 */
internal class DownmixAudioProcessor(
    private val enabled: Boolean,
    private val routePcmChannels: Int,
) : BaseAudioProcessor() {

    private companion object {
        private const val TAG = "DownmixProcessor"
        private const val CENTRE_GAIN = 0.7071f
        private const val SURROUND_GAIN = 0.7071f
        private const val BACK_GAIN = 0.5f
        /** Half: a full-scale 5.1 mix sums to ~1.207, so six decibels leaves the limiter room. */
        private const val HEADROOM = 0.5f
        /** Where the limiter starts to bend. Below this the mix passes through unchanged. */
        private const val KNEE = 0.75f

        /**
         * Per input channel, what it adds to left and right. Media3 delivers samples in ascending
         * channel-mask order, so these are the layouts those counts name:
         * FL FR FC LFE BL BR SL SR. The LFE gets no row: folding it in would cost more headroom
         * than the rest of the mix together.
         */
        private fun layoutFor(count: Int): Array<FloatArray>? = when (count) {
            3 -> arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(CENTRE_GAIN, CENTRE_GAIN))
            4 -> arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f),
                floatArrayOf(SURROUND_GAIN, 0f), floatArrayOf(0f, SURROUND_GAIN))
            5 -> arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(CENTRE_GAIN, CENTRE_GAIN),
                floatArrayOf(SURROUND_GAIN, 0f), floatArrayOf(0f, SURROUND_GAIN))
            6 -> arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(CENTRE_GAIN, CENTRE_GAIN),
                floatArrayOf(0f, 0f),
                floatArrayOf(SURROUND_GAIN, 0f), floatArrayOf(0f, SURROUND_GAIN))
            7 -> arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(CENTRE_GAIN, CENTRE_GAIN),
                floatArrayOf(0f, 0f),
                floatArrayOf(BACK_GAIN, BACK_GAIN),
                floatArrayOf(SURROUND_GAIN, 0f), floatArrayOf(0f, SURROUND_GAIN))
            8 -> arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(CENTRE_GAIN, CENTRE_GAIN),
                floatArrayOf(0f, 0f),
                floatArrayOf(BACK_GAIN, 0f), floatArrayOf(0f, BACK_GAIN),
                floatArrayOf(SURROUND_GAIN, 0f), floatArrayOf(0f, SURROUND_GAIN))
            else -> null
        }

        /** Anything wider than 7.1: keep the 7.1 skeleton, fold the rest into the front pair. */
        private fun widen(count: Int): Array<FloatArray> {
            val base = layoutFor(8)!!
            return Array(count) { channel ->
                when {
                    channel < base.size -> base[channel]
                    (channel - base.size) % 2 == 0 -> floatArrayOf(SURROUND_GAIN, 0f)
                    else -> floatArrayOf(0f, SURROUND_GAIN)
                }
            }
        }
    }

    // Written by onConfigure, read on the playback thread. null = not folding.
    private var gains: Array<FloatArray>? = null
    private var inputChannels = 0
    private var limited = false

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        gains = null
        val channels = inputAudioFormat.channelCount
        val pcm = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        // Nothing to fold: a bitstream, an already-stereo track, or a route wide enough to carry
        // the channels as they are. An unanswerable route (zero) is folded anyway — the whole point
        // is that the platform's own fold clips.
        if (!enabled || !pcm || channels <= 2 ||
            (routePcmChannels > 0 && routePcmChannels >= channels)
        ) {
            return AudioFormat.NOT_SET
        }
        val fold = layoutFor(channels) ?: if (channels < 8) return AudioFormat.NOT_SET else widen(channels)
        gains = fold
        inputChannels = channels
        limited = false
        Log.i(TAG, "folding $channels channels to stereo for a $routePcmChannels-channel route")
        return AudioFormat(inputAudioFormat.sampleRate, 2, inputAudioFormat.encoding)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val gains = this.gains ?: return
        val channels = inputChannels
        val floats = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        // Normalised as it is read, so the same knee applies to both encodings.
        val scale = if (floats) 1f else 1f / 32768f
        val sampleBytes = if (floats) 4 else 2
        val frames = inputBuffer.remaining() / (sampleBytes * channels)
        val out = replaceOutputBuffer(frames * 2 * sampleBytes)
        for (frame in 0 until frames) {
            var left = 0f
            var right = 0f
            for (channel in 0 until channels) {
                val sample = if (floats) inputBuffer.getFloat() else inputBuffer.getShort() * scale
                val gain = gains[channel]
                if (gain[0] != 0f) left += sample * gain[0]
                if (gain[1] != 0f) right += sample * gain[1]
            }
            val l = bend(left * HEADROOM)
            val r = bend(right * HEADROOM)
            if (floats) {
                out.putFloat(l)
                out.putFloat(r)
            } else {
                out.putShort(quantize(l))
                out.putShort(quantize(r))
            }
        }
        out.flip()
    }

    /** Linear below the knee, then a curve that climbs to one and stops: continuous, slope 1. */
    private fun bend(sample: Float): Float {
        val magnitude = abs(sample)
        if (magnitude <= KNEE) return sample
        if (!limited) {
            limited = true
            Log.i(TAG, "the fold reached the limiter, this mix is mastered at full scale")
        }
        val over = magnitude - KNEE
        val shaped = KNEE + (1f - KNEE) * over / (over + (1f - KNEE))
        return if (sample < 0f) -shaped else shaped
    }

    /** Full scale is 32768 below zero and 32767 above it; one must be held down a step to fit. */
    private fun quantize(sample: Float): Short =
        (sample * 32768f).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
}
