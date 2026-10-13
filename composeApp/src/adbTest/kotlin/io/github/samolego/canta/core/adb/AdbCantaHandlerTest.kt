package io.github.samolego.canta.core.adb

import io.github.samolego.canta.core.DeviceConnectionFailure
import io.github.samolego.canta.packages.AppIcon
import io.github.samolego.canta.packages.HelperFraming
import io.github.samolego.canta.packages.OperationResult
import io.github.samolego.canta.packages.PROTOCOL_VERSION
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.packages.PackageDetailsList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdbCantaHandlerTest {

    /**
     * A device with the current helper (unless [remoteHash] says otherwise).
     * The helper reports [infoLine] and fails the operations in [failing];
     * with [crashAfterResults] set, operation streams break after that many results.
     */
    private class FakeDevice(
        var remoteHash: String? = AdbHelperArtifact.SHA256,
        val pushFixesHash: Boolean = true,
        val failing: Set<String> = emptySet(),
        val infoLine: String = INFO_LINE,
        val sdk: Int = 34,
        var crashAfterResults: Int? = null,
    ) : AdbTransport {
        val commands = mutableListOf<String>()
        val pushes = mutableListOf<String>()

        override suspend fun shell(command: String): ShellResult {
            commands += command
            return when {
                command.startsWith("sha256sum ") ->
                    remoteHash?.let { ShellResult(0, "$it  ${AdbHelper.REMOTE_PATH}\n") } ?: ShellResult(1, "")
                command.endsWith(" info") -> ShellResult(0, infoLine + "\n")
                command == "getprop ro.build.version.sdk" -> ShellResult(0, "$sdk\n")
                else -> ShellResult(127, "", "unexpected: $command")
            }
        }

        override fun shellLines(command: String): Flow<String> = flow {
            commands += command
            val args = command.substringAfter("helper.Main ").split(" ")
            when (args.first()) {
                "icons" -> {
                    emit("WARNING: linker: something unrelated")
                    emit(HelperFraming.encodeLine(AppIcon.serializer(), AppIcon("android", PNG)))
                    emit(HelperFraming.encodeLine(AppIcon.serializer(), AppIcon("com.broken", byteArrayOf(1, 2, 3))))
                    emit(HelperFraming.encodeLine(AppIcon.serializer(), AppIcon("com.a", PNG)))
                }
                "uninstall", "reinstall" -> args.drop(1).filter { it != "--reset" }.forEachIndexed { index, name ->
                    if (index == crashAfterResults) throw ShellCommandException(command, exitCode = 137, stderr = "connection lost")
                    // Packages named "*.silent" get no result at all.
                    if (!name.endsWith(".silent")) {
                        val result = OperationResult(name, success = name !in failing, status = if (name in failing) 1 else 0)
                        emit(HelperFraming.encodeLine(OperationResult.serializer(), result))
                    }
                }
                else -> error("unexpected: $command")
            }
        }

        override suspend fun push(bytes: ByteArray, remotePath: String, mode: Int) {
            pushes += remotePath
            if (pushFixesHash) remoteHash = AdbHelperArtifact.SHA256
        }
    }

    private class TestHandler(transport: AdbTransport, var connected: Boolean = true) : AdbCantaHandler(transport) {
        override val isConnected get() = connected
        suspend fun connect() = onConnected()
    }

    private companion object {
        val INFO_LINE = infoLine(PROTOCOL_VERSION)

        fun infoLine(protocolVersion: Int) = HelperFraming.encodeLine(
            PackageDetailsList.serializer(),
            PackageDetailsList(
                protocolVersion,
                listOf(
                    PackageDetails(packageName = "com.a", label = "App A", installed = true),
                    PackageDetails(packageName = "com.updated", isSystem = true, hasSystemUpdate = true, installed = true),
                    PackageDetails(packageName = "com.c", label = "App C", isSystem = true),
                ),
            ),
        )

        /** A 1x1 PNG. */
        val PNG = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(),
            0x89.toByte(), 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C.toByte(), 0x63, 0x60, 0x00, 0x02, 0x00,
            0x00, 0x05, 0x00, 0x01, 0xE9.toByte(), 0xFA.toByte(), 0xDC.toByte(), 0xD8.toByte(), 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
            0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte(),
        )
    }

    @Test
    fun connectingPushesAMissingHelper() = runTest {
        val device = FakeDevice(remoteHash = null)
        assertTrue(TestHandler(device).connect())
        assertEquals(listOf(AdbHelper.REMOTE_PATH), device.pushes)
    }

    @Test
    fun upToDateHelperIsNotPushedAgain() = runTest {
        val device = FakeDevice()
        assertTrue(TestHandler(device).connect())
        assertTrue(device.pushes.isEmpty())
    }

    @Test
    fun connectingFailsWhenTheHelperCantBeSetUp() = runTest {
        val handler = TestHandler(FakeDevice(remoteHash = null, pushFixesHash = false))
        assertFalse(handler.connect())
        assertEquals(DeviceConnectionFailure.HelperUnavailable, handler.lastConnectionFailure)
    }

    @Test
    fun devicesOlderThanTheHelperSupportsAreRefusedBeforeAnyPush() = runTest {
        val device = FakeDevice(remoteHash = null, sdk = AdbHelperArtifact.MIN_SDK - 1)
        val handler = TestHandler(device)

        assertFalse(handler.connect())
        assertEquals(DeviceConnectionFailure.UnsupportedAndroidVersion, handler.lastConnectionFailure)
        assertTrue(device.pushes.isEmpty())
    }

    @Test
    fun appsComeFromTheHelper() = runTest {
        val apps = TestHandler(FakeDevice()).loadApps().associateBy { it.packageName }
        assertEquals(setOf("com.a", "com.updated", "com.c"), apps.keys)
        assertEquals("App A", apps.getValue("com.a").name)
        assertTrue(apps.getValue("com.c").isUninstalled)
    }

    @Test
    fun resetAndExistenceComeFromTheCachedPackages() = runTest {
        val device = FakeDevice()
        val handler = TestHandler(device)
        assertTrue(handler.packageExists("com.a"))
        assertFalse(handler.packageExists("com.example.missing"))
        assertTrue(handler.canResetToFactory("com.updated"))
        assertFalse(handler.canResetToFactory("com.a"))
        assertEquals(1, device.commands.count { it.endsWith(" info") }, "info is read once and cached")
    }

    @Test
    fun uninstallStreamsOneResultPerPackage() = runTest {
        val device = FakeDevice(failing = setOf("com.b"))
        val results = TestHandler(device)
            .uninstallApps(listOf("com.a", "com.b", "com.x.silent", "bad;name"), resetToFactory = true)
            .toList()
            .associate { it.packageName to it.success }

        assertEquals(mapOf("bad;name" to false, "com.a" to true, "com.b" to false, "com.x.silent" to false), results)
        assertTrue(device.commands.none { "bad;name" in it }, "invalid names never reach the shell")
        assertTrue(device.commands.any { it.endsWith("uninstall --reset com.a com.b com.x.silent") })
    }

    @Test
    fun largeSelectionsAreSplitAcrossHelperRuns() = runTest {
        val device = FakeDevice()
        val names = List(120) { "com.p$it" }

        val results = TestHandler(device).uninstallApps(names).toList()

        val runs = device.commands.filter { " uninstall " in it }
        assertEquals(listOf(50, 50, 20), runs.map { it.substringAfter(" uninstall ").split(" ").size })
        assertEquals(names, results.map { it.packageName }, "one result per package, in order")
        assertTrue(results.all { it.success })
    }

    @Test
    fun brokenStreamFailsTheRestAndReverifiesTheHelper() = runTest {
        val device = FakeDevice(crashAfterResults = 1)
        val handler = TestHandler(device)

        val results = handler.uninstallApps(listOf("com.a", "com.b", "com.c")).toList()
        assertEquals(mapOf("com.a" to true, "com.b" to false, "com.c" to false), results.associate { it.packageName to it.success })

        device.crashAfterResults = null
        val checksBefore = device.commands.count { it.startsWith("sha256sum ") }
        assertEquals(listOf(true), handler.reinstallApps(listOf("com.a")).toList().map { it.success })
        assertTrue(device.commands.count { it.startsWith("sha256sum ") } > checksBefore, "helper is verified again")
    }

    @Test
    fun helperOfAnotherProtocolVersionIsNotTrusted() = runTest {
        val handler = TestHandler(FakeDevice(infoLine = infoLine(PROTOCOL_VERSION + 1)))
        assertTrue(handler.loadApps().isEmpty())
        assertFalse(handler.packageExists("com.a"))
    }

    @Test
    fun reinstallUsesTheHelper() = runTest {
        val device = FakeDevice()
        val results = TestHandler(device).reinstallApps(listOf("com.c")).toList()
        assertEquals(listOf(true), results.map { it.success })
        assertTrue(device.commands.any { it.endsWith("reinstall com.c") })
    }

    @Test
    fun iconsStreamInOrderSkippingNoiseAndUndecodableImages() = runTest {
        val icons = TestHandler(FakeDevice()).loadIcons().toList()
        assertEquals(listOf("android", "com.a"), icons.map { it.first })
    }

    @Test
    fun nothingRunsWhileDisconnected() = runTest {
        val device = FakeDevice()
        val handler = TestHandler(device, connected = false)
        assertTrue(handler.loadApps().isEmpty())
        assertTrue(handler.uninstallApps(listOf("com.a")).toList().isEmpty())
        assertFalse(handler.packageExists("com.a"))
        assertTrue(device.commands.isEmpty())
    }
}
