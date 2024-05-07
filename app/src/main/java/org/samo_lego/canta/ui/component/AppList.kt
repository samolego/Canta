package org.samo_lego.canta.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.samo_lego.canta.util.AppInfo


@Composable
fun AppList(
    getApps: () -> List<AppInfo>,
) {
    val availableApps = remember { mutableStateListOf<AppInfo>() }
    val selectedAppsForRemoval = remember { mutableStateListOf<String>() }

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (availableApps.isEmpty()) {
            val installed = getApps()
            for (app in installed) {
                availableApps.add(app)
            }
            isLoading = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(availableApps.size) { appInfoIx ->
                    val appInfo = availableApps[appInfoIx]
                    AppTile(
                        appInfo = appInfo,
                        isSelected = selectedAppsForRemoval.contains(appInfo.packageName),
                        onCheckChanged = { checked ->
                            if (checked) {
                                selectedAppsForRemoval.add(appInfo.packageName)
                            } else {
                                selectedAppsForRemoval.remove(appInfo.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}
