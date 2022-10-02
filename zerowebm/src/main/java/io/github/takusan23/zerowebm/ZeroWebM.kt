package io.github.takusan23.zerowebm

import java.io.File

val file = File("init.webm")

/** テスト用、WebMの構造は MKVToolNix が神レベルでわかりやすいです */
fun main(arg: Array<String>) {
    val zeroWebM = ZeroWebM()
    val ebmlHeader = zeroWebM.createEBMLHeader()
    val segment = zeroWebM.createSegment(
        muxingAppName = ZeroWebM.MUXING_APP,
        writingAppName = ZeroWebM.WRITING_APP,
        videoTrackId = ZeroWebM.VIDEO_TRACK_ID,
        videoCodecName = ZeroWebM.VIDEO_CODEC,
        videoWidth = ZeroWebM.VIDEO_WIDTH,
        videoHeight = ZeroWebM.VIDEO_HEIGHT,
        audioTrackId = ZeroWebM.AUDIO_TRACK_ID,
        audioCodecName = ZeroWebM.AUDIO_CODEC,
        channelCount = ZeroWebM.CHANNELS,
        samplingRate = ZeroWebM.SAMPLING_FREQUENCY
    )

    println(ebmlHeader.toElementBytes().toHexString())
    println(segment.toElementBytes().toHexString())
    file.appendBytes(ebmlHeader.toElementBytes())
    file.appendBytes(segment.toElementBytes())

}

class ZeroWebM {

