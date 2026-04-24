package com.yindong.music.audio

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * 自定义RenderersFactory - 集成AmplitudeAudioProcessor到音频渲染链
 */
@UnstableApi
class AmplitudeRenderersFactory(
    context: Context,
    private val amplitudeProcessor: AmplitudeAudioProcessor
) : DefaultRenderersFactory(context) {

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        // 创建带自定义AudioProcessor的AudioSink
        val customAudioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(amplitudeProcessor))
            .build()

        // 添加MediaCodec音频渲染器，使用自定义的AudioSink
        out.add(
            MediaCodecAudioRenderer(
                context,
                mediaCodecSelector,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                customAudioSink
            )
        )

        // 添加其他扩展渲染器（FFmpeg等），但不重复添加音频渲染器
        // 注意：我们不调用super.buildAudioRenderers来避免重复创建音频渲染器
        // 如果需要视频渲染器，可以单独添加
    }
}
