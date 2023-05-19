package io.github.takusan23.zeromirror.tool

import org.junit.Assert.assertEquals
import org.junit.Test

/** 単位変換コードがちゃんと動くか */
class NumberConverterTest {

    @Test
    fun convertTest() {
        // Mbps
        assertEquals("1 Mbps", NumberConverter.convert(1_000_000))
        assertEquals("5 Mbps", NumberConverter.convert(5_000_000))
        assertEquals("10 Mbps", NumberConverter.convert(10_000_000))

        // Kbps
        assertEquals("192 Kbps", NumberConverter.convert(192_000))
        assertEquals("384 Kbps", NumberConverter.convert(384_000))
        assertEquals("64 Kbps", NumberConverter.convert(64_000))
    }

}