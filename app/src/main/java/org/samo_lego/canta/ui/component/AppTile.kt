package org.samo_lego.canta.ui.component

import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.samo_lego.canta.util.AppInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTile(
    appInfo: AppInfo,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    onShowDialog: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(
            onClick = {
                // Show dialog about app
                onShowDialog()
            },
        ),
        headlineContent = { Text(appInfo.name) },
        supportingContent = {
            Column {
                Text(appInfo.packageName)
                FlowRow {
                    if (appInfo.removalInfo != null) {
                        RemovalBadge(removalRecommendation = appInfo.removalInfo!!)
                    }
                    if (appInfo.isSystemApp) {
                        SystemBadge()
                    }
                }
            }
        },
        leadingContent = {
            AppIconImage(appInfo)
        },
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    )
}

@Composable
fun AppIconImage(
    appInfo: AppInfo,
) {
    val context = LocalContext.current
    val appIcon = try {
        context.packageManager.getApplicationIcon(appInfo.packageName)
    } catch (e: NameNotFoundException) {
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
        model = ImageRequest.Builder(context)
            .data(appIconImage)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = Modifier
            .padding(4.dp)
            .width(48.dp)
            .height(48.dp)
    )
}


/*
@Preview
@Composable
fun InstalledAppTileDemo() {
    InstalledAppTile(
        appInfo = AppInfo(
            "Canta",
            packageName = "org.samo_lego.canta",
            isSystemApp = false,

        ),
        isSelected =,
        onCheckChanged =
    )
}*/