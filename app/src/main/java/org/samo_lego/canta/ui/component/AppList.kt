package org.samo_lego.canta.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.samo_lego.canta.ui.dialog.AppInfoDialog
import org.samo_lego.canta.ui.viewmodel.AppListViewModel
import org.samo_lego.canta.util.AppInfo


@Composable
fun AppList() {
    val selectedAppsForRemoval = remember { mutableStateListOf<String>() }
    val appListModel = viewModel<AppListViewModel>()
    val context = LocalContext.current
    var showAppDialog by remember {
        mutableStateOf<AppInfo?>(null)
    }

    LaunchedEffect(Unit) {
        if (appListModel.appList.isEmpty()) {
            appListModel.loadInstalled(context.packageManager, context.filesDir)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        if (showAppDialog != null) {
            AppInfoDialog(
                appInfo = showAppDialog!!,
                onDismiss = { showAppDialog = null },
            )
        }

        if (appListModel.isLoading) {
            LoadingAppsInfo()
        } else {
            if (appListModel.isLoadingBadges) {
                LoadingBadgesIndicator()
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
            ) {
                items(appListModel.appList, key = { it.packageName }) { appInfo ->
                    AppTile(
                        appInfo = appInfo,
                        isSelected = selectedAppsForRemoval.contains(appInfo.packageName),
                        onCheckChanged = { checked ->
                            if (checked) {
                                selectedAppsForRemoval.add(appInfo.packageName)
                            } else {
                                selectedAppsForRemoval.remove(appInfo.packageName)
                            }
                        },
                        onShowDialog = {
                            showAppDialog = appInfo
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingBadgesIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(4.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text("Loading badges ...")
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingAppsInfo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text("Loading apps")
        }
    }
}

@Preview
@Composable
fun AppListPreview() {
    AppList()
}
