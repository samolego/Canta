package io.github.samolego.canta.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.app_icon
import io.github.samolego.canta.generated.resources.app_settings
import io.github.samolego.canta.generated.resources.app_size
import io.github.samolego.canta.generated.resources.community_mostly_other_rom
import io.github.samolego.canta.generated.resources.community_mostly_other_version
import io.github.samolego.canta.generated.resources.community_row_all
import io.github.samolego.canta.generated.resources.community_row_device
import io.github.samolego.canta.generated.resources.community_row_family
import io.github.samolego.canta.generated.resources.community_no_data
import io.github.samolego.canta.generated.resources.copy_package_name_to_clipboard
import io.github.samolego.canta.generated.resources.no_description_available
import io.github.samolego.canta.ui.badge.Badge
import io.github.samolego.canta.ui.badge.BadgeChip
import io.github.samolego.canta.ui.component.AppIconImage
import io.github.samolego.canta.ui.component.CLOSE_BUTTON_CLEARANCE
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.ExpandIcon
import io.github.samolego.canta.ui.component.label
import io.github.samolego.canta.ui.component.text.UrlText
import io.github.samolego.canta.ui.component.WIDE_DIALOG_WIDTH
import io.github.samolego.canta.util.CommunityScore
import io.github.samolego.canta.util.Verdict
import io.github.samolego.canta.util.VerdictScope
import io.github.samolego.canta.util.displayed
import io.github.samolego.canta.util.formatFileSize
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Tallest the (scrolling) bloat description may get. */
private val DESCRIPTION_MAX_HEIGHT = 480.dp

@Composable
fun AppInfoDialog(
    appInfo: AppInfo,
    /** Community score, shown below the bloat info; null when analytics is off or there is none. */
    score: CommunityScore? = null,
    platform: CantaPlatform,
    handler: CantaHandler,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    CantaDialog(
        onDismissRequest = onDismiss,
        widthFraction = WIDE_DIALOG_WIDTH,
        onClose = onDismiss,
        buttons = if (!appInfo.isUninstalled) ({
            Button(
                onClick = { scope.launch { handler.openAppDetails(appInfo.packageName) } },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(Res.string.app_settings))
            }
        }) else null,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Kept clear of the close button on both sides, so it stays centered.
            Row(modifier = Modifier.padding(horizontal = CLOSE_BUTTON_CLEARANCE)) {
                AppIconImage(
                    appIconImage = appInfo.icon,
                    contentDescription = stringResource(Res.string.app_icon, appInfo.name),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text(text = appInfo.name)
                    Text(
                        text = appInfo.versionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    appInfo.apkSize?.let { size ->
                        Text(
                            text = stringResource(Res.string.app_size, formatFileSize(size)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                    .clickable { platform.copyToClipboard(appInfo.packageName) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    modifier = Modifier.size(12.dp),
                    contentDescription = stringResource(Res.string.copy_package_name_to_clipboard),
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = appInfo.packageName, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        if (score != null && score.hasInfo) {
            CommunityInfo(score.displayed(appInfo.removalLevel))
            Spacer(modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.heightIn(max = DESCRIPTION_MAX_HEIGHT).verticalScroll(rememberScrollState())) {
            SelectionContainer {
                UrlText(
                    text = appInfo.description ?: stringResource(Res.string.no_description_available),
                )
            }
        }
    }
}

/**
 * The community verdicts as a table: the row the verdict is taken from ([CommunityScore.chosen])
 * first, the others (your device, your ROM family, all ROMs) below it, collapsed until expanded.
 */
@Composable
private fun CommunityInfo(score: CommunityScore) {
    val chosen = score.chosen
    val deviceLabel = stringResource(Res.string.community_row_device)
    val familyLabel = stringResource(Res.string.community_row_family, score.familyLabel)
    val allLabel = stringResource(Res.string.community_row_all)
    val rows = buildList {
        // Without a known ROM there are only the scores across all ROMs.
        if (score.deviceRom != null) {
            add(Triple(VerdictScope.VERSION, deviceLabel, score.device))
            add(Triple(VerdictScope.FAMILY, familyLabel, score.family))
        }
        add(Triple(VerdictScope.OVERALL, allLabel, score.overall))
    }.sortedBy { (scope, _, _) -> if (scope == chosen?.scope) 0 else 1 }
    val others = rows.drop(1)
    var expanded by remember { mutableStateOf(false) }
    // The rows are padded for their highlight; bleed by the same amount so their text lines up
    // with the rest of the dialog.
    Column(modifier = Modifier.bleedHorizontally(ROW_PADDING)) {
        val (_, label, row) = rows.first()
        CommunityRow(
            label = label,
            verdict = row?.verdict,
            modifier = if (others.isEmpty()) Modifier else Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { expanded = !expanded },
            trailing = { if (others.isNotEmpty()) ExpandIcon(expanded) },
        )
        AnimatedVisibility(visible = expanded) {
            Column {
                others.forEach { (_, otherLabel, otherRow) ->
                    CommunityRow(label = otherLabel, verdict = otherRow?.verdict)
                }
            }
        }
        // The verdict comes mostly from another ROM (or ROM version): say so.
        if (chosen != null && !chosen.actionable && chosen.otherRomLabel != null) {
            Text(
                text = stringResource(
                    if (chosen.scope == VerdictScope.FAMILY) Res.string.community_mostly_other_version
                    else Res.string.community_mostly_other_rom,
                    chosen.otherRomLabel,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Inner horizontal padding of a table row, so the highlight of the clickable one has room. */
private val ROW_PADDING = 8.dp

/** Widens the content by [bleed] on both sides (into the dialog's padding), keeping its own width in the layout. */
private fun Modifier.bleedHorizontally(bleed: Dp) = layout { measurable, constraints ->
    val px = bleed.roundToPx()
    val widened = if (constraints.hasBoundedWidth) {
        constraints.copy(minWidth = constraints.minWidth + 2 * px, maxWidth = constraints.maxWidth + 2 * px)
    } else constraints
    val placeable = measurable.measure(widened)
    layout((placeable.width - 2 * px).coerceAtLeast(0), placeable.height) { placeable.place(-px, 0) }
}

/** One table row: where the reports come from, and its verdict badge ("No data" without one: fewer than 30 reports). */
@Composable
private fun CommunityRow(
    label: String,
    verdict: Verdict?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val badge = verdict?.let { Badge.forVerdict(it) }
    Row(
        // Padded on every row, so they line up with the clickable (highlighted) first one.
        modifier = modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (badge != null) {
                BadgeChip(badge)
            } else {
                Text(
                    text = stringResource(Res.string.community_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        // Same width on every row, so the badges line up with the expand icon's row.
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { trailing() }
    }
}
