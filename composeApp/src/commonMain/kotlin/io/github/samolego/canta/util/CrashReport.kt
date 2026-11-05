package io.github.samolego.canta.util

import io.ktor.http.encodeURLParameter

/**
 * Local crash reports. A report is plain text with app version, platform, OS version,
 * a timestamp and the stack trace; nothing else. It is only ever stored on the device,
 * and leaves it only if the user chooses to open a GitHub issue with it.
 */
object CrashReport {
    /** Largest stored report, in characters. */
    const val MAX_LENGTH = 16 * 1024

    /** Longest allowed issue URL; browsers and GitHub reject much more than this. */
    const val MAX_URL_LENGTH = 7000

    private const val NEW_ISSUE_URL = "https://github.com/samolego/Canta/issues/new"
    private const val TRUNCATED_MARKER = "\n... (truncated)"

    /** Builds the report text, cutting the stack trace so the result fits in [MAX_LENGTH]. */
    fun format(
        appVersion: String,
        platform: String,
        osVersion: String,
        timestamp: String,
        stackTrace: String,
    ): String {
        val header = "Canta $appVersion\nPlatform: $platform\nOS: $osVersion\nTime: $timestamp\n\n"
        return truncate(header + stackTrace, MAX_LENGTH)
    }

    /** The URL that opens a prefilled GitHub issue; the report is cut so the URL stays within [MAX_URL_LENGTH]. */
    fun issueUrl(report: String): String {
        val title = "Crash: " + (report.lineSequence().firstOrNull { it.startsWith("Caused by") || it.contains("Exception") || it.contains("Error") }
            ?.trim()?.take(80) ?: "uncaught exception")
        val prefix = "$NEW_ISSUE_URL?title=${title.encodeURLParameter()}&body="
        val budget = MAX_URL_LENGTH - prefix.length
        // Percent-encoding can grow the text up to 3x; shrink until the encoded body fits.
        var length = minOf(report.length, budget)
        var body = report.take(length).encodeURLParameter()
        while (body.length > budget && length > 0) {
            length = (length * budget / body.length).coerceAtMost(length - 1)
            body = report.take(length).encodeURLParameter()
        }
        return prefix + body
    }

    internal fun truncate(text: String, maxLength: Int): String =
        if (text.length <= maxLength) text else text.take(maxLength - TRUNCATED_MARKER.length) + TRUNCATED_MARKER
}

/** Where the last crash report is kept; only the platforms with an uncaught-exception handler have one. */
interface CrashReportStore {
    /** The stored report, or `null` when the app did not crash last time. */
    fun read(): String?

    /** Deletes the stored report. */
    fun delete()
}
