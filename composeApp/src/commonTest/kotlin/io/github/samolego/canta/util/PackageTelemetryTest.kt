package io.github.samolego.canta.util

import io.github.samolego.canta.data.proto.PendingUninstallProto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class PackageTelemetryTest {
    private val now = Instant.parse("2026-10-29T12:00:00Z")

    private fun entry(pkg: String, boot: String = "boot1", age: kotlin.time.Duration = 1.days) =
        PendingUninstallProto(pkg, boot, "2026-10-26", "2-10", (now - age).toEpochMilliseconds())

    private fun ev(pkg: String, type: TelemetryEventType) =
        TelemetryEvent.ofPending(entry(pkg), type)

    private val uninstalled = PackageState(uninstalled = true)
    private fun installed(sinceBoot: Boolean? = null) = PackageState(uninstalled = false, installedSinceBoot = sinceBoot)

    private fun resolve(
        e: PendingUninstallProto,
        state: PackageState?,
        mode: ResolveMode = ResolveMode.START,
        boot: String? = "boot2",
        fingerprint: String? = null,
        deviceKey: String? = null,
    ) = resolvePending(listOf(e), mode, boot, fingerprint, listOfNotNull(state?.let { e.pkg to it }).toMap(), now, deviceKey)

    private fun assertResult(
        result: Pair<List<TelemetryEvent>, List<PendingUninstallProto>>,
        events: List<TelemetryEvent>,
        remaining: List<PendingUninstallProto>,
    ) {
        assertEquals(events, result.first)
        assertEquals(remaining, result.second)
    }

    private val none = emptyList<TelemetryEvent>()
    private val gone = emptyList<PendingUninstallProto>()

    @Test
    fun stillUninstalledSurvivedInBothModes() {
        for (mode in ResolveMode.entries) {
            val e = entry("com.a")
            assertResult(resolve(e, uninstalled, mode), listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT)), listOf(e.copy(survived = true)))
        }
    }

    @Test
    fun survivedKeepsTheDeviceFromUninstallTime() {
        val e = entry("com.a").copy(brand = "xiaomi", rom = "hyperos", romVersion = 1, sdk = 34)
        val (events, _) = resolve(e, uninstalled)
        assertEquals(DeviceContext("xiaomi", "hyperos", 1, 34), events.single().device)
        assertEquals(DeviceContext.UNKNOWN, entry("com.a").device())
    }

    @Test
    fun alreadySurvivedAndStillUninstalledSendsNothing() {
        val e = entry("com.a").copy(survived = true)
        assertResult(resolve(e, uninstalled), none, listOf(e))
    }

    @Test
    fun survivedThenReinstalledIsReinstalled() {
        for (mode in ResolveMode.entries) {
            val e = entry("com.a").copy(survived = true)
            assertResult(resolve(e, installed(), mode), listOf(ev("com.a", TelemetryEventType.REINSTALLED)), gone)
        }
    }

    @Test
    fun sameBootIsKept() {
        val e = entry("com.a")
        assertResult(resolve(e, uninstalled, boot = "boot1"), none, listOf(e))
        assertResult(resolve(e, installed(true), boot = "boot1"), none, listOf(e))
    }

    @Test
    fun unknownBootIsKept() {
        val e = entry("com.a")
        assertResult(resolve(e, uninstalled, boot = null), none, listOf(e))
    }

    @Test
    fun incomparableBootIdsAreKept() {
        val e = entry("com.a", boot = "${BOOT_COUNT_PREFIX}3")
        assertResult(resolve(e, uninstalled, boot = "boot2"), none, listOf(e))
        // same kind, different count: a reboot
        assertEquals(1, resolve(e, uninstalled, boot = "${BOOT_COUNT_PREFIX}4").first.size)
    }

    @Test
    fun unknownPackageIsKeptUntilExpiry() {
        val e = entry("com.gone")
        assertResult(resolve(e, null), none, listOf(e))
        assertResult(resolve(entry("com.gone", age = 31.days), null), none, gone)
    }

    @Test
    fun expiredIsDroppedWithoutReport() {
        val old = entry("com.a", age = 31.days)
        assertResult(resolve(old, uninstalled), none, gone)
        assertResult(resolve(old.copy(survived = true), installed()), none, gone)
        assertResult(resolve(old, installed(true)), none, gone)
    }

    @Test
    fun bootCheckBackBeforeBootCompletedIsSilent() {
        val e = entry("com.a").copy(buildFingerprint = "fp")
        assertResult(resolve(e, installed(), ResolveMode.BOOT, fingerprint = "fp"), none, gone)
    }

    @Test
    fun bootCheckUnknownFingerprintIsSilent() {
        assertResult(resolve(entry("com.a").copy(buildFingerprint = "fp"), installed(), ResolveMode.BOOT, fingerprint = null), none, gone)
        assertResult(resolve(entry("com.a"), installed(), ResolveMode.BOOT, fingerprint = "fp"), none, gone)
    }

    @Test
    fun bootCheckChangedFingerprintIsVoided() {
        val e = entry("com.a").copy(buildFingerprint = "old")
        assertResult(resolve(e, installed(), ResolveMode.BOOT, fingerprint = "new"), listOf(ev("com.a", TelemetryEventType.VOIDED)), gone)
    }

    @Test
    fun bootCheckIgnoresTheReinstallSignal() {
        assertResult(resolve(entry("com.a"), installed(true), ResolveMode.BOOT, fingerprint = "fp"), none, gone)
    }

    @Test
    fun startChangedFingerprintIsVoidedWhateverTheSignal() {
        val e = entry("com.a").copy(buildFingerprint = "old")
        for (signal in listOf(true, false, null)) {
            assertResult(resolve(e, installed(signal), fingerprint = "new"), listOf(ev("com.a", TelemetryEventType.VOIDED)), gone)
        }
    }

    @Test
    fun startReinstalledAfterBootIsSurvivedAndReinstalled() {
        for (fp in listOf("fp", null)) {
            val e = entry("com.a").copy(buildFingerprint = "fp")
            assertResult(
                resolve(e, installed(true), fingerprint = fp),
                listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT), ev("com.a", TelemetryEventType.REINSTALLED)),
                gone,
            )
        }
    }

    @Test
    fun startAmbiguousIsVoided() {
        val e = entry("com.a").copy(buildFingerprint = "fp")
        assertResult(resolve(e, installed(false), fingerprint = "fp"), listOf(ev("com.a", TelemetryEventType.VOIDED)), gone)
        assertResult(resolve(e, installed(null), fingerprint = "fp"), listOf(ev("com.a", TelemetryEventType.VOIDED)), gone)
        assertResult(resolve(entry("com.a"), installed(null)), listOf(ev("com.a", TelemetryEventType.VOIDED)), gone)
    }

    @Test
    fun entriesAreResolvedIndependently() {
        val a = entry("com.a")
        val b = entry("com.b")
        val c = entry("com.c")
        val result = resolvePending(
            listOf(a, b, c), ResolveMode.START, "boot2", null,
            mapOf("com.a" to uninstalled, "com.b" to installed(false)), now,
        )
        assertEquals(
            listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT), ev("com.b", TelemetryEventType.VOIDED)),
            result.first,
        )
        assertEquals(listOf(a.copy(survived = true), c), result.second)
    }

    @Test
    fun entryFromAnotherDeviceIsKeptUntouched() {
        for (mode in ResolveMode.entries) for (state in listOf(uninstalled, installed(true), installed())) {
            val e = entry("com.a").copy(deviceKey = "A", buildFingerprint = "old")
            assertResult(resolve(e, state, mode, fingerprint = "new", deviceKey = "B"), none, listOf(e))
        }
    }

    @Test
    fun entryFromTheSameDeviceIsResolved() {
        val e = entry("com.a").copy(deviceKey = "A")
        assertResult(resolve(e, uninstalled, deviceKey = "A"), listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT)), listOf(e.copy(survived = true)))
    }

    @Test
    fun entryWithoutDeviceKeyOrUnknownCurrentKeyBehavesAsBefore() {
        val legacy = entry("com.a")
        assertResult(resolve(legacy, uninstalled, deviceKey = "B"), listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT)), listOf(legacy.copy(survived = true)))
        val keyed = entry("com.a").copy(deviceKey = "A")
        assertResult(resolve(keyed, uninstalled, deviceKey = null), listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT)), listOf(keyed.copy(survived = true)))
    }

    @Test
    fun snapshotStatesResolveInBootMode() {
        val states = mapOf("com.a" to uninstalled, "com.b" to installed())
        val a = entry("com.a")
        val b = entry("com.b")
        val c = entry("com.c")
        val (events, remaining) = resolvePending(listOf(a, b, c), ResolveMode.BOOT, "boot2", null, states, now)
        assertEquals(listOf(ev("com.a", TelemetryEventType.SURVIVED_REBOOT)), events)
        assertEquals(listOf(a.copy(survived = true), c), remaining)
    }

    @Test
    fun survivedThenBackAfterOsUpdateIsDroppedSilently() {
        val e = entry("com.a").copy(survived = true, buildFingerprint = "old")
        assertResult(resolve(e, installed(), fingerprint = "new"), none, gone)
        assertResult(resolve(e, installed(), fingerprint = "old"), listOf(ev("com.a", TelemetryEventType.REINSTALLED)), gone)
    }

    @Test
    fun pendingEntriesMatchOnPackageAndDevice() {
        val a = entry("com.a").copy(deviceKey = "A")
        assertEquals(true, a.isFor("com.a", "A"))
        assertEquals(false, a.isFor("com.a", "B"))
        assertEquals(false, a.isFor("com.b", "A"))
        assertEquals(true, entry("com.a").isFor("com.a", "B"))
    }
}
