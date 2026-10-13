package io.github.samolego.canta.core.adb

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SplitLinesTest {

    @Test
    fun joinsLinesSplitAcrossChunks() = runTest {
        val lines = flowOf("ab", "c\nde", "f\r\n\ng", "h").splitLines().toList()
        assertEquals(listOf("abc", "def", "", "gh"), lines)
    }

    @Test
    fun emitsNothingForEmptyOutput() = runTest {
        assertEquals(emptyList(), flowOf("", "").splitLines().toList())
    }
}
