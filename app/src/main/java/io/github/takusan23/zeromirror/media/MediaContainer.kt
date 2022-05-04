package io.github.takusan23.zeromirror.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.zeromirror.tool.UniqueFileTool
import java.io.File
import java.nio.ByteBuffer

/**
 * [MediaMuxer]をラップしただけ
 *
 * @param uniqueFile ファイルを生成するクラス
 */
class MediaContainer(private val uniqueFile: UniqueFileTool) {

    /** コンテナフォーマットへ格納するやつ */
    private var mediaMuxer: MediaMuxer? = null

    /** 映像を入れるインデックス番号 */
    private var videoTrackIndex = -1

    init {
        // 最初のファイルを作る
        createContainer()
    }

    /**
     * 映像データを書き込む用意をする
     *
     * @param mediaFormat [MediaCodec.getOutputFormat]を入れればいいと思うよ
     */
    fun setVideoFormat(mediaFormat: MediaFormat) {
        // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
        videoTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
        mediaMuxer!!.start()
    }

    /** エンコードされたデータ（H.264）を書き込む */
    fun writeVideoData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        mediaMuxer!!.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
    }

    /** コンテナファイル（mp4）を作り直す */
    fun createContainer() {
        // ここで MPEG2-TS が使えればHLSで配信できた世界線もあったかも
        mediaMuxer = MediaMuxer(uniqueFile.generateFile().path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    /**
     * 今のコンテナファイルのリソース開放を行う。
     * 次のファイル[createContainer]の前に行うこと。
     *
     * @return 完成した[File]
     */
    fun release(): File {
        mediaMuxer?.release()
        return uniqueFile.currentFile()
    }

}