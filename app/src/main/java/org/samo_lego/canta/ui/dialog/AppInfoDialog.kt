package org.samo_lego.canta.ui.dialog

import UrlText
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.samo_lego.canta.R
import org.samo_lego.canta.ui.component.AppIconImage
import org.samo_lego.canta.util.AppInfo
import org.samo_lego.canta.util.BloatData
import org.samo_lego.canta.util.InstallData
import org.samo_lego.canta.util.RemovalRecommendation
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoDialog(
        appInfo: AppInfo,
        onDismiss: () -> Unit,
) {
    val bloatDescription = appInfo.description
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val appIcon =
            try {
                context.packageManager.getApplicationIcon(appInfo.packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

    val appSize =
            try {
                val packageInfo = context.packageManager.getPackageInfo(appInfo.packageName, 0)
                val appFile = packageInfo.applicationInfo?.sourceDir?.let { File(it) }
                appFile?.let { formatFileSize(context, it.length()) } ?: "? MB"
            } catch (e: Exception) {
                null
            }

    BasicAlertDialog(
            modifier =
                    Modifier.fillMaxWidth(0.8f)
                            .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    MaterialTheme.shapes.large
                            ),
            properties =
                    DialogProperties(
                            decorFitsSystemWindows = true,
                            usePlatformDefaultWidth = false,
                    ),
            onDismissRequest = onDismiss,
    ) {
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

                    Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                        Text(text = appInfo.name)
                        if (appSize != null) {
                            Text(
                                    text = appSize,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                Row(
                        modifier =
                                Modifier.background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                MaterialTheme.shapes.small
                                        )
                                        .clickable(
                                                onClick = {
                                                    clipboardManager.setText(
                                                            AnnotatedString(appInfo.packageName)
                                                    )
                                                }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                            Icons.Default.ContentCopy,
                            modifier = Modifier.align(Alignment.CenterVertically).size(12.dp),
                            contentDescription =
                                    stringResource(R.string.copy_package_name_to_clipboard),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = appInfo.packageName,
                            style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            val maxScrollableHeight = (screenHeight * 0.6f)
            Column(
                modifier = Modifier
                    .heightIn(max = maxScrollableHeight)
                    .verticalScroll(rememberScrollState())
            ) {
            if (bloatDescription != null) {
                SelectionContainer {
                    UrlText(text = bloatDescription)
                }
            } else {
                Text(
                        text = stringResource(R.string.no_description_available),
                        style = MaterialTheme.typography.bodySmall
                )
            }
        }
            if (!appInfo.isUninstalled) {
                Row(modifier = Modifier.align(Alignment.End)) {
                    Button(
                        onClick = {
                            val intent =
                                Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", appInfo.packageName, null)
                                }
                            context.startActivity(intent)
                        },
                        colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                            MaterialTheme.colorScheme.secondaryContainer,
                            contentColor =
                            MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.app_settings))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppInfoDialogPreview() {
    AppInfoDialog(
            appInfo =
                    AppInfo(
                            "App name",
                            packageName =
                                    "com.example.app.very.long.package.name.that.should.overflow",
                            isSystemApp = false,
                            isUninstalled = false,
                            versionCode = 1,
                            versionName = "1.0",
                            isDisabled = false,
                            bloatData = null,
                    ),
            onDismiss = {}
    )
}

@Preview
@Composable
fun AppInfoDialogPreviewShort() {
    AppInfoDialog(
            appInfo =
                    AppInfo(
                            "App name",
                            packageName = "com.example.app",
                            isSystemApp = false,
                            isUninstalled = false,
                            versionCode = 1,
                            versionName = "1.0",
                            isDisabled = true,
                            bloatData =
                                    BloatData(
                                            InstallData.OEM,
                                            "Long app description. A link to a site: https://play.google.com/store/apps/details?id=com.android.vending",
                                            RemovalRecommendation.EXPERT,
                                    ),
                    ),
            onDismiss = {}
    )
}
