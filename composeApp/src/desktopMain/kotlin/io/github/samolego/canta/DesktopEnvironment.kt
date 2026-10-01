package io.github.samolego.canta

import io.github.samolego.canta.core.CantaAppDependencies
import io.github.samolego.canta.core.DesktopCantaHandler
import io.github.samolego.canta.core.DesktopCantaPlatform
import io.github.samolego.canta.core.cantaAppDependencies
import io.github.samolego.canta.data.FileAppStorage
import java.io.File

/** Builds the [CantaAppDependencies] for the desktop target; data lives in `~/.canta`. */
fun provideDesktopDependencies(): CantaAppDependencies {
    val directory = File(System.getProperty("user.home") ?: ".", ".canta")
    return cantaAppDependencies(
        storage = { name -> FileAppStorage(File(directory, name)) },
        platform = DesktopCantaPlatform(),
        handler = { DesktopCantaHandler() },
    )
}
