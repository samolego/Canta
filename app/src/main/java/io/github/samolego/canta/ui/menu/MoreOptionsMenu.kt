package io.github.samolego.canta.ui.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.samolego.canta.R
import io.github.samolego.canta.ui.viewmodel.AppListViewModel

@Composable
fun MoreOptionsMenu(
        showMenu: Boolean,
        showBadgeInfoDialog: () -> Unit,
        openLogsScreen: () -> Unit,
        openSettingsScreen: () -> Unit,
        onDismiss: () -> Unit,
) {
    val appListViewModel = viewModel<AppListViewModel>()

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(200.dp)
    ) {
        // Clear selection (only shown when apps are selected)
        if (appListViewModel.selectedApps.isNotEmpty()) {
            DropdownMenuItem(
                    text = {
                        Text(
                                pluralStringResource(
                                        R.plurals.clear_selected_apps,
                                        appListViewModel.selectedApps.size,
                                        appListViewModel.selectedApps.size
                                )
                        )
                    },
                    onClick = {
                        appListViewModel.selectedApps.clear()
                        onDismiss()
                    }
            )
            Divider()
        }

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

        // Settings page
        DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = {
                    openSettingsScreen()
                    onDismiss()
                }
        )
    }
}
