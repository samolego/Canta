package io.github.samolego.canta.ui.badge

import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.data.app.RemovalLevel
import io.github.samolego.canta.data.bloat.BloatData
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.util.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals

class BadgesForTest {

    private fun app(
        pkg: String = "com.example.app",
        level: RemovalLevel? = null,
        system: Boolean = false,
        disabled: Boolean = false,
    ) = AppInfo(
        PackageDetails(packageName = pkg, installed = true, isSystem = system, disabled = disabled),
        bloatData = level?.let { BloatData(description = null, removalLevel = it) },
    )

    @Test
    fun removalLevelBadgePerLevel() {
        val expected = mapOf(
            RemovalLevel.RECOMMENDED to Badge.RECOMMENDED,
            RemovalLevel.ADVANCED to Badge.ADVANCED,
            RemovalLevel.EXPERT to Badge.EXPERT,
            RemovalLevel.UNSAFE to Badge.UNSAFE,
        )
        assertEquals(RemovalLevel.entries.toSet(), expected.keys)
        for ((level, badge) in expected) assertEquals(listOf(badge), badgesFor(app(level = level)))
    }

    @Test
    fun noBadgesWithoutLevel() {
        assertEquals(emptyList(), badgesFor(app()))
    }

    @Test
    fun communityBadgesWithoutUadBadge() {
        assertEquals(listOf(Badge.SAFE), badgesFor(app(), Verdict.SAFE))
        assertEquals(listOf(Badge.BOOTLOOP), badgesFor(app(), Verdict.BOOTLOOP))
        assertEquals(listOf(Badge.BREAKS), badgesFor(app(), Verdict.BREAKS))
        // Unclear only appears in the app's details, never on the tile.
        for (v in listOf(Verdict.UNCLEAR, Verdict.UNKNOWN, Verdict.INSUFFICIENT)) {
            assertEquals(emptyList(), badgesFor(app(), v), "verdict $v")
        }
    }

    @Test
    fun uadBadgeHidesCommunityBadgesExceptBootloop() {
        for (level in RemovalLevel.entries) {
            val uad = Badge.entries.single { it.name == level.name }
            assertEquals(listOf(uad, Badge.BOOTLOOP), badgesFor(app(level = level), Verdict.BOOTLOOP), "$level")
            for (v in listOf(Verdict.SAFE, Verdict.BREAKS, Verdict.UNCLEAR)) {
                assertEquals(listOf(uad), badgesFor(app(level = level), v), "$level / $v")
            }
        }
    }

    @Test
    fun systemDisabledAndCantaPredicates() {
        assertEquals(listOf(Badge.SYSTEM), badgesFor(app(system = true)))
        assertEquals(listOf(Badge.DISABLED), badgesFor(app(disabled = true)))
        assertEquals(listOf(Badge.CANTA), badgesFor(app(pkg = CANTA_PACKAGE_NAME)))
    }

    @Test
    fun orderFollowsEnumOrder() {
        val badges = badgesFor(
            app(pkg = CANTA_PACKAGE_NAME, level = RemovalLevel.EXPERT, system = true, disabled = true),
            Verdict.BOOTLOOP,
        )
        assertEquals(
            listOf(Badge.EXPERT, Badge.BOOTLOOP, Badge.SYSTEM, Badge.DISABLED, Badge.CANTA),
            badges,
        )
        assertEquals(Badge.entries.filter { it in badges }, badges)
    }

    @Test
    fun badgeForEachVerdict() {
        assertEquals(Badge.SAFE, Badge.forVerdict(Verdict.SAFE))
        assertEquals(Badge.BOOTLOOP, Badge.forVerdict(Verdict.BOOTLOOP))
        assertEquals(Badge.BREAKS, Badge.forVerdict(Verdict.BREAKS))
        assertEquals(Badge.UNCLEAR, Badge.forVerdict(Verdict.UNCLEAR))
        assertEquals(null, Badge.forVerdict(Verdict.INSUFFICIENT))
        assertEquals(null, Badge.forVerdict(Verdict.UNKNOWN))
    }
}
