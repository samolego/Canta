package org.samo_lego.canta.ui.dialog

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.samo_lego.canta.ui.component.AppIconImage
import org.samo_lego.canta.util.AppInfo


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoDialog(
    appInfo: AppInfo,
    onDismiss: () -> Unit,
) {
    val bloatDescripton = appInfo.description
    val clipboardManager = LocalClipboardManager.current

    val context = LocalContext.current
    val appIcon = try {
        context.packageManager.getApplicationIcon(appInfo.packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }


    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large),
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        ),
        content = {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row {
                        if (appIcon != null) {
                            AppIconImage(
                                appIconImage = appIcon,
                                contentDescription = "${appInfo.name} icon"
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = appInfo.name,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.shapes.small
                            )
                            .clickable(
                                onClick = {
                                    // Copy package name to clipboard
                                    clipboardManager.setText(AnnotatedString(appInfo.packageName))
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(12.dp),
                            contentDescription = "Copy package name to clipboard",
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterVertically),
                            text = appInfo.packageName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                if (bloatDescripton != null) {
                    Text(text = bloatDescripton, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = "No description available",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
    )
}

@Preview
@Composable
fun AppInfoDialogPreview() {
    AppInfoDialog(
        appInfo = AppInfo(
            "App name",
            packageName = "com.example.app.very.long.package.name.that.should.overflow",
            isSystemApp = false,
            isUninstalled = false,
            versionCode = 1,
            versionName = "1.0",
            bloatData = null,
        ),
        onDismiss = {}
    )
}

@Preview
@Composable
fun AppInfoDialogPreviewShort() {
    AppInfoDialog(
        appInfo = AppInfo(
            "App name",
            packageName = "com.example.app",
            isSystemApp = false,
            isUninstalled = false,
            versionCode = 1,
            versionName = "1.0",
            bloatData = null,
        ),
        onDismiss = {}
    )
}
