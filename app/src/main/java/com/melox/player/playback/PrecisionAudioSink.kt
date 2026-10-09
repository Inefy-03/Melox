package com.melox.player.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener

@UnstableApi
internal class PrecisionRenderersFactory(
    context: Context,
    private val highPrecisionEnabled: Boolean,
    private val onFloatOutputChanged: (Boolean) -> Unit,
) : DefaultRenderersFactory(context) {
    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) = Unit

    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) = Unit

    override fun buildCameraMotionRenderers(
        context: Context,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) = Unit

    override fun buildImageRenderers(context: Context, out: ArrayList<Renderer>) = Unit

    override fun createSecondaryRenderer(
        renderer: Renderer,
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput,
    ): Renderer? = null

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink = object : ForwardingAudioSink(
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(highPrecisionEnabled)
            .setEnableAudioOutputPlaybackParameters(false)
            .build(),
    ) {
        override fun setListener(listener: AudioSink.Listener) {
            super.setListener(object : AudioSink.Listener by listener {
                override fun onAudioTrackInitialized(config: AudioSink.AudioTrackConfig) {
                    listener.onAudioTrackInitialized(config)
                    onFloatOutputChanged(highPrecisionEnabled && config.encoding == C.ENCODING_PCM_FLOAT)
                }

            })
        }
    }
}
