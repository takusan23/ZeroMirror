package io.github.takusan23.zeromirror.media

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.view.Surface
import io.github.takusan23.zeromirror.CaptureVideoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 画面録画して H.264 でエンコードする
 *
 * 次のファイルにする流れ
 *
 * [stopWriteContainer] -> 音声と結合する -> [createContainer] -> ふりだしにもどる
 *
 * 画面録画のみを配信する機能があるためこのクラスでも[CaptureVideoManager]を使っている
 *
 * @param displayDpi DPI
 * @param mediaProjection [MediaProjection]、内部音声を収録するのに使います。
 */
class ScreenVideoEncoder(private val displayDpi: Int, private val mediaProjection: MediaProjection) {

    /** 映像エンコーダー、MediaCodecをラップした */
    private val videoEncoder = VideoEncoder()

    /** mp4ファイルにするやつ */
    private var mediaMuxer: MediaMuxer? = null

    /** mp4ファイル */
    private var h264File: File? = null

    /** 画面録画に使う */
    private var virtualDisplay: VirtualDisplay? = null

    /** エンコーダーから払い出される Surface */
    private var encoderSurface: Surface? = null

    /** 映像トラックの番号、-1はまだ追加してない */
    private var videoTrackIndex = INIT_INDEX_NUMBER

    /** MediaFormatキャッシュしておく */
    private var outputMediaFormat: MediaFormat? = null

    /** 書込み可能な状態の場合はtrue */
    private var isWritable = false

    /**
     * H.264 エンコーダーを初期化する
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
        videoEncoder.prepareEncoder(videoWidth, videoHeight, bitRate, frameRate, iFrameInterval)
        encoderSurface = videoEncoder.createInputSurface()
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "io.github.takusan23.zeromirror",
            videoWidth,
            videoHeight,
            displayDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            encoderSurface!!,
            null,
            null
        )
    }

    /**
     * エンコーダーを起動する。
     * H.264になる。すっと一時停止します。
     */
    suspend fun start() = withContext(Dispatchers.Default) {
        videoEncoder.startVideoEncode(
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                // エンコードされたデータが来る
                if (isWritable) {
                    // 書き込む...
                    // なんか例外が出る、コンテナファイルを切り替えてるせいなのかもしれない
                    // java.lang.IllegalStateException: writeSampleData returned an error
                    try {
                        mediaMuxer!!.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onOutputFormatAvailable = {
                outputMediaFormat = it
                // 初回時はここで初期化
                addTrackAndStart()
            }
        )
    }

    /**
     * mp4ファイルへの書き込みを辞める
     *
     * @return 書き込んでいたファイル、[createContainer]を呼んでない場合はnull
     */
    fun stopWriteContainer(): File {
        isWritable = false
        mediaMuxer!!.stop()
        videoTrackIndex = INIT_INDEX_NUMBER
        return h264File!!
    }

    /**
     * コンテナファイルを初期化する
     *
     * @param h264File 書き込むファイル
     */
    suspend fun createContainer(h264File: File) = withContext(Dispatchers.Default) {
        // 一応消しておく
        this@ScreenVideoEncoder.h264File = h264File
        h264File.delete()
        mediaMuxer = MediaMuxer(h264File.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // 2個目以降のファイルはここで初期化出来る
        addTrackAndStart()
    }

    /** 終了時に呼ぶ */
    fun release() {
        videoEncoder.release()
        mediaMuxer?.release()
        virtualDisplay?.release()
        encoderSurface?.release()
    }

    /** [mediaMuxer]へ[outputMediaFormat]を追加して、[MediaMuxer]をスタートする */
    private fun addTrackAndStart() {
        if (videoTrackIndex == INIT_INDEX_NUMBER && outputMediaFormat != null) {
            videoTrackIndex = mediaMuxer!!.addTrack(outputMediaFormat!!)
            mediaMuxer!!.start()
            isWritable = true
        }
    }

    companion object {
        /** インデックス番号初期値 */
        private const val INIT_INDEX_NUMBER = -1
    }

}