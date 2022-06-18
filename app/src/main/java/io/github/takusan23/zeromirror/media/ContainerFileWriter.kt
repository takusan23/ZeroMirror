package io.github.takusan23.zeromirror.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 *
 * エンコードされたデータをファイル（コンテナ）に書き込む
 *
 * @param includeAudio 内部音声を含める場合はtrue
 * @param isWebM WebMコンテナを利用する場合はtrue、mp4の場合はfalse
 */
class ContainerFileWriter(private val includeAudio: Boolean = false, private val isWebM: Boolean = false) {

    /** コンテナへ書き込むやつ */
    private var mediaMuxer: MediaMuxer? = null

    /** 現在のファイル */
    private var currentFile: File? = null

    /** MediaMuxer 起動中の場合はtrue */
    private var isRunning = false

    /** オーディオトラックの番号 */
    private var audioTrackIndex = INIT_INDEX_NUMBER

    /** 映像トラックの番号 */
    private var videoTrackIndex = INIT_INDEX_NUMBER

    /** オーディオのフォーマット */
    private var audioFormat: MediaFormat? = null

    /** 映像のフォーマット */
    private var videoFormat: MediaFormat? = null

    /**
     * コンテナを作成する か 作り直す
     * 作り直す場合は [stopAndRelease] を呼び出す
     *
     * @param videoPath 動画ファイルのパス
     */
    fun createContainer(videoPath: String) {
        // ファイルを作成
        val containerFormat = if (isWebM) MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM else MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        currentFile = File(videoPath)
        mediaMuxer = MediaMuxer(videoPath, containerFormat)

        // 再生成する場合はパラメーター持っているのですぐ開始する
        if (includeAudio) {
            videoFormat?.also { setVideoTrack(it) }
            audioFormat?.also { setAudioTrack(it) }
            // トラック追加ができた（つまり再生成時）はスタートさせる
            // 初回時はトラック追加後にスタートする
            startIfAddedFormat()
        } else {
            videoFormat?.also { setVideoTrack(it) }
            startIfAddedFormat()
        }
    }

    /**
     * 映像トラックを追加する
     * 情報が出揃ったら書き込みを始める
     *
     * @param mediaFormat 映像トラックの情報
     */
    fun setVideoTrack(mediaFormat: MediaFormat) {
        videoTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
        videoFormat = mediaFormat
        startIfAddedFormat()
    }

    /**
     * 音声トラックを追加する
     * 情報が出揃ったら書き込みを始める
     *
     * @param mediaFormat 音声トラックの情報
     */
    fun setAudioTrack(mediaFormat: MediaFormat) {
        audioTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
        audioFormat = mediaFormat
        startIfAddedFormat()
    }

    /**
     * 映像データを書き込む
     *
     * @param byteBuf MediaCodec からもらえるやつ
     * @param bufferInfo MediaCodec からもらえるやつ
     */
    fun writeVideo(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (isRunning) {
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
        if (isRunning) {
            mediaMuxer?.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
        }
    }

    /**
     * 書き込みを終了し、動画ファイルを完成させる
     * その他リリース開放もやる
     *
     * @return 書き込んでいたファイル
     */
    fun stopAndRelease(): File {
        isRunning = false
        videoTrackIndex = -1
        audioTrackIndex = -1
        mediaMuxer?.stop()
        mediaMuxer?.release()
        return currentFile!!
    }

    /** フォーマットの登録が済んでいるかを確認して、確認OKならMediaMuxerを開始する */
    private fun startIfAddedFormat() {
        println("videoTrackIndex = "+videoTrackIndex)
        println("audioTrackIndex = "+audioTrackIndex)
        if (includeAudio) {
            // 音声を含める場合
            if (videoTrackIndex != INIT_INDEX_NUMBER && audioTrackIndex != INIT_INDEX_NUMBER) {
                mediaMuxer!!.start()
                isRunning = true
            }
        } else {
            // 映像のみ
            if (videoTrackIndex != INIT_INDEX_NUMBER) {
                mediaMuxer!!.start()
                isRunning = true
            }
        }
    }

    companion object {
        /** インデックス番号初期値 */
        private const val INIT_INDEX_NUMBER = -1
    }


}