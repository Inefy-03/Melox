package com.melox.player.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/** Uses Media3's float-capable Sonic without its integer-only silence-skipping processor. */
@UnstableApi
internal class PcmPlaybackProcessorChain(gain: (() -> Float)? = null) : AudioProcessorChain {
    private val sonic = SonicAudioProcessor()
    private val processors = listOfNotNull(gain?.let(::ReplayGainAudioProcessor), sonic)
        .toTypedArray<AudioProcessor>()

    override fun getAudioProcessors(): Array<AudioProcessor> = processors

    override fun applyPlaybackParameters(parameters: PlaybackParameters): PlaybackParameters {
        sonic.setSpeed(parameters.speed)
        sonic.setPitch(parameters.pitch)
        return parameters
    }

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean = false

    override fun getMediaDuration(playoutDuration: Long): Long =
        if (sonic.isActive) sonic.getMediaDuration(playoutDuration) else playoutDuration

    override fun getSkippedOutputFrameCount(): Long = 0L
}

/** Applies file gain before the sink's independent focus and pause-fade volume. */
@UnstableApi
internal class ReplayGainAudioProcessor(
    private val multiplier: () -> Float,
) : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
            else -> AudioProcessor.AudioFormat.NOT_SET
        }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val gain = multiplier()
        val output = replaceOutputBuffer(inputBuffer.remaining())
        when {
            gain == 1f -> output.put(inputBuffer)
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.hasRemaining()) {
                    output.putFloat((inputBuffer.getFloat() * gain).coerceIn(-1f, 1f))
                }
            }
            else -> {
                while (inputBuffer.hasRemaining()) {
                    val sample = (inputBuffer.getShort() * gain).coerceIn(-32768f, 32767f)
                    output.putShort(sample.roundToInt().toShort())
                }
            }
        }
        output.flip()
    }
}
