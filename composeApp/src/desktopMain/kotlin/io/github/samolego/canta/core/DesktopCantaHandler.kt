package io.github.samolego.canta.core

import dadb.Dadb
import dadb.adbserver.AdbServer
import io.github.samolego.canta.core.adb.AdbCantaHandler
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Desktop [CantaHandler] powered by <a href="https://github.com/mobile-dev-inc/dadb">dadb</a>.
 *
 * Devices are discovered through the local `adb` server (USB devices) and by
 * scanning the standard TCP emulator ports. Selection is explicit: the UI
 * lists [availableDevices] and connects with [selectDevice]. Everything past
 * the connection is shared with the web target through [AdbCantaHandler].
 */
class DesktopCantaHandler private constructor(
    private val transport: DadbTransport,
) : AdbCantaHandler(transport) {

    constructor() : this(DadbTransport())

    companion object {
        private const val TAG = "DesktopCantaHandler"
        private const val ADB_CONNECT_TIMEOUT_MS = 2000
        private const val ADB_SOCKET_TIMEOUT_MS = 10000

        /** Cheap round trip to check that a connection works. */
        private const val PING = "echo ok"
        private const val GET_SERIAL = "getprop ro.serialno"
        private const val GET_MODEL = "getprop ro.product.model"
    }

    /** Guards [discovered] and switching [DadbTransport.connection]. */
    private val mutex = Mutex()

    /** Discovered but not yet selected connections, keyed by [CantaDevice.id]. */
    private val discovered = mutableMapOf<String, Dadb>()

    override val isConnected: Boolean
        get() = transport.connection != null

    override val deviceDiscovery: DeviceDiscovery
        get() = DeviceDiscovery.List

    override suspend fun availableDevices(): List<CantaDevice> = withContext(Dispatchers.IO) {
        mutex.withLock { discoverDevices() }
    }

    override suspend fun selectDevice(device: CantaDevice) {
        lastConnectionFailure = null
        val connected = withContext(Dispatchers.IO) {
            mutex.withLock {
                val candidate = discovered.remove(device.id)
                if (candidate == null) {
                    LogUtils.w(TAG, "Selected device '${device.id}' is no longer discovered")
                    lastConnectionFailure = DeviceConnectionFailure.DeviceUnavailable
                    return@withLock false
                }
                try {
                    candidate.shell(PING)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "Selected device '${device.displayName}' is unreachable", e)
                    lastConnectionFailure = DeviceConnectionFailure.Unknown(e.message ?: e.toString())
                    candidate.closeQuietly()
                    return@withLock false
                }
                transport.connection?.takeIf { it !== candidate }?.closeQuietly()
                transport.connection = candidate
                discovered.values.forEach { it.closeQuietly() }
                discovered.clear()
                LogUtils.i(TAG, "ADB device selected: '${device.displayName}'")
                true
            }
        }
        if (connected && !onConnected()) {
            // Without the helper nothing works; drop the connection.
            transport.connection?.closeQuietly()
            transport.connection = null
        }
    }

    /**
     * Discovers reachable devices, keying connections by serial so a device
     * visible through both the adb server and the TCP scan is listed once.
     * Must hold [mutex]; performs blocking I/O.
     */
    private fun discoverDevices(): List<CantaDevice> {
        val found = linkedMapOf<String, Dadb>()
        val devices = mutableListOf<CantaDevice>()

        fun addCandidate(candidate: Dadb) {
            val serial = candidate.property(GET_SERIAL)
            val model = candidate.property(GET_MODEL)
            if (serial.isEmpty() && model.isEmpty()) {
                LogUtils.w(TAG, "Ignoring unreachable ADB device (unauthorized?)")
                candidate.closeQuietly()
                return
            }
            val id = serial.ifEmpty { "device-${found.size}" }
            if (found.containsKey(id)) {
                candidate.closeQuietly()
                return
            }
            found[id] = candidate
            devices += CantaDevice(
                id = id,
                displayName = model.ifEmpty { serial },
                detail = serial.ifEmpty { null },
            )
        }

        // 1) Devices connected through the local adb server (USB).
        runCatching { AdbServer.listDadbs() }
            .getOrDefault(emptyList())
            .forEach(::addCandidate)
        // 2) Fallback: TCP devices/emulators on the standard port range.
        runCatching {
            Dadb.list(
                connectTimeout = ADB_CONNECT_TIMEOUT_MS,
                socketTimeout = ADB_SOCKET_TIMEOUT_MS,
            )
        }.getOrNull().orEmpty().forEach(::addCandidate)

        // Retire cached connections that were not rediscovered (but never the
        // active one, which the user explicitly selected).
        val active = transport.connection
        discovered.values
            .filter { it !in found.values && it !== active }
            .forEach { it.closeQuietly() }
        discovered.clear()
        discovered.putAll(found)
        if (active != null && active !in found.values) {
            LogUtils.w(TAG, "Active ADB device is no longer reachable")
        }
        LogUtils.i(TAG, "Discovered ${devices.size} ADB device(s)")
        return devices
    }

    /** Output of a `getprop`-style [command], or empty if the device can't answer. */
    private fun Dadb.property(command: String): String =
        runCatching { shell(command).output.trim() }.getOrDefault("")

    private fun Dadb.closeQuietly() {
        runCatching { close() }
    }
}
