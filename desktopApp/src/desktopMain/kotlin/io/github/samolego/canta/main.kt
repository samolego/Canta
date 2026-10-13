package io.github.samolego.canta

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.samolego.canta.ui.CantaApp

fun main() = application {
    val deps = provideDesktopDependencies()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Canta",
        state = rememberWindowState(width = 420.dp, height = 780.dp)
    ) {
        CantaApp(deps = deps, closeApp = ::exitApplication)
    }
}
