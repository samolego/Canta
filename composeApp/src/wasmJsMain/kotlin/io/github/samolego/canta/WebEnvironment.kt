package io.github.samolego.canta

import io.github.samolego.canta.core.CantaAppDependencies
import io.github.samolego.canta.core.WebCantaHandler
import io.github.samolego.canta.core.WebCantaPlatform
import io.github.samolego.canta.core.cantaAppDependencies
import io.github.samolego.canta.data.LocalStorageAppStorage

/**
 * Builds the [CantaAppDependencies] for the web (WASM) target. Everything,
 * including the bloat list cache, persists in `localStorage`.
 */
fun provideWebDependencies(): CantaAppDependencies =
    cantaAppDependencies(
        storage = { name -> LocalStorageAppStorage("canta/$name") },
        platform = WebCantaPlatform(),
        handler = { WebCantaHandler() },
    )
