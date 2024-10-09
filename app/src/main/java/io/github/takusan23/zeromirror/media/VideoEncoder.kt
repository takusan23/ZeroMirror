package io.github.takusan23.zeromirror.media

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import io.github.takusan23.zeromirror.media.opengl.InputSurface
import io.github.takusan23.zeromirror.media.opengl.TextureRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
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
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class VideoEncoder {

    /** MediaCodec エンコーダー */
    private var mediaCodec: MediaCodec? = null

    /**
     * Surface入力のMediaCodecの場合、 presentationTimeUs の値が System.nanoTime を足した値になっているため、その分を引くため
     * 引かないと音声エンコーダーと合わなくなってしまう。
     */
    private var startUs = 0L

    /**
     * OpenGL はスレッドでコンテキストを識別するので、OpenGL 関連はこの openGlRelatedDispatcher から呼び出す。
     * どういうことかと言うと、OpenGL は makeCurrent したスレッド以外で、OpenGL の関数を呼び出してはいけない。
     * （makeCurrent したスレッドのみ swapBuffers 等できる）。
     *
     * 独自 Dispatcher を作ることで、処理するスレッドを指定できたりする。
     */
    private val openGlRelatedDispatcher = newSingleThreadContext("ZeroMirrorOpenGlRelatedDispatcher")

    /** OpenGL でテクスチャとして使える Surface */
    private var inputOpenGlSurface: InputSurface? = null

    /**
     * MediaCodec への入力で使う Surface を用意する。
     * OpenGL で描画された後エンコーダーに行く。
     * [prepareEncoder]の後に呼ばないとだめ。
     *
     * @return 入力で使う Surface
     */
    var drawSurface: Surface? = null
        private set

    /** 映像の代わりに代替画像を表示する場合は true */
    var isDrawAltImage = false

    /**
     * エンコーダーを初期化する
     *
     * @param videoWidth 動画の幅
     * @param videoHeight 動画の高さ
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param iFrameInterval キーフレーム生成間隔
     * @param codecName [MediaFormat.MIMETYPE_VIDEO_VP9]や[MediaFormat.MIMETYPE_VIDEO_AVC]など
     * @param altImageBitmap [VideoEncoder.isDrawAltImage]で表示する際の画像
     */
    suspend fun prepareEncoder(
        videoWidth: Int,
        videoHeight: Int,
        bitRate: Int,
        frameRate: Int,
        iFrameInterval: Int = 1,
        codecName: String,
        altImageBitmap: Bitmap
    ) = withContext(openGlRelatedDispatcher) { // OpenGL は makeCurrent したスレッドでしか swapBuffers 等できないので withContext でスレッドを指定する
        val videoEncodeFormat = MediaFormat.createVideoFormat(codecName, videoWidth, videoHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }
        // エンコーダー用意
        mediaCodec = MediaCodec.createEncoderByType(codecName).apply {
            configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        // エンコーダー での入力前に OpenGL を経由する
        inputOpenGlSurface = InputSurface(mediaCodec!!.createInputSurface(), TextureRenderer())
        inputOpenGlSurface?.makeCurrent()
        inputOpenGlSurface?.createRender(videoWidth, videoHeight)
        inputOpenGlSurface?.setAltImageTexture(altImageBitmap)

        // OpenGL で使うテクスチャに描画する Surface
        drawSurface = inputOpenGlSurface?.drawSurface
    }

    /**
     * エンコーダーを開始して、OpenGL での描画を開始する。
     * キャンセルされるまで一時停止し続けます。
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

        // OpenGL のメインループ？
        // OpenGL はコンテキストの識別にスレッドを使うので、OpenGL 用スレッド（Dispatcher）を指定
        val openGlRendererJob = launch(openGlRelatedDispatcher) {
            while (isActive) {
                try {
                    if (isDrawAltImage) {
                        // 代替画像を表示
                        inputOpenGlSurface?.drawAltImage()
                        inputOpenGlSurface?.swapBuffers()
                        delay(16) // 60fps が 16ミリ秒 らしいので適当に待つ
                    } else {
                        // 映像フレームが来ていれば OpenGL のテクスチャを更新
                        val isNewFrameAvailable = inputOpenGlSurface?.awaitIsNewFrameAvailable()
                        // 描画する
                        if (isNewFrameAvailable == true) {
                            inputOpenGlSurface?.makeCurrent()
                            inputOpenGlSurface?.updateTexImage()
                            inputOpenGlSurface?.drawImage()
                            inputOpenGlSurface?.swapBuffers()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // エンコーダーのループ
        val encoderJob = launch {
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

        // キャンセルされるまでコルーチンを一時停止
        openGlRendererJob.join()
        encoderJob.join()
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
            inputOpenGlSurface?.release()
            drawSurface?.release()
            mediaCodec?.stop()
            mediaCodec?.release()
            openGlRelatedDispatcher.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 10_000L

    }
}