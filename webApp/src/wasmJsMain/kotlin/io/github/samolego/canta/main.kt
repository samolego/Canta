package io.github.samolego.canta

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.samolego.canta.ui.CantaApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val deps = provideWebDependencies()

    ComposeViewport(viewportContainerId = "cantaApp") {
        CantaApp(deps = deps, closeApp = {})
    }
}
