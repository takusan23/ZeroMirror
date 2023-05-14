package io.github.takusan23.zeromirror.media

import android.annotation.SuppressLint
import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 内部音声をPCMで取り出して、AAC / Opusでエンコードする
 *
 * ファイル保存は別クラスに譲渡している
 *
 * @param mediaProjection [MediaProjection]、内部音声を収録するのに使います。
 */
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioEncoder(mediaProjection: MediaProjection) {

    /** 音声エンコーダー、MediaCodecをラップした */
    private val audioEncoder = AudioEncoder()

    /** 内部音声がとれる */
    private var audioRecord: AudioRecord? = null

    init {
        // 内部音声取るのに使う
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection).apply {
            addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            addMatchingUsage(AudioAttributes.USAGE_GAME)
            addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        }.build()
        val audioFormat = AudioFormat.Builder().apply {
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(48_000)
            setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        }.build()
        audioRecord = AudioRecord.Builder().apply {
            setAudioPlaybackCaptureConfig(playbackConfig)
            setAudioFormat(audioFormat)
        }.build()
        audioRecord!!.startRecording()
    }

    /**
     * エンコーダーを初期化する
     *
     * @param sampleRate サンプリングレート
     * @param channelCount チャンネル数
     * @param bitRate ビットレート
     * @param isOpus コーデックにOpusを利用する場合はtrue。動画のコーデックにVP9を利用している場合は必須
     */
    fun prepareEncoder(
        sampleRate: Int = 48_000,
        channelCount: Int = 2,
        bitRate: Int = 192_000,
        isOpus: Boolean = false,
    ) = audioEncoder.prepareEncoder(sampleRate, channelCount, bitRate, isOpus)

    /**
     * エンコーダーを起動する。
     * 内部音声を取り出して AAC / Opus にエンコードする、エンコード中はずっと一時停止します
     *
     * @param onOutputBufferAvailable エンコードされたデータが流れてきます
     * @param onOutputFormatAvailable エンコード後のMediaFormatが入手できる
     */
    suspend fun start(
        onOutputBufferAvailable: suspend (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: suspend (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.Default) {
        // エンコードする
        audioEncoder.startAudioEncode(
            onRecordInput = { bytes ->
                // PCM音声を取り出しエンコする
                return@startAudioEncode audioRecord!!.read(bytes, 0, bytes.size)
            },
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                // エンコードされたデータが来る
                // 書き込む...
                // なんか例外が出る、コンテナファイルを切り替えてるせいなのかもしれない
                // java.lang.IllegalStateException: writeSampleData returned an error
                try {
                    onOutputBufferAvailable(byteBuffer, bufferInfo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onOutputFormatAvailable = onOutputFormatAvailable
        )
    }

    /**
     * このエンコーダー内部で持っている時間をリセットします
     * 次の動画ファイルに切り替えた際に呼び出す
     */
    fun resetInternalTime() {
        audioEncoder.resetInternalTime()
    }

    /** 終了時に呼ぶこと、いいね？ */
    fun release() {
        audioEncoder.release()
        audioRecord?.stop()
        audioRecord?.release()
    }

}