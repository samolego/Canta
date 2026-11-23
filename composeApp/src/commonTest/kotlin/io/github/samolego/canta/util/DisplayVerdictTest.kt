package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.RemovalLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayVerdictTest {
    @Test
    fun safeBecomesUnclearForExpertAndUnsafe() {
        assertEquals(Verdict.UNCLEAR, displayVerdict(Verdict.SAFE, RemovalLevel.EXPERT))
        assertEquals(Verdict.UNCLEAR, displayVerdict(Verdict.SAFE, RemovalLevel.UNSAFE))
    }

    @Test
    fun safeKeptForOtherBadges() {
        for (badge in listOf(null, RemovalLevel.RECOMMENDED, RemovalLevel.ADVANCED)) {
            assertEquals(Verdict.SAFE, displayVerdict(Verdict.SAFE, badge))
        }
    }

    @Test
    fun warningsNeverChange() {
        for (v in listOf(Verdict.BOOTLOOP, Verdict.BREAKS, Verdict.UNCLEAR, Verdict.INSUFFICIENT)) {
            assertEquals(v, displayVerdict(v, RemovalLevel.UNSAFE))
        }
    }

    @Test
    fun scoreRowsAreRewritten() {
        fun s(v: Verdict) = PackageScore("a.b", 300, false, v, null)
        val shown = CommunityScore(s(Verdict.SAFE), s(Verdict.SAFE), "X").displayed(RemovalLevel.EXPERT)
        assertEquals(Verdict.UNCLEAR, shown.overall?.verdict)
        assertEquals(Verdict.UNCLEAR, shown.device?.verdict)
    }
}
