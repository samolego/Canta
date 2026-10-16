package io.github.samolego.canta.core.adb

import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.packages.AppIcon
import io.github.samolego.canta.packages.HelperFraming
import io.github.samolego.canta.packages.OperationResult
import io.github.samolego.canta.packages.isValidPackageName
import io.github.samolego.canta.packages.PROTOCOL_VERSION
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.packages.PackageDetailsList
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer

/**
 * Drives the on-device helper (the `:helper` module): a dex-only APK run as
 * the shell user via `app_process`, which reads, uninstalls and reinstalls
 * packages through the framework's `PackageManager`/`PackageInstaller`, with
 * the same code the Android app runs through Shizuku.
 *
 * The bundled APK is pushed to [REMOTE_PATH] only when the copy on the device
 * is missing or its SHA-256 differs from [AdbHelperArtifact.SHA256].
 */
class AdbHelper(private val transport: AdbTransport) {

    companion object {
        private const val TAG = "AdbHelper"
        const val REMOTE_PATH = "/data/local/tmp/canta-helper.apk"
        private const val MAIN_CLASS = "io.github.samolego.canta.helper.Main"
        /** Package names per helper run; keeps commands well under adbd's length limits. */
        private const val OPERATION_CHUNK_SIZE = 50
        /**
         * `st_mode` for the pushed file: regular file (`S_IFREG`, octal
         * 0100000) with rw-r--r-- (octal 0644). adbd expects the file type
         * bits; without them it ignores the permissions.
         */
        private const val FILE_MODE = 0x8000 or 0b110_100_100
    }

    private val installMutex = Mutex()

    /** Whether the helper on the device is verified to match the bundled one. */
    private var installed = false

    /**
     * Whether the device runs an Android version the helper supports
     * ([AdbHelperArtifact.MIN_SDK]). A version that can't be read is let
     * through; setting the helper up then decides.
     */
    suspend fun supportsDevice(): Boolean {
        val sdk = runCatching { transport.shell("getprop ro.build.version.sdk").stdout.trim().toIntOrNull() }.getOrNull()
        if (sdk == null || sdk >= AdbHelperArtifact.MIN_SDK) return true
        LogUtils.w(TAG, "Device runs SDK $sdk; the helper needs ${AdbHelperArtifact.MIN_SDK}")
        return false
    }

