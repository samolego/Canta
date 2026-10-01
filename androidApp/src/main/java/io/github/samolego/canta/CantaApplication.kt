package io.github.samolego.canta

import android.app.Application
import io.github.samolego.canta.core.AndroidCantaPlatform
import io.github.samolego.canta.core.CantaAppDependencies

class CantaApplication : Application() {

    val platform: AndroidCantaPlatform by lazy { AndroidCantaPlatform(this) }

    val dependencies: CantaAppDependencies by lazy { provideAndroidDependencies(this, platform) }

    override fun onCreate() {
        super.onCreate()
        // Force-initialize dependencies (settings/presets load) eagerly.
        dependencies.handler
    }
}
