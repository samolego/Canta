package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.AppBadgeInfo
import io.github.samolego.canta.data.app.AppInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** What happened to a package; [wire] is the value sent to the server. */
enum class TelemetryEventType(val wire: String) {
    UNINSTALLED("uninstalled"),
    SURVIVED_REBOOT("survived_reboot"),
    REINSTALLED("reinstalled"),

    /** The uninstall is void as survival evidence: the package came back by itself (system update, restore). */
    VOIDED("voided"),
}

/**
 * One anonymous event. Only system packages can be turned into one, through [of];
 * there is no way to construct one for a user-installed app.
 */
class TelemetryEvent private constructor(
    val event: TelemetryEventType,
    val pkg: String,
    /** ISO date of the (UTC) Monday of the week the cohort was uninstalled in. */
    val cohortWeek: String,
    /** Size of the uninstall batch, see [batchBucketOf]. */
    val batchBucket: String,
    /** The device the cohort was uninstalled on (not necessarily the current one, after an OS update). */
    val device: DeviceContext,
) {
    override fun equals(other: Any?) = other is TelemetryEvent &&
        event == other.event && pkg == other.pkg &&
        cohortWeek == other.cohortWeek && batchBucket == other.batchBucket && device == other.device

    override fun hashCode() = listOf(event, pkg, cohortWeek, batchBucket, device).hashCode()

    override fun toString() = "TelemetryEvent($event, $pkg, $cohortWeek, $batchBucket, $device)"

    companion object {
        /** The event for [app], or null (the app is never reported) if it isn't a system app. */
        fun of(
            app: AppInfo,
            event: TelemetryEventType,
            cohortWeek: String,
            batchBucket: String,
            device: DeviceContext = DeviceContext.UNKNOWN,
        ): TelemetryEvent? =
            if (app.isSystemApp) TelemetryEvent(event, app.packageName, cohortWeek, batchBucket, device) else null

        /** Events for the system apps among [apps]; everything else is dropped. */
        fun allOf(
            apps: Collection<AppInfo>,
            event: TelemetryEventType,
            cohortWeek: String,
            batchBucket: String,
            device: DeviceContext = DeviceContext.UNKNOWN,
        ) = apps.mapNotNull { of(it, event, cohortWeek, batchBucket, device) }

        /**
         * The event for a pending uninstall entry. Entries are only ever created from
         * system apps (see [of]), so no further check is needed.
         */
        internal fun ofPending(
            entry: io.github.samolego.canta.data.proto.PendingUninstallProto,
            event: TelemetryEventType,
        ) = TelemetryEvent(event, entry.pkg, entry.cohortWeek, entry.batchBucket, entry.device())
    }
}

/** UTC Monday (ISO date) of the week containing [instant]. */
fun cohortWeekOf(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.UTC).date
    return date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY).toString()
}

/** Coarse uninstall batch size, so exact counts are never sent. */
fun batchBucketOf(count: Int): String = when {
    count <= 1 -> "1"
    count <= 10 -> "2-10"
    else -> "11+"
}

enum class Verdict {
    INSUFFICIENT, BOOTLOOP, BREAKS, SAFE, UNCLEAR,

    /** A verdict this version doesn't know. */
    UNKNOWN;

    companion object {
        fun parse(value: String?): Verdict = entries.firstOrNull {
            it != UNKNOWN && it.name.equals(value, ignoreCase = true)
        } ?: UNKNOWN
    }
}

/** Whether the verdict says something (not "too few reports" or unknown). */
val Verdict.isInformative: Boolean get() = this != Verdict.INSUFFICIENT && this != Verdict.UNKNOWN

/**
 * The verdict to show next to the app's UAD [badge]. Never "safe" when UAD rates the package
 * Expert or Unsafe: a wrong "safe" can brick phones, so it shows as unclear instead.
 */
fun displayVerdict(verdict: Verdict, badge: AppBadgeInfo?): Verdict =
    if (verdict == Verdict.SAFE && (badge == AppBadgeInfo.EXPERT || badge == AppBadgeInfo.UNSAFE)) Verdict.UNCLEAR
    else verdict

