package io.github.takusan23.zerowebm


/**
 * EBML要素のデータクラス
 *
 * @param tagId [MatroskaTags]
 * @param value データ
 * @param dataSize データサイズ。データの大きさを表すやつ
 */
data class EBMLElement(
    val tagId: MatroskaTags,
    val value: ByteArray,
    val dataSize: ByteArray = createDataSize(value),
) {
    /** ID DataSize Value を繋げたバイト配列を返す */
    fun toElementBytes() = (tagId.id + dataSize + value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EBMLElement

        if (tagId != other.tagId) return false
        if (!value.contentEquals(other.value)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tagId.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}