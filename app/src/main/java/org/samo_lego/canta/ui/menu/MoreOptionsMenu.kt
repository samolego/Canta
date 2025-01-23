package org.samo_lego.canta.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.samo_lego.canta.R

@Composable
fun MoreOptionsMenu(
        showMenu: Boolean,
        showBadgeInfoDialog: () -> Unit,
        showLogsDialog: () -> Unit,
        onDismiss: () -> Unit,
) {
    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
    ) {
        // Badge info dialog
        Row(
                modifier =
                        Modifier.fillMaxWidth().clickable { showBadgeInfoDialog() }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
        ) { Text(stringResource(R.string.badge_info)) }

        // Logs dialog
        Row(
                modifier = Modifier.fillMaxWidth().clickable { showLogsDialog() }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
        ) { Text(stringResource(R.string.logs)) }
    }
}
