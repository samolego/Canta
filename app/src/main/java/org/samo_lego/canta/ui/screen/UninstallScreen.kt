package org.samo_lego.canta.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.samo_lego.canta.ui.component.AppList
import org.samo_lego.canta.util.AppInfo
import org.samo_lego.canta.util.Filter


@Composable
fun UninstallScreen(
    uninstallApp: (String) -> Boolean,
    getInstalledApps: () -> List<AppInfo>,
) {
    val installedApps = remember { mutableStateListOf<AppInfo>() }
    val selectedAppsForRemoval = remember { mutableStateListOf<String>() }
    val filters by remember { mutableStateOf(emptySet<Filter>()) }
    val lastRemovalFilter by remember { mutableStateOf<Filter?>(null) }

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (installedApps.isEmpty()) {
            val installed = getInstalledApps()
            for (app in installed) {
                installedApps.add(app)
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TabRow(
            selectedTabIndex = 0,
            // backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Tab(
                selected = true,
                onClick = { /* Handle tab click */ },
                text = { Text(text = "Installed") },
                //icon = { Icon(Icons.Default.Delete) }
            )
            Tab(
                selected = false,
                onClick = { /* Handle tab click */ },
                text = { Text(text = "Uninstalled") },
            )
        }
        AppList(
            fabAction = uninstallApp,
            getApps = { installedApps },
        )
    }
}
