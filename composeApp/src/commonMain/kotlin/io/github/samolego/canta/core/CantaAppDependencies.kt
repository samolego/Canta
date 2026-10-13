package io.github.samolego.canta.core

import io.github.samolego.canta.data.AppBlobStorage
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.bloat.BloatRepository
import io.github.samolego.canta.data.preset.CantaPresetStore
import io.ktor.client.HttpClient

/**
 * Bundle of the platform implementations handed to the UI.
 * Built once per platform at application start (see [cantaAppDependencies])
 * and passed down explicitly, keeping the UI free of global singletons.
 */
class CantaAppDependencies(
    val handler: CantaHandler,
    val settings: CantaSettings,
    val presetStore: CantaPresetStore,
    val platform: CantaPlatform,
    val bloatRepository: BloatRepository,
)

/** Names of the persisted blobs; each platform maps them to a file or storage key. */
object StoredData {
    const val SETTINGS = "app_settings.pb"
    const val PRESETS = "presets.pb"
    const val BLOAT_LIST = "bloat_list.json"
}

/**
 * Wires up the shared stores and services. Platforms only decide where data
 * lives ([storage], keyed by [StoredData] names) and provide their
 * [platform] and [handler].
 */
fun cantaAppDependencies(
    storage: (name: String) -> AppBlobStorage,
    platform: CantaPlatform,
    handler: (CantaSettings) -> CantaHandler,
): CantaAppDependencies {
    val settings = CantaSettings(storage(StoredData.SETTINGS))
    return CantaAppDependencies(
        handler = handler(settings),
        settings = settings,
        presetStore = CantaPresetStore(storage(StoredData.PRESETS)),
        platform = platform,
        // Ktor picks the platform's engine: OkHttp on Android/desktop (the
        // jvmCommonMain dependency), the browser's fetch on web.
        bloatRepository = BloatRepository(HttpClient(), storage(StoredData.BLOAT_LIST), settings),
    )
}
