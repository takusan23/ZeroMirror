package io.github.takusan23.zeromirror.tool

/**
 * Byte -> MB 単位変換など
 *
 * テストコードが存在します。
 */
object NumberConverter {

    /**
     * 適切な単位に変換する
     *
     * bps / Kbps / Mbps のみ対応
     *
     * @param bit ビット
     * @return 単位を付けて返します
     */
    fun convert(bit: Int): String {
        return when (bit) {
            // この書き方すき
            in 1_000 until 1_000_000 -> "${bit / 1_000} Kbps"
            in 1_000_000 until 1_000_000_000 -> "${bit / 1_000_000} Mbps"
            else -> "$bit Bps"
        }
    }

}