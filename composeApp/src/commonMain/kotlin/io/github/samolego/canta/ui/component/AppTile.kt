package io.github.samolego.canta.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.data.app.AppInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppTile(
    modifier: Modifier = Modifier,
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
                        if (appInfo.badgeInfo != null) {
                            AppBadge(type = appInfo.badgeInfo!!)
                        }
                        if (appInfo.isSystemApp) {
                            SystemBadge()
                        }
                        if (appInfo.isDisabled) {
                            DisabledBadge()
                        }
                        if (appInfo.packageName == CANTA_PACKAGE_NAME) {
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

/** Size app icons are shown at; Android renders them at exactly this size. */
val APP_ICON_SIZE = 48.dp

@Composable
fun AppIconImage(
    appInfo: AppInfo,
) {
    AppIconImage(
        appIconImage = appInfo.icon,
        contentDescription = appInfo.name,
    )
}

@Composable
fun AppIconImage(
    appIconImage: ImageBitmap?,
    contentDescription: String,
) {
    if (appIconImage != null) {
        Image(
            bitmap = appIconImage,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(4.dp).size(APP_ICON_SIZE)
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = contentDescription,
            modifier = Modifier.padding(4.dp).size(APP_ICON_SIZE)
        )
    }
}
