package io.github.samolego.canta.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Forwards a log line to the platform log (logcat, stderr, browser console). */
internal expect fun platformLog(level: LogUtils.LogLevel, tag: String, message: String, throwable: Throwable?)

/**
 * In-app log shown on the logs page, mirrored to the platform log.
 */
object LogUtils {
    private val logs = mutableStateListOf<LogEntry>()

    private val TIME_FORMAT = LocalTime.Format {
        hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
    }

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    fun w(tag: String, message: String) = log(LogLevel.WARNING, tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, throwable)

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        platformLog(level, tag, message, throwable)
        val text = throwable?.let { "$message\n${it.stackTraceToString()}" } ?: message
        logs.add(LogEntry(level, tag, text, currentTimeMillis()))
    }

    fun getLogs(): List<LogEntry> = logs

    data class LogEntry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val timestamp: Long,
    ) {
        /** Local wall-clock time, e.g. `13:07:57.528`. */
        fun getFormattedTime(): String =
            TIME_FORMAT.format(
                Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).time
            )

        /** The line shown on the logs page and copied to the clipboard. */
        fun format(): String = "[${getFormattedTime()}] $level $tag: $message"
    }

    enum class LogLevel(val color: Color) {
        INFO(Color.Green),
        WARNING(Color.Yellow),
        ERROR(Color.Red)
    }
}
