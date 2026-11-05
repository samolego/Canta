package io.github.samolego.canta.util

import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.data.proto.BootSnapshotProto
import io.github.samolego.canta.data.proto.PendingUninstallProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Prefix of the Android fallback boot id (`Settings.Global.BOOT_COUNT`). */
const val BOOT_COUNT_PREFIX = "boot_count:"

/** Which check is resolving the pending uninstalls. */
enum class ResolveMode {
    /** Right after the device finished booting: an app that is back was not there when the boot completed. */
    BOOT,

    /** When Canta starts, possibly long after the boot. */
    START,
}

/**
 * What is known about a package on the device right now.
 * [installedSinceBoot]: reinstalled for the user after the current boot started; null if unknown.
 */
class PackageState(val uninstalled: Boolean, val installedSinceBoot: Boolean? = null)

/** Both builds known and different: the OS was updated since the uninstall. */
private fun fingerprintChanged(then: String, now: String?) = then.isNotEmpty() && !now.isNullOrEmpty() && then != now

/**
 * Decides what to do with the uninstalls waiting for their "did it survive a
 * reboot" report. Returns the events to send and the entries that stay pending.
 * Silence only ever means "the device never completed a boot after the uninstall";
 * every other outcome is reported.
 *
 *  - older than [TelemetryConfig.PENDING_MAX_AGE]: dropped
 *  - recorded on another device ([PendingUninstallProto.deviceKey] differs from [currentDeviceKey],
 *    both known): kept untouched until it expires
 *  - same boot as when uninstalled (or boot unknown): kept
 *  - package unknown ([states] has no entry): kept until it expires (another device may be
 *    connected, e.g. on desktop and web; nothing device-specific is stored but the boot id,
 *    which a different device never matches)
 *  - rebooted, not reported yet, still uninstalled: `survived_reboot`, kept as survived
 *  - rebooted, already reported as survived, installed again: `reinstalled` (dropped silently if the
 *    build changed: an OS update restored it)
 *  - rebooted, not reported yet, installed again:
 *    - build changed ([currentFingerprint] differs): `voided` (an OS update restored it), dropped
 *    - [ResolveMode.BOOT]: nothing, dropped (it was back before the boot completed: a bootloop recovery)
 *    - [ResolveMode.START]: `survived_reboot` + `reinstalled` if it was reinstalled after the boot
 *      started, else `voided` (can't tell), dropped
 */
@OptIn(ExperimentalTime::class)
fun resolvePending(
    pending: List<PendingUninstallProto>,
    mode: ResolveMode,
    currentBootId: String?,
    currentFingerprint: String?,
    states: Map<String, PackageState>,
    now: Instant,
    currentDeviceKey: String? = null,
): Pair<List<TelemetryEvent>, List<PendingUninstallProto>> {
    val cutoff = now - TelemetryConfig.PENDING_MAX_AGE
    val events = mutableListOf<TelemetryEvent>()
    val remaining = mutableListOf<PendingUninstallProto>()
    for (entry in pending) {
        if (Instant.fromEpochMilliseconds(entry.uninstalledAt) < cutoff) continue
        // Ids of different kinds (boot_id vs the "boot_count:" fallback) can't be compared.
        val comparable = entry.bootId.startsWith(BOOT_COUNT_PREFIX) == currentBootId?.startsWith(BOOT_COUNT_PREFIX)
        val state = states[entry.pkg]
        val otherDevice = entry.deviceKey.isNotEmpty() && currentDeviceKey != null && entry.deviceKey != currentDeviceKey
        if (otherDevice || currentBootId == null || !comparable || entry.bootId == currentBootId || state == null) {
            remaining += entry
            continue
        }
        fun report(type: TelemetryEventType) = events.add(TelemetryEvent.ofPending(entry, type))
        when {
            entry.survived -> when {
                state.uninstalled -> remaining += entry
                // An OS update brought it back, not the user.
                fingerprintChanged(entry.buildFingerprint, currentFingerprint) -> {}
                else -> report(TelemetryEventType.REINSTALLED)
            }
            state.uninstalled -> {
                report(TelemetryEventType.SURVIVED_REBOOT)
                remaining += entry.copy(survived = true)
            }
            fingerprintChanged(entry.buildFingerprint, currentFingerprint) -> report(TelemetryEventType.VOIDED)
            mode == ResolveMode.BOOT -> {}
            state.installedSinceBoot == true -> {
                report(TelemetryEventType.SURVIVED_REBOOT)
                report(TelemetryEventType.REINSTALLED)
            }
            else -> report(TelemetryEventType.VOIDED)
        }
    }
    return events to remaining
}

