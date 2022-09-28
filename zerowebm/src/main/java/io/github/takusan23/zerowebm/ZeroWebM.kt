package io.github.takusan23.zerowebm

import java.io.File

val file = File("init.webm")

/** テスト用、WebMの構造は MKVToolNix が神レベルでわかりやすいです */
fun main(arg: Array<String>) {
    val zeroWebM = ZeroWebM()
    val ebmlHeader = zeroWebM.createEBMLHeader()
    val segment = zeroWebM.createSegment()

    file.appendBytes(ebmlHeader.toElementBytes())
    file.appendBytes(segment.toElementBytes())

    // Segment > Tracks > Audio の CodecPrivate に入れる中身
    // OpusHeaderをつくる
    val opusHeader = "OpusHead".toAscii() + byteArrayOf(0x01.toByte()) + byteArrayOf(0x02.toByte()) + byteArrayOf(0x00.toByte(), 0x00.toByte()) + byteArrayOf(0x80.toByte(), 0xBB.toByte()) + byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte())

    println(EBMLElement(MatroskaTags.CodecPrivate, opusHeader).toElementBytes().toHexString(" "))

}

class ZeroWebM {

    companion object {
        const val DOC_TYPE_WEBM = "webm"
        const val MUXING_APP = "zeromirror_zerowebm"
        const val WRITING_APP = "zeromirror_zerowebm"

        const val VIDEO_TRACK_ID = 1
        const val VIDEO_CODEC = "V_VP9"
        const val VIDEO_WIDTH = 1280
        const val VIDEO_HEIGHT = 720

        const val AUDIO_TRACK_ID = 2
        const val AUDIO_CODEC = "A_OPUS"
        const val SAMPLING_FREQUENCY = 48_000.0F
        const val CHANNELS = 2

        const val SIMPLE_BLOCK_FLAGS_KEYFRAME = 0x80
        const val SIMPLE_BLOCK_FLAGS = 0x00

        val UNKNOWN_SIZE = byteArrayOf(0x01, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
    }

    /** 前回 [createSimpleBlock] を呼んだときの Timescale */
    var prevCreateClusterTimescale = 0

    fun createSegment(): EBMLElement {
        val info = createInfo()
        val tracks = EBMLElement(MatroskaTags.Tracks, createAudioTrackEntryElement().toElementBytes() + createVideoTrackEntryElement().toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /** トラックが映像のみのセグメントを作る */
    fun createVideoSegment(): EBMLElement {
        val info = createInfo()
        val tracks = EBMLElement(MatroskaTags.Tracks, createVideoTrackEntryElement().toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /** トラックが音声のみのセグメントを作る */
    fun createAudioSegment(): EBMLElement {
        val info = createInfo()
        val tracks = EBMLElement(MatroskaTags.Tracks, createAudioTrackEntryElement().toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /**
     * データを追加していく。
     * そのため返すのはバイト配列
     *
     * @param trackNumber トラック番号、映像なのか音声なのか
     * @param timescaleMs エンコードしたデータの時間
     * @param byteArray エンコードされたデータ
     * @param isKeyFrame キーフレームの場合は true
     */
    fun appendSimpleBlock(trackNumber: Int, timescaleMs: Int, byteArray: ByteArray, isKeyFrame: Boolean = false): ByteArray {
        // Clusterからの相対時間
        val simpleBlockTimescale = timescaleMs - prevCreateClusterTimescale
        return if (simpleBlockTimescale > Short.MAX_VALUE) {
            // 16ビットを超える時間の場合は、Clusterを追加し直してからSimpleBlockを追加する
            prevCreateClusterTimescale = timescaleMs
            (createStreamingCluster(timescaleMs).toElementBytes() + createSimpleBlock(trackNumber, 0, byteArray, isKeyFrame).toElementBytes())
        } else {
            // Clusterを作り直さない
            createSimpleBlock(trackNumber, simpleBlockTimescale, byteArray, isKeyFrame).toElementBytes()
        }
    }

    /**
     * Clusterの中に入れるSimpleBlockを作る
     *
     * @param trackNumber トラック番号、映像なのか音声なのか
     * @param simpleBlockTimescale エンコードしたデータの時間
     * @param byteArray エンコードされたデータ
     * @param isKeyFrame キーフレームの場合は true
     */
    fun createSimpleBlock(trackNumber: Int, simpleBlockTimescale: Int, byteArray: ByteArray, isKeyFrame: Boolean): EBMLElement {
        val vIntTrackNumberBytes = trackNumber.toVInt()
        val simpleBlockBytes = simpleBlockTimescale.toClusterTimescale()
        // flags。キーフレームかどうかぐらいしか入れることなさそう
        val flagsBytes = byteArrayOf((if (isKeyFrame) SIMPLE_BLOCK_FLAGS_KEYFRAME else SIMPLE_BLOCK_FLAGS).toByte())
        val simpleBlockValue = vIntTrackNumberBytes + simpleBlockBytes + flagsBytes + byteArray
        return EBMLElement(MatroskaTags.SimpleBlock, simpleBlockValue)
    }

    /**
     * ストリーミング可能な Cluster を作成する（長さ不明）
     *
     * @param timescaleMs 開始時間。ミリ秒
     */
    fun createStreamingCluster(timescaleMs: Int = 0): EBMLElement {
        // 時間を16進数にして2バイトのバイト配列にする
        val timescaleBytes = timescaleMs.toByteArrayFat() // Cluster 直下の Timescale は 16ビットを超えるので
        val timescale = EBMLElement(MatroskaTags.Timestamp, timescaleBytes)
        val clusterValue = timescale.toElementBytes()

        return EBMLElement(MatroskaTags.Cluster, clusterValue, UNKNOWN_SIZE)
    }

    /** 音声のTrackを作成する */
    fun createAudioTrackEntryElement(): EBMLElement {
        // 音声トラック情報
        val audioTrackNumber = EBMLElement(MatroskaTags.TrackNumber, AUDIO_TRACK_ID.toByteArray())
        val audioTrackUid = EBMLElement(MatroskaTags.TrackUID, AUDIO_TRACK_ID.toByteArray())
        val audioCodecId = EBMLElement(MatroskaTags.CodecID, AUDIO_CODEC.toAscii())
        val audioTrackType = EBMLElement(MatroskaTags.TrackType, AUDIO_TRACK_ID.toByteArray())
        // Segment > Tracks > Audio の CodecPrivate に入れる中身
        // OpusHeaderをつくる
        // https://www.rfc-editor.org/rfc/rfc7845
        // Version = 0x01
        // Channel Count = 0x02
        // Pre-Skip = 0x00 0x00
        // Input Sample Rate ( little endian ) 0x80 0xBB 0x00 0x00
        // Output Gain 0x00 0x00
        // Mapping Family 0x00
        // ??? 0x00 0x00
        val opusHeader = "OpusHead".toAscii() + byteArrayOf(0x01.toByte()) + byteArrayOf(0x02.toByte()) + byteArrayOf(0x00.toByte(), 0x00.toByte()) + byteArrayOf(0x80.toByte(), 0xBB.toByte()) + byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val codecPrivate = EBMLElement(MatroskaTags.CodecPrivate, opusHeader)
        // サンプリングレートはfloatなのでひと手間必要
        val sampleFrequency = EBMLElement(MatroskaTags.SamplingFrequency, SAMPLING_FREQUENCY.toBits().toByteArray())
        val channels = EBMLElement(MatroskaTags.Channels, CHANNELS.toByteArray())
        val audioTrack = EBMLElement(MatroskaTags.AudioTrack, sampleFrequency.toElementBytes() + channels.toElementBytes())
        val audioTrackEntryValue = audioTrackNumber.toElementBytes() + audioTrackUid.toElementBytes() + audioCodecId.toElementBytes() + audioTrackType.toElementBytes() + codecPrivate.toElementBytes() + audioTrack.toElementBytes()
        return EBMLElement(MatroskaTags.TrackEntry, audioTrackEntryValue)
    }

    /** 映像のTrackを作成する */
    fun createVideoTrackEntryElement(): EBMLElement {
        // 動画トラック情報
        val videoTrackNumber = EBMLElement(MatroskaTags.TrackNumber, VIDEO_TRACK_ID.toByteArray())
        val videoTrackUid = EBMLElement(MatroskaTags.TrackUID, VIDEO_TRACK_ID.toByteArray())
        val videoCodecId = EBMLElement(MatroskaTags.CodecID, VIDEO_CODEC.toAscii())
        val videoTrackType = EBMLElement(MatroskaTags.TrackType, VIDEO_TRACK_ID.toByteArray())
        val pixelWidth = EBMLElement(MatroskaTags.PixelWidth, VIDEO_WIDTH.toByteArray())
        val pixelHeight = EBMLElement(MatroskaTags.PixelHeight, VIDEO_HEIGHT.toByteArray())
        val videoTrack = EBMLElement(MatroskaTags.VideoTrack, pixelWidth.toElementBytes() + pixelHeight.toElementBytes())
        val videoTrackEntryValue = videoTrackNumber.toElementBytes() + videoTrackUid.toElementBytes() + videoCodecId.toElementBytes() + videoTrackType.toElementBytes() + videoTrack.toElementBytes()
        return EBMLElement(MatroskaTags.TrackEntry, videoTrackEntryValue)
    }

    /** Info要素を作成する。 */
    fun createInfo(): EBMLElement {
        val timestampScale = EBMLElement(MatroskaTags.TimestampScale, 1000000.toByteArray())
        val muxingApp = EBMLElement(MatroskaTags.MuxingApp, MUXING_APP.toAscii())
        val writingApp = EBMLElement(MatroskaTags.WritingApp, WRITING_APP.toAscii())

        val infoValue = timestampScale.toElementBytes() + muxingApp.toElementBytes() + writingApp.toElementBytes()
        return EBMLElement(MatroskaTags.Info, infoValue)
    }

    /** EBMLヘッダー（WebMだよ～）を作成する */
    fun createEBMLHeader(): EBMLElement {
        // ハードコートしてるけど別にいいはず
        val ebmlVersion = EBMLElement(MatroskaTags.EBMLVersion, byteArrayOf(0x01))
        val readVersion = EBMLElement(MatroskaTags.EBMLReadVersion, byteArrayOf(0x01))
        val maxIdLength = EBMLElement(MatroskaTags.EBMLMaxIDLength, byteArrayOf(0x04))
        val maxSizeLength = EBMLElement(MatroskaTags.EBMLMaxSizeLength, byteArrayOf(0x08))
        val docType = EBMLElement(MatroskaTags.DocType, DOC_TYPE_WEBM.toAscii())
        val docTypeVersion = EBMLElement(MatroskaTags.DocTypeVersion, byteArrayOf(0x02))
        val docTypeReadVersion = EBMLElement(MatroskaTags.DocTypeReadVersion, byteArrayOf(0x02))

        // ヘッダー部を書く
        val ebmlValue = ebmlVersion.toElementBytes() + readVersion.toElementBytes() + maxIdLength.toElementBytes() + maxSizeLength.toElementBytes() + docType.toElementBytes() + docTypeVersion.toElementBytes() + docTypeReadVersion.toElementBytes()
        return EBMLElement(MatroskaTags.EBML, ebmlValue)
    }

}

/**
 * IDの大きさも可変長なのでサイズを出す。
 * IDの先頭バイトで、IDの長さを出すことができます
 */
fun Byte.calcIdSize() = this.toInt().let { int ->
    when {
        (int and 0x80) != 0 -> 1
        (int and 0x40) != 0 -> 2
        (int and 0x20) != 0 -> 3
        (int and 0x10) != 0 -> 4
        (int and 0x08) != 0 -> 5
        (int and 0x04) != 0 -> 6
        (int and 0x02) != 0 -> 7
        (int and 0x01) != 0 -> 8
        else -> -1// 多分無い
    }
}

/** 数値を V_INT でエンコードする */
fun Int.toVInt(): ByteArray {
    val valueByteArray = this.toByteArray()
    val valueSize = when (valueByteArray.size) {
        1 -> 0b1000_0000
        2 -> 0b0100_0000
        3 -> 0b0010_0000
        4 -> 0b0001_0000
        5 -> 0b0000_1000
        6 -> 0b0000_0100
        7 -> 0b0000_0010
        else -> 0b0000_0001
    }
    return valueByteArray.apply {
        // TODO これだと多分無理
        this[0] = (valueSize or this[0].toInt()).toByte()
    }
}

/**
 * Valueの[ByteArray]から必要なデータサイズを作って返す
 * 値を表すデータサイズすらも可変長なので...
 */
internal fun createDataSize(valueBytes: ByteArray): ByteArray {
    val size = valueBytes.size
    // Intを16進数にしてByteArrayにする
    val dataSizeBytes = size.toByteArrayFat()
    val firstDataSizeByte = dataSizeBytes.first()
    // データサイズ自体も可変長なので、何バイト分がデータサイズなのか記述する
    // V_INT とかいうやつで、1が先頭から何番目に立ってるかで残りのバイト数が分かるようになってる
    // 1000 0000 -> 7 ビット ( 1xxx xxxx )
    // 0100 0000 -> 14 ビット ( 01xx xxxx xxxx xxxx )
    val dataSizeBytesSize = when (dataSizeBytes.size) {
        1 -> 0b1000_0000
        2 -> 0b0100_0000
        3 -> 0b0010_0000
        4 -> 0b0001_0000
        5 -> 0b0000_1000
        6 -> 0b0000_0100
        7 -> 0b0000_0010
        else -> 0b0000_0001
    }
    // データサイズのバイトの先頭に V_INT のやつを OR する
    val dataSize = dataSizeBytes.apply {
        this[0] = (dataSizeBytesSize or firstDataSizeByte.toInt()).toByte()
    }
    return dataSize
}

/** Intを16進数にして2バイトのバイト配列にする */
internal fun Int.toClusterTimescale() = byteArrayOf((this shr 8).toByte(), this.toByte())

/** Intを16進数にして[ByteArray]にする。4バイトで返す。 */
internal fun Int.toByteArrayFat() = byteArrayOf(
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    this.toByte(),
)

/**
 * Intを16進数にして[ByteArray]に突っ込んで返す。4バイトまで
 *
 * TODO うまく動いてないので直す
 */
internal fun Int.toByteArray(): ByteArray {
    // ビット演算子何も分からん
    // ただIntから変換した16進数を入れるByteArrayのサイズを出したかっただけなんや...
    return when {
        this < 0xFF -> byteArrayOf(this.toByte())
        this shr 8 == 0 -> byteArrayOf(
            (this shr 8).toByte(),
            this.toByte(),
        )
        this shr 16 == 0 -> byteArrayOf(
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
        else -> byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
    }
}

/** 文字列を ASCII のバイト配列に変換する */
internal fun String.toAscii() = this.toByteArray(charset = Charsets.US_ASCII)

/** 16進数に変換するやつ */
internal fun ByteArray.toHexString(separator: String = "") = this.joinToString(separator = separator) { "%02x".format(it) }

/** 16進数に変換するやつ */
internal fun Byte.toHexString() = "%02x".format(this)

/** ByteをInt型に変換する。Byteは符号なしの値である必要があります。 */
internal fun Byte.toUnsignedInt() = this.toInt() and 0xFF

internal fun Int.toUnsignedInt() = this and 0xFF