/** This score with [displayVerdict] applied. */
fun PackageScore.displayed(badge: AppBadgeInfo?): PackageScore {
    val shown = displayVerdict(verdict, badge)
    return if (shown == verdict) this else copy(verdict = shown)
}

/** This score with [displayVerdict] applied to all rows. */
fun CommunityScore.displayed(badge: AppBadgeInfo?): CommunityScore =
    copy(overall = overall?.displayed(badge), family = family?.displayed(badge), device = device?.displayed(badge))

data class PackageScore(
    val pkg: String,
    /** Floored to hundreds by the server, so 0 means fewer than 100. */
    val uninstalls: Int,
    val smallBatch: Boolean,
    val verdict: Verdict,
    val updatedAt: String?,
    /** ROM family with the most uninstalls of this package (overall rows only), null if unknown. */
    val mainRom: String? = null,
    /** [mainRom]'s share (0..1) of the uninstalls from known ROM families. */
    val mainRomShare: Double? = null,
    /** ROM version most of this family row's reports came from (family rows only), null if unknown. */
    val mainVersion: Int? = null,
    /** [mainVersion]'s share (0..1) of the family row's uninstalls. */
    val mainVersionShare: Double? = null,
)

/** Which row a [ChosenVerdict] comes from. */
enum class VerdictScope { VERSION, FAMILY, OVERALL }

/**
 * The verdict picked by [chooseVerdict]. Only [actionable] ones may drive badges and warnings;
 * otherwise it is information only, mostly from another ROM ([otherRomLabel], "One UI").
 */
data class ChosenVerdict(
    val verdict: Verdict,
    val scope: VerdictScope,
    val actionable: Boolean,
    /** "One UI" for an overall verdict, "HyperOS 2" for a family one; set when not [actionable]. */
    val otherRomLabel: String? = null,
)

/** Share of uninstalls from one other ROM above which the overall verdict is information only. */
const val OTHER_ROM_DOMINANCE = 0.8

/** Share of a family row's uninstalls from one other version above which it is information only. */
const val OTHER_VERSION_DOMINANCE = 0.8

/**
 * Picks the verdict for a device on ROM [deviceRom] (null when unknown): the ROM version's row,
 * else the ROM family's (information only when mostly from another version), else the overall one. The overall one is only information when it comes
 * mostly from another ROM. Null if there is no row at all.
 */
fun chooseVerdict(
    overall: PackageScore?,
    family: PackageScore?,
    device: PackageScore?,
    deviceRom: String?,
    deviceRomVersion: Int = 0,
): ChosenVerdict? {
    if (device != null && device.verdict.isInformative) return ChosenVerdict(device.verdict, VerdictScope.VERSION, true)
    if (family != null && family.verdict.isInformative) {
        // Mostly another version's reports: information only, and the overall row (a superset) is no better.
        val mainVersion = family.mainVersion
        val dominated = deviceRom != null && deviceRomVersion > 0 && mainVersion != null &&
            mainVersion != deviceRomVersion && (family.mainVersionShare ?: 0.0) > OTHER_VERSION_DOMINANCE
        return ChosenVerdict(
            family.verdict, VerdictScope.FAMILY, actionable = !dominated,
            otherRomLabel = if (dominated) DeviceContext("", deviceRom!!, mainVersion!!, 0).romLabel() else null,
        )
    }
    if (overall == null) return null
    val main = overall.mainRom
    val dominated = deviceRom != null && main != null && main != deviceRom &&
        (overall.mainRomShare ?: 0.0) > OTHER_ROM_DOMINANCE
    return ChosenVerdict(
        overall.verdict, VerdictScope.OVERALL, actionable = !dominated,
        otherRomLabel = if (dominated) romName(main!!) else null,
    )
}

/**
 * What the community says about a package: [overall] across all devices and,
 * when published for the user's ROM, [family] (all versions) and [device] (this version).
 * [deviceLabel] names the ROM version ("HyperOS 1"), [familyLabel] the ROM ("HyperOS");
 * [deviceRom] is the ROM family, null when the device's is unknown.
 */
