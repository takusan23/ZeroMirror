package io.github.takusan23.zerowebm

/** Matroska の タグIDの列挙型 */
enum class MatroskaTags(val id: ByteArray) {
    EBML(byteArrayOf(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte())),
    EBMLVersion(byteArrayOf(0x42.toByte(), 0x86.toByte())),
    EBMLReadVersion(byteArrayOf(0x42.toByte(), 0xF7.toByte())),
    EBMLMaxIDLength(byteArrayOf(0x42.toByte(), 0xF2.toByte())),
    EBMLMaxSizeLength(byteArrayOf(0x42.toByte(), 0xF3.toByte())),
    DocType(byteArrayOf(0x42.toByte(), 0x82.toByte())),
    DocTypeVersion(byteArrayOf(0x42.toByte(), 0x87.toByte())),
    DocTypeReadVersion(byteArrayOf(0x42.toByte(), 0x85.toByte())),

    Segment(byteArrayOf(0x18.toByte(), 0x53.toByte(), 0x80.toByte(), 0x67.toByte())),
    SeekHead(byteArrayOf(0x11.toByte(), 0x4D.toByte(), 0x9B.toByte(), 0x74.toByte())),
    Seek(byteArrayOf(0x4D.toByte(), 0xBB.toByte())),
    SeekID(byteArrayOf(0x53.toByte(), 0xAB.toByte())),
    SeekPosition(byteArrayOf(0x53.toByte(), 0xAC.toByte())),

    Info(byteArrayOf(0x15.toByte(), 0x49.toByte(), 0xA9.toByte(), 0x66.toByte())),
    Duration(byteArrayOf(0x44.toByte(), 0x89.toByte())),
    SegmentUUID(byteArrayOf(0x73.toByte(), 0xA4.toByte())),
    TimestampScale(byteArrayOf(0x2A.toByte(), 0xD7.toByte(), 0xB1.toByte())),
    MuxingApp(byteArrayOf(0x4D.toByte(), 0x80.toByte())),
    WritingApp(byteArrayOf(0x57.toByte(), 0x41.toByte())),

    Tracks(byteArrayOf(0x16.toByte(), 0x54.toByte(), 0xAE.toByte(), 0x6B.toByte())),
    TrackEntry(byteArrayOf(0xAE.toByte())),
    TrackNumber(byteArrayOf(0xD7.toByte())),
    TrackUID(byteArrayOf(0x73.toByte(), 0xC5.toByte())),
    FlagLacing(byteArrayOf(0x9C.toByte())),
    Language(byteArrayOf(0x22.toByte(), 0xB5.toByte(), 0x9C.toByte())),
    TrackType(byteArrayOf(0x83.toByte())),
    DefaultDuration(byteArrayOf(0x23.toByte(), 0xE3.toByte(), 0x83.toByte())),
    TrackTimecodeScale(byteArrayOf(0x23.toByte(), 0x31.toByte(), 0x4F.toByte())),
    CodecID(byteArrayOf(0x86.toByte())),
    CodecPrivate(byteArrayOf(0x63.toByte(), 0xA2.toByte())),
    CodecName(byteArrayOf(0x25.toByte(), 0x86.toByte(), 0x88.toByte())),
    VideoTrack(byteArrayOf(0xE0.toByte())),
    PixelWidth(byteArrayOf(0xB0.toByte())),
    PixelHeight(byteArrayOf(0xBA.toByte())),
    FrameRate(byteArrayOf(0x23.toByte(), 0x83.toByte(), 0xE3.toByte())),
    AudioTrack(byteArrayOf(0xE1.toByte())),
    SamplingFrequency(byteArrayOf(0xB5.toByte())),
    Channels(byteArrayOf(0x9F.toByte())),

    Cues(byteArrayOf(0x1C.toByte(), 0x53.toByte(), 0xBB.toByte(), 0x6B.toByte())),

    Cluster(byteArrayOf(0x1F.toByte(), 0x43.toByte(), 0xB6.toByte(), 0x75.toByte())),
    Timestamp(byteArrayOf(0xE7.toByte())),
    SimpleBlock(byteArrayOf(0xA3.toByte())),

    Void(byteArrayOf(0xEC.toByte())),

    /** 解析に失敗した場合のみ */
    Undefined(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()));

    companion object {

        /**
         * バイト配列からタグの名前を探す。ない場合は[Undefined]。
         */
        fun findTag(byteArray: ByteArray) = values().firstOrNull { it.id.toList() == byteArray.toList() } ?: Undefined
    }
}