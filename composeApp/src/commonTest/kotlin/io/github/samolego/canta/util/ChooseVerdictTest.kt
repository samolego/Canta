package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.AppBadgeInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChooseVerdictTest {
    private fun row(v: Verdict, mainRom: String? = null, share: Double? = null, mainVersion: Int? = null, versionShare: Double? = null) =
        PackageScore("a.b", 300, false, v, null, mainRom, share, mainVersion, versionShare)

    private fun score(
        overall: PackageScore? = null,
        family: PackageScore? = null,
        device: PackageScore? = null,
        rom: String? = "hyperos",
        version: Int = 1,
    ) = CommunityScore(overall, device, "HyperOS 1", family, rom, "HyperOS", version)

    @Test
    fun versionRowWins() {
        val s = score(row(Verdict.SAFE), row(Verdict.BREAKS), row(Verdict.BOOTLOOP))
        assertEquals(ChosenVerdict(Verdict.BOOTLOOP, VerdictScope.VERSION, true), s.chosen)
        assertEquals(Verdict.BOOTLOOP, s.verdict)
    }

    @Test
    fun familyRowWhenVersionNotInformative() {
        for (v in listOf(Verdict.INSUFFICIENT, Verdict.UNKNOWN)) {
            val s = score(row(Verdict.SAFE, "oneui", 0.99), row(Verdict.BREAKS), row(v))
            assertEquals(ChosenVerdict(Verdict.BREAKS, VerdictScope.FAMILY, true), s.chosen)
        }
        assertEquals(VerdictScope.FAMILY, score(row(Verdict.SAFE), row(Verdict.BREAKS), null).chosen?.scope)
    }

    @Test
    fun familyDominatedByOtherVersionIsInfoOnlyAndDoesNotFallThrough() {
        val s = score(row(Verdict.SAFE), row(Verdict.BREAKS, mainVersion = 2, versionShare = 0.9))
        assertEquals(ChosenVerdict(Verdict.BREAKS, VerdictScope.FAMILY, false, "HyperOS 2"), s.chosen)
        assertEquals(Verdict.UNKNOWN, s.verdict)
    }

    @Test
    fun familyActionableWhenVersionMixedOwnOrUnknown() {
        for (f in listOf(
            row(Verdict.BREAKS, mainVersion = 2, versionShare = 0.8), // not above the threshold
            row(Verdict.BREAKS, mainVersion = 1, versionShare = 0.95), // the user's own version
            row(Verdict.BREAKS), // no data
        )) {
            assertEquals(ChosenVerdict(Verdict.BREAKS, VerdictScope.FAMILY, true), score(family = f).chosen)
        }
        // Unknown device version.
        val s = score(family = row(Verdict.BREAKS, mainVersion = 2, versionShare = 0.99), version = 0)
        assertEquals(true, s.chosen?.actionable)
    }

    @Test
    fun versionRowStillBeatsDominatedFamily() {
        val s = score(family = row(Verdict.BREAKS, mainVersion = 2, versionShare = 0.9), device = row(Verdict.SAFE))
        assertEquals(ChosenVerdict(Verdict.SAFE, VerdictScope.VERSION, true), s.chosen)
    }

    @Test
    fun overallActionableWhenMixedOrOwnRom() {
        val insufficient = row(Verdict.INSUFFICIENT)
        for (o in listOf(
            row(Verdict.BREAKS), // no main rom data
            row(Verdict.BREAKS, "oneui", 0.5), // mixed
            row(Verdict.BREAKS, "oneui", 0.8), // not above the threshold
            row(Verdict.BREAKS, "hyperos", 0.95), // the user's own ROM
        )) {
            val s = score(o, insufficient, insufficient)
            assertEquals(ChosenVerdict(Verdict.BREAKS, VerdictScope.OVERALL, true), s.chosen)
            assertEquals(Verdict.BREAKS, s.verdict)
        }
    }

    @Test
    fun overallDominatedByOtherRomIsInfoOnly() {
        val s = score(row(Verdict.BOOTLOOP, "oneui", 0.9), device = row(Verdict.INSUFFICIENT))
        assertEquals(ChosenVerdict(Verdict.BOOTLOOP, VerdictScope.OVERALL, false, "One UI"), s.chosen)
        // No badge, no warning.
        assertEquals(Verdict.UNKNOWN, s.verdict)
        assertEquals(Verdict.UNKNOWN, s.displayed(AppBadgeInfo.UNSAFE).verdict)
        assertTrue(s.hasInfo)
    }

    @Test
    fun unknownDeviceRomMakesOverallActionable() {
        val s = score(row(Verdict.BOOTLOOP, "oneui", 0.99), rom = null)
        assertEquals(ChosenVerdict(Verdict.BOOTLOOP, VerdictScope.OVERALL, true), s.chosen)
        assertEquals(Verdict.BOOTLOOP, s.verdict)
    }

    @Test
    fun nothingAtAll() {
        assertNull(score().chosen)
        assertEquals(Verdict.UNKNOWN, score().verdict)
    }

    @Test
    fun displayRuleStillApplies() {
        val s = score(family = row(Verdict.SAFE)).displayed(AppBadgeInfo.EXPERT)
        assertEquals(Verdict.UNCLEAR, s.chosen?.verdict)
    }
}
