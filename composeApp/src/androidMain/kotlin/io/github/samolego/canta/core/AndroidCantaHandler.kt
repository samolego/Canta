package io.github.samolego.canta.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import io.github.samolego.canta.core.shizuku.ShizukuPermission
import io.github.samolego.canta.core.shizuku.ShizukuProcess
import io.github.samolego.canta.core.shizuku.ShizukuServices
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.packages.OperationResult
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.packages.PackageOperations
import io.github.samolego.canta.packages.canResetToFactory
import io.github.samolego.canta.packages.getAllPackages
import io.github.samolego.canta.packages.getInfoForPackage
import io.github.samolego.canta.packages.inDisplayOrder
import io.github.samolego.canta.packages.isSafeDeletionPath
import io.github.samolego.canta.packages.loadIconBitmap
import io.github.samolego.canta.packages.normalizeFsPath
import io.github.samolego.canta.packages.readSafely
import io.github.samolego.canta.packages.toPackageDetails
import io.github.samolego.canta.ui.component.APP_ICON_SIZE
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Android [CantaHandler]: reads packages in-process and changes them through
 * Shizuku, with the same [PackageOperations] the ADB helper uses.
 */
class AndroidCantaHandler(
    private val context: Context,
) : CantaHandler {

    private companion object {
        const val TAG = "AndroidCantaHandler"

        /** A large folder tree can take a while for `rm -rf`; give it a generous bound. */
        const val DELETE_PATH_TIMEOUT_MS = 300_000L
    }

    private val packageManager: PackageManager get() = context.packageManager

    private val operations by lazy { PackageOperations(ShizukuServices, context) }

    /**
     * Runs the hundreds of package reads and icon renders. Not
     * [Dispatchers.Default]: Compose resources load there while the main
     * thread waits for them (`runBlocking` in `stringResource`), so a queue of
     * our work on it would freeze the UI until the queue drains.
     */
    private val packageDispatcher = Dispatchers.IO.limitedParallelism(Runtime.getRuntime().availableProcessors())

    private val iconSizePx: Int
        get() = (APP_ICON_SIZE.value * context.resources.displayMetrics.density).roundToInt()

    /** The packages from the last [loadApps], whose icons [loadIcons] renders. */
    @Volatile
    private var packages: List<Pair<PackageDetails, ApplicationInfo?>> = emptyList()

    override val isAuthorized: Boolean
        get() = ShizukuPermission.isCantaAuthorized()

    override val privilegeStatus: PrivilegeStatus
        get() = ShizukuPermission.privilegeStatus(packageManager)

    override suspend fun loadApps(): List<AppInfo> = withContext(packageDispatcher) {
        // Reading labels loads each app's resources, which is slow on a cold
        // start; read packages in parallel, like the ADB helper does.
        packages = packageManager.getAllPackages()
            .map { info -> async { readSafely(info) { info.toPackageDetails(packageManager) to info.applicationInfo } } }
            .awaitAll()
            .filterNotNull()
        packages.map { (details, _) -> AppInfo(details) }
    }

    /**
     * Renders icons in parallel but emits them in display order, so the list
     * fills in top-down while the rest are still rendering.
     */
    override fun loadIcons(): Flow<Pair<String, ImageBitmap>> = flow {
        val appInfos = packages.associate { (details, appInfo) -> details.packageName to appInfo }
        val sizePx = iconSizePx
        coroutineScope {
            val icons = packages.map { it.first }.inDisplayOrder().map { details ->
                async(packageDispatcher) {
                    appInfos[details.packageName]
                        ?.let { packageManager.loadIconBitmap(it, sizePx) }
                        ?.let { details.packageName to it.asImageBitmap() }
                }
            }
            icons.forEach { icon -> icon.await()?.let { emit(it) } }
        }
    }

    override suspend fun canResetToFactory(packageName: String): Boolean =
        packageManager.getInfoForPackage(packageName)?.applicationInfo?.canResetToFactory == true

    override fun uninstallApps(packageNames: List<String>, resetToFactory: Boolean): Flow<OperationResult> =
        flow { packageNames.forEach { emit(operations.uninstall(it, resetToFactory)) } }.flowOn(Dispatchers.IO)

    override fun reinstallApps(packageNames: List<String>): Flow<OperationResult> =
        flow { packageNames.forEach { emit(operations.reinstall(it)) } }.flowOn(Dispatchers.IO)

    override fun deletePaths(paths: List<String>): Flow<OperationResult> =
        flow { paths.forEach { emit(deletePath(it)) } }.flowOn(Dispatchers.IO)

    /**
     * Deletes [path]. Shared-storage targets go through the filesystem API,
     * which needs "All files access" (`MANAGE_EXTERNAL_STORAGE`, or
     * WRITE_EXTERNAL_STORAGE before API 30). `/data/local/tmp` — unreachable
     * via the File API — stays on `rm -rf` under Shizuku's identity; the
     * command is passed as argv to [ShizukuProcess.exec], and every path is
     * re-checked against the user-storage allowlist first.
     */
    private fun deletePath(path: String): OperationResult {
        val normalized = normalizeFsPath(path)
        if (normalized == null || !isSafeDeletionPath(path)) {
            LogUtils.w(TAG, "Refusing to delete '$path'")
            return failed(path, "Path is not in a user-storage area")
        }
        return if (normalized.startsWith("/data/local/tmp/")) {
            deleteViaShizuku(path, normalized)
        } else {
            deleteViaFileApi(path, normalized)
        }
    }

    private fun failed(path: String, message: String) =
        OperationResult(path, success = false, status = OperationResult.STATUS_NO_RESULT, message = message)

    private fun deleteViaFileApi(path: String, normalized: String): OperationResult {
        if (!hasAllFilesAccess()) {
            return failed(path, "All files access is not granted")
        }
        return try {
            if (File(normalized).deleteRecursively()) {
                OperationResult(path, success = true, status = 0)
            } else {
                failed(path, "Could not delete $normalized")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Deleting '$path' failed", e)
            failed(path, e.message ?: e.toString())
        }
    }

    private fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun deleteViaShizuku(path: String, normalized: String): OperationResult {
        return try {
            val result = ShizukuProcess.execForResult(DELETE_PATH_TIMEOUT_MS, "rm", "-rf", normalized)
            when {
                result == null -> failed(path, "Deletion timed out")
                result.first == 0 -> OperationResult(path, success = true, status = 0)
                else -> OperationResult(path, success = false, status = result.first, message = result.second)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Deleting '$path' failed", e)
            failed(path, e.message ?: e.toString())
        }
    }

    override suspend fun requestAuthorization() {
        val deferred = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            ShizukuPermission.requestShizukuPermission { result ->
                deferred.complete(result == PackageManager.PERMISSION_GRANTED)
            }
        }
        deferred.await()
    }

    override suspend fun packageExists(packageName: String): Boolean =
        packageManager.getInfoForPackage(packageName) != null

    override suspend fun openAppDetails(packageName: String) {
        try {
            val uri = android.net.Uri.fromParts("package", packageName, null)
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to open app details for '$packageName'", e)
        }
    }
}
