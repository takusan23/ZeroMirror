package io.github.takusan23.zeromirror.media

import android.annotation.SuppressLint
import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 内部音声をPCMで取り出して、AACでエンコードする
 *
 * @param parentFolder 音声ファイル保存先
 * @param mediaProjection [MediaProjection]、内部音声を収録するのに使います。
 */
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioEncoder(parentFolder: File, mediaProjection: MediaProjection) {

    /** 音声エンコーダー、MediaCodecをラップした */
    private val audioEncoder = AudioEncoder()

    /** AACファイルにするやつ */
    private var mediaMuxer: MediaMuxer? = null

    /** 内部音声がとれる */
    private var audioRecord: AudioRecord? = null

    /** AACファイル */
    private val aacFile = File(parentFolder, AUDIO_FILE_NAME)

    /** 音声トラックの番号、-1はまだ追加してない */
    private var audioTrackIndex = INIT_INDEX_NUMBER

    /** MediaFormatキャッシュしておく */
    private var outputMediaFormat: MediaFormat? = null

    /** 書込み可能な状態の場合はtrue */
    private var isWritable = false

    init {
        // 内部音声取るのに使う
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!).apply {
            addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            addMatchingUsage(AudioAttributes.USAGE_GAME)
            addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        }.build()
        val audioFormat = AudioFormat.Builder().apply {
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(44_100)
            setChannelMask(AudioFormat.CHANNEL_IN_MONO)
        }.build()
        audioRecord = AudioRecord.Builder().apply {
            setAudioPlaybackCaptureConfig(playbackConfig)
            setAudioFormat(audioFormat)
        }.build()
        audioRecord!!.startRecording()
        // MediaMuxer初期化
        resetContainerFile()
    }

    /**
     * AAC エンコーダーを初期化する
     *
     * @param sampleRate サンプリングレート
     * @param channelCount チャンネル数
     * @param bitRate ビットレート
     */
    fun prepareEncoder(
        sampleRate: Int = 44_100,
        channelCount: Int = 1,
        bitRate: Int = 192_000,
    ) = audioEncoder.prepareEncoder(sampleRate, channelCount, bitRate)

    /**
     * エンコーダーを起動する。
     * 内部音声を取り出してAACにエンコードする、エンコード中はずっと一時停止します
     */
    suspend fun start() = withContext(Dispatchers.Default) {
        // エンコードする
        audioEncoder.startAudioEncode(
            onRecordInput = { bytes ->
                // PCM音声を取り出しエンコする
                return@startAudioEncode audioRecord!!.read(bytes, 0, bytes.size)
            },
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                // エンコード後のデータ
                if (isWritable) {
                    // 書き込む...
                    mediaMuxer!!.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
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
     * aacファイルへの書き込みを辞める
     *
     * @return ファイルパス
     */
    fun stopWriteContainer(): String {
        isWritable = false
        mediaMuxer?.stop()
        audioTrackIndex = INIT_INDEX_NUMBER
        return aacFile.path
    }

    /** コンテナファイルを初期化する */
    fun resetContainerFile() {
        // 前回のファイルを消してから
        aacFile.delete()
        mediaMuxer = MediaMuxer(aacFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // 2個目以降のファイルはここで初期化出来る
        addTrackAndStart()
    }

    /** 終了時に呼ぶこと、いいね？ */
    fun release() {
        audioEncoder.release()
        audioRecord?.stop()
        audioRecord?.release()
        mediaMuxer?.release()
    }

    /** [mediaMuxer]へ[outputMediaFormat]を追加して、[MediaMuxer]をスタートする */
    private fun addTrackAndStart() {
        if (audioTrackIndex == INIT_INDEX_NUMBER && outputMediaFormat != null) {
            audioTrackIndex = mediaMuxer!!.addTrack(outputMediaFormat!!)
            mediaMuxer!!.start()
            isWritable = true
        }
    }

    companion object {
        /** 音声ファイル名 */
        private const val AUDIO_FILE_NAME = "temp_audio.aac"

        /** インデックス番号初期値 */
        private const val INIT_INDEX_NUMBER = -1
    }
}