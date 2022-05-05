package io.github.takusan23.zeromirror.media

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.zeromirror.tool.UniqueFileTool
import java.io.File
import java.nio.ByteBuffer

/**
 * 映像動画、音声動画
 *
 * @param uniqueFile ファイルを生成するクラス
 */
class MediaContainer(context: Context, private val uniqueFile: UniqueFileTool) {

    private val audioTempFile = File(context.getExternalFilesDir(null), "temp_audio.aac")
    private val videoTempFile = File(context.getExternalFilesDir(null), "temp_video.mp4")

    private var audioMediaMuxer: MediaMuxer? = null
    private var videoMediaMuxer: MediaMuxer? = null

    /** 映像を入れるインデックス番号 */
    private var videoTrackIndex = -1

    /** 音声を入れるインデックス番号 */
    private var audioTrackIndex = -1

    /** 一個前の映像データ、多分一つ前のデータを送信するようにしないとちゃんと再生できない？ */
    private var prevVideoFile: File? = null

    var isAudioStart = false
        private set
    var isVideoStart = false
        private set

    init {
        // 最初のファイルを作る
        createContainer()
    }

    /**
     * 映像データを書き込む用意をする（コンテナに映像トラックを追加する）
     *
     * @param mediaFormat [MediaCodec.getOutputFormat]を入れればいいと思うよ
     */
    fun setVideoFormat(mediaFormat: MediaFormat) {
        // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
        try {
            videoTrackIndex = videoMediaMuxer!!.addTrack(mediaFormat)
            videoMediaMuxer!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isVideoStart = true
    }

    /**
     * 音声データを書き込む用意をする（コンテナに音声トラックを追加する）
     *
     * @param mediaFormat [MediaCodec.getOutputFormat]を入れればいいと思うよ
     */
    fun setAudioFormat(mediaFormat: MediaFormat) {
        try {
            audioTrackIndex = audioMediaMuxer!!.addTrack(mediaFormat)
            audioMediaMuxer!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isAudioStart = true
    }

    /** エンコードされた映像データを書き込む */
    fun writeVideoData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        videoMediaMuxer!!.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
    }

    /** エンコードされた音声データを書き込む */
    fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        audioMediaMuxer!!.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
    }

    /** コンテナファイル（mp4）を作り直す */
    fun createContainer() {
        // ここで MPEG2-TS が使えればHLSで配信できた世界線もあったかも
        audioMediaMuxer = MediaMuxer(audioTempFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        videoMediaMuxer = MediaMuxer(videoTempFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    /**
     * 一個前の動画ファイルを返す
     *
     * @return 一つ前の動画ファイル、ない場合はnull
     */
    fun getPrevVideoFile() = prevVideoFile

    /** [audioTempFile]と[videoTempFile]を一つにする */
    @SuppressLint("WrongConstant")
    fun startMix(): File {
        // これから書き込むファイル
        val resultFile = uniqueFile.generateFile()
        val mergeMediaMuxer = MediaMuxer(resultFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // それぞれ取り出してMediaMuxerへ書き込む
        val trackIndexToExtractorPairList = listOf(audioTempFile.path, videoTempFile.path)
            .map { path -> MediaExtractor().apply { setDataSource(path) } }
            .filter { mediaExtractor -> mediaExtractor.trackCount > 0 } // 0以上のみ、何故かない場合があるので
            .map { mediaExtractor ->
                // トラックを取り出して、フォーマットを取得
                val trackFormat = mediaExtractor.getTrackFormat(0)
                mediaExtractor.selectTrack(0)
                // FormatとExtractorを返す
                trackFormat to mediaExtractor
            }
            .map { (format, extractor) ->
                // フォーマットをMediaMuxerに渡して、トラックを追加してもらう
                val videoTrackIndex = mergeMediaMuxer.addTrack(format)
                videoTrackIndex to extractor
            }
        mergeMediaMuxer.start()
        // 映像と音声を一つの動画ファイルに書き込んでいく
        trackIndexToExtractorPairList.forEach { (index, extractor) ->
            val byteBuffer = ByteBuffer.allocate(1024 * 4096)
            val bufferInfo = MediaCodec.BufferInfo()
            // データが無くなるまで回す
            while (true) {
                // データを読み出す
                val offset = byteBuffer.arrayOffset()
                bufferInfo.size = extractor.readSampleData(byteBuffer, offset)
                // もう無い場合
                if (bufferInfo.size < 0) break
                // 書き込む
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                mergeMediaMuxer.writeSampleData(index, byteBuffer, bufferInfo)
                // 次のデータに進める
                extractor.advance()
            }
            // あとしまつ
            extractor.release()
        }
        // あとしまつ
        mergeMediaMuxer.stop()
        mergeMediaMuxer.release()
        // audioTempFile / videoTempFile を消す
        listOf(audioTempFile.path, videoTempFile.path)
            .map { File(it) }
            .forEach { it.delete() }
        // 合成後のファイルを返す
        return resultFile
    }

    /**
     * 今のコンテナファイルのリソース開放を行う。
     * 次のファイル[createContainer]の前に行うこと。
     *
     * @return 完成した[File]
     */
    fun release(): File {
        isAudioStart = false
        isVideoStart = false
        // java.lang.IllegalStateException: Error during stop(), muxer would have stopped already
        //     at android.media.MediaMuxer.nativeStop(Native Method)
        try {
            audioMediaMuxer?.stop()
            videoMediaMuxer?.stop()
            audioMediaMuxer?.release()
            videoMediaMuxer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prevVideoFile = uniqueFile.currentFile()
        return prevVideoFile!!
    }

}