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

    private val zeroWebM = ZeroWebM()
    private val appendBytes = mutableListOf<ByteArray>()

    /**
     * 初期化セグメントを作成する
     *
     * @return ファイル
     */
    suspend fun createInitSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = zeroWebM.createEBMLHeader()
            val segment = zeroWebM.createSegment()
            // val cluster = zeroWebM.createStreamingCluster()

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(segment.toElementBytes())
            // initFile.appendBytes(cluster.toElementBytes())
        }
    }

    /**
     * メディアセグメントを作成する
     *
     * @return ファイル
     */
    suspend fun createMediaSegment(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).also { segmentFile ->
            // ConcurrentModificationException 対策にforを使う
            for (i in 0 until appendBytes.size) {
                segmentFile.appendBytes(appendBytes[i])
            }
            appendBytes.clear()
        }
    }

    fun appendVideoEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        val byteArray = toByteArray(byteBuffer)
        val isKeyFrame = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
        appendBytes += zeroWebM.appendSimpleBlock(ZeroWebM.VIDEO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
    }

    fun appendAudioEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        val byteArray = toByteArray(byteBuffer)
        val isKeyFrame = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
        appendBytes += zeroWebM.appendSimpleBlock(ZeroWebM.AUDIO_TRACK_ID, (bufferInfo.presentationTimeUs / 1000).toInt(), byteArray, isKeyFrame)
    }

    /** [ByteBuffer]を[ByteArray]に変換する */
    private fun toByteArray(byteBuffer: ByteBuffer) = ByteArray(byteBuffer.limit()).apply {
        byteBuffer.get(this)
    }
}