package one.only.player.feature.player.service.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import one.only.player.core.model.AudioEqualizerBand

// 固定 10 段峰值滤波串联，参数由播放线程之外的偏好流写入
@OptIn(UnstableApi::class)
internal class EqualizerAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var settings: AudioEqualizerSettings = AudioEqualizerSettings()

    private var channelCount = 0
    private var filterState = DoubleArray(0)
    private val coefficients = DoubleArray(BAND_COUNT * COEFFICIENTS_PER_BAND)
    private var processedSamples = DoubleArray(0)
    private var appliedLevels: List<Int>? = null
    private var isBypassing = true
    private var outputGain = 1.0

    fun applySettings(settings: AudioEqualizerSettings) {
        this.settings = settings
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size <= 0) return
        val outputBuffer = replaceOutputBuffer(size)
        val currentSettings = settings
        if (currentSettings.shouldBypass) {
            if (!isBypassing) clearFilterState()
            isBypassing = true
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        isBypassing = false
        updateCoefficients(currentSettings)
        processPcm(inputBuffer, outputBuffer)
        outputBuffer.flip()
    }

    override fun onFlush(streamMetadata: StreamMetadata) {
        // configure 只协商下一段格式；旧音频排空后，flush 才启用新格式
        channelCount = inputAudioFormat.channelCount
        filterState = DoubleArray(BAND_COUNT * channelCount.coerceAtLeast(0) * STATE_PER_CHANNEL)
        appliedLevels = null
        isBypassing = true
        clearFilterState()
    }

    override fun onReset() {
        channelCount = 0
        filterState = DoubleArray(0)
        processedSamples = DoubleArray(0)
        appliedLevels = null
    }

    private fun clearFilterState() {
        filterState.fill(0.0)
        outputGain = 1.0
    }

    private fun processPcm(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
    ) {
        val isFloatPcm = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val sampleCount = inputBuffer.remaining() / if (isFloatPcm) BYTES_PER_FLOAT else BYTES_PER_SHORT
        if (processedSamples.size < sampleCount) processedSamples = DoubleArray(sampleCount)
        var peak = 0.0
        for (index in 0 until sampleCount) {
            val sample = if (isFloatPcm) inputBuffer.float.toDouble() else inputBuffer.short / SHORT_SCALE
            val filtered = filterSample(sample, index % channelCount)
            processedSamples[index] = filtered
            peak = maxOf(peak, abs(filtered))
        }

        // 整块共用峰值限制，声道间保持比例；恢复增益时缓慢释放，避免硬削波
        val targetGain = if (peak > MAX_OUTPUT_LEVEL) MAX_OUTPUT_LEVEL / peak else 1.0
        val release = exp(-1.0 / (inputAudioFormat.sampleRate * GAIN_RELEASE_SECONDS))
        for (index in 0 until sampleCount) {
            if (index % channelCount == 0) outputGain = min(targetGain, 1.0 - (1.0 - outputGain) * release)
            val sample = processedSamples[index] * outputGain
            if (isFloatPcm) {
                outputBuffer.putFloat(sample.toFloat())
            } else {
                outputBuffer.putShort((sample * SHORT_SCALE).roundToInt().toShort())
            }
        }
    }

    private fun filterSample(
        sample: Double,
        channel: Int,
    ): Double {
        var value = sample
        for (band in 0 until BAND_COUNT) {
            val coefficientOffset = band * COEFFICIENTS_PER_BAND
            val stateOffset = (band * channelCount + channel) * STATE_PER_CHANNEL
            val x1 = filterState[stateOffset]
            val x2 = filterState[stateOffset + 1]
            val filtered = coefficients[coefficientOffset] * value +
                coefficients[coefficientOffset + 1] * x1 +
                coefficients[coefficientOffset + 2] * x2 -
                coefficients[coefficientOffset + 3] * filterState[stateOffset + 2] -
                coefficients[coefficientOffset + 4] * filterState[stateOffset + 3]
            filterState[stateOffset + 1] = x1
            filterState[stateOffset] = value
            filterState[stateOffset + 3] = filterState[stateOffset + 2]
            filterState[stateOffset + 2] = filtered
            value = filtered
        }
        return value
    }

    // 曲线变化时保留滤波状态，参数固定为十段
    private fun updateCoefficients(currentSettings: AudioEqualizerSettings) {
        val levels = currentSettings.bandLevelsDb
        if (levels == appliedLevels) return
        appliedLevels = levels

        val sampleRate = inputAudioFormat.sampleRate
        for (band in 0 until BAND_COUNT) {
            computeBandCoefficients(
                band = band,
                frequencyHz = AudioEqualizerBand.entries[band].centerFrequencyHz,
                levelDb = levels[band],
                sampleRate = sampleRate,
            )
        }
    }

    private fun computeBandCoefficients(
        band: Int,
        frequencyHz: Int,
        levelDb: Int,
        sampleRate: Int,
    ) {
        val offset = band * COEFFICIENTS_PER_BAND
        // 中心频率逼近奈奎斯特时峰值滤波会失稳，直接直通
        if (levelDb == 0 || frequencyHz >= sampleRate * MAX_FREQUENCY_RATIO) {
            coefficients[offset] = 1.0
            for (index in 1 until COEFFICIENTS_PER_BAND) coefficients[offset + index] = 0.0
            return
        }

        val angularFrequency = 2.0 * PI * frequencyHz / sampleRate
        val amplitude = 10.0.pow(levelDb / 40.0)
        val alpha = sin(angularFrequency) / (2.0 * BAND_Q)
        val cosine = cos(angularFrequency)
        val a0 = 1.0 + alpha / amplitude
        coefficients[offset] = (1.0 + alpha * amplitude) / a0
        coefficients[offset + 1] = -2.0 * cosine / a0
        coefficients[offset + 2] = (1.0 - alpha * amplitude) / a0
        coefficients[offset + 3] = -2.0 * cosine / a0
        coefficients[offset + 4] = (1.0 - alpha / amplitude) / a0
    }

    private companion object {
        private val BAND_COUNT = AudioEqualizerBand.entries.size
        private const val COEFFICIENTS_PER_BAND = 5
        private const val STATE_PER_CHANNEL = 4
        private const val BYTES_PER_SHORT = 2
        private const val BYTES_PER_FLOAT = 4
        private const val SHORT_SCALE = 32768.0
        private const val MAX_OUTPUT_LEVEL = 0.98
        private const val GAIN_RELEASE_SECONDS = 0.1

        // 相邻中心频率约差一个倍频程，Q 取 1.414
        private const val BAND_Q = 1.4142135623730951
        private const val MAX_FREQUENCY_RATIO = 0.45
    }
}