data class CommunityScore(
    val overall: PackageScore?,
    val device: PackageScore?,
    val deviceLabel: String,
    val family: PackageScore? = null,
    val deviceRom: String? = null,
    val familyLabel: String = "",
    val deviceRomVersion: Int = 0,
) {
    /** Whether any row has a verdict worth showing. */
    val hasInfo: Boolean
        get() = listOf(overall, family, device).any { it?.verdict?.isInformative == true }

    /** The verdict to show, see [chooseVerdict]. */
    val chosen: ChosenVerdict? get() = chooseVerdict(overall, family, device, deviceRom, deviceRomVersion)

    /** The verdict to act on (badge, bootloop warning): the chosen one if actionable, else unknown. */
    val verdict: Verdict
        get() = chosen?.takeIf { it.actionable }?.verdict ?: Verdict.UNKNOWN
}

@Serializable
private class PayloadEvent(val e: String, val p: String, val w: String, val b: String)

@Serializable
private class PayloadDevice(val brand: String, val rom: String, val romVersion: Int, val sdk: Int)

@Serializable
private class Payload(val v: Int, val platform: String, val device: PayloadDevice, val events: List<PayloadEvent>)

@Serializable
private class Challenge(val challenge: String, val sig: String)

private val compactJson = Json
private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Opt-in anonymous package-safety telemetry: fire-and-forget, no identifiers,
 * no retries, nothing persisted. Events are queued in memory and sent
 * [TelemetryConfig.DEBOUNCE] after the last one, or on [flush].
 *
 * Every send checks [isEnabled] first, which is the single gate.
 *
 * [queue] is meant to be called from one thread at a time (the UI thread).
 */
