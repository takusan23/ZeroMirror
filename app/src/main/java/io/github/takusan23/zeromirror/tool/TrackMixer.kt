package io.github.takusan23.zeromirror.tool

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 映像ファイルと音声ファイルを一つにファイルにする
 */
object TrackMixer {

    /**
     * 音声と映像を一つにする
     *
     * @param mergeFileList 音声と映像のファイルパスを配列に入れて渡して
     * @param resultFile 書き込み先ファイル
     */
    @SuppressLint("WrongConstant")
    suspend fun startMix(mergeFileList: List<String>, resultFile: File) = withContext(Dispatchers.Default) {
        // これから書き込むファイル
        val mergeMediaMuxer = MediaMuxer(resultFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // それぞれ取り出してMediaMuxerへ書き込む
        val trackIndexToExtractorPairList = mergeFileList
            .map { path -> MediaExtractor().apply { setDataSource(path) } }
            .filter { mediaExtractor -> mediaExtractor.trackCount > 0 } // 1以上のみ、何故かない場合があるので
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
        // トラック数が1以上のみでフィルターをかけたので空の場合がある
        if (trackIndexToExtractorPairList.isEmpty()) {
            mergeMediaMuxer.release()
            return@withContext
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
    }
}