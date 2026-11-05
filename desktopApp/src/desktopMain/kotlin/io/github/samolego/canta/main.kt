package io.github.samolego.canta

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.samolego.canta.ui.CantaApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import io.github.samolego.canta.util.FileCrashReportStore
import java.io.File

fun main() = application {
    val crashReports = FileCrashReportStore(
        File(File(System.getProperty("user.home") ?: ".", ".canta"), "crash_report.txt")
    ).also {
        it.installHandler(
            appVersion = BuildInfo.VERSION_NAME,
            platform = "desktop",
            osVersion = "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
        )
    }
    val deps = provideDesktopDependencies()

    Window(
        onCloseRequest = {
            // Send queued analytics, but never hold the window open for long.
            runBlocking { withTimeoutOrNull(5000) { deps.telemetry.flush() } }
            exitApplication()
        },
        title = "Canta",
        state = rememberWindowState(width = 420.dp, height = 780.dp)
    ) {
        CantaApp(deps = deps, closeApp = ::exitApplication, crashReports = crashReports)
    }
}
