package io.github.takusan23.zeromirror.media

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 画面録画して H.264 でエンコードする
 *
 * 次のファイルにする流れ
 *
 * [stopWriteContainer] -> 音声と結合する -> [resetContainerFile] -> ふりだしにもどる
 *
 * @param parentFolder 動画保存先
 * @param displayDpi DPI
 * @param mediaProjection [MediaProjection]、内部音声を収録するのに使います。
 */
class ScreenVideoEncoder(parentFolder: File, private val displayDpi: Int, private val mediaProjection: MediaProjection) {

    /** 映像エンコーダー、MediaCodecをラップした */
    private val videoEncoder = VideoEncoder()

    /** mp4ファイルにするやつ */
    private var mediaMuxer: MediaMuxer? = null

    /** mp4ファイル */
    private val mp4File = File(parentFolder, VIDEO_FILE_NAME)

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

    init {
        resetContainerFile()
    }

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
                    mediaMuxer!!.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
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
     * @return ファイルパス
     */
    fun stopWriteContainer(): String {
        isWritable = false
        mediaMuxer!!.stop()
        videoTrackIndex = INIT_INDEX_NUMBER
        return mp4File.path
    }

    /** コンテナファイルを初期化する */
    fun resetContainerFile() {
        // 前回のファイルを消してから
        mp4File.delete()
        mediaMuxer = MediaMuxer(mp4File.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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
        /** 映像ファイル名 */
        private const val VIDEO_FILE_NAME = "temp_video.mp4"

        /** インデックス番号初期値 */
        private const val INIT_INDEX_NUMBER = -1
    }

}