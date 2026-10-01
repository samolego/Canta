package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.core.PrivilegeStatus
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.close
import io.github.samolego.canta.generated.resources.grant_shizuku_permission_to_canta
import io.github.samolego.canta.generated.resources.install_shizuku
import io.github.samolego.canta.generated.resources.start_shizuku_service
import io.github.samolego.canta.generated.resources.take_action
import io.github.samolego.canta.generated.resources.shizuku_required
import io.github.samolego.canta.generated.resources.shizuku_requirement_description
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.WIDE_DIALOG_WIDTH
import io.github.samolego.canta.ui.theme.GreenOk
import io.github.samolego.canta.ui.theme.Orange

private const val SHIZUKU_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"
private const val SHIZUKU_GITHUB_URL = "https://github.com/RikkaApps/Shizuku/releases"

/**
 * Walks the user through the Shizuku setup. [onClose] with `true` means
 * "request authorization and continue"; `false` means the user dismissed it.
 */
@Composable
fun ShizukuRequirementDialog(
    onClose: (shouldProceed: Boolean) -> Unit,
    handler: CantaHandler,
    platform: CantaPlatform,
) {
    // Re-read on resume: the user fixes these steps in the Shizuku app and comes back.
    var status by remember { mutableStateOf(handler.privilegeStatus) }
    var isAuthorized by remember { mutableStateOf(handler.isAuthorized) }
    LifecycleResumeEffect(handler) {
        status = handler.privilegeStatus
        isAuthorized = handler.isAuthorized
        onPauseOrDispose {}
    }

    CantaDialog(
        onDismissRequest = { onClose(false) },
        title = stringResource(Res.string.shizuku_required),
        widthFraction = WIDE_DIALOG_WIDTH,
        buttons = {
            TextButton(onClick = { onClose(false) }) { Text(stringResource(Res.string.close)) }
        },
    ) {
        Text(
            text = stringResource(Res.string.shizuku_requirement_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RequirementItem(
                text = stringResource(Res.string.install_shizuku),
                isCompleted = status != PrivilegeStatus.NOT_AVAILABLE,
                onActionClick = { platform.openUrl(SHIZUKU_PLAY_STORE_URL) },
            )
            RequirementItem(
                text = stringResource(Res.string.start_shizuku_service),
                isCompleted = status == PrivilegeStatus.ACTIVE || status == PrivilegeStatus.NOT_AUTHORIZED,
                onActionClick = { platform.openUrl(SHIZUKU_GITHUB_URL) },
            )
            RequirementItem(
                text = stringResource(Res.string.grant_shizuku_permission_to_canta),
                isCompleted = isAuthorized,
                onActionClick = { onClose(true) },
            )
        }
    }
}

@Composable
private fun RequirementItem(
    text: String,
    isCompleted: Boolean,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isCompleted) GreenOk else Orange,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color =
            if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )

        if (!isCompleted) {
            IconButton(onClick = onActionClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = stringResource(Res.string.take_action),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
