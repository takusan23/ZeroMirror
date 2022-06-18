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

    /** サンプリングレート、エンコードの際に使うので */
    private var sampleRate: Int = 44_100

    /**
     * エンコーダーを初期化する
     *
     * @param sampleRate サンプリングレート
     * @param channelCount チャンネル数
     * @param bitRate ビットレート
     * @param isOpus コーデックにOpusを利用する場合はtrue。動画のコーデックにVP9を利用している場合は必須
     */
    fun prepareEncoder(
        sampleRate: Int = 44_100,
        channelCount: Int = 1,
        bitRate: Int = 192_000,
        isOpus: Boolean = false,
    ) {
        this@AudioEncoder.sampleRate = sampleRate
        val codec = if (isOpus) MediaFormat.MIMETYPE_AUDIO_OPUS else MediaFormat.MIMETYPE_AUDIO_AAC
        val audioEncodeFormat = MediaFormat.createAudioFormat(codec, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
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

        // 経過時間の計算、AOSPの内部音声コードをそのままパクります
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/SystemUI/src/com/android/systemui/screenrecord/ScreenInternalAudioRecorder.java;l=252;drc=a9f632187cec8873f3f2429022a25b867b2b7d4b?q=internalaudio
        var mTotalBytes = 0
        var mPresentationTime = 0L

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
                        mediaCodec!!.queueInputBuffer(inputBufferId, 0, readByteSize, mPresentationTime, 0)
                        mTotalBytes += readByteSize
                        mPresentationTime = 1000000L * (mTotalBytes / 2) / sampleRate
                    }
                }
                // 出力
                val outputBufferId = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId >= 0) {
                    val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferId)!!
                    if (bufferInfo.size > 1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            // ファイルに書き込む...
                            onOutputBufferAvailable(outputBuffer, bufferInfo)
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