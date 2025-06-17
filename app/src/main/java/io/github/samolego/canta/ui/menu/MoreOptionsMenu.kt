package io.github.samolego.canta.ui.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.R
import io.github.samolego.canta.ui.navigation.Screen
import io.github.samolego.canta.ui.viewmodel.AppListViewModel

@Composable
fun MoreOptionsMenu(
        showMenu: Boolean,
        showBadgeInfoDialog: () -> Unit,
        navigateToPage: (route: String) -> Unit,
        onDismiss: () -> Unit,
        appListViewModel: AppListViewModel,
) {

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(200.dp)
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
                    navigateToPage(Screen.Logs.route)
                    onDismiss()
                }
        )

        // Settings page
        DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = {
                    navigateToPage(Screen.Settings.route)
                    onDismiss()
                }
        )

        DropdownMenuItem(
            text = { Text("Presets") },
            onClick = {
                navigateToPage(Screen.Presets.route)
                onDismiss()
            }
        )
    }
}
