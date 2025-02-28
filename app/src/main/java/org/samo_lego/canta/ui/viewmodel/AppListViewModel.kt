package org.samo_lego.canta.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.Collator
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.samo_lego.canta.extension.getAllPackagesInfo
import org.samo_lego.canta.extension.mutableStateSetOf
import org.samo_lego.canta.packageName
import org.samo_lego.canta.ui.dialog.cantaBloatData
import org.samo_lego.canta.util.AppInfo
import org.samo_lego.canta.util.BloatData
import org.samo_lego.canta.util.BloatUtils
import org.samo_lego.canta.util.Filter
import org.samo_lego.canta.util.LogUtils
import org.samo_lego.canta.util.SettingsStore
import java.io.File
import java.util.Locale

class AppListViewModel : ViewModel() {

    companion object {
        private const val TAG = "AppListViewModel"
        private var apps by mutableStateOf<List<AppInfo>>(emptyList())
    }

    var selectedApps = mutableStateSetOf<String>()

    var searchQuery by mutableStateOf("")
    var showSystem by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingBadges by mutableStateOf(false)
        private set

    var selectedFilter by mutableStateOf(Filter.any)

    private val sortedList by derivedStateOf {
        isLoading = true
        val comparator = compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::name)
        apps.filter { selectedFilter.shouldShow(it) }.sortedWith(comparator).also {
            isLoading = false
        }
    }

    val appList by derivedStateOf {
        sortedList.filter {
            it.name.contains(searchQuery, true) || it.packageName.contains(
                searchQuery, true
            )
        }.filter {
            it.isSystemApp || !showSystem
        }
    }

    suspend fun loadInstalled(packageManager: PackageManager, filesDir: File, context: Context) {
        isLoading = true

        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            apps = packageManager.getAllPackagesInfo()
            val endPackages = System.currentTimeMillis()
            LogUtils.i(TAG, "Loaded packages in ${endPackages - start}ms")
            isLoading = false

            isLoadingBadges = true
            // Load app data file
            val uadList = File(filesDir, "uad_lists.json")
            val bloatFetcher = BloatUtils()

            // Get the auto-update preference
            val settingsStore = SettingsStore(context)
            val autoUpdate = settingsStore.autoUpdateBloatListFlow.first()

            val uadLists =
                if (!uadList.exists() || (bloatFetcher.checkForUpdates(settingsStore.getLatestCommitHash()) && autoUpdate)) {
                    uadList.createNewFile()

                    val (json, hash) = bloatFetcher.fetchBloatList(uadList)

                    // Write the hash to settings
                    settingsStore.setLatestCommitHash(hash)

                    json

                } else {
                    // Just read the file
                    JSONObject(uadList.readText())
                }

            // Parse json to map
            val bloatMap = mutableMapOf<String, BloatData>()
            for (key in uadLists.keys()) {
                val json = uadLists.getJSONObject(key)
                val bloatData = BloatData.fromJson(json)

                bloatMap[key] = bloatData
            }

            // Add Canta app info
            bloatMap[packageName] = cantaBloatData(context)

            // Assign bloat data to apps
            apps = apps.map { app ->
                if (bloatMap[app.packageName] != null) {
                    app.copy(bloatData = bloatMap[app.packageName])
                } else {
                    app
                }
            }
            isLoadingBadges = false
            val end = System.currentTimeMillis()
            LogUtils.i(TAG, "Loaded badges in ${end - endPackages}ms")
        }
    }

    /**
     * Changes app status from installed to uninstalled or vice versa.
     */
    fun changeAppStatus(packageName: String) {
        apps = apps.map {
            if (it.packageName == packageName) {
                it.copy(isUninstalled = !it.isUninstalled)
            } else {
                it
            }
        }
    }
}
