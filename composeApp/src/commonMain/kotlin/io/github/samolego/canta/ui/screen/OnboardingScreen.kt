package io.github.samolego.canta.ui.screen

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import io.github.samolego.canta.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.accept
import io.github.samolego.canta.generated.resources.contribute
import io.github.samolego.canta.generated.resources.exit
import io.github.samolego.canta.generated.resources.onboarding_badges_title
import io.github.samolego.canta.generated.resources.onboarding_change_later
import io.github.samolego.canta.generated.resources.onboarding_change_title
import io.github.samolego.canta.generated.resources.onboarding_contribute_subtitle
import io.github.samolego.canta.generated.resources.onboarding_contribute_title
import io.github.samolego.canta.generated.resources.onboarding_dont_share
import io.github.samolego.canta.generated.resources.onboarding_ip_note
import io.github.samolego.canta.generated.resources.onboarding_ip_title
import io.github.samolego.canta.generated.resources.onboarding_never_sent
import io.github.samolego.canta.generated.resources.onboarding_never_sent_title
import io.github.samolego.canta.generated.resources.onboarding_opt_out_note
import io.github.samolego.canta.generated.resources.onboarding_shared
import io.github.samolego.canta.generated.resources.onboarding_shared_title
import io.github.samolego.canta.generated.resources.onboarding_welcome_subtitle
import io.github.samolego.canta.generated.resources.onboarding_welcome_title
import io.github.samolego.canta.generated.resources.privacy_policy
import io.github.samolego.canta.generated.resources.show_whats_sent
import io.github.samolego.canta.ui.dialog.NoWarrantyContent
import io.github.samolego.canta.ui.dialog.TelemetryPreviewDialog
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://github.com/samolego/Canta/blob/master/privacy_policy.md"

/** Widest the onboarding content gets, so it stays readable in a desktop window. */
private val MAX_CONTENT_WIDTH = 560.dp

private const val PAGE_WARRANTY = 0
private const val PAGE_CONTRIBUTE = 1
private const val PAGE_COUNT = 2


/**
 * First-run flow: the no-warranty disclaimer, then the analytics opt-in.
 * The disclaimer can't be skipped: only Accept leads past it, and system
 * back on this start destination just leaves the app.
 *
 * @param onFinish called with whether the user chose to contribute.
 */
@Composable
fun OnboardingScreen(
    platform: CantaPlatform,
    closeApp: () -> Unit,
    onFinish: (contribute: Boolean) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(PAGE_WARRANTY) }

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxSize(),
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(300)))
                    .togetherWith(
                        slideOutHorizontally(tween(300)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)),
                    )
                    .using(SizeTransform(clip = false))
            },
        ) { current ->
            when (current) {
                PAGE_WARRANTY -> WarrantyPage(closeApp = closeApp, onAccept = { page = PAGE_CONTRIBUTE })
                else -> ContributePage(platform = platform, onFinish = onFinish)
            }
        }
    }
}

/** Scrollable [content] on top, a fixed bottom bar with the dots and the left / right actions. */
@Composable
private fun OnboardingPage(
    pageIndex: Int,
    badge: @Composable () -> Unit,
    title: StringResource,
    subtitle: StringResource,
    negative: @Composable () -> Unit,
    positive: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            badge()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
        PageDots(current = pageIndex, count = PAGE_COUNT)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            negative()
            positive()
        }
    }
}

/** [icon] in a filled circle, the header of a page. */
@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val color = if (index == current) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun WarrantyPage(closeApp: () -> Unit, onAccept: () -> Unit) {
    OnboardingPage(
        pageIndex = PAGE_WARRANTY,
        badge = {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)),
            )
        },
        title = Res.string.onboarding_welcome_title,
        subtitle = Res.string.onboarding_welcome_subtitle,
        negative = { TextButton(onClick = closeApp) { Text(stringResource(Res.string.exit)) } },
        positive = { Button(onClick = onAccept) { Text(stringResource(Res.string.accept)) } },
    ) {
        NoWarrantyContent()
    }
}

@Composable
private fun ContributePage(platform: CantaPlatform, onFinish: (Boolean) -> Unit) {
    var showPreview by rememberSaveable { mutableStateOf(false) }

    // Equal prominence on purpose: neither choice is pre-selected, both are full buttons.
    OnboardingPage(
        pageIndex = PAGE_CONTRIBUTE,
        badge = { IconBadge(Icons.Default.Favorite) },
        title = Res.string.onboarding_contribute_title,
        subtitle = Res.string.onboarding_contribute_subtitle,
        negative = {
            OutlinedButton(onClick = { onFinish(false) }) { Text(stringResource(Res.string.onboarding_dont_share)) }
        },
        positive = {
            Button(onClick = { onFinish(true) }) { Text(stringResource(Res.string.contribute)) }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoRow(Icons.Default.CheckCircle, Res.string.onboarding_shared_title, Res.string.onboarding_shared)
            InfoRow(Icons.Default.Block, Res.string.onboarding_never_sent_title, Res.string.onboarding_never_sent)
            InfoRow(Icons.Default.Lock, Res.string.onboarding_ip_title, Res.string.onboarding_ip_note)
            InfoRow(Icons.Default.Info, Res.string.onboarding_badges_title, Res.string.onboarding_opt_out_note)
            InfoRow(Icons.Default.Settings, Res.string.onboarding_change_title, Res.string.onboarding_change_later)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            TextButton(onClick = { showPreview = true }) { Text(stringResource(Res.string.show_whats_sent)) }
            TextButton(onClick = { platform.openUrl(PRIVACY_POLICY_URL) }) {
                Text(stringResource(Res.string.privacy_policy))
            }
        }
    }

    if (showPreview) TelemetryPreviewDialog(onDismiss = { showPreview = false })
}

@Composable
private fun InfoRow(icon: ImageVector, title: StringResource, body: StringResource) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
