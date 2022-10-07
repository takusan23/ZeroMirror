package io.github.takusan23.zeromirror.dash

import android.media.MediaCodec
import io.github.takusan23.zerowebm.ZeroWebM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * [ZeroWebM]のラッパー
 *
 * エンコーダーからの出力を一時的に持つ [appendVideoEncodeData] と
 * 実際にファイルに書き込む [createVideoMediaSegment] は同じスレッドから呼び出す必要があります（多分）。
 *
 * 別スレッドで [createVideoMediaSegment] を呼び出すと多分映像が乱れてしまいます。
 */
class ZeroWebMWriter {

    /** WebM コンテナフォーマット を扱うクラス、音声用 */
    private val audioZeroWebM = ZeroWebM()

    /** WebM コンテナフォーマット を扱うクラス、映像用 */
    private val videoZeroWebM = ZeroWebM()

    /** 映像のデータ、ファイルに書き込んだらクリアされる */
    private val videoAppendBytes = arrayListOf<ByteArray>()

    /** 音声のデータ、ファイルに書き込んだらクリアされる */
    private val audioAppendBytes = arrayListOf<ByteArray>()

    /**
     * 音声の初期化セグメントを作成します
     *
     * @param filePath ファイルパス
     * @param channelCount チャンネル数
     * @param samplingRate サンプリングレート
     */
    suspend fun createAudioInitSegment(
        filePath: String,
        channelCount: Int,
        samplingRate: Int,
    ) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = audioZeroWebM.createEBMLHeader()
            val audioTrackSegment = audioZeroWebM.createAudioSegment(
                muxingAppName = ZeroWebM.MUXING_APP,
                writingAppName = ZeroWebM.WRITING_APP,
                trackId = ZeroWebM.AUDIO_TRACK_ID,
                codecName = ZeroWebM.OPUS_CODEC_NAME,
                channelCount = channelCount,
                samplingRate = samplingRate.toFloat()
            )

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(audioTrackSegment.toElementBytes())
        }
    }

    /**
     * 映像の初期化セグメントを作成します
     *
     * @param filePath ファイルパス
     * @param videoHeight 動画の高さ
     * @param videoWidth 動画の幅
     */
    suspend fun createVideoInitSegment(
        filePath: String,
        codecName: String = ZeroWebM.VP9_CODEC_NAME,
        videoWidth: Int,
        videoHeight: Int,
    ) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = videoZeroWebM.createEBMLHeader()
            val segment = videoZeroWebM.createVideoSegment(
                muxingAppName = ZeroWebM.MUXING_APP,
                writingAppName = ZeroWebM.WRITING_APP,
                trackId = ZeroWebM.VIDEO_TRACK_ID,
                codecName = codecName,
                videoWidth = videoWidth,
                videoHeight = videoHeight
            )

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(segment.toElementBytes())
        }
    }

    /**
     * 音声のメディアセグメントを作る
     *
     * @param filePath ファイル
     */
    suspend fun createAudioMediaSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { segmentFile ->
            // ConcurrentModificationException 対策にforを使う
            for (i in 0 until audioAppendBytes.size) {
                segmentFile.appendBytes(audioAppendBytes[i])
            }
            audioAppendBytes.clear()
        }
    }

    /**
     * 映像のメディアセグメントを作る
     *
     * @param filePath ファイル
     */
    suspend fun createVideoMediaSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { segmentFile ->
            // ConcurrentModificationException 対策にforを使う
            for (i in 0 until videoAppendBytes.size) {
                segmentFile.appendBytes(videoAppendBytes[i])
            }
            videoAppendBytes.clear()
        }
    }

    /**
     * MediaCodecがエンコードした音声データを保持する
     *
     * @param bufferInfo MediaCodecでもらえる
     * @param byteBuffer エンコード結果
     */
    fun appendAudioEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        val byteArray = toByteArray(byteBuffer)
        val isKeyFrame = true /*bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME*/
        audioAppendBytes += audioZeroWebM.appendSimpleBlock(ZeroWebM.AUDIO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
    }

    /**
     * MediaCodecがエンコードした映像データを保持する
     *
     * @param bufferInfo MediaCodecでもらえる
     * @param byteBuffer エンコード結果
     */
    fun appendVideoEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        val byteArray = toByteArray(byteBuffer)
        val isKeyFrame = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
        videoAppendBytes += videoZeroWebM.appendSimpleBlock(ZeroWebM.VIDEO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
    }

    /** [ByteBuffer]を[ByteArray]に変換する */
    private fun toByteArray(byteBuffer: ByteBuffer) = ByteArray(byteBuffer.remaining()).also { byteArray ->
        byteBuffer.get(byteArray)
    }

}