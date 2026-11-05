package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.packages.PackageDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeScoresTest {
    private fun app(pkg: String, system: Boolean = true) =
        AppInfo(PackageDetails(packageName = pkg, installed = true, isSystem = system))

    private val apps = (1..300).map { app("com.sys.app$it") } + app(FAKE_BOOTLOOP_PACKAGE)
    private val device = DeviceContext("xiaomi", "hyperos", 1, 34)

    @Test
    fun deterministic() {
        assertEquals(fakeScores(apps, device), fakeScores(apps.reversed(), device))
    }

    @Test
    fun coversEveryVerdict() {
        val scores = fakeScores(apps, device)
        val overall = scores.values.mapNotNull { it.overall?.verdict }.toSet()
        assertEquals(setOf(Verdict.BOOTLOOP, Verdict.BREAKS, Verdict.SAFE, Verdict.UNCLEAR, Verdict.INSUFFICIENT), overall)
        val deviceVerdicts = scores.values.mapNotNull { it.device?.verdict }.toSet()
        assertTrue(deviceVerdicts.containsAll(setOf(Verdict.BOOTLOOP, Verdict.BREAKS, Verdict.SAFE)))
    }

    @Test
    fun aboutHalfHaveADifferentDeviceVerdict() {
        val withVerdict = fakeScores(apps, device).filterKeys { it != FAKE_BOOTLOOP_PACKAGE }.values.filter { it.overall?.verdict?.isInformative == true }
        val different = withVerdict.count { it.device != null && it.device.verdict != it.overall!!.verdict }
        assertTrue(different > withVerdict.size / 8 && different < withVerdict.size * 3 / 4, "$different of ${withVerdict.size}")
        assertTrue(withVerdict.all { it.device == null || it.device.verdict != it.overall!!.verdict })
        assertEquals("HyperOS", withVerdict.first().familyLabel)
        assertEquals("HyperOS 1", withVerdict.first().deviceLabel)
    }

    @Test
    fun coversEveryScope() {
        val chosen = fakeScores(apps, device).values.mapNotNull { it.chosen }
        assertTrue(chosen.any { it.scope == VerdictScope.VERSION && it.actionable })
        assertTrue(chosen.any { it.scope == VerdictScope.FAMILY && it.actionable })
        assertTrue(chosen.any { it.scope == VerdictScope.OVERALL && it.actionable })
        val infoOnly = chosen.filter { !it.actionable }
        assertTrue(infoOnly.isNotEmpty())
        assertTrue(infoOnly.any { it.scope == VerdictScope.OVERALL && it.otherRomLabel == "One UI" })
        assertTrue(infoOnly.any { it.scope == VerdictScope.FAMILY && it.otherRomLabel == "HyperOS 2" })
        assertTrue(infoOnly.all { it.otherRomLabel != null })
        // Without a device nothing is information only.
        assertTrue(fakeScores(apps, null).values.mapNotNull { it.chosen }.all { it.actionable })
    }

    @Test
    fun onlySystemApps() {
        val mixed = listOf(app("com.user.one", false), app(FAKE_BOOTLOOP_PACKAGE, false)) + apps
        val scores = fakeScores(mixed, null)
        assertFalse("com.user.one" in scores)
        assertTrue(fakeScores(listOf(app(FAKE_BOOTLOOP_PACKAGE, false)), null).isEmpty())
        assertNull(scores["com.user.one"])
    }

    @Test
    fun eggIsAlwaysBootloop() {
        val egg = fakeScores(listOf(app(FAKE_BOOTLOOP_PACKAGE)), null).getValue(FAKE_BOOTLOOP_PACKAGE)
        assertEquals(Verdict.BOOTLOOP, egg.overall!!.verdict)
        assertEquals(Verdict.BOOTLOOP, egg.device!!.verdict)
        assertEquals(Verdict.BOOTLOOP, egg.verdict)
        val onDevice = fakeScores(listOf(app(FAKE_BOOTLOOP_PACKAGE)), device).getValue(FAKE_BOOTLOOP_PACKAGE)
        assertEquals(ChosenVerdict(Verdict.BOOTLOOP, VerdictScope.VERSION, true), onDevice.chosen)
    }

    @Test
    fun numbersArePlausible() {
        for (s in fakeScores(apps, device).values) {
            for (row in listOfNotNull(s.overall, s.family, s.device)) {
                // Floored to hundreds like the server's output; 0 means fewer than 100.
                assertTrue(row.uninstalls in 0..2000)
                assertEquals(0, row.uninstalls % 100)
            }
        }
    }
}
