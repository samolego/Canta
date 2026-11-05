package io.github.samolego.canta.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.community_verdict_bootloop
import io.github.samolego.canta.generated.resources.community_verdict_breaks
import io.github.samolego.canta.generated.resources.community_verdict_safe
import io.github.samolego.canta.generated.resources.community_verdict_unclear
import io.github.samolego.canta.util.Verdict
import org.jetbrains.compose.resources.stringResource

/** The community verdict as text, or null when there is nothing worth showing. */
@Composable
fun Verdict.label(): String? = when (this) {
    Verdict.SAFE -> stringResource(Res.string.community_verdict_safe)
    Verdict.BOOTLOOP -> stringResource(Res.string.community_verdict_bootloop)
    Verdict.BREAKS -> stringResource(Res.string.community_verdict_breaks)
    Verdict.UNCLEAR -> stringResource(Res.string.community_verdict_unclear)
    Verdict.INSUFFICIENT, Verdict.UNKNOWN -> null
}

/** Small warning next to the app's badges; only for verdicts that warn ([Verdict.BOOTLOOP], [Verdict.BREAKS]). */
@Composable
fun CommunityBadge(verdict: Verdict) {
    val color = when (verdict) {
        Verdict.BOOTLOOP -> MaterialTheme.colorScheme.error
        Verdict.BREAKS -> Color(0xFFF9A825)
        else -> return
    }
    AppBadgeContent(label = verdict.label() ?: return, icon = Icons.Default.Warning, color = color)
}
