package com.melox.player.optimization

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import com.melox.player.data.library.parseAudioTagProperties
import com.melox.player.model.PLAYBACK_SPEED_VALUES
import com.melox.player.playback.PcmPlaybackProcessorChain
import com.melox.player.playback.replayGainMultiplier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class PcmPlaybackProcessorChainTest {
    @Test
    fun normalSpeedBypassesProcessingForBothPcmFormats() {
        for (encoding in listOf(C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT)) {
            val chain = PcmPlaybackProcessorChain()
            val processor = chain.audioProcessors.single()
            chain.applyPlaybackParameters(PlaybackParameters.DEFAULT)
            val format = AudioProcessor.AudioFormat(48_000, 2, encoding)
            assertEquals(format, processor.configure(format))
            assertFalse(processor.isActive)
            assertEquals(1_234_567L, chain.getMediaDuration(1_234_567L))
            assertFalse(chain.applySkipSilenceEnabled(true))
        }
    }

    @Test
    fun everySpeedKeepsFloatStereoAndMapsFrameCountsToMediaTime() {
        for (rate in listOf(44_100, 48_000, 96_000)) {
            for (speed in PLAYBACK_SPEED_VALUES.filter { it != 1f }) {
                val chain = PcmPlaybackProcessorChain()
                val frames = rate / 2
                val output = process(chain, rate, 2, speed, C.ENCODING_PCM_FLOAT, frames)
                val actualFrames = output.size / 2
                val expectedFrames = frames / speed
                assertEquals("$rate Hz at $speed", expectedFrames.toDouble(), actualFrames.toDouble(),
                    maxOf(128.0, expectedFrames * 0.01))
                assertTrue(output.all { it.isFinite() && abs(it) <= 0.2f })
                assertTrue(output.any { it != 0f })
                val playedUs = actualFrames * 1_000_000L / rate
                assertEquals(500_000.0, chain.getMediaDuration(playedUs).toDouble(), 10_000.0)
            }
        }
    }

    @Test
    fun floatSpeedPreservesSubSixteenBitSignalsAndPitch() {
        val rate = 48_000
        val output = process(PcmPlaybackProcessorChain(), rate, 1, 2f,
            C.ENCODING_PCM_FLOAT, rate, amplitude = 0.000001f)
        assertTrue(output.any { abs(it) > 0.0000001f })
        assertTrue(output.all { abs(it) < 0.000002f })
        var crossings = 0
        val start = 256
        val end = output.size - 256
        for (index in start + 1 until end) {
            if (output[index - 1] <= 0f && output[index] > 0f) crossings++
        }
        val frequency = crossings * rate.toDouble() / (end - start)
        assertEquals(440.0, frequency, 6.0)
    }

    @Test
    fun integerPlaybackStillChangesSpeedAndSeekFlushDiscardsThePreviousSignal() {
        val chain = PcmPlaybackProcessorChain()
        val integerOutput = process(chain, 48_000, 1, 1.5f, C.ENCODING_PCM_16BIT, 48_000)
        assertEquals(32_000.0, integerOutput.size.toDouble(), 320.0)
        val floatOutput = process(chain, 48_000, 2, 0.75f, C.ENCODING_PCM_FLOAT, 24_000)
        assertTrue(floatOutput.any { it != 0f })
        val afterSeek = process(chain, 48_000, 2, 2f, C.ENCODING_PCM_FLOAT, 24_000, amplitude = 0f)
        assertTrue(afterSeek.isNotEmpty())
        assertTrue(afterSeek.all { it == 0f })
        chain.audioProcessors.single().reset()
        assertFalse(chain.audioProcessors.single().isActive)
    }

    @Test
    fun replayGainPrefersAValidTrackTagAndFallsBackToAlbumOrUnity() {
        val album = "REPLAYGAIN_ALBUM_GAIN" to arrayOf("-6.0206 dB")
        val both = parseAudioTagProperties(mapOf(album, "replaygain_track_gain" to arrayOf("+6.0206 dB")))
        assertEquals(2f, replayGainMultiplier(both.replayGainDb), 0.0001f)
        val fallback = parseAudioTagProperties(mapOf(album, "REPLAYGAIN_TRACK_GAIN" to arrayOf("NaN", "")))
        assertEquals(0.5f, replayGainMultiplier(fallback.replayGainDb), 0.0001f)
        assertEquals(1f, replayGainMultiplier(parseAudioTagProperties(emptyMap()).replayGainDb), 0f)
    }

    @Test
    fun replayGainAppliesToFloatAndIntegerPcmWithoutWrappingClippedSamples() {
        var multiplier = 2f
        val chain = PcmPlaybackProcessorChain { multiplier }
        val processor = chain.audioProcessors.first()
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())

        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        input.putFloat(0.25f).putFloat(-0.75f).flip()
        processor.queueInput(input)
        val floats = processor.output
        assertEquals(0.5f, floats.float, 0f)
        assertEquals(-1f, floats.float, 0f)

        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        input.clear()
        input.putShort(8_000).putShort(24_000).flip()
        processor.queueInput(input)
        val amplified = processor.output
        assertEquals(16_000, amplified.short.toInt())
        assertEquals(32_767, amplified.short.toInt())

        multiplier = 1f
        input.clear()
        input.putShort(-27_000).flip()
        processor.queueInput(input)
        assertEquals(-27_000, processor.output.short.toInt())
    }

    private fun process(
        chain: PcmPlaybackProcessorChain,
        rate: Int,
        channels: Int,
        speed: Float,
        encoding: Int,
        frames: Int,
        amplitude: Float = 0.125f,
    ): FloatArray {
        chain.applyPlaybackParameters(PlaybackParameters(speed, 1f))
        val processor = chain.audioProcessors.single()
        val format = AudioProcessor.AudioFormat(rate, channels, encoding)
        assertEquals(format, processor.configure(format))
        assertTrue(processor.isActive)
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        val samples = ArrayList<Float>()
        fun drain() {
            val buffer = processor.output.order(ByteOrder.nativeOrder())
            while (buffer.hasRemaining()) {
                samples += if (encoding == C.ENCODING_PCM_FLOAT) buffer.float else buffer.short / 32768f
            }
        }
        val bytes = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        var offset = 0
        while (offset < frames) {
            val count = minOf(1_024, frames - offset)
            val input = ByteBuffer.allocateDirect(count * channels * bytes).order(ByteOrder.nativeOrder())
            repeat(count) { frame ->
                repeat(channels) { channel ->
                    val value = (amplitude * sin(2 * PI * (440 + channel * 220) * (offset + frame) / rate)).toFloat()
                    if (encoding == C.ENCODING_PCM_FLOAT) input.putFloat(value)
                    else input.putShort((value * 32767).toInt().toShort())
                }
            }
            input.flip()
            processor.queueInput(input)
            assertFalse(input.hasRemaining())
            drain()
            offset += count
        }
        processor.queueEndOfStream()
        drain()
        assertTrue(processor.isEnded)
        return samples.toFloatArray()
    }
}
