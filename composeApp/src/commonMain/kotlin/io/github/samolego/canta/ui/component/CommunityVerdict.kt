package io.github.samolego.canta.ui.component

import androidx.compose.runtime.Composable
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
