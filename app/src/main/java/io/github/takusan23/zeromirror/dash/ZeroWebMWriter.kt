package io.github.takusan23.zeromirror.dash

import android.media.MediaCodec
import io.github.takusan23.zerowebm.ZeroWebM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** [ZeroWebM]のラッパー */
class ZeroWebMWriter {

    /** WebM コンテナフォーマット を扱うクラス、音声用 */
    private val audioZeroWebM = ZeroWebM()

    /** WebM コンテナフォーマット を扱うクラス、映像用 */
    private val videoZeroWebM = ZeroWebM()

    /** 映像のデータ、ファイルに書き込んだらクリアされる */
    private val videoAppendBytes = arrayListOf<ByteArray>()

    /** 音声のデータ、ファイルに書き込んだらクリアされる */
    private val audioAppendBytes = arrayListOf<ByteArray>()

    // 多分、映像を書き込む処理と、セグメントファイルを作り直す処理が同時に走ったりすると（それぞれ別のJob）、同時に書き込みされて？映像が乱れてしまう。
    // 別々の Job から書き込みされても、直列にしか書き込まないようにするための Mutex。

    /** 映像データの書き込みを同時に行わないように */
    private val videoFileWriteMutex = Mutex()

    /** 音声データの書き込みを同時に行わないように */
    private val audioFileWriteMutex = Mutex()

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
        audioFileWriteMutex.withLock {
            File(filePath).also { segmentFile ->
                // ConcurrentModificationException 対策にforを使う
                for (i in 0 until audioAppendBytes.size) {
                    segmentFile.appendBytes(audioAppendBytes[i])
                }
                audioAppendBytes.clear()
            }
        }
    }

    /**
     * 映像のメディアセグメントを作る
     *
     * @param filePath ファイル
     */
    suspend fun createVideoMediaSegment(filePath: String) = withContext(Dispatchers.IO) {
        videoFileWriteMutex.withLock {
            File(filePath).also { segmentFile ->
                // ConcurrentModificationException 対策にforを使う
                for (i in 0 until videoAppendBytes.size) {
                    segmentFile.appendBytes(videoAppendBytes[i])
                }
                videoAppendBytes.clear()
            }
        }
    }

    /**
     * MediaCodecがエンコードした音声データを保持する
     *
     * @param bufferInfo MediaCodecでもらえる
     * @param byteBuffer エンコード結果
     */
    suspend fun appendAudioEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        audioFileWriteMutex.withLock {
            val byteArray = byteBuffer.asByteArray()
            // Opusの場合は常にキーフレームかもしれないです
            val isKeyFrame = true
            audioAppendBytes += audioZeroWebM.appendSimpleBlock(ZeroWebM.AUDIO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
        }
    }

    /**
     * MediaCodecがエンコードした映像データを保持する
     *
     * @param bufferInfo MediaCodecでもらえる
     * @param byteBuffer エンコード結果
     */
    suspend fun appendVideoEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        videoFileWriteMutex.withLock {
            val byteArray = byteBuffer.asByteArray()
            val isKeyFrame = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
            videoAppendBytes += videoZeroWebM.appendSimpleBlock(ZeroWebM.VIDEO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
        }
    }

    /** [ByteBuffer]を[ByteArray]に変換する */
    private fun ByteBuffer.asByteArray() = ByteArray(this.remaining()).also { byteArray ->
        this.get(byteArray)
    }

}