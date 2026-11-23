package io.github.samolego.canta.ui.badge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DisabledByDefault
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.data.app.RemovalLevel
import io.github.samolego.canta.generated.resources.*
import io.github.samolego.canta.util.Verdict
import org.jetbrains.compose.resources.StringResource

/** Sections of the badge info screen. A null [description] shows just the title. */
enum class BadgeGroup(val title: StringResource, val description: StringResource?) {
    REMOVAL(Res.string.badge_group_removal_title, Res.string.badge_group_removal_description),
    COMMUNITY(Res.string.badge_group_community_title, Res.string.badge_group_community_description),
    OTHER(Res.string.badge_group_other_title, null),
}

/** When a badge also shows on the app tile. */
enum class TileRule(val allows: (BadgeSubject) -> Boolean) {
    ALWAYS({ true }),
    NEVER({ false }),

    /** Only when there is no UAD badge: on the tile, UAD's rating comes first. */
    WITHOUT_REMOVAL_LEVEL({ it.app.removalLevel == null }),
}

/** What the badges are decided from. */
data class BadgeSubject(val app: AppInfo, val verdict: Verdict)

/** Close to the UAD Expert / Unsafe colors: a possible bootloop is as serious. */
private val CommunityBootloopRed = Color(0xFFE6005C)
private val CommunitySafeGreen = Color(0xFF43A047)
private val CommunityReinstalledYellow = Color(0xFFF9A825)
private val CommunityUnclearGray = Color(0xFF78909C)

/**
 * Every badge Canta can show. Declaration order is the display order, both on
 * the app tile and on the badge info screen. To add a badge, add one entry
 * (and its two strings).
 *
 * Community badges set [verdict] (and match it). [tile] decides whether the badge also shows on
 * the app tile; the app's details and the badge info screen always show every badge.
 */
enum class Badge(
    val group: BadgeGroup,
    val icon: ImageVector,
    val label: StringResource,
    val description: StringResource,
    val color: @Composable () -> Color,
    val verdict: Verdict? = null,
    val tile: TileRule = TileRule.ALWAYS,
    val appliesTo: (BadgeSubject) -> Boolean = { verdict != null && it.verdict == verdict },
) {
    RECOMMENDED(
        BadgeGroup.REMOVAL, Icons.Default.Check,
        Res.string.badge_recommended_label, Res.string.badge_recommended_description,
        { Color.Green }, appliesTo = { it.app.removalLevel == RemovalLevel.RECOMMENDED },
    ),
    ADVANCED(
        BadgeGroup.REMOVAL, Icons.Default.Settings,
        Res.string.badge_advanced_label, Res.string.badge_advanced_description,
        { Color.Yellow }, appliesTo = { it.app.removalLevel == RemovalLevel.ADVANCED },
    ),
    EXPERT(
        BadgeGroup.REMOVAL, Icons.Default.Warning,
        Res.string.badge_expert_label, Res.string.badge_expert_description,
        { Color.Red }, appliesTo = { it.app.removalLevel == RemovalLevel.EXPERT },
    ),
    UNSAFE(
        BadgeGroup.REMOVAL, Icons.Default.Close,
        Res.string.badge_unsafe_label, Res.string.badge_unsafe_description,
        { Color.Magenta }, appliesTo = { it.app.removalLevel == RemovalLevel.UNSAFE },
    ),
    SAFE(
        BadgeGroup.COMMUNITY, Icons.Default.Check,
        Res.string.community_verdict_safe, Res.string.badge_safe_description,
        { CommunitySafeGreen }, verdict = Verdict.SAFE, tile = TileRule.WITHOUT_REMOVAL_LEVEL,
    ),
    BOOTLOOP(
        BadgeGroup.COMMUNITY, Icons.Default.Warning,
        Res.string.community_verdict_bootloop, Res.string.badge_bootloop_description,
        // Always: a missed bootloop warning can brick a phone, whatever UAD says.
        { CommunityBootloopRed }, verdict = Verdict.BOOTLOOP,
    ),
    BREAKS(
        BadgeGroup.COMMUNITY, Icons.Default.Settings,
        Res.string.community_verdict_breaks, Res.string.badge_breaks_description,
        { CommunityReinstalledYellow }, verdict = Verdict.BREAKS, tile = TileRule.WITHOUT_REMOVAL_LEVEL,
    ),
    UNCLEAR(
        BadgeGroup.COMMUNITY, Icons.Default.QuestionMark,
        Res.string.community_verdict_unclear, Res.string.badge_unclear_description,
        { CommunityUnclearGray }, verdict = Verdict.UNCLEAR, tile = TileRule.NEVER,
    ),
    SYSTEM(
        BadgeGroup.OTHER, Icons.Default.Android,
        Res.string.badge_system_label, Res.string.badge_system_description,
        { Color.DarkGray }, appliesTo = { it.app.isSystemApp },
    ),
    DISABLED(
        BadgeGroup.OTHER, Icons.Default.DisabledByDefault,
        Res.string.badge_disabled_label, Res.string.badge_disabled_description,
        { MaterialTheme.colorScheme.tertiary }, appliesTo = { it.app.isDisabled },
    ),
    CANTA(
        BadgeGroup.OTHER, Icons.Default.RestoreFromTrash,
        Res.string.badge_canta_label, Res.string.badge_canta_description,
        { Color.Red.copy(alpha = 0.7f) }, appliesTo = { it.app.packageName == CANTA_PACKAGE_NAME },
    );

    companion object {
        /** The community badge for [verdict]; null for verdicts without one (too few reports, unknown). */
        fun forVerdict(verdict: Verdict): Badge? = entries.firstOrNull { it.verdict == verdict }
    }
}

/** The badges on [app]'s tile, in display order. [verdict] is the actionable community verdict. */
fun badgesFor(app: AppInfo, verdict: Verdict = Verdict.UNKNOWN): List<Badge> =
    BadgeSubject(app, verdict).let { subject ->
        Badge.entries.filter { it.appliesTo(subject) && it.tile.allows(subject) }
    }
