package org.samo_lego.canta.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.samo_lego.canta.APP_NAME
import org.samo_lego.canta.extension.getInstalledAppsInfo
import org.samo_lego.canta.extension.getUninstalledAppsInfo
import org.samo_lego.canta.ui.component.AppList
import org.samo_lego.canta.ui.viewmodel.AppListViewModel
import org.samo_lego.canta.ui.viewmodel.ShizukuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantaApp(
    launchShizuku: () -> Unit,
    uninstallApp: (String) -> Boolean,
    reinstallApp: (String) -> Boolean,
) {
    var selectedTab by remember { mutableStateOf(Tab.INSTALLED) }
    val shizukuModel = viewModel<ShizukuViewModel>()
    val appListModel = viewModel<AppListViewModel>()

    val pm = LocalContext.current.packageManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = APP_NAME) },
                navigationIcon = { /* Add navigation icon */ },
                actions = { /* Add top bar actions */ },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        bottomBar = { /* Add bottom bar */ },
        floatingActionButton = {

        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                // backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Tab(
                    selected = selectedTab == Tab.INSTALLED,
                    onClick = {
                        selectedTab = Tab.INSTALLED
                    },
                    text = { Text(text = "Installed") },
                    //icon = { Icon(Icons.Default.Delete) }
                )
                Tab(
                    selected = selectedTab == Tab.UNINSTALLED,
                    onClick = { selectedTab = Tab.UNINSTALLED },
                    text = { Text(text = "Uninstalled") },
                )
            }
            when (selectedTab) {
                Tab.INSTALLED -> AppList(
                    fabAction = uninstallApp,
                    getApps = {
                        return@AppList pm.getInstalledAppsInfo()
                    }
                )

                Tab.UNINSTALLED -> AppList(
                    fabAction = reinstallApp,
                    getApps = {
                        return@AppList pm.getUninstalledAppsInfo()
                    }
                )
            }
        }
    }
}

private enum class Tab {
    INSTALLED,
    UNINSTALLED,
}