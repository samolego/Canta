package org.samo_lego.canta.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.samo_lego.canta.R
import org.samo_lego.canta.extension.add
import org.samo_lego.canta.ui.AppsType
import org.samo_lego.canta.ui.dialog.AppInfoDialog
import org.samo_lego.canta.ui.viewmodel.AppListViewModel
import org.samo_lego.canta.util.AppInfo
import org.samo_lego.canta.util.LogUtils
import org.samo_lego.canta.util.RemovalRecommendation

@Composable
fun AppList(
    appType: AppsType = AppsType.INSTALLED,
    appListModel: AppListViewModel,
    enableSelectAll: Boolean = false,
) {
    val context = LocalContext.current
    var showAppDialog by remember { mutableStateOf<AppInfo?>(null) }

    val appList by remember {
        derivedStateOf {
            appListModel.appList.filter {
                when (appType) {
                    AppsType.INSTALLED -> !it.isUninstalled
                    AppsType.UNINSTALLED -> it.isUninstalled
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (appListModel.appList.isEmpty()) {
            appListModel.loadInstalled(context.packageManager, context.filesDir, context)
        }
    }

    Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
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
            if (appType == AppsType.UNINSTALLED || enableSelectAll && appListModel.selectedApps.isNotEmpty() && appListModel.selectedFilter.removalRecommendation == RemovalRecommendation.RECOMMENDED) {
                SelectAllOption(
                    onCheckedChange = {
                        if (!it) {
                            appListModel.selectedApps.clear()
                        } else {
                            appList.map { it.packageName }.forEach { appListModel.selectedApps.add(it) }
                        }
                    }
                )
            }
            if (appList.isNotEmpty()) {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = WindowInsets.navigationBars.asPaddingValues()
                ) {
                    items(appList, key = { it.packageName }) { appInfo ->
                        AppTile(
                                appInfo = appInfo,
                                isSelected =
                                        appListModel.selectedApps.contains(appInfo.packageName),
                                onCheckChanged = { checked ->
                                    if (checked) {
                                        appListModel.selectedApps.add(appInfo.packageName)
                                    } else {
                                        appListModel.selectedApps.remove(appInfo.packageName)
                                    }
                                },
                                onShowDialog = { showAppDialog = appInfo }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            } else {
                Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                ) { Text(stringResource(R.string.no_apps_found)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingBadgesIndicator() {
    Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.loading_badges))
    }
}

@Composable
fun SelectAllOption(
    onCheckedChange: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(false) }
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.select_all))
        Spacer(modifier = Modifier.size(8.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = {
                checked = !checked
                onCheckedChange(checked)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingAppsInfo() {
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column {
            CircularProgressIndicator(
                    modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.loading_apps))
        }
    }
}