    /**
     * Makes sure the current helper is on the device, pushing it if needed.
     * Returns whether it is ready to use.
     */
    suspend fun ensureInstalled(): Boolean = installMutex.withLock {
        if (installed) return true
        installed = try {
            if (remoteChecksum() == AdbHelperArtifact.SHA256) {
                LogUtils.i(TAG, "Helper on device is up to date")
            } else {
                LogUtils.i(TAG, "Pushing helper to $REMOTE_PATH")
                transport.push(Res.readBytes(AdbHelperArtifact.RESOURCE_PATH), REMOTE_PATH, FILE_MODE)
            }
            // Verify: a missing `sha256sum` or failed push must not pass.
            (remoteChecksum() == AdbHelperArtifact.SHA256).also { ok ->
                if (!ok) LogUtils.w(TAG, "Helper checksum mismatch after push")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to install helper", e)
            false
        }
        installed
    }

    /** All installed and uninstalled packages, or null if the helper failed. */
    suspend fun packages(): List<PackageDetails>? {
        val list = runCommand("info", PackageDetailsList.serializer()) ?: return null
        if (list.protocolVersion != PROTOCOL_VERSION) {
            // Can only happen if the checksum check was bypassed; don't trust the data.
            LogUtils.w(TAG, "Helper speaks protocol ${list.protocolVersion}, expected $PROTOCOL_VERSION")
            return null
        }
        return list.packages
    }

    /**
     * Every app's PNG icon, streamed in display order (installed apps first,
     * alphabetically) as the helper renders them. Stops quietly on failure,
     * so icons already emitted stay useful.
     */
    fun icons(sizePx: Int): Flow<AppIcon> = flow {
        if (!ensureInstalled()) return@flow
        transport.shellLines(helperCommand("icons $sizePx")).collect { line ->
            // Skip anything that isn't a message, e.g. linker warnings.
            runCatching { HelperFraming.decodeLine(AppIcon.serializer(), line) }.getOrNull()?.let { emit(it) }
        }
    }.catch { e ->
        LogUtils.e(TAG, "Helper icon stream failed", e)
        installMutex.withLock { installed = false }
    }

    /** Uninstalls [packageNames] for the user; one result per package, as each completes. */
    fun uninstall(packageNames: List<String>, resetToFactory: Boolean): Flow<OperationResult> =
        operate(if (resetToFactory) "uninstall --reset" else "uninstall", packageNames)

    /** Reinstalls [packageNames] for the user; one result per package, as each completes. */
    fun reinstall(packageNames: List<String>): Flow<OperationResult> = operate("reinstall", packageNames)

    /** Disables [packageNames] for the user; one result per package, as each completes. */
    fun disable(packageNames: List<String>): Flow<OperationResult> = operate("disable", packageNames)

    /** Enables [packageNames] for the user; one result per package, as each completes. */
    fun enable(packageNames: List<String>): Flow<OperationResult> = operate("enable", packageNames)

    /**
     * Runs a package [command] in chunks of [OPERATION_CHUNK_SIZE] names and
     * streams its results. Every package gets exactly one result: invalid
     * names and packages the helper never reported on count as failed.
     */
    private fun operate(command: String, packageNames: List<String>): Flow<OperationResult> = flow {
        val (valid, invalid) = packageNames.partition(::isValidPackageName)
        invalid.forEach { emit(failed(it, "Invalid package name")) }
        if (valid.isEmpty()) return@flow
        if (!ensureInstalled()) {
            valid.forEach { emit(failed(it, "Helper unavailable")) }
            return@flow
        }
        for (chunk in valid.chunked(OPERATION_CHUNK_SIZE)) {
            val pending = chunk.toMutableSet()
            try {
                transport.shellLines(helperCommand("$command ${chunk.joinToString(" ")}")).collect { line ->
                    val result = runCatching { HelperFraming.decodeLine(OperationResult.serializer(), line) }.getOrNull()
                    if (result != null && pending.remove(result.packageName)) emit(result)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Helper '$command' failed", e)
                installMutex.withLock { installed = false }
            }
            pending.forEach { emit(failed(it, "No result from helper")) }
        }
    }

    private fun failed(packageName: String, message: String) =
        OperationResult(packageName, success = false, status = OperationResult.STATUS_NO_RESULT, message = message)

    private fun helperCommand(command: String) = "CLASSPATH=$REMOTE_PATH app_process /system/bin $MAIN_CLASS $command"

    private suspend fun <T> runCommand(command: String, serializer: KSerializer<T>): T? {
        if (!ensureInstalled()) return null
        return try {
            val result = transport.shell(helperCommand(command))
            if (result.exitCode != 0) {
                LogUtils.w(TAG, "Helper '$command' exited with ${result.exitCode}: ${result.stderr.take(500)}")
                // Possibly deleted or replaced behind our back; re-verify next time.
                installMutex.withLock { installed = false }
                return null
            }
            // The message is the last line; some devices print linker warnings first.
            HelperFraming.decodeLine(serializer, result.stdout.lineSequence().last { it.isNotBlank() })
        } catch (e: Exception) {
            LogUtils.e(TAG, "Helper '$command' failed", e)
            null
        }
    }

    private suspend fun remoteChecksum(): String? {
        val result = transport.shell("sha256sum $REMOTE_PATH 2>/dev/null")
        return result.stdout.trim().substringBefore(' ').takeIf { result.exitCode == 0 && it.length == 64 }
    }
}
