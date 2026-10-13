package io.github.samolego.canta.util

/** Desktop: stdout. Web: `println` writes to the browser console. */
internal actual fun platformLog(level: LogUtils.LogLevel, tag: String, message: String, throwable: Throwable?) {
    println("${level.name.first()}/$tag: $message" + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
}
