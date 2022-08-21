package io.github.takusan23.zeromirror.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 音声エンコーダー
 * MediaCodecを使いやすくしただけ
 *
 * 内部音声が生のまま（PCM）送られてくるので、 AAC / Opus にエンコードする。
 */
class AudioEncoder {

    /** MediaCodec エンコーダー */
    private var mediaCodec: MediaCodec? = null

    /** 動画を切り替えた際に presentationTimeUs を0から始めたいため、 totalBytes とかを0にしても効果がなかった... */
    private var prevPresentationTimeUs = 0L

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
        channelCount: Int = 1,
        bitRate: Int = 192_000,
        isOpus: Boolean = false,
    ) {
        val codec = if (isOpus) MediaFormat.MIMETYPE_AUDIO_OPUS else MediaFormat.MIMETYPE_AUDIO_AAC
        val audioEncodeFormat = MediaFormat.createAudioFormat(codec, 48_000, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
        }
        // エンコーダー用意
        mediaCodec = MediaCodec.createEncoderByType(codec).apply {
            configure(audioEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    /**
     * エンコーダーを開始する。同期モードを使うのでコルーチンを使います（スレッドでも良いけど）
     *
     * @param onRecordInput ByteArrayを渡すので、音声データを入れて、サイズを返してください
     * @param onOutputBufferAvailable エンコードされたデータが流れてきます
     * @param onOutputFormatAvailable エンコード後のMediaFormatが入手できる
     */
    suspend fun startAudioEncode(
        onRecordInput: (ByteArray) -> Int,
        onOutputBufferAvailable: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.Default) {
        val bufferInfo = MediaCodec.BufferInfo()
        mediaCodec!!.start()

        // エンコーダー開始時間
        val encoderStartTimeUs = System.nanoTime()
        // 経過時間を増加させていく
        var prevPresentationTimeUs = 0L

        try {
            while (isActive) {
                // もし -1 が返ってくれば configure() が間違ってる
                val inputBufferId = mediaCodec!!.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferId >= 0) {
                    // AudioRecodeのデータをこの中に入れる
                    val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferId)!!
                    val capacity = inputBuffer.capacity()
                    // サイズに合わせて作成
                    val byteArray = ByteArray(capacity)
                    // byteArrayへデータを入れてもらう
                    val readByteSize = onRecordInput(byteArray)
                    if (readByteSize > 0) {
                        // 書き込む。書き込んだデータは[onOutputBufferAvailable]で受け取れる
                        inputBuffer.put(byteArray, 0, readByteSize)
                        mediaCodec!!.queueInputBuffer(inputBufferId, 0, readByteSize, prevPresentationTimeUs, 0)
                        prevPresentationTimeUs = System.nanoTime() - encoderStartTimeUs
                    }
                }
                // 出力
                val outputBufferId = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId >= 0) {
                    val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferId)!!
                    if (bufferInfo.size > 1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            // BufferInfoを作り直す
                            // BufferInfo内にある presentationTimeUs の値が多分MediaCodecスタート時から常に増え続けている
                            // ファイルを作り直した際は0から始めてほしいため作り直す必要がある
                            val fixBufferInfo = MediaCodec.BufferInfo().apply {
                                // 動画切り替え時は0を入れる
                                if (this@AudioEncoder.prevPresentationTimeUs == 0L) {
                                    this@AudioEncoder.prevPresentationTimeUs = bufferInfo.presentationTimeUs
                                    set(bufferInfo.offset, bufferInfo.size, 0, bufferInfo.flags)
                                } else {
                                    set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs - this@AudioEncoder.prevPresentationTimeUs, bufferInfo.flags)
                                }
                            }
                            // ファイルに書き込む...
                            onOutputBufferAvailable(outputBuffer, fixBufferInfo)
                        }
                    }
                    // 返却
                    mediaCodec!!.releaseOutputBuffer(outputBufferId, false)
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // MediaFormat、MediaMuxerに入れるときに使うやつ
                    // たぶんこっちのほうが先に呼ばれる
                    onOutputFormatAvailable(mediaCodec!!.outputFormat)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * このエンコーダー内部で持っている時間をリセットします
     * 次の動画ファイルに切り替えた際に呼び出す
     */
    fun resetInternalTime() {
        prevPresentationTimeUs = 0L
    }

    /** リソースを開放する */
    fun release() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 10_000L

    }
}