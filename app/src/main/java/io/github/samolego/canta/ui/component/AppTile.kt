package io.github.samolego.canta.ui.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.samolego.canta.packageName
import io.github.samolego.canta.util.BloatData
import io.github.samolego.canta.util.InstallData
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.RemovalRecommendation
import io.github.samolego.canta.util.apps.AppInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTile(
    modifier: Modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
    appInfo: AppInfo,
    isSelected: Boolean,
    enabled: Boolean = true,
    onCheckChanged: (Boolean) -> Unit,
    onShowDialog: () -> Unit,
    showBorder: Boolean = isSelected,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    checkedBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = 0.2f
    ),
) {
    Card(
            modifier = modifier,
            elevation =
                    CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
            border =
                    if (showBorder) {
                        BorderStroke(2.dp, checkedColor)
                    } else null
    ) {
        ListItem(
                colors =
                        ListItemDefaults.colors(
                                containerColor =
                                        if (isSelected) {
                                            checkedBackgroundColor
                                        } else {
                                            MaterialTheme.colorScheme
                                                .surface
                                                .copy(alpha = if (enabled) 1f else 0.5f)
                                        }
                        ),
                modifier =
                        Modifier.clickable(
                                onClick = onShowDialog,
                        ),
                headlineContent = { Text(appInfo.name) },
                supportingContent = {
                    Column {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                        ) { Text(appInfo.packageName, modifier = Modifier.weight(9f)) }

                        FlowRow {
                            if (appInfo.removalInfo != null) {
                                RemovalBadge(type = appInfo.removalInfo!!)
                            }
                            if (appInfo.isSystemApp) {
                                SystemBadge()
                            }
                            if (appInfo.isDisabled) {
                                DisabledBadge()
                            }
                            if (appInfo.packageName == packageName) {
                                CantaBadge()
                            }
                        }
                    }
                },
                leadingContent = { AppIconImage(appInfo) },
                trailingContent = {
                    Checkbox(
                            checked = isSelected,
                            onCheckedChange = onCheckChanged,
                            enabled = enabled,
                            colors =
                                    CheckboxDefaults.colors(
                                            checkedColor = checkedColor,
                                            uncheckedColor =
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                    )
                }
        )
    }
}


@Composable
fun SelectedAppTile(
    modifier: Modifier = Modifier,
    appInfo: AppInfo,
    onCheckChanged: (Boolean) -> Unit,
    onShowDialog: () -> Unit,
) {
    AppTile(
        modifier = modifier,
        appInfo = appInfo,
        isSelected = true,
        showBorder = false,
        onCheckChanged = onCheckChanged,
        onShowDialog = onShowDialog,
        checkedColor = MaterialTheme.colorScheme.error,
        checkedBackgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
    )
}

@Composable
fun AppIconImage(
        appInfo: AppInfo,
) {
    val context = LocalContext.current
    val appIcon =
            try {
                context.packageManager.getApplicationIcon(appInfo.packageName)
            } catch (e: Exception) {
                LogUtils.w("AppTile", "Cannot find app icon: ${e.stackTraceToString()}")
                null
            }

    if (appIcon != null) {
        AppIconImage(
                appIconImage = appIcon,
                contentDescription = appInfo.name,
        )
    }
}

@Composable
fun AppIconImage(
        appIconImage: Drawable,
        contentDescription: String,
) {
    val context = LocalContext.current
    AsyncImage(
            model = ImageRequest.Builder(context).data(appIconImage).crossfade(true).build(),
            contentDescription = contentDescription,
            modifier = Modifier.padding(4.dp).width(48.dp).height(48.dp)
    )
}

@Preview
@Composable
fun CantaAppTileDemo() {
    AppTile(
            appInfo =
                    AppInfo(
                            "Canta",
                            packageName = "io.github.samolego.canta",
                            isSystemApp = false,
                            isDisabled = false,
                            isUninstalled = false,
                            versionCode = 1,
                            versionName = "1.0",
                            bloatData =
                                    BloatData(
                                            installData = InstallData.OEM,
                                            description = "Canta is not a system app",
                                            removal = RemovalRecommendation.RECOMMENDED,
                                    ),
                    ),
            isSelected = false,
            enabled = false,
            onCheckChanged = {},
            onShowDialog = {},
    )
}

@Preview
@Composable
fun LongTileDemo() {
    AppTile(
            appInfo =
                    AppInfo(
                            "Canta",
                            packageName =
                                    "very.long.package.name.that.should.overflow.and.maybe.it.will.overflow.even.2.lines",
                            isSystemApp = true,
                            isUninstalled = false,
                            versionCode = 1,
                            versionName = "1.0",
                            isDisabled = true,
                            bloatData =
                                    BloatData(
                                            installData = InstallData.OEM,
                                            description = "Canta is a system app",
                                            removal = RemovalRecommendation.RECOMMENDED,
                                    ),
                    ),
            isSelected = false,
            onCheckChanged = {},
            onShowDialog = {},
    )
}