    companion object {

        /// ここはそれぞれ違う

        const val VIDEO_TRACK_ID = 1
        const val VIDEO_CODEC = "V_VP9"
        const val VIDEO_WIDTH = 1280
        const val VIDEO_HEIGHT = 720

        const val AUDIO_TRACK_ID = 2
        const val AUDIO_CODEC = "A_OPUS"
        const val SAMPLING_FREQUENCY = 48_000.0F
        const val CHANNELS = 2

        ///

        const val MUXING_APP = "zeromirror_zerowebm"
        const val WRITING_APP = "zeromirror"

        const val SIMPLE_BLOCK_FLAGS_KEYFRAME = 0x80
        const val SIMPLE_BLOCK_FLAGS = 0x00

        const val EBML_VERSION = 0x01
        const val EBML_READ_VERSION = 0x01
        const val EBML_MAX_ID_LENGTH = 0x04
        const val EBML_MAXSIZE_LENGTH = 0x08
        const val EBML_DOCTYPE_WEBM = "webm"
        const val EBML_DOCTYOE_VERSION = 0x02
        const val EBML_DOCTYPE_READ_VERSION = 0x02

        const val INFO_TIMESCALE_MS = 1000000

        const val VP8_CODEC_NAME = "V_VP8"
        const val VP9_CODEC_NAME = "V_VP9"
        const val OPUS_CODEC_NAME = "A_OPUS"

        const val OPUS_HEAD = "OpusHead"
        const val OPUS_VERSION = 0x01

        // TrackType

        /** 音声トラック */
        const val TRACK_TYPE_AUDIO = 2

        /** 映像トラック */
        const val TRACK_TYPE_VIDEO = 1

        val UNKNOWN_SIZE = byteArrayOf(0x01, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
    }

    /** 前回 [createSimpleBlock] を呼んだときの Timescale */
    private var prevCreateClusterTimescale = 0

    /**
     * トラックが音声と映像のセグメントを作る
     * 引数は [createAudioSegment] [createVideoSegment] を参照
     */
    fun createSegment(
        muxingAppName: String,
        writingAppName: String,
        videoTrackId: Int,
        videoUid: Int = videoTrackId,
        videoCodecName: String,
        videoWidth: Int,
        videoHeight: Int,
        audioTrackId: Int,
        audioUid: Int = audioTrackId,
        audioCodecName: String,
        channelCount: Int,
        samplingRate: Float,
    ): EBMLElement {
        val info = createInfo(INFO_TIMESCALE_MS, muxingAppName, writingAppName)
        val videoTrack = createVideoTrackEntryElement(videoTrackId, videoUid, videoCodecName, videoWidth, videoHeight)
        val audioTrack = createAudioTrackEntryElement(audioTrackId, audioUid, audioCodecName, channelCount, samplingRate)
        val tracks = EBMLElement(MatroskaTags.Tracks, videoTrack.toElementBytes() + audioTrack.toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /**
     * トラックが映像のみのセグメントを作る
     *
     * @param muxingAppName WebMを書き込むライブラリの名前を入れる
     * @param writingAppName ライブラリを利用しているアプリの名前を入れる
     * @param trackId トラック番号
     * @param uid [trackId]入れてもいいかも
     * @param codecName コーデック名
     * @param videoWidth 動画の幅
     * @param videoHeight 動画の高さ
     */
    fun createVideoSegment(
        muxingAppName: String,
        writingAppName: String,
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        videoWidth: Int,
        videoHeight: Int,
    ): EBMLElement {
        val info = createInfo(INFO_TIMESCALE_MS, muxingAppName, writingAppName)
        val tracks = EBMLElement(MatroskaTags.Tracks, createVideoTrackEntryElement(trackId, uid, codecName, videoWidth, videoHeight).toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /**
     * トラックが音声のみのセグメントを作る
     *
     * @param muxingAppName WebMを書き込むライブラリの名前を入れる
     * @param writingAppName ライブラリを利用しているアプリの名前を入れる
     * @param trackId トラック番号
     * @param uid [trackId]入れてもいいかも
     * @param channelCount チャンネル数、ステレオなら2
     * @param codecName コーデック名
     * @param samplingRate サンプリングレート
     */
    fun createAudioSegment(
        muxingAppName: String,
        writingAppName: String,
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        channelCount: Int,
        samplingRate: Float,
    ): EBMLElement {
        val info = createInfo(INFO_TIMESCALE_MS, muxingAppName, writingAppName)
        val tracks = EBMLElement(MatroskaTags.Tracks, createAudioTrackEntryElement(trackId, uid, codecName, channelCount, samplingRate).toElementBytes())
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
    fun appendSimpleBlock(
        trackNumber: Int,
        timescaleMs: Int,
        byteArray: ByteArray,
        isKeyFrame: Boolean = false,
    ): ByteArray {
        // Clusterからの相対時間
        val simpleBlockTimescale = timescaleMs - prevCreateClusterTimescale
        return if (simpleBlockTimescale > Short.MAX_VALUE) {
            // 16ビットを超える時間の場合は、Clusterを追加し直してからSimpleBlockを追加する
            // TODO Clusterの後はキーフレームであるとより良い
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
    fun createSimpleBlock(
        trackNumber: Int,
        simpleBlockTimescale: Int,
        byteArray: ByteArray,
        isKeyFrame: Boolean,
    ): EBMLElement {
        val vIntTrackNumberBytes = trackNumber.toVInt()
        val simpleBlockBytes = simpleBlockTimescale.toClusterTimescale()
        // flags。キーフレームかどうかぐらいしか入れることなさそう
        val flagsBytes = byteArrayOf((if (isKeyFrame) SIMPLE_BLOCK_FLAGS_KEYFRAME else SIMPLE_BLOCK_FLAGS).toByte())
        val simpleBlockValue = vIntTrackNumberBytes + simpleBlockBytes + flagsBytes + byteArray
        return EBMLElement(MatroskaTags.SimpleBlock, simpleBlockValue)
    }

    /**
     * ストリーミング可能な Cluster を作成する。
     * データサイズが不定になっている。
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

    /**
     * 音声のTrackを作成する
     *
     * @param trackId トラック番号
     * @param uid [trackId]入れてもいいかも
     * @param channelCount チャンネル数、ステレオなら2
     * @param codecName コーデック名
     * @param samplingRate サンプリングレート
     */
    fun createAudioTrackEntryElement(
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        channelCount: Int,
        samplingRate: Float,
    ): EBMLElement {
        // 音声トラック情報
        val audioTrackNumber = EBMLElement(MatroskaTags.TrackNumber, trackId.toByteArray())
        val audioTrackUid = EBMLElement(MatroskaTags.TrackUID, uid.toByteArray())
        val audioCodecId = EBMLElement(MatroskaTags.CodecID, codecName.toAscii())
        val audioTrackType = EBMLElement(MatroskaTags.TrackType, TRACK_TYPE_AUDIO.toByteArray())
        // Segment > Tracks > Audio の CodecPrivate に入れる中身
        // OpusHeaderをつくる
        // https://www.rfc-editor.org/rfc/rfc7845
        // Version = 0x01
        // Channel Count = 0x02
        // Pre-Skip = 0x00 0x00
        // Input Sample Rate ( little endian ) 0x80 0xBB 0x00 0x00 . Kotlin は Big endian なので反転する
        // Output Gain 0x00 0x00
        // Mapping Family 0x00
        // ??? 0x00 0x00
        val opusHeader = OPUS_HEAD.toAscii() + byteArrayOf(OPUS_VERSION.toByte()) + byteArrayOf(channelCount.toByte()) + byteArrayOf(0x00.toByte(), 0x00.toByte()) + samplingRate.toInt().toClusterTimescale().reversed() + byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val codecPrivate = EBMLElement(MatroskaTags.CodecPrivate, opusHeader)
        // サンプリングレートはfloatなのでひと手間必要
        val sampleFrequency = EBMLElement(MatroskaTags.SamplingFrequency, samplingRate.toBits().toByteArray())
        val channels = EBMLElement(MatroskaTags.Channels, channelCount.toByteArray())
        val audioTrack = EBMLElement(MatroskaTags.AudioTrack, sampleFrequency.toElementBytes() + channels.toElementBytes())
        val audioTrackEntryValue = audioTrackNumber.toElementBytes() + audioTrackUid.toElementBytes() + audioCodecId.toElementBytes() + audioTrackType.toElementBytes() + codecPrivate.toElementBytes() + audioTrack.toElementBytes()
        return EBMLElement(MatroskaTags.TrackEntry, audioTrackEntryValue)
    }

    /**
     * 映像のTrackを作成する
     *
     * @param trackId トラック番号
     * @param uid [trackId]入れてもいいかも
     * @param codecName コーデック名
     * @param videoWidth 動画の幅
     * @param videoHeight 動画の高さ
     */
    fun createVideoTrackEntryElement(
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        videoWidth: Int,
        videoHeight: Int,
    ): EBMLElement {
        // 動画トラック情報
        val videoTrackNumber = EBMLElement(MatroskaTags.TrackNumber, trackId.toByteArray())
        val videoTrackUid = EBMLElement(MatroskaTags.TrackUID, uid.toByteArray())
        val videoCodecId = EBMLElement(MatroskaTags.CodecID, codecName.toAscii())
        val videoTrackType = EBMLElement(MatroskaTags.TrackType, TRACK_TYPE_VIDEO.toByteArray())
        val pixelWidth = EBMLElement(MatroskaTags.PixelWidth, videoWidth.toByteArray())
        val pixelHeight = EBMLElement(MatroskaTags.PixelHeight, videoHeight.toByteArray())
        val videoTrack = EBMLElement(MatroskaTags.VideoTrack, pixelWidth.toElementBytes() + pixelHeight.toElementBytes())
        val videoTrackEntryValue = videoTrackNumber.toElementBytes() + videoTrackUid.toElementBytes() + videoCodecId.toElementBytes() + videoTrackType.toElementBytes() + videoTrack.toElementBytes()
        return EBMLElement(MatroskaTags.TrackEntry, videoTrackEntryValue)
    }

    /**
     * Info要素を作成する
     *
     * @param timescaleMs [INFO_TIMESCALE_MS]かな
     * @param muxingAppName WebMを書き込むライブラリの名前を入れる
     * @param writingAppName ライブラリを利用しているアプリの名前を入れる
     */
    fun createInfo(
        timescaleMs: Int,
        muxingAppName: String,
        writingAppName: String,
    ): EBMLElement {
        val timestampScale = EBMLElement(MatroskaTags.TimestampScale, timescaleMs.toByteArray())
        val muxingApp = EBMLElement(MatroskaTags.MuxingApp, muxingAppName.toAscii())
        val writingApp = EBMLElement(MatroskaTags.WritingApp, writingAppName.toAscii())

        val infoValue = timestampScale.toElementBytes() + muxingApp.toElementBytes() + writingApp.toElementBytes()
        return EBMLElement(MatroskaTags.Info, infoValue)
    }

    /**
     * EBMLヘッダー（WebMだよ～）を作成する
     *
     * @param version EBMLバージョン
     * @param readVersion パーサーの最小バージョン
     * @param maxIdLength IDの最大長
     * @param maxSizeLength データサイズの最大長
     * @param docType WebM
     * @param docTypeVersion DocTypeインタープリターのバージョン
     * @param docTypeReadVersion DocTypeパーサーの最小バージョン
     */
    fun createEBMLHeader(
        version: Int = EBML_VERSION,
        readVersion: Int = EBML_READ_VERSION,
        maxIdLength: Int = EBML_MAX_ID_LENGTH,
        maxSizeLength: Int = EBML_MAXSIZE_LENGTH,
        docType: String = EBML_DOCTYPE_WEBM,
        docTypeVersion: Int = EBML_DOCTYOE_VERSION,
        docTypeReadVersion: Int = EBML_DOCTYPE_READ_VERSION,
    ): EBMLElement {
        // ハードコートしてるけど別にいいはず
        val ebmlVersion = EBMLElement(MatroskaTags.EBMLVersion, version.toByteArray())
        val ebmlEBMLReadVersion = EBMLElement(MatroskaTags.EBMLReadVersion, readVersion.toByteArray())
        val ebmlEBMLMaxIDLength = EBMLElement(MatroskaTags.EBMLMaxIDLength, maxIdLength.toByteArray())
        val ebmlEBMLMaxSizeLength = EBMLElement(MatroskaTags.EBMLMaxSizeLength, maxSizeLength.toByteArray())
        val ebmlDocType = EBMLElement(MatroskaTags.DocType, docType.toAscii())
        val ebmlDocTypeVersion = EBMLElement(MatroskaTags.DocTypeVersion, docTypeVersion.toByteArray())
        val ebmlDocTypeReadVersion = EBMLElement(MatroskaTags.DocTypeReadVersion, docTypeReadVersion.toByteArray())

        // ヘッダー部を書く
        val ebmlValue = ebmlVersion.toElementBytes() + ebmlEBMLReadVersion.toElementBytes() + ebmlEBMLMaxIDLength.toElementBytes() + ebmlEBMLMaxSizeLength.toElementBytes() + ebmlDocType.toElementBytes() + ebmlDocTypeVersion.toElementBytes() + ebmlDocTypeReadVersion.toElementBytes()
        return EBMLElement(MatroskaTags.EBML, ebmlValue)
    }

}

/**
 * IDの大きさも可変長なのでサイズを出す。
 * IDの先頭バイトで、IDの長さを出すことができます
 */
internal fun Byte.calcIdSize() = this.toInt().let { int ->
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
internal fun Int.toVInt(): ByteArray {
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
internal fun ByteArray.toHexString(separator: String = " ") = this.joinToString(separator = separator) { "%02x".format(it) }

/** 16進数に変換するやつ */
internal fun Byte.toHexString() = "%02x".format(this)

/** ByteをInt型に変換する。Byteは符号なしの値である必要があります。 */
internal fun Byte.toUnsignedInt() = this.toInt() and 0xFF

internal fun Int.toUnsignedInt() = this and 0xFF