/** Whether this entry is for [pkg] on the device [deviceKey]; entries without a key (legacy) match any device. */
internal fun PendingUninstallProto.isFor(pkg: String, deviceKey: String) =
    this.pkg == pkg && (this.deviceKey.isEmpty() || deviceKey.isEmpty() || this.deviceKey == deviceKey)

/** The device the uninstall happened on; unknown for entries from before it was recorded. */
fun PendingUninstallProto.device() = DeviceContext(brand.ifBlank { "unknown" }, rom.ifBlank { "unknown" }, romVersion, sdk)

/**
 * Ties [Telemetry] to what happens in the app: reports uninstalls and
 * reinstalls done through Canta and, once per start, which uninstalls survived
 * a reboot. Does nothing while analytics is off.
 */
@OptIn(ExperimentalTime::class)
class PackageTelemetry(
    private val handler: CantaHandler,
    private val telemetry: Telemetry,
    private val settings: CantaSettings,
    private val clock: Clock = Clock.System,
) {
    private val pendingLock = Mutex()

    val analyticsEnabled: Flow<Boolean> get() = settings.analyticsEnabledFlow

    /** The connected device, or null if it can't be read. */
    suspend fun deviceContext(): DeviceContext? = handler.deviceContext()

    /** Community scores, or null when analytics is off or they couldn't be fetched. */
    suspend fun fetchScores() =
        if (settings.analyticsEnabledFlow.first()) telemetry.fetchScores(handler.deviceContext()) else null

    /** Forgets everything queued and pending (analytics turned off). */
    suspend fun clearPending() {
        telemetry.clear()
        pendingLock.withLock { settings.setPendingReboot(emptyList()) }
    }

    /** Records that [apps] were uninstalled as part of one user action on [batchSize] apps. */
    suspend fun recordUninstalls(apps: List<AppInfo>, batchSize: Int) {
        if (!settings.analyticsEnabledFlow.first()) return
        val systemApps = apps.filter { it.isSystemApp }
        if (systemApps.isEmpty()) return
        val bootId = handler.bootId() ?: return
        val now = clock.now()
        val week = cohortWeekOf(now)
        val bucket = batchBucketOf(batchSize)
        val device = handler.deviceContext() ?: DeviceContext.UNKNOWN
        val fingerprint = handler.buildFingerprint().orEmpty()
        val deviceKey = handler.deviceKey().orEmpty()
        telemetry.queue(TelemetryEvent.allOf(systemApps, TelemetryEventType.UNINSTALLED, week, bucket, device))
        pendingLock.withLock {
            val replaced = systemApps.map { it.packageName }.toSet()
            val keep = settings.pendingRebootFlow.first().filter { e -> replaced.none { e.isFor(it, deviceKey) } }
            val added = systemApps.map {
                PendingUninstallProto(
                    it.packageName, bootId, week, bucket, now.toEpochMilliseconds(),
                    device.brand, device.rom, device.romVersion, device.sdk,
                    buildFingerprint = fingerprint, deviceKey = deviceKey,
                )
            }
            settings.setPendingReboot(keep + added)
        }
    }

    /** Records that [apps] were reinstalled; only those with a pending uninstall (known cohort) are reported. */
    suspend fun recordReinstalls(apps: List<AppInfo>) {
        if (!settings.analyticsEnabledFlow.first()) return
        pendingLock.withLock {
            val pending = settings.pendingRebootFlow.first()
            if (pending.isEmpty()) return
            val deviceKey = handler.deviceKey().orEmpty()
            val events = apps.mapNotNull { app ->
                val entry = pending.firstOrNull { it.isFor(app.packageName, deviceKey) } ?: return@mapNotNull null
                TelemetryEvent.of(app, TelemetryEventType.REINSTALLED, entry.cohortWeek, entry.batchBucket, entry.device())
            }
            telemetry.queue(events)
            val reinstalled = apps.map { it.packageName }
            settings.setPendingReboot(pending.filter { e -> reinstalled.none { e.isFor(it, deviceKey) } })
        }
    }

    /** Reports what became of the pending uninstalls, given the freshly loaded [apps] (Canta start). */
    suspend fun resolveSurvivors(apps: List<AppInfo>) {
        if (!settings.analyticsEnabledFlow.first()) return
        val bootId = handler.bootId() ?: return
        val fingerprint = handler.buildFingerprint()
        val deviceKey = handler.deviceKey()
        pendingLock.withLock {
            val pending = settings.pendingRebootFlow.first()
            if (pending.isEmpty()) return
            val byPackage = apps.associateBy { it.packageName }
            val states = pending.mapNotNull { entry ->
                val app = byPackage[entry.pkg] ?: return@mapNotNull null
                entry.pkg to PackageState(app.isUninstalled, if (app.isUninstalled) null else handler.installedSinceBoot(entry.pkg))
            }.toMap()
            val (events, remaining) = resolvePending(pending, ResolveMode.START, bootId, fingerprint, states, clock.now(), deviceKey)
            if (!telemetry.sendAndWait(events)) return
            if (remaining != pending) settings.setPendingReboot(remaining)
        }
    }

    /**
     * Called when the device finished booting (no privileged access then, so the caller reads
     * the package states): the states of the pending packages, read at that moment so that a later
     * reinstall outside Canta doesn't look like a bootloop recovery; null if there is nothing to
     * check or analytics is off.
     */
    suspend fun snapshotAtBoot(
        bootId: String?,
        fingerprint: String?,
        stateOf: (String) -> PackageState?,
    ): BootSnapshotProto? {
        if (bootId == null || !settings.analyticsEnabledFlow.first()) return null
        val pending = pendingLock.withLock { settings.pendingRebootFlow.first() }
        if (pending.isEmpty()) return null
        val states = pending.mapNotNull { e -> stateOf(e.pkg)?.let { e.pkg to it } }.toMap()
        return BootSnapshotProto(
            bootId, fingerprint.orEmpty(),
            uninstalled = states.filterValues { it.uninstalled }.keys.toList(),
            installed = states.filterValues { !it.uninstalled }.keys.toList(),
        )
    }

    /**
     * The check after the device finished booting, using the [snapshot] taken at that moment.
     * A snapshot of another boot is stale and ignored (the next start's check handles it).
     * Waits for the reports to be delivered and only then updates the pending uninstalls;
     * returns false if sending failed and the check should be retried, true when the snapshot
     * is done with.
     */
    suspend fun resolveAfterBoot(snapshot: BootSnapshotProto, currentBootId: String?): Boolean {
        if (!settings.analyticsEnabledFlow.first()) return true
        if (currentBootId == null || snapshot.bootId != currentBootId) return true
        val deviceKey = handler.deviceKey()
        val states = snapshot.uninstalled.associateWith { PackageState(uninstalled = true) } +
            snapshot.installed.associateWith { PackageState(uninstalled = false) }
        pendingLock.withLock {
            val pending = settings.pendingRebootFlow.first()
            if (pending.isEmpty()) return true
            val (events, remaining) = resolvePending(
                pending, ResolveMode.BOOT, currentBootId, snapshot.buildFingerprint.ifEmpty { null }, states, clock.now(), deviceKey,
            )
            if (!telemetry.sendAndWait(events)) return false
            if (remaining != pending) settings.setPendingReboot(remaining)
            return true
        }
    }
}