@OptIn(ExperimentalTime::class)
class Telemetry(
    private val client: HttpClient,
    private val platform: String,
    private val scope: CoroutineScope,
    private val isEnabled: suspend () -> Boolean,
) {
    private companion object {
        const val TAG = "Telemetry"
    }

    private val pending = Channel<TelemetryEvent>(Channel.UNLIMITED)
    private var debounce: Job? = null

    /** Adds [events] to the queue and restarts the debounce timer. */
    fun queue(events: List<TelemetryEvent>) {
        if (events.isEmpty()) return
        events.forEach { pending.trySend(it) }
        debounce?.cancel()
        debounce = scope.launch {
            delay(TelemetryConfig.DEBOUNCE)
            sendPending()
        }
    }

    /** Sends everything queued right now (app going to background, window closing). */
    suspend fun flush() {
        debounce?.cancel()
        debounce = null
        // In our own scope, so cancelling the caller (activity going away) can't lose the drained events.
        scope.launch { sendPending() }.join()
    }

    /** Drops everything queued (analytics turned off). */
    fun clear() {
        debounce?.cancel()
        debounce = null
        while (pending.tryReceive().isSuccess) Unit
    }

    /** Sends [events] and waits; returns false if something was not delivered (or analytics is off). */
    suspend fun sendAndWait(events: List<TelemetryEvent>): Boolean {
        if (events.isEmpty()) return true
        return send(events)
    }

    private suspend fun sendPending() {
        val events = buildList {
            while (true) add(pending.tryReceive().getOrNull() ?: break)
        }
        send(events)
    }

    /** One request carries a single device object, so events are split by device before chunking. */
    private suspend fun send(events: List<TelemetryEvent>): Boolean {
        if (events.isEmpty()) return true
        if (!isEnabled()) return false
        var delivered = true
        for ((device, forDevice) in events.groupBy { it.device }) {
            for (chunk in forDevice.chunked(TelemetryConfig.MAX_EVENTS)) {
                try {
                    if (!sendChunk(device, chunk)) delivered = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LogUtils.w(TAG, "Dropped ${chunk.size} telemetry events: $e")
                    return false
                }
            }
        }
        return delivered
    }

    private suspend fun sendChunk(device: DeviceContext, events: List<TelemetryEvent>): Boolean {
        val payload = buildPayload(device, events)
        val challengeResponse = client.post(TelemetryConfig.BASE_URL + "challenge") {
            rpcHeaders()
            setBody("{}")
        }
        if (!challengeResponse.status.isSuccess()) {
            LogUtils.w(TAG, "Challenge request failed: ${challengeResponse.status}")
            return false
        }
        val challenge = lenientJson.decodeFromString<Challenge>(challengeResponse.bodyAsText())
        val x = solvePow(challenge.challenge, payload, events.size)

        // The payload is embedded as a string so the server hashes exactly what we did.
        val body = JsonObject(
            mapOf(
                "payload" to JsonPrimitive(payload),
                "challenge" to JsonPrimitive(challenge.challenge),
                "sig" to JsonPrimitive(challenge.sig),
                "x" to JsonPrimitive(x),
            )
        ).toString()
        val response = client.post(TelemetryConfig.BASE_URL + "record") {
            rpcHeaders()
            setBody(body)
        }
        if (response.status.isSuccess()) {
            val types = events.groupingBy { it.event.wire }.eachCount()
            LogUtils.i(TAG, "Sent ${events.size} telemetry events $types")
        } else {
            LogUtils.w(TAG, "Telemetry rejected: ${response.status} ${response.bodyAsText().take(200)}")
        }
        return response.status.isSuccess()
    }

    private fun HttpRequestBuilder.rpcHeaders() {
        header("apikey", TelemetryConfig.PUBLISHABLE_KEY)
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }

    /** The exact JSON text that is hashed and sent. */
    fun buildPayload(device: DeviceContext, events: List<TelemetryEvent>): String =
        compactJson.encodeToString(payloadOf(device, events))

    private fun payloadOf(device: DeviceContext, events: List<TelemetryEvent>) = Payload(
        v = 1,
        platform = platform,
        device = PayloadDevice(device.brand, device.rom, device.romVersion, device.sdk),
        events = events.map { PayloadEvent(it.event.wire, it.pkg, it.cohortWeek, it.batchBucket) },
    )

    /**
     * Community scores by package name, or null if the overall ones couldn't be
     * fetched. With a known ROM in [device] the scores for the ROM family and
     * for its version are fetched too; if either fails, those rows are left out.
     */
    suspend fun fetchScores(device: DeviceContext? = null): Map<String, CommunityScore>? {
        val overall = fetchRows("package_scores", "{}") ?: return null
        val known = device != null && device.hasRom
        suspend fun forRom(version: Int) = fetchRows(
            "package_scores_for", """{"p_rom":"${device!!.rom}","p_rom_version":$version}""",
        ).orEmpty()
        val family = if (known) forRom(-1) else emptyMap()
        val forDevice = if (known) forRom(device!!.romVersion) else emptyMap()
        val label = device?.romLabel().orEmpty()
        val familyLabel = if (known) romName(device!!.rom) else ""
        val rom = if (known) device!!.rom else null
        return (overall.keys + family.keys + forDevice.keys).associateWith {
            CommunityScore(overall[it], forDevice[it], label, family[it], rom, familyLabel, device?.romVersion ?: 0)
        }
    }

    private suspend fun fetchRows(function: String, body: String): Map<String, PackageScore>? = try {
        val response = client.post(TelemetryConfig.BASE_URL + function) {
            rpcHeaders()
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            LogUtils.w(TAG, "Fetching $function failed: ${response.status}")
            null
        } else {
            parseScores(response.bodyAsText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogUtils.w(TAG, "Fetching $function failed: $e")
        null
    }
}

internal fun parseScores(json: String): Map<String, PackageScore> =
    Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
        val o = element.jsonObject
        val pkg = o["pkg"]?.jsonPrimitive?.content ?: return@mapNotNull null
        PackageScore(
            pkg = pkg,
            uninstalls = o["uninstalls"]?.jsonPrimitive?.intOrNull ?: 0,
            smallBatch = o["small_batch"]?.jsonPrimitive?.booleanOrNull ?: false,
            verdict = Verdict.parse(o["verdict"]?.jsonPrimitive?.content),
            updatedAt = o["updated_at"]?.jsonPrimitive?.content,
            mainRom = (o["main_rom"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content,
            mainRomShare = (o["main_rom_share"] as? JsonPrimitive)?.doubleOrNull,
            mainVersion = (o["main_version"] as? JsonPrimitive)?.intOrNull,
            mainVersionShare = (o["main_version_share"] as? JsonPrimitive)?.doubleOrNull,
        )
    }.associateBy { it.pkg }
