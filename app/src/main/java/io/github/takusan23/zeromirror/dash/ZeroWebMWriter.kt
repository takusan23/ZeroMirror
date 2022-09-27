package io.github.takusan23.zeromirror.dash

import android.media.MediaCodec
import io.github.takusan23.zerowebm.ZeroWebM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * [ZeroWebM]のラッパー
 */
class ZeroWebMWriter {

    /** WebM コンテナフォーマット を扱うクラス */
    private val zeroWebM = ZeroWebM()

    /** 映像のデータ、ファイルに書き込んだらクリアされる */
    private val videoAppendBytes = arrayListOf<ByteArray>()

    /** 音声のデータ、ファイルに書き込んだらクリアされる */
    private val audioAppendBytes = arrayListOf<ByteArray>()

    /**
     * 初期化セグメントを作成する
     *
     * @return ファイル
     */
    suspend fun createInitSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = zeroWebM.createEBMLHeader()
            val segment = zeroWebM.createSegment()

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(segment.toElementBytes())
        }
    }

    /**
     * 音声の初期化セグメントを作成します
     *
     * @param filePath ファイルパス
     */
    suspend fun createAudioInitSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = zeroWebM.createEBMLHeader()
            val audioTrackSegment = zeroWebM.createAudioSegment()

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(audioTrackSegment.toElementBytes())
        }
    }

    /**
     * 映像の初期化セグメントを作成します
     *
     * @param filePath ファイルパス
     */
    suspend fun createVideoInitSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = zeroWebM.createEBMLHeader()
            val segment = zeroWebM.createVideoSegment()

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
        val isKeyFrame = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
        audioAppendBytes += zeroWebM.appendSimpleBlock(ZeroWebM.AUDIO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
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
        videoAppendBytes += zeroWebM.appendSimpleBlock(ZeroWebM.VIDEO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
    }

    /** [ByteBuffer]を[ByteArray]に変換する */
    private fun toByteArray(byteBuffer: ByteBuffer) = ByteArray(byteBuffer.limit()).apply {
        byteBuffer.get(this)
    }
}