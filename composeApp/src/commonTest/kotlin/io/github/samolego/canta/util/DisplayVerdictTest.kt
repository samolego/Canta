package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.AppBadgeInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayVerdictTest {
    @Test
    fun safeBecomesUnclearForExpertAndUnsafe() {
        assertEquals(Verdict.UNCLEAR, displayVerdict(Verdict.SAFE, AppBadgeInfo.EXPERT))
        assertEquals(Verdict.UNCLEAR, displayVerdict(Verdict.SAFE, AppBadgeInfo.UNSAFE))
    }

    @Test
    fun safeKeptForOtherBadges() {
        for (badge in listOf(null, AppBadgeInfo.RECOMMENDED, AppBadgeInfo.ADVANCED)) {
            assertEquals(Verdict.SAFE, displayVerdict(Verdict.SAFE, badge))
        }
    }

    @Test
    fun warningsNeverChange() {
        for (v in listOf(Verdict.BOOTLOOP, Verdict.BREAKS, Verdict.UNCLEAR, Verdict.INSUFFICIENT)) {
            assertEquals(v, displayVerdict(v, AppBadgeInfo.UNSAFE))
        }
    }

    @Test
    fun scoreRowsAreRewritten() {
        fun s(v: Verdict) = PackageScore("a.b", 300, false, v, null)
        val shown = CommunityScore(s(Verdict.SAFE), s(Verdict.SAFE), "X").displayed(AppBadgeInfo.EXPERT)
        assertEquals(Verdict.UNCLEAR, shown.overall?.verdict)
        assertEquals(Verdict.UNCLEAR, shown.device?.verdict)
    }
}
