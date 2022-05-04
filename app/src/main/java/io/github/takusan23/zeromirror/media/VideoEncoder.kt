package io.github.takusan23.zeromirror.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 動画エンコーダー。MediaCodec何も分からん
 *
 * 入力Surface から H.264 をエンコードする。
 */
class VideoEncoder {

    /** MediaCodec エンコーダー */
    private var codec: MediaCodec? = null

    /**
     * エンコーダーを初期化する
     *
     * @param videoWidth 動画の幅
     * @param videoHeight 動画の高さ
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param iFrameInterval Iフレーム
     */
    fun prepareEncoder(
        videoWidth: Int,
        videoHeight: Int,
        bitRate: Int,
        frameRate: Int,
        iFrameInterval: Int = 10,
    ) {
        // avc が H.264 のことだと思う
        val videoEncodeFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
        // エンコーダー用意
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    /**
     * 入力で使うSurfaceを用意する。
     * [prepareEncoder]の後に呼ばないとだめ。
     *
     * @return 入力で使うSurface
     */
    fun createInputSurface() = codec!!.createInputSurface()

    /**
     * エンコーダーを開始する。同期モードを使うのでコルーチンを使います（スレッドでも良いけど）
     *
     * @param onOutputBufferAvailable H.264に圧縮されたデータが流れてきた際に呼ばれる
     * @param onOutputFormatChanged フォーマットが貰えます、MediaMuxerへセットしてください
     */
    suspend fun start(
        onOutputBufferAvailable: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatChanged: (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.IO) {
        // 多分使い回す
        val bufferInfo = MediaCodec.BufferInfo()
        // MediaFormatを渡したらtrue
        var isNotifyMediaFormat = false
        codec?.start()

        try {
            while (isActive) {
                // もし -1 が返ってくれば configure() が間違ってる
                val outputBufferId = codec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId >= 0) {
                    val outputBuffer = codec!!.getOutputBuffer(outputBufferId)!!
                    if (bufferInfo.size > 1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            // ファイルに書き込む...
                            onOutputBufferAvailable(outputBuffer, bufferInfo)
                        } else if (!isNotifyMediaFormat) {
                            // 多分映像データより先に呼ばれる
                            isNotifyMediaFormat = true
                            // MediaMuxerへ映像トラックを追加するのはこのタイミングで行う
                            // このタイミングでやると固有のパラメーターがセットされたMediaFormatが手に入る(csd-0 とか)
                            // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
                            onOutputFormatChanged(codec!!.outputFormat)
                        }
                    }
                    // 返却
                    codec!!.releaseOutputBuffer(outputBufferId, false)
                }
            }
        } catch (e: Exception) {
            // なぜか例外を吐くので
            // java.lang.IllegalStateException
            // at android.media.MediaCodec.native_dequeueOutputBuffer(Native Method)
            e.printStackTrace()
        }
    }

    /** リソースを開放する */
    fun release() {
        codec?.stop()
        codec?.release()
    }

    companion object {

        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 10_000L

    }
}