@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.samolego.canta.core

import io.github.samolego.canta.core.adb.AdbCantaHandler
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.webadb.Adb
import io.github.samolego.canta.webadb.AdbDaemonWebUsbDevice
import io.github.samolego.canta.webadb.AdbDaemonWebUsbDeviceManager
import io.github.samolego.canta.webadb.authenticate
import io.github.samolego.canta.webadb.authenticateOptions
import io.github.samolego.canta.webadb.await
import io.github.samolego.canta.webadb.loadAdbModule
import io.github.samolego.canta.webadb.loadCredentialStoreModule
import io.github.samolego.canta.webadb.loadWebUsbModule
import io.github.samolego.canta.webadb.navigatorPlatform
import io.github.samolego.canta.webadb.navigatorUsb
import io.github.samolego.canta.webadb.newAdb
import io.github.samolego.canta.webadb.newCredentialStore
import io.github.samolego.canta.webadb.newWebUsbDeviceManager
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Web (WASM) [CantaHandler] powered by ya-webadb.
 *
 * [pickSystemDevice] opens the browser's WebUSB chooser, connects to the
 * chosen device and performs the ADB RSA handshake. Everything past the
 * connection is shared with the desktop target through [AdbCantaHandler].
 */
class WebCantaHandler private constructor(
    private val transport: WebAdbTransport,
) : AdbCantaHandler(transport) {

    constructor() : this(WebAdbTransport())

    companion object {
        private const val TAG = "WebCantaHandler"
    }

    override val isConnected: Boolean
        get() = transport.connection != null

    override val deviceDiscovery: DeviceDiscovery
        get() = DeviceDiscovery.SystemPicker

    override val connectionSupported: Boolean
        get() = navigatorUsb() != null

    override suspend fun pickSystemDevice(): CantaDevice? {
        lastConnectionFailure = null
        return try {
            val manager = deviceManager()
            if (manager == null) {
                lastConnectionFailure = DeviceConnectionFailure.UnsupportedBrowser
                return null
            }
            // Null means the user dismissed the browser picker, not an error.
            val device = manager.requestDevice().await() ?: return null
            if (!activate(connect(device))) return null
            LogUtils.i(TAG, "WebUSB device selected: '${device.name.ifEmpty { device.serial }}'")
            device.toCantaDevice()
        } catch (e: Exception) {
            lastConnectionFailure = e.toConnectionFailure()
            LogUtils.e(TAG, "Failed to connect to a device", e)
            null
        }
    }

    private suspend fun deviceManager(): AdbDaemonWebUsbDeviceManager? {
        val usb = navigatorUsb()
        if (usb == null) {
            LogUtils.i(TAG, "WebUSB is not supported by this browser")
            return null
        }
        return newWebUsbDeviceManager(loadWebUsbModule().await(), usb)
    }

    /** Opens [device] and performs the ADB handshake (may show the device's RSA prompt). */
    private suspend fun connect(device: AdbDaemonWebUsbDevice): Adb {
        val connection = device.connect().await()
        val adbModule = loadAdbModule().await()
        val credentialStore = newCredentialStore(loadCredentialStoreModule().await(), "Canta")
        val options = authenticateOptions(device.serial, connection, credentialStore)
        return newAdb(adbModule, authenticate(adbModule, options).await())
    }

    /**
     * Replaces the active connection with [connected] and sets up the helper.
     * Returns false (dropping the connection) if the helper can't be set up.
     */
    private suspend fun activate(connected: Adb): Boolean {
        transport.connection?.let { previous -> runCatching { previous.close().await() } }
        transport.connection = connected
        if (onConnected()) return true
        transport.connection = null
        runCatching { connected.close().await() }
        return false
    }

    private fun Exception.toConnectionFailure(): DeviceConnectionFailure {
        val detail = message ?: toString()
        return when {
            detail.contains("access denied", ignoreCase = true) ->
                DeviceConnectionFailure.AccessDenied(usbAccessHint())
            detail.contains("already in use", ignoreCase = true) ||
                detail.contains("device busy", ignoreCase = true) ||
                detail.contains("resource busy", ignoreCase = true) ->
                DeviceConnectionFailure.DeviceBusy(usbAccessHint())
            else -> DeviceConnectionFailure.Unknown(detail)
        }
    }

    /**
     * "Access denied" on `USBDevice.open()` is an OS-level permission issue,
     * not a grant issue: missing udev rules on Linux, missing WinUSB driver
     * on Windows. Sniff the host OS so the UI can say the actual fix.
     */
    private fun usbAccessHint(): UsbAccessHint {
        val platform = runCatching { navigatorPlatform() }.getOrNull().orEmpty()
        return when {
            // Checked before "win": macOS reports "Darwin", which contains "win".
            platform.contains("mac", ignoreCase = true) -> UsbAccessHint.Generic
            platform.contains("linux", ignoreCase = true) -> UsbAccessHint.LinuxUdev
            platform.contains("win", ignoreCase = true) -> UsbAccessHint.WindowsDriver
            else -> UsbAccessHint.Generic
        }
    }

    private fun AdbDaemonWebUsbDevice.toCantaDevice(): CantaDevice =
        CantaDevice(id = serial, displayName = name.ifEmpty { serial }, detail = serial)
}
