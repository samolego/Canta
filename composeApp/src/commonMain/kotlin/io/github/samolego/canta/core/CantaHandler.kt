package io.github.samolego.canta.core

import androidx.compose.ui.graphics.ImageBitmap
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.packages.OperationResult
import kotlinx.coroutines.flow.Flow

/**
 * Status of the privileged runtime (Shizuku / Sui on Android).
 */
enum class PrivilegeStatus {
    /** Privileged runtime is running and Canta is authorized. */
    ACTIVE,

    /** Runtime is running but hasn't authorized Canta yet. */
    NOT_AUTHORIZED,

    /** Runtime is installed but its service isn't running. */
    NOT_RUNNING,

    /** No privileged runtime is installed (always the case on desktop and web). */
    NOT_AVAILABLE
}

/**
 * OS-specific flavor of [DeviceConnectionFailure.AccessDenied], so the UI
 * can point at the real fix (udev rules on Linux, WinUSB driver on Windows)
 * instead of generic replug advice.
 */
enum class UsbAccessHint {
    Generic,
    LinuxUdev,
    WindowsDriver,
}

/**
 * Why connecting to a device failed. Carried from the handler to the UI so
 * failures can be explained instead of silently reloading the device list.
 */
sealed interface DeviceConnectionFailure {
    /** The OS/browser refused to open the device (e.g. WebUSB access denied). */
    data class AccessDenied(val hint: UsbAccessHint = UsbAccessHint.Generic) : DeviceConnectionFailure

    /** The device is already open, e.g. claimed by another tab or program. */
    data class DeviceBusy(val hint: UsbAccessHint = UsbAccessHint.Generic) : DeviceConnectionFailure

    /** The selected device disappeared from discovery. */
    data object DeviceUnavailable : DeviceConnectionFailure

    /** Canta's on-device helper couldn't be placed or verified on the device. */
    data object HelperUnavailable : DeviceConnectionFailure

    /** The device runs an Android version older than Canta supports. */
    data object UnsupportedAndroidVersion : DeviceConnectionFailure

    /** The platform cannot reach devices at all (e.g. no WebUSB support). */
    data object UnsupportedBrowser : DeviceConnectionFailure

    /** Anything else, with the technical detail for diagnosis. */
    data class Unknown(val detail: String) : DeviceConnectionFailure
}

/**
 * A physical device that Canta can manage over ADB (desktop) or WebUSB (web).
 *
 * @param id opaque identifier used to reconnect ([CantaHandler.selectDevice]).
 * @param displayName human-readable name (model, or serial when unknown).
 * @param detail secondary line (usually the serial), or null.
 */
data class CantaDevice(
    val id: String,
    val displayName: String,
    val detail: String? = null,
)

/** How the user picks the device Canta manages. */
enum class DeviceDiscovery {
    /** Always the local device (Android, through Shizuku). */
    None,

    /** Canta lists reachable devices itself (desktop, through ADB). */
    List,

    /** A system picker grants access to and selects a device (web, the WebUSB chooser). */
    SystemPicker,
}

/**
 * Abstract contract that takes care of the platform-specific, low-level
 * operations: loading installed/uninstalled apps and uninstalling/reinstalling.
 *
 * Each target provides its own implementation:
 *  - Android: powered by Shizuku + hidden API bypass.
 *  - Desktop: powered by ADB (dadb).
 *  - Web/WASM: powered by WebUSB (ya-webadb).
 *
 * Operations report their real outcome: they return once the device has
 * answered, not when the command was merely dispatched.
 */
interface CantaHandler {
    val isAuthorized: Boolean

    /** State of the privileged runtime; only meaningful on Android (Shizuku). */
    val privilegeStatus: PrivilegeStatus
        get() = if (isAuthorized) PrivilegeStatus.ACTIVE else PrivilegeStatus.NOT_AVAILABLE

    /** Installed apps and apps uninstalled for the user (still on the device). */
    suspend fun loadApps(): List<AppInfo>

    /**
     * Icons for the apps from the last [loadApps], as (package name, icon)
     * pairs streamed in display order, so the list can show before they are
     * all rendered: in-process on Android, by the on-device helper over ADB.
     */
    fun loadIcons(): Flow<Pair<String, ImageBitmap>>

    /** Whether [packageName] is an updated system app that can be reset to its factory version. */
    suspend fun canResetToFactory(packageName: String): Boolean

    /**
     * Uninstalls [packageNames] for the user, optionally resetting updated
     * system apps to their factory version first. Emits one result per
     * package as each one completes.
     */
    fun uninstallApps(packageNames: List<String>, resetToFactory: Boolean = false): Flow<OperationResult>

    /** Reinstalls apps previously uninstalled for the user; one result per package. */
    fun reinstallApps(packageNames: List<String>): Flow<OperationResult>

    /** Whether the given package is on the device (installed or uninstalled for the user). */
    suspend fun packageExists(packageName: String): Boolean

    /**
     * Opens the system's app details (App info) page for [packageName]:
     * the Settings screen on Android itself, the on-device Settings screen
     * over ADB elsewhere. Only called for installed apps.
     */
    suspend fun openAppDetails(packageName: String) {}

    /**
     * Requests authorization from the privileged runtime (Android: the
     * Shizuku permission prompt). Only used where [deviceDiscovery] is
     * [DeviceDiscovery.None].
     */
    suspend fun requestAuthorization() {}

    /** How the device is chosen; see [DeviceDiscovery]. */
    val deviceDiscovery: DeviceDiscovery
        get() = DeviceDiscovery.None

    /** Whether the user must pick a device before apps can load. */
    val requiresDeviceSelection: Boolean
        get() = deviceDiscovery != DeviceDiscovery.None

    /** Devices available for selection ([DeviceDiscovery.List] only). */
    suspend fun availableDevices(): List<CantaDevice> = emptyList()

    /** Connects to a device previously returned by [availableDevices]. */
    suspend fun selectDevice(device: CantaDevice) {}

    /**
     * Opens the system device picker and connects to the chosen device
     * ([DeviceDiscovery.SystemPicker] only). Returns null when cancelled/failed.
     */
    suspend fun pickSystemDevice(): CantaDevice? = null

    /**
     * Whether the platform can reach devices at all. Web returns false when
     * the browser has no WebUSB support; everything else defaults to true.
     */
    val connectionSupported: Boolean
        get() = true

    /** Why the last connection attempt failed, if any, so the UI can explain it. */
    val lastConnectionFailure: DeviceConnectionFailure?
        get() = null
}
