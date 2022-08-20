package io.github.takusan23.zeromirror.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * エンコードされたデータをファイル（コンテナ）に書き込む
 *
 * @param includeAudio 内部音声を含める場合はtrue
 * @param isWebM WebMコンテナを利用する場合はtrue、mp4の場合はfalse
 * @param isMp4FastStart mp4 の moovブロック を先頭に移動するには true 、ストリーミング可能になります
 * @param tempFile 一時ファイル。詳しくは [currentFile]
 */
class ContainerFileWriter(
    private val includeAudio: Boolean = false,
    private val isWebM: Boolean = false,
    private val isMp4FastStart: Boolean = true,
    private val tempFile: File,
) {
    /** コンテナへ書き込むやつ */
    private var mediaMuxer: MediaMuxer? = null

    /**
     * 現在のファイル、出力ファイル
     * mp4の場合は moovブロック を先頭に持ってくる関係で [stopWriter] するまで書き込まれません。
     */
    private var currentFile: File? = null

    /** MediaMuxer 起動中の場合はtrue */
    private var isRunning = false

    /** オーディオトラックの番号 */
    private var audioTrackIndex = INVALID_INDEX_NUMBER

    /** 映像トラックの番号 */
    private var videoTrackIndex = INVALID_INDEX_NUMBER

    /** オーディオのフォーマット */
    private var audioFormat: MediaFormat? = null

    /** 映像のフォーマット */
    private var videoFormat: MediaFormat? = null

    /** 書き込み可能かどうか */
    private var isWritable = true

    /** MediaMuxerで書き込んでいる動画ファイルの [InputStream]、その都度開くより良さそう */
    private var inputStream: InputStream? = null

    /** 初期化セグメントを作成したか */
    var hasInitSegment = false

    /** コンテナフォーマット / MediaMuxer を再生成する */
    suspend fun resetOrCreateContainerFile() = withContext(Dispatchers.IO) {
        tempFile.delete()
        hasInitSegment = false
        mediaMuxer = MediaMuxer(tempFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM)
        inputStream = tempFile.inputStream()
        // 再生成する場合はパラメーター持っているので入れておく
        videoFormat?.also { setVideoTrack(it) }
        audioFormat?.also { setAudioTrack(it) }
    }

    /**
     * MediaMuxerで書き込み中のファイルから、WebMの初期化セグメントの部分を切り出す。
     * 初期化セグメントの位置は Clusterタグ が始まる前まで
     *
     * @param filePath 書き込み先ファイルのファイルパス
     * @return ファイル
     */
    suspend fun sliceInitSegmentFile(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { file ->
            // 書き込まれないようにしておく
            isWritable = false
            val readRecordFile = tempFile.readBytes()
            // 初期化セグメントの範囲を探す
            // きれいな実装じゃない...
            var initSegmentLength = -1
            for (i in readRecordFile.indices) {
                if (
                    readRecordFile[i] == 0x1F.toByte()
                    && readRecordFile[i + 1] == 0x43.toByte()
                    && readRecordFile[i + 2] == 0xB6.toByte()
                    && readRecordFile[i + 3] == 0x75.toByte()
                ) {
                    initSegmentLength = i
                    break
                }
            }
            if (initSegmentLength == -1) {
                return@withContext
            }
            // 初期化セグメントを書き込む
            file.writeBytes(readRecordFile.copyOfRange(0, initSegmentLength))
            // 読み出した位置分スキップ
            inputStream?.skip(initSegmentLength.toLong())
            // 書き込み可にする
            isWritable = true
            hasInitSegment = true
        }
    }

    /**
     * MediaMuxerで書き込み中のファイルから、前回切り出した範囲から今書き込み中の範囲までを切り出す。
     * 前回切り出した範囲は[sliceInitSegmentFile]も対象。
     *
     * @param filePath 書き込み先ファイルのファイルパス
     * @return ファイル
     */
    suspend fun sliceSegmentFile(filePath: String) = withContext(Dispatchers.IO) {
        println(filePath)
        File(filePath).also { file ->
            // 書き込まれないようにしておく
            isWritable = false
            // 前回までの範囲をスキップして切り出す
            inputStream?.also { stream ->
                val byteArray = ByteArray(stream.available())
                println("書き込みサイズ = ${byteArray.size}")
                stream.read(byteArray)
                file.writeBytes(byteArray)
            }
            // 書き込み可にする
            isWritable = true
        }
    }

    /**
     * 映像トラックを追加する
     *
     * @param mediaFormat 映像トラックの情報
     */
    fun setVideoTrack(mediaFormat: MediaFormat) {
        // MediaMuxer 開始前のみ追加できるので
        if (!isRunning) {
            videoTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
        }
        videoFormat = mediaFormat
    }

    /**
     * 音声トラックを追加する
     *
     * @param mediaFormat 音声トラックの情報
     */
    fun setAudioTrack(mediaFormat: MediaFormat) {
        // MediaMuxer 開始前のみ追加できるので
        if (!isRunning) {
            audioTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
        }
        audioFormat = mediaFormat
    }

    /**
     * 書き込みを開始させる。
     * これ以降のフォーマット登録を受け付けないので、ファイル再生成まで登録されません [resetOrCreateContainerFile]
     */
    fun start() {
        if (!isRunning) {
            mediaMuxer!!.start()
            isRunning = true
        }
    }

    /**
     * 映像データを書き込む
     *
     * @param byteBuf MediaCodec からもらえるやつ
     * @param bufferInfo MediaCodec からもらえるやつ
     */
    fun writeVideo(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isRunning && videoTrackIndex != INVALID_INDEX_NUMBER && isWritable) {
            mediaMuxer?.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
        }
    }

    /**
     * 音声データを書き込む
     *
     * @param byteBuf MediaCodec からもらえるやつ
     * @param bufferInfo MediaCodec からもらえるやつ
     */
    fun writeAudio(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isRunning && audioTrackIndex != INVALID_INDEX_NUMBER && isWritable) {
            mediaMuxer?.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
        }
    }

    /** リソース開放 */
    fun release() {
        // 起動していなければ終了もさせない
        if (isRunning) {
            mediaMuxer?.stop()
            mediaMuxer?.release()
        }
        inputStream?.close()
        isRunning = false
        videoTrackIndex = INVALID_INDEX_NUMBER
        audioTrackIndex = INVALID_INDEX_NUMBER
    }

    companion object {
        /** インデックス番号初期値、無効な値 */
        private const val INVALID_INDEX_NUMBER = -1

        /** mp4で moovブロック 移動前のファイル名 */
        const val TEMP_VIDEO_FILENAME = "temp_video_file"
    }

}