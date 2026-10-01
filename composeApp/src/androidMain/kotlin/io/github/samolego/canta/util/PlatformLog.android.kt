package io.github.samolego.canta.util

import android.util.Log

internal actual fun platformLog(level: LogUtils.LogLevel, tag: String, message: String, throwable: Throwable?) {
    when (level) {
        LogUtils.LogLevel.INFO -> Log.i(tag, message, throwable)
        LogUtils.LogLevel.WARNING -> Log.w(tag, message, throwable)
        LogUtils.LogLevel.ERROR -> Log.e(tag, message, throwable)
    }
}
