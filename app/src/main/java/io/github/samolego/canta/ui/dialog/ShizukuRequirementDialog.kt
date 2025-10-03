package io.github.samolego.canta.ui.dialog

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.samolego.canta.R
import io.github.samolego.canta.SHIZUKU_PACKAGE_NAME
import io.github.samolego.canta.ui.theme.GreenOk
import io.github.samolego.canta.ui.theme.Orange
import io.github.samolego.canta.util.shizuku.ShizukuPermission
import io.github.samolego.canta.util.shizuku.ShizukuStatus

const val SHIZUKU_PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE_NAME"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuRequirementDialog(
    onClose: (shouldProceed: Boolean) -> Unit,
    shizukuStatus: ShizukuStatus,
) {
    val context = LocalContext.current
    val launchShizuku = {
        // Try to open Shizuku app
        val intent =
            context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
        if (intent != null) {
            context.startActivity(intent)
        }
    }


    BasicAlertDialog(
        modifier =
        Modifier
            .fillMaxWidth(0.9f)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.shapes.large
            ),
        properties =
        DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        ),
        onDismissRequest = { onClose(false) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                text = stringResource(R.string.shizuku_required),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Description
            Text(
                text = stringResource(R.string.shizuku_requirement_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Requirements list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Install Shizuku
                RequirementItem(
                    text = stringResource(R.string.install_shizuku),
                    isCompleted = shizukuStatus != ShizukuStatus.NOT_INSTALLED,
                    onActionClick =
                    {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(SHIZUKU_PLAY_STORE_URL)
                            )
                        context.startActivity(intent)
                    }
                )

                // Start Shizuku service
                RequirementItem(
                    text = stringResource(R.string.start_shizuku_service),
                    isCompleted = shizukuStatus != ShizukuStatus.NOT_INSTALLED && shizukuStatus != ShizukuStatus.NOT_ACTIVE,
                    onActionClick = launchShizuku,
                )

                // Authorize Canta
                RequirementItem(
                    text = stringResource(R.string.grant_shizuku_permission_to_canta),
                    isCompleted = shizukuStatus == ShizukuStatus.ACTIVE && ShizukuPermission.isCantaAuthorized(),
                    onActionClick = {
                        ShizukuPermission.requestShizukuPermission { permission ->
                            onClose(permission == PackageManager.PERMISSION_GRANTED)
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Close button
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onClose(false) },
            ) { Text(stringResource(R.string.close)) }
        }
    }
}

@Composable
private fun RequirementItem(
    text: String,
    isCompleted: Boolean,
    enabled: Boolean = true,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1.0f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status indicator dot
        Icon(
            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isCompleted) GreenOk else Orange,
            modifier = Modifier.size(16.dp)
        )

        // Requirement text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color =
            if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )

        // Action button (only shown if not completed and action is available)
        if (!isCompleted && onActionClick != null) {
            IconButton(onClick = onActionClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "Take action",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
