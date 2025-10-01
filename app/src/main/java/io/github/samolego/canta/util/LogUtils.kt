package io.github.samolego.canta.util

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtils {
    private val logs = mutableStateListOf<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog(LogLevel.WARNING, tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        addLog(LogLevel.ERROR, tag, message + (throwable?.message?.let { "\n$it" } ?: ""))
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun addLog(level: LogLevel, tag: String, message: String) {
        // We need to run in main thread due to the logs being
        // a not thread-safe list
        GlobalScope.launch(Dispatchers.Main) {
            logs.add(LogEntry(level, tag, message, System.currentTimeMillis()))
        }
    }

    fun getLogs(): List<LogEntry> = logs

    data class LogEntry(
            val level: LogLevel,
            val tag: String,
            val message: String,
            val timestamp: Long
    ) {
        fun getFormattedTime(): String = dateFormat.format(Date(timestamp))
    }

    enum class LogLevel(val color: Color) {
        DEBUG(Color.Gray),
        INFO(Color.Green),
        WARNING(Color.Yellow),
        ERROR(Color.Red)
    }
}
