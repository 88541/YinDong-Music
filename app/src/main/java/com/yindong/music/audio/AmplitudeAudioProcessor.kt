package com.yindong.music.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 实时音频振幅处理器 - 从ExoPlayer音频流中提取音量数据
 * 用于驱动封面跳动等视觉效果
 */
@UnstableApi
class AmplitudeAudioProcessor : AudioProcessor {

    private var inputAudioFormat: AudioFormat? = null
    private var outputAudioFormat: AudioFormat? = null
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var isEnded = false

    // 回调接口，用于传递计算出的振幅
    var onAmplitudeUpdate: ((Float) -> Unit)? = null
    
    // 播放状态标记（由主线程更新，音频处理线程只读）
    @Volatile
    var isPlayerPlaying: Boolean = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        outputBuffer = EMPTY_BUFFER
        inputBuffer = EMPTY_BUFFER
        isEnded = false
        Log.d("AmplitudeAudioProcessor", "Configured: encoding=${inputAudioFormat.encoding}, sampleRate=${inputAudioFormat.sampleRate}, bytesPerFrame=${inputAudioFormat.bytesPerFrame}")
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (isEnded) {
            return
        }

        val size = inputBuffer.remaining()
        if (size == 0) {
            return
        }

        // 复制数据用于振幅计算（不改变原始inputBuffer的位置）
        val inputCopy = ByteArray(size)
        inputBuffer.duplicate().get(inputCopy)

        // 计算振幅
        calculateAmplitude(inputCopy)

        // 将数据传递给输出缓冲区
        this.inputBuffer = inputBuffer
    }

    private fun calculateAmplitude(data: ByteArray) {
        val format = inputAudioFormat ?: return

        try {
            var sumSquares = 0.0
            var sampleCount = 0

            when (format.encoding) {
                C.ENCODING_PCM_16BIT -> {
                    for (i in 0 until data.size - 1 step 2) {
                        val sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toShort().toInt()
                        val normalized = sample / 32768.0
                        sumSquares += normalized * normalized
                        sampleCount++
                    }
                }
                C.ENCODING_PCM_FLOAT -> {
                    val buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder())
                    while (buffer.hasRemaining()) {
                        val sample = buffer.float.toDouble()
                        sumSquares += sample * sample
                        sampleCount++
                    }
                }
                else -> return
            }

            if (sampleCount > 0) {
                val rms = kotlin.math.sqrt(sumSquares / sampleCount)

                // 映射到缩放比例 (1.0 ~ 1.15) - 增强灵敏度
                val amplitude = when {
                    rms > 0.20 -> 1.10f + (rms - 0.20).toFloat() * 0.25f
                    rms > 0.10 -> 1.05f + (rms - 0.10).toFloat() * 0.5f
                    rms > 0.05 -> 1.02f + (rms - 0.05).toFloat() * 0.6f
                    rms > 0.01 -> 1.005f + (rms - 0.01).toFloat() * 0.625f
                    else -> 1.0f + rms.toFloat() * 0.5f
                }.coerceIn(1.0f, 1.15f)

                // 只在播放时回调振幅
                if (isPlayerPlaying) {
                    onAmplitudeUpdate?.invoke(amplitude)
                }
            }
        } catch (e: Exception) {
            Log.e("AmplitudeAudioProcessor", "Error calculating amplitude", e)
        }
    }

    override fun queueEndOfStream() {
        isEnded = true
        inputBuffer = EMPTY_BUFFER
    }

    override fun getOutput(): ByteBuffer {
        return if (inputBuffer.hasRemaining()) {
            inputBuffer
        } else {
            EMPTY_BUFFER
        }
    }

    override fun isEnded(): Boolean = isEnded && !inputBuffer.hasRemaining()

    override fun flush() {
        inputBuffer = EMPTY_BUFFER
        outputBuffer = EMPTY_BUFFER
        isEnded = false
    }

    override fun reset() {
        inputAudioFormat = null
        outputAudioFormat = null
        inputBuffer = EMPTY_BUFFER
        outputBuffer = EMPTY_BUFFER
        isEnded = false
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}
