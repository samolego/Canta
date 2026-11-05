package io.github.samolego.canta.util

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashReportTest {

    private fun report(trace: String) = CrashReport.format("1.2.3", "desktop", "Linux 6.1", "2026-10-29T10:00:00Z", trace)

    @Test
    fun formatsHeaderAndStackTrace() {
        val text = report("java.lang.IllegalStateException: boom\n\tat a.B.c(B.kt:1)")
        assertEquals(
            "Canta 1.2.3\nPlatform: desktop\nOS: Linux 6.1\nTime: 2026-10-29T10:00:00Z\n\n" +
                "java.lang.IllegalStateException: boom\n\tat a.B.c(B.kt:1)",
            text,
        )
    }

    @Test
    fun capsLongReportsKeepingTheHeader() {
        val text = report("x".repeat(100_000))
        assertEquals(CrashReport.MAX_LENGTH, text.length)
        assertTrue(text.startsWith("Canta 1.2.3\n"))
        assertTrue(text.endsWith("(truncated)"))
    }

    @Test
    fun shortReportIsNotTruncated() {
        assertEquals("abc", CrashReport.truncate("abc", 3))
    }

    @Test
    fun issueUrlIsEncodedAndPointsAtTheRepository() {
        val url = CrashReport.issueUrl(report("java.lang.IllegalStateException: boom & more"))
        assertTrue(url.startsWith("https://github.com/samolego/Canta/issues/new?title="))
        assertContains(url, "&body=")
        assertContains(url, "boom%20%26%20more")
        assertTrue(' ' !in url && '\n' !in url)
    }

    @Test
    fun issueUrlStaysWithinTheLimitEvenWhenEncodingGrowsIt() {
        // Newlines, tabs and non-ASCII characters expand 3x or more when encoded.
        val trace = "java.lang.Error: x\n" + "\tat ščž.Ü(Ü.kt:1)\n".repeat(2_000)
        val url = CrashReport.issueUrl(report(trace))
        assertTrue(url.length <= CrashReport.MAX_URL_LENGTH, "url was ${url.length} chars")
        assertContains(url, "Canta%201.2.3")
    }

    @Test
    fun shortReportFitsInTheUrlInFull() {
        val url = CrashReport.issueUrl("Canta 1\n\nError: x")
        assertTrue(url.endsWith("&body=Canta%201%0A%0AError%3A%20x"), url)
    }
}
