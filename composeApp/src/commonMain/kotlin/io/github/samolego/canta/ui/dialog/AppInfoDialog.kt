package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.app_icon
import io.github.samolego.canta.generated.resources.app_size
import io.github.samolego.canta.generated.resources.copy_package_name_to_clipboard
import io.github.samolego.canta.generated.resources.no_description_available
import io.github.samolego.canta.ui.component.AppIconImage
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.text.UrlText
import io.github.samolego.canta.ui.component.WIDE_DIALOG_WIDTH
import io.github.samolego.canta.util.formatFileSize
import org.jetbrains.compose.resources.stringResource

/** Tallest the (scrolling) bloat description may get. */
private val DESCRIPTION_MAX_HEIGHT = 480.dp

@Composable
fun AppInfoDialog(
    appInfo: AppInfo,
    platform: CantaPlatform,
    onDismiss: () -> Unit,
) {
    CantaDialog(onDismissRequest = onDismiss, widthFraction = WIDE_DIALOG_WIDTH) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
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
        Column(modifier = Modifier.heightIn(max = DESCRIPTION_MAX_HEIGHT).verticalScroll(rememberScrollState())) {
            SelectionContainer {
                UrlText(
                    text = appInfo.description ?: stringResource(Res.string.no_description_available),
                )
            }
        }
    }
}
