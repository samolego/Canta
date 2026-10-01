package io.github.samolego.canta.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FileSizeTest {

    @Test
    fun formatsWithSiUnits() {
        assertEquals("0 B", formatFileSize(0))
        assertEquals("980 B", formatFileSize(980))
        assertEquals("1.5 kB", formatFileSize(1_500))
        assertEquals("9.9 MB", formatFileSize(9_940_000))
        assertEquals("98 MB", formatFileSize(98_374_499))
        assertEquals("1.2 GB", formatFileSize(1_234_567_890))
    }

    @Test
    fun roundsBeforeChoosingTheUnit() {
        assertEquals("999 B", formatFileSize(999))
        assertEquals("1.0 kB", formatFileSize(1_000))
        assertEquals("1.0 MB", formatFileSize(999_999))
        assertEquals("10 MB", formatFileSize(9_999_999))
        assertEquals("10 kB", formatFileSize(9_960))
    }
}
