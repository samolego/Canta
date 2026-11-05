package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.packages.PackageDetails
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class TelemetryTest {
    private val challenge = "00112233445566778899aabbccddeeff.1790000000"
    private val payload =
        """{"v":1,"platform":"android","device":{"brand":"xiaomi","rom":"hyperos","romVersion":1,"sdk":34},"events":[{"e":"uninstalled","p":"com.example.bloat","w":"2026-10-26","b":"1"}]}"""

    private val device = DeviceContext("xiaomi", "hyperos", 1, 34)

    /** The payload shape of protocol drafts without a device; only used for the fixed PoW vector. */
    private val powPayload =
        """{"v":1,"platform":"android","events":[{"e":"uninstalled","p":"com.example.bloat","w":"2026-10-26","b":"1"}]}"""

    private fun app(pkg: String, system: Boolean) =
        AppInfo(PackageDetails(packageName = pkg, installed = true, isSystem = system))

    private fun events(n: Int, device: DeviceContext = this.device) = TelemetryEvent.allOf(
        (1..n).map { app("com.sys.p$it", true) }, TelemetryEventType.UNINSTALLED, "2026-10-26", "11+", device,
    )

    @Test
    fun powTestVector() = runTest {
        assertEquals("9f64c178c97cfafaf406d56d9ba201eba9380ce501e72926a12657a8634ba162", payloadHash(powPayload))
        assertEquals(1844674407370955UL, powTarget(1))
        assertEquals(9463L, solvePow(challenge, powPayload, 1))
    }

    @Test
    fun powTargets() {
        assertEquals(1844674407370955UL, powTarget(1))
        assertEquals(1844674407370955UL / 250UL, powTarget(250))
        assertEquals(18446744073709551615UL / 100_000UL, powTarget(10))
    }

    @Test
    fun solveTiming() = runTest {
        val p = Telemetry(client(), "android", CoroutineScope(Job()), { true }).buildPayload(device, events(100))
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val x = solvePow(challenge, p, 100)
        println("solvePow n=100: x=$x in ${start.elapsedNow()}")
    }

    @Test
    fun payloadIsExact() {
        val t = Telemetry(client(), "android", CoroutineScope(Job()), { true })
        val e = TelemetryEvent.of(app("com.example.bloat", true), TelemetryEventType.UNINSTALLED, "2026-10-26", "1", device)!!
        assertEquals(payload, t.buildPayload(device, listOf(e)))
    }

    @Test
    fun voidedIsSentWithItsWireName() {
        val t = Telemetry(client(), "android", CoroutineScope(Job()), { true })
        val e = TelemetryEvent.of(app("com.example.bloat", true), TelemetryEventType.VOIDED, "2026-10-26", "1", device)!!
        assertEquals("voided", TelemetryEventType.VOIDED.wire)
        assertTrue(t.buildPayload(device, listOf(e)).contains("\"e\":\"voided\""))
    }

    @Test
    fun bucketsAndWeeks() {
        assertEquals("1", batchBucketOf(1))
        assertEquals("2-10", batchBucketOf(2))
        assertEquals("2-10", batchBucketOf(10))
        assertEquals("11+", batchBucketOf(11))
        // 2026-10-25 is a Sunday, 2026-10-26 a Monday.
        assertEquals("2026-10-19", cohortWeekOf(Instant.parse("2026-10-25T23:59:59Z")))
        assertEquals("2026-10-26", cohortWeekOf(Instant.parse("2026-10-26T00:00:00Z")))
        assertEquals("2026-10-26", cohortWeekOf(Instant.parse("2026-11-01T23:59:59Z")))
        assertEquals("2026-12-28", cohortWeekOf(Instant.parse("2027-01-02T12:00:00Z")))
    }

    @Test
    fun nonSystemAppsAreNeverIncluded() {
        assertNull(TelemetryEvent.of(app("com.user", false), TelemetryEventType.UNINSTALLED, "w", "1"))
        val all = TelemetryEvent.allOf(
            listOf(app("com.user", false), app("com.sys", true)), TelemetryEventType.UNINSTALLED, "w", "1",
        )
        assertEquals(listOf("com.sys"), all.map { it.pkg })
    }

    @Test
    fun largeBatchesAreSplitAndSolved() = runTest {
        val records = mutableListOf<HttpRequestData>()
        val bodies = mutableListOf<String>()
        val t = Telemetry(client(records, bodies), "desktop", CoroutineScope(Job()), { true })
        t.queue(events(251))
        t.flush()
        val recordBodies = records.zip(bodies).filter { it.first.url.encodedPath.endsWith("record") }.map { it.second }
        assertEquals(2, recordBodies.size)
        val sizes = recordBodies.map {
            val inner = Json.parseToJsonElement(it).jsonObject["payload"]!!.jsonPrimitive.content
            Json.parseToJsonElement(inner).jsonObject["events"]!!.let { e -> (e as kotlinx.serialization.json.JsonArray).size }
        }
        assertEquals(listOf(250, 1), sizes)
        val first = Json.parseToJsonElement(recordBodies[0]).jsonObject
        assertEquals(challenge, first["challenge"]!!.jsonPrimitive.content)
        assertEquals("abcd", first["sig"]!!.jsonPrimitive.content)
        assertTrue(first["x"]!!.jsonPrimitive.int >= 0)
        assertEquals("sb_publishable_2gyUfMGAKMro9MtNgVXxjg_uv8vxGy-", records[0].headers["apikey"])
    }

    @Test
    fun eventsAreSplitByDevice() = runTest {
        val bodies = mutableListOf<String>()
        val t = Telemetry(client(bodies = bodies), "android", CoroutineScope(Job()), { true })
        val updated = device.copy(romVersion = 2)
        t.queue(events(2) + events(1, updated))
        t.flush()
        val payloads = bodies.filter { it.contains("\"payload\"") }.map {
            Json.parseToJsonElement(Json.parseToJsonElement(it).jsonObject["payload"]!!.jsonPrimitive.content).jsonObject
        }
        assertEquals(2, payloads.size)
        assertEquals(listOf(1, 2), payloads.map { it["device"]!!.jsonObject["romVersion"]!!.jsonPrimitive.int })
        assertEquals(listOf(2, 1), payloads.map { (it["events"] as kotlinx.serialization.json.JsonArray).size })
    }

    @Test
    fun deviceScoresAreFetchedWhenRomKnown() = runTest {
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            bodies += (request.body as? TextContent)?.text.orEmpty()
            val row = if (request.url.encodedPath.endsWith("package_scores_for")) "bootloop" else "safe"
            respond(
                """[{"pkg":"a.b","uninstalls":40,"small_batch":true,"verdict":"$row","updated_at":"x"}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val t = Telemetry(HttpClient(engine), "web", CoroutineScope(Job()), { true })
        val s = assertNotNull(t.fetchScores(device))["a.b"]!!
        assertEquals(Verdict.SAFE, s.overall!!.verdict)
        assertEquals(Verdict.BOOTLOOP, s.device!!.verdict)
        assertEquals(Verdict.BOOTLOOP, s.verdict)
        assertEquals("HyperOS 1", s.deviceLabel)
        assertEquals("""{"p_rom":"hyperos","p_rom_version":1}""", bodies.last())
        assertEquals("""{"p_rom":"hyperos","p_rom_version":-1}""", bodies[1])
        assertEquals(3, paths.size)
        assertEquals(Verdict.BOOTLOOP, s.family!!.verdict)
        assertEquals("HyperOS", s.familyLabel)

        paths.clear()
        val noDevice = assertNotNull(t.fetchScores(DeviceContext.UNKNOWN))["a.b"]!!
        assertNull(noDevice.device)
        assertEquals(Verdict.SAFE, noDevice.verdict)
        assertEquals(1, paths.size)
    }

    @Test
    fun failedFamilyOrVersionCallDropsOnlyThoseRows() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("package_scores_for")) respondError(HttpStatusCode.InternalServerError)
            else respond(
                """[{"pkg":"a.b","uninstalls":40,"small_batch":true,"verdict":"safe","updated_at":"x"}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val s = assertNotNull(Telemetry(HttpClient(engine), "web", CoroutineScope(Job()), { true }).fetchScores(device))["a.b"]!!
        assertNull(s.family)
        assertNull(s.device)
        assertEquals(Verdict.SAFE, s.verdict)
    }

    @Test
    fun mainRomFieldsParseAndAreOptional() {
        val rows = parseScores(
            """[{"pkg":"a.b","uninstalls":12,"verdict":"breaks","main_rom":"hyperos","main_rom_share":0.85},
                {"pkg":"c.d","uninstalls":12,"verdict":"safe","main_rom":null,"main_rom_share":null},
                {"pkg":"e.f","uninstalls":12,"verdict":"safe"}]""",
        )
        assertEquals("hyperos", rows["a.b"]!!.mainRom)
        assertEquals(0.85, rows["a.b"]!!.mainRomShare)
        assertNull(rows["c.d"]!!.mainRom)
        assertNull(rows["c.d"]!!.mainRomShare)
        assertNull(rows["e.f"]!!.mainRom)
        assertNull(rows["e.f"]!!.mainRomShare)
    }

    @Test
    fun mainVersionFieldsParseAndAreOptional() {
        val rows = parseScores(
            """[{"pkg":"a.b","uninstalls":12,"verdict":"breaks","main_version":2,"main_version_share":0.9},
                {"pkg":"c.d","uninstalls":12,"verdict":"safe","main_version":null,"main_version_share":null},
                {"pkg":"e.f","uninstalls":12,"verdict":"safe"}]""",
        )
        assertEquals(2, rows["a.b"]!!.mainVersion)
        assertEquals(0.9, rows["a.b"]!!.mainVersionShare)
        assertNull(rows["c.d"]!!.mainVersion)
        assertNull(rows["e.f"]!!.mainVersionShare)
    }

    @Test
    fun disabledSendsNothing() = runTest {
        val records = mutableListOf<HttpRequestData>()
        val t = Telemetry(client(records), "web", CoroutineScope(Job()), { false })
        t.queue(events(3))
        t.flush()
        assertTrue(records.isEmpty())
    }

    @Test
    fun failuresAreDropped() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val t = Telemetry(HttpClient(engine), "web", CoroutineScope(Job()), { true })
        t.queue(events(2))
        t.flush()
        assertEquals(1, engine.requestHistory.size)
        t.flush() // nothing left to retry
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun scoresParse() = runTest {
        val engine = MockEngine {
            respond(
                """[{"pkg":"a.b","uninstalls":12,"small_batch":false,"verdict":"safe","updated_at":"x"},
                    {"pkg":"c.d","uninstalls":1,"small_batch":true,"verdict":"new_thing","updated_at":"y"}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val scores = assertNotNull(Telemetry(HttpClient(engine), "web", CoroutineScope(Job()), { true }).fetchScores())
        assertEquals(Verdict.SAFE, scores["a.b"]!!.overall!!.verdict)
        assertEquals(Verdict.UNKNOWN, scores["c.d"]!!.overall!!.verdict)
        assertTrue(scores["c.d"]!!.overall!!.smallBatch)
        assertFalse(scores["a.b"]!!.overall!!.smallBatch)

        val failing = MockEngine { respondError(HttpStatusCode.BadRequest) }
        assertNull(Telemetry(HttpClient(failing), "web", CoroutineScope(Job()), { true }).fetchScores())
    }

    /** Hands out [challenge]; an easy-to-solve one is not possible, so the real PoW runs. */
    private fun client(
        records: MutableList<HttpRequestData> = mutableListOf(),
        bodies: MutableList<String> = mutableListOf(),
    ) = HttpClient(MockEngine { request ->
        records += request
        bodies += (request.body as? TextContent)?.text.orEmpty()
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        if (request.url.encodedPath.endsWith("challenge")) {
            respond("""{"challenge":"$challenge","sig":"abcd"}""", HttpStatusCode.OK, json)
        } else {
            respond("""{"accepted":1}""", HttpStatusCode.OK, json)
        }
    })
}
