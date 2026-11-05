package io.github.samolego.canta.util

import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** [CrashReportStore] backed by a single file in the app's private storage. */
class FileCrashReportStore(private val file: File) : CrashReportStore {
    override fun read(): String? = runCatching { if (file.exists()) file.readText() else null }.getOrNull()

    override fun delete() {
        runCatching { file.delete() }
    }

    /**
     * Installs an uncaught-exception handler that writes a [CrashReport] to the file
     * and then hands over to the previous handler.
     */
    @OptIn(ExperimentalTime::class)
    fun installHandler(appVersion: String, platform: String, osVersion: String) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val report = CrashReport.format(
                    appVersion = appVersion,
                    platform = platform,
                    osVersion = osVersion,
                    timestamp = Clock.System.now().toString(),
                    stackTrace = throwable.stackTraceToString(),
                )
                file.absoluteFile.parentFile?.mkdirs()
                file.writeText(report)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
