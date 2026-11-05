package io.github.samolego.canta.core

import io.github.samolego.canta.data.AppBlobStorage
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.bloat.BloatRepository
import io.github.samolego.canta.data.preset.CantaPresetStore
import io.github.samolego.canta.util.PackageTelemetry
import io.github.samolego.canta.util.Telemetry
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

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
    val telemetry: Telemetry,
    /** Shared, so the UI and background checks use the same lock around the pending uninstalls. */
    val packageTelemetry: PackageTelemetry,
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
    /** Reported with analytics: "android", "desktop" or "web". */
    platformName: String,
    handler: (CantaSettings) -> CantaHandler,
): CantaAppDependencies {
    val settings = CantaSettings(storage(StoredData.SETTINGS))
    val client = HttpClient()
    val cantaHandler = handler(settings)
    val telemetry = Telemetry(
        client = client,
        platform = platformName,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        isEnabled = { settings.analyticsEnabledFlow.first() },
    )
    return CantaAppDependencies(
        handler = cantaHandler,
        settings = settings,
        presetStore = CantaPresetStore(storage(StoredData.PRESETS)),
        platform = platform,
        bloatRepository = BloatRepository(client, storage(StoredData.BLOAT_LIST), settings),
        telemetry = telemetry,
        packageTelemetry = PackageTelemetry(cantaHandler, telemetry, settings),
    )
}
