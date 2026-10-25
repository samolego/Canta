package io.github.samolego.canta.core.adb

import androidx.compose.ui.graphics.ImageBitmap
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.DeviceConnectionFailure
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.packages.OperationResult
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.packages.canResetToFactory
import io.github.samolego.canta.packages.isSafeDeletionPath
import io.github.samolego.canta.packages.normalizeFsPath
import io.github.samolego.canta.packages.shellQuote
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
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

    /** Reads packages through the helper and caches them for subsequent lookups. */
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

    /** Streams helper icons, dropping undecodable images without aborting the stream. */
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

    /** Checks the cached package metadata for a removable system-app update. */
    override suspend fun canResetToFactory(packageName: String): Boolean =
        packages[packageName]?.canResetToFactory == true

    /** Streams helper uninstall results, or emits nothing when disconnected. */
    override fun uninstallApps(packageNames: List<String>, resetToFactory: Boolean): Flow<OperationResult> =
        if (isConnected) helper.uninstall(packageNames, resetToFactory) else emptyFlow()

    /** Streams helper reinstall results, or emits nothing when disconnected. */
    override fun reinstallApps(packageNames: List<String>): Flow<OperationResult> =
        if (isConnected) helper.reinstall(packageNames) else emptyFlow()

    /** Deletes each requested device path and emits one result per path, including failures. */
    override fun deletePaths(paths: List<String>): Flow<OperationResult> = flow {
        for (path in paths) {
            emit(deletePath(path))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Deletes [path] through `adb shell rm -rf`. The normalized path is
     * single-quoted for the remote shell and re-checked against the
     * user-storage allowlist.
     */
    private suspend fun deletePath(path: String): OperationResult {
        val normalized = normalizeFsPath(path)
        if (normalized == null || !isSafeDeletionPath(path)) {
            LogUtils.w(TAG, "Refusing to delete '$path'")
            return failed(path, "Path is not in a user-storage area")
        }
        if (!isConnected) return failed(path, "Not connected")
        return try {
            val result = transport.shell("rm -rf ${shellQuote(normalized)}")
            if (result.exitCode == 0) {
                OperationResult(path, success = true, status = 0)
            } else {
                failed(path, result.stderr.ifBlank { "rm exited with ${result.exitCode}" })
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Deleting '$path' failed", e)
            failed(path, e.message ?: e.toString())
        }
    }

    /** Creates a failed deletion result whose package-name field identifies the path. */
    private fun failed(path: String, message: String) =
        OperationResult(path, success = false, status = OperationResult.STATUS_NO_RESULT, message = message)

    /** Checks cached packages, loading them first when the cache is empty. */
    override suspend fun packageExists(packageName: String): Boolean {
        if (!isConnected) return false
        if (packages.isEmpty()) loadApps()
        return packageName in packages
    }

    /** Opens device settings for a validated package name while connected. */
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
