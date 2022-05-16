package io.github.takusan23.zeromirror.tool

import org.junit.Assert.assertEquals
import org.junit.Test

/** 単位変換コードがちゃんと動くか */
class DisplayConverterTest {

    @Test
    fun convertTest() {
        // Mbps
        assertEquals("1 Mbps", DisplayConverter.convert(1_000_000))
        assertEquals("5 Mbps", DisplayConverter.convert(5_000_000))
        assertEquals("10 Mbps", DisplayConverter.convert(10_000_000))

        // Kbps
        assertEquals("192 Kbps", DisplayConverter.convert(192_000))
        assertEquals("384 Kbps", DisplayConverter.convert(384_000))
        assertEquals("64 Kbps", DisplayConverter.convert(64_000))
    }

}