package io.github.samolego.canta

import android.content.Context
import io.github.samolego.canta.core.AndroidCantaHandler
import io.github.samolego.canta.core.AndroidCantaPlatform
import io.github.samolego.canta.core.CantaAppDependencies
import io.github.samolego.canta.core.StoredData
import io.github.samolego.canta.core.cantaAppDependencies
import io.github.samolego.canta.data.FileAppStorage
import java.io.File

/**
 * Builds the [CantaAppDependencies] for the Android target. Call once from the
 * Android application class; the MainActivity attaches/detaches itself to the
 * [platform] for activity-scoped flows (biometrics).
 */
fun provideAndroidDependencies(context: Context, platform: AndroidCantaPlatform): CantaAppDependencies {
    val appContext = context.applicationContext
    // Settings and presets stay where the DataStore-based releases wrote them,
    // so existing data is picked up in place.
    val legacyDataStoreDirectory = File(appContext.filesDir, "datastore")
    return cantaAppDependencies(
        storage = { name ->
            val directory = if (name == StoredData.BLOAT_LIST) appContext.filesDir else legacyDataStoreDirectory
            FileAppStorage(File(directory, name))
        },
        platform = platform,
        handler = { AndroidCantaHandler(appContext) },
    )
}
