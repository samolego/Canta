package org.samo_lego.canta.ui.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.samo_lego.canta.R

@Composable
fun MoreOptionsMenu(
        showMenu: Boolean,
        showBadgeInfoDialog: () -> Unit,
        openLogsScreen: () -> Unit,
        onDismiss: () -> Unit,
) {
    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(180.dp)
    ) {
        // Badge info dialog
        DropdownMenuItem(
                text = { Text(stringResource(R.string.badge_info)) },
                onClick = {
                    showBadgeInfoDialog()
                    onDismiss()
                }
        )

        // Logs page
        DropdownMenuItem(
                text = { Text(stringResource(R.string.logs)) },
                onClick = {
                    openLogsScreen()
                    onDismiss()
                }
        )
    }
}
