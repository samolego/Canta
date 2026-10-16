package io.github.samolego.canta.core.adb

import androidx.compose.ui.graphics.ImageBitmap
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.DeviceConnectionFailure
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.packages.OperationResult
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.packages.canResetToFactory
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Everything the ADB-backed handlers (desktop via dadb, web via WebUSB) share.
 * All package work goes through the on-device [AdbHelper]; subclasses only
 * implement discovering and connecting devices.
 */
abstract class AdbCantaHandler(private val transport: AdbTransport) : CantaHandler {

    private companion object {
        const val TAG = "AdbCantaHandler"
        const val ICON_SIZE_PX = 96
    }

    private val helper = AdbHelper(transport)

    /** The packages from the last [loadApps], for lookups that need no device round trip. */
    private var packages: Map<String, PackageDetails> = emptyMap()

    /** Whether a device is connected and the transport can reach it. */
    protected abstract val isConnected: Boolean

    override val isAuthorized: Boolean
        get() = isConnected

    final override var lastConnectionFailure: DeviceConnectionFailure? = null
        protected set

    /**
     * Call once a device is connected: sets up the helper, without which
     * nothing works. Returns false (with [lastConnectionFailure] set) if the
     * device is too old for it or it can't be set up; the caller should then
     * drop the connection.
     */
    protected suspend fun onConnected(): Boolean {
        packages = emptyMap()
        lastConnectionFailure = when {
            !helper.supportsDevice() -> DeviceConnectionFailure.UnsupportedAndroidVersion
            !helper.ensureInstalled() -> DeviceConnectionFailure.HelperUnavailable
            else -> return true
        }
        return false
    }

    override suspend fun loadApps(): List<AppInfo> {
        if (!isConnected) return emptyList()
        val details = helper.packages()
        if (details == null) {
            LogUtils.w(TAG, "Could not read packages from the helper")
            return emptyList()
        }
        packages = details.associateBy { it.packageName }
        return details.map { AppInfo(it) }
    }

    override fun loadIcons(): Flow<Pair<String, ImageBitmap>> {
        if (!isConnected) return emptyFlow()
        return helper.icons(ICON_SIZE_PX)
            .mapNotNull { icon ->
                runCatching { icon.packageName to icon.png.decodeToImageBitmap() }
                    .onFailure { LogUtils.w(TAG, "Undecodable icon for '${icon.packageName}': $it") }
                    .getOrNull()
            }
            .flowOn(Dispatchers.Default) // Decoding PNGs is CPU work; keep it off the UI thread.
    }

    override suspend fun canResetToFactory(packageName: String): Boolean =
        packages[packageName]?.canResetToFactory == true

    override fun uninstallApps(packageNames: List<String>, resetToFactory: Boolean): Flow<OperationResult> =
        if (isConnected) helper.uninstall(packageNames, resetToFactory) else emptyFlow()

    override fun reinstallApps(packageNames: List<String>): Flow<OperationResult> =
        if (isConnected) helper.reinstall(packageNames) else emptyFlow()

    override fun disableApps(packageNames: List<String>): Flow<OperationResult> =
        if (isConnected) helper.disable(packageNames) else emptyFlow()

    override fun enableApps(packageNames: List<String>): Flow<OperationResult> =
        if (isConnected) helper.enable(packageNames) else emptyFlow()

    override suspend fun packageExists(packageName: String): Boolean {
        if (!isConnected) return false
        if (packages.isEmpty()) loadApps()
        return packageName in packages
    }

    override suspend fun openAppDetails(packageName: String) {
        if (!isConnected) return
        if (packageName.any { !it.isLetterOrDigit() && it != '.' && it != '_' }) {
            LogUtils.w(TAG, "Refusing to open app details for suspicious package name")
            return
        }
        runCatching {
            transport.shell(
                "am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:$packageName"
            )
        }.onFailure { LogUtils.e(TAG, "Failed to open app details for '$packageName'", it) }
    }
}
