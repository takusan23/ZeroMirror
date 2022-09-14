package io.github.takusan23.zeromirror.websocket

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.zeromirror.media.QtFastStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * WebSocketで配信するための MP4 を作成します。
 * エンコードされたデータをファイル（コンテナ）に書き込む。
 *
 * @param tempFile 一時ファイル。詳しくは [currentFile]
 */
class WSContainerWriter(private val tempFile: File) {
    /** コンテナへ書き込むやつ */
    private var mediaMuxer: MediaMuxer? = null

    /**
     * 現在のファイル、出力ファイル
     * mp4の場合は moovブロック を先頭に持ってくる関係で [stopAndRelease] するまで書き込まれません。
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

    /**
     * コンテナを作成する か 作り直す
     *
     * @param videoPath 動画ファイルのパス
     */
    fun createContainer(videoPath: String) {
        // ファイルを作成
        // mp4 で faststart する場合は moovブロック を先頭に持ってくる関係で MediaMuxer へ渡すFileは tempFile です
        currentFile = File(videoPath)
        mediaMuxer = MediaMuxer(tempFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // 再生成する場合はパラメーター持っているので入れておく
        videoFormat?.also { setVideoTrack(it) }
        audioFormat?.also { setAudioTrack(it) }
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
     * これ以降のフォーマット登録を受け付けないので、ファイル再生成まで登録されません [createContainer]
     */
    fun start() {
        if (!isRunning) {
            mediaMuxer?.start()
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
        if (isRunning && videoTrackIndex != INVALID_INDEX_NUMBER) {
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
        if (isRunning && audioTrackIndex != INVALID_INDEX_NUMBER) {
            mediaMuxer?.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
        }
    }

    /**
     * 書き込みを終了し、動画ファイルを完成させる
     * qt-faststart の処理があるためサスペンド関数にしてみた
     *
     * @return 書き込んでいたファイル
     */
    suspend fun stopAndRelease() = withContext(Dispatchers.IO) {
        release()
        // mp4 で faststart する場合は moovブロック を移動する
        // 移動させることで、ダウンロードしながら再生が可能（ MediaMuxer が作る mp4 はすべてダウンロードしないと再生できない）
        QtFastStart.fastStart(tempFile, currentFile)
        currentFile!!
    }

    /**
     * リソース開放
     */
    fun release() {
        // 起動していなければ終了もさせない
        if (isRunning) {
            mediaMuxer?.stop()
            mediaMuxer?.release()
        }
        isRunning = false
        videoTrackIndex = -1
        audioTrackIndex = -1
    }


    companion object {
        /** インデックス番号初期値、無効な値 */
        private const val INVALID_INDEX_NUMBER = -1
    }

}