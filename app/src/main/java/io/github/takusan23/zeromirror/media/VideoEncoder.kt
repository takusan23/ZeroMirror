package io.github.takusan23.zeromirror.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 動画エンコーダー
 * MediaCodecを使いやすくしただけ
 *
 * VP9の場合は画面解像度が厳しい？
 * 1920x1080 1280x720 とかなら問題ないけど、ディスプレイの画面解像度を入れると例外を吐く？
 *
 * 入力Surface から H.264 / VP9 をエンコードする。
 */
class VideoEncoder {

    /** MediaCodec エンコーダー */
    private var mediaCodec: MediaCodec? = null

    /** MediaCodecの入力Surface */
    private var inputSurface: Surface? = null

    /**
     * Surface入力のMediaCodecの場合、 presentationTimeUs の値が System.nanoTime を足した値になっているため、その分を引くため
     * 引かないと音声エンコーダーと合わなくなってしまう。
     */
    private var startUs = 0L

    /**
     * エンコーダーを初期化する
     *
     * @param videoWidth 動画の幅
     * @param videoHeight 動画の高さ
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param iFrameInterval キーフレーム生成間隔
     * @param isVp9 VP9コーデックを利用する場合はtrue
     */
    fun prepareEncoder(
        videoWidth: Int,
        videoHeight: Int,
        bitRate: Int,
        frameRate: Int,
        iFrameInterval: Int = 1,
        isVp9: Boolean = false,
    ) {
        // コーデックの選択
        val codec = if (isVp9) MediaFormat.MIMETYPE_VIDEO_VP9 else MediaFormat.MIMETYPE_VIDEO_AVC
        val videoEncodeFormat = MediaFormat.createVideoFormat(codec, videoWidth, videoHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
        // エンコーダー用意
        mediaCodec = MediaCodec.createEncoderByType(codec).apply {
            configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    /**
     * 入力で使うSurfaceを用意する。
     * [prepareEncoder]の後に呼ばないとだめ。
     *
     * @return 入力で使うSurface
     */
    fun createInputSurface() = mediaCodec!!.createInputSurface().apply {
        inputSurface = this
    }

    /**
     * エンコーダーを開始する。同期モードを使うのでコルーチンを使います（スレッドでも良いけど）
     *
     * @param onOutputBufferAvailable エンコードされたデータが流れてきます
     * @param onOutputFormatAvailable エンコード後のMediaFormatが入手できる
     */
    suspend fun startVideoEncode(
        onOutputBufferAvailable: suspend (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: suspend (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.Default) {
        // 多分使い回す
        val bufferInfo = MediaCodec.BufferInfo()
        mediaCodec?.start()
        startUs = System.nanoTime() / 1000L

        try {
            while (isActive) {
                // もし -1 が返ってくれば configure() が間違ってる
                val outputBufferId = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId >= 0) {
                    val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferId)!!
                    if (bufferInfo.size > 1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            // BufferInfoを作り直す
                            // BufferInfo内にある presentationTimeUs の値がすでに謎の値 ( System#nanoTime ) で足した状態で始まる
                            // Surface入力にしているので、こちらが制御することはできない
                            // そのせいで、音声エンコーダーの presentationTimeUs と値が合わなくなる
                            // なので音声エンコーダーと歩調を合わせるため作り直す
                            val fixBufferInfo = MediaCodec.BufferInfo().apply {
                                set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs - startUs, bufferInfo.flags)
                            }
                            if (fixBufferInfo.presentationTimeUs > 0) {
                                // ファイルに書き込む...
                                onOutputBufferAvailable(outputBuffer, fixBufferInfo)
                            }
                        }
                    }
                    // 返却
                    mediaCodec!!.releaseOutputBuffer(outputBufferId, false)
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // 多分映像データより先に呼ばれる
                    // MediaMuxerへ映像トラックを追加するのはこのタイミングで行う
                    // このタイミングでやると固有のパラメーターがセットされたMediaFormatが手に入る(csd-0 とか)
                    // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
                    onOutputFormatAvailable(mediaCodec!!.outputFormat)
                }
            }
        } catch (e: Exception) {
            // なぜか例外を吐くので
            // java.lang.IllegalStateException
            // at android.media.MediaCodec.native_dequeueOutputBuffer(Native Method)
            e.printStackTrace()
        }
    }

    /**
     * このエンコーダー内部で持っている時間をリセットします
     * 次の動画ファイルに切り替えた際に呼び出す
     */
    fun resetInternalTime() {
        startUs = System.nanoTime() / 1000L
    }

    /** リソースを開放する */
    fun release() {
        try {
            inputSurface?.release()
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