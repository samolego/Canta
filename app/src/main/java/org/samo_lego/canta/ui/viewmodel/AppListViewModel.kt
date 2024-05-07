package org.samo_lego.canta.ui.viewmodel

import android.icu.text.Collator
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.samo_lego.canta.util.AppInfo
import java.util.Locale

class AppListViewModel : ViewModel() {

    companion object {
        private const val TAG = "AppListViewModel"
        private var apps by mutableStateOf<List<AppInfo>>(emptyList())
    }

    var search by mutableStateOf("")
    var showSystemApps by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
        private set

    private val sortedList by derivedStateOf {
        val comparator = compareBy<AppInfo> {
            when {
                it.isSystemApp -> 0
                //it.hasCustomProfile -> 1
                else -> 1
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::name))
        apps.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    val appList by derivedStateOf {
        sortedList.filter {
            it.name.contains(search, true) || it.packageName.contains(
                search, true
            )
        }.filter { showSystemApps }
    }

    /*suspend fun fetchAppList(pm: PackageManager) {

        isRefreshing = true

        withContext(Dispatchers.IO) {
            val start = SystemClock.elapsedRealtime()

            val allPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val packages = allPackages.list

            apps = packages.map {
                val appInfo = it.applicationInfo
                val uid = appInfo.uid
                val profile = Natives.getAppProfile(it.packageName, uid)
                AppInfo(
                    name = appInfo.loadLabel(pm).toString(),
                    packageInfo = it,
                    profile = profile,
                )
            }.filter { it.packageName != ksuApp.packageName }
            Log.i(TAG, "Load cost: ${SystemClock.elapsedRealtime() - start}")
        }
    }*/

}