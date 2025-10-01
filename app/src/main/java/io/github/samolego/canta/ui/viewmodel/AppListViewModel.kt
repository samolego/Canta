package io.github.samolego.canta.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.Collator
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.samolego.canta.R
import io.github.samolego.canta.data.SettingsStore
import io.github.samolego.canta.extension.getAllPackagesInfo
import io.github.samolego.canta.extension.mutableStateSetOf
import io.github.samolego.canta.packageName
import io.github.samolego.canta.util.AppInfo
import io.github.samolego.canta.util.BloatData
import io.github.samolego.canta.util.BloatUtils
import io.github.samolego.canta.util.Filter
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

    val selectedAppsSorted by derivedStateOf {
        sortedList.filter { selectedApps.contains(it.packageName) }
    }

    private val nameComparator = compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::name)
    private val sortedList by derivedStateOf {
        isLoading = true

        apps.filter { selectedFilter.shouldShow(it) }.sortedWith(nameComparator).also {
            isLoading = false
        }
    }

    val appList by derivedStateOf {
        sortedList
                .filter {
                    it.name.contains(searchQuery, true) ||
                            it.packageName.contains(searchQuery, true)
                }
                .filter { it.isSystemApp || !showSystem }
    }

    suspend fun loadInstalled(packageManager: PackageManager, context: Context) {
        isLoading = true
        val filesDir = context.filesDir

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
            // ideally it should be injected using DI but for now here SettingsStore.getInstance()
            // is used as it provides the singleton instance of SettingsStore
            // earlier it was creating a new instance of SettingsStore every time loadInstalled was
            // called
            // which is a bad practice as there should only be one instance of the preferences in
            // app
            val settingsStore = SettingsStore.getInstance()
            val autoUpdate = settingsStore.autoUpdateBloatListFlow.first()

            val uadLists: JSONObject =
                    try {
                        if (!uadList.exists() ||
                                (autoUpdate &&
                                    bloatFetcher.checkForUpdates(settingsStore.getLatestCommitHash())
                                )
                        ) {
                            uadList.createNewFile()
                            val (json, hash) = bloatFetcher.fetchBloatList(uadList)
                            // Write the hash to settings
                            if (json.length() > 0 && hash.isNotEmpty()) {
                                // in the case of exception the fetchBloatList stills -
                                // returns the *empty json and empty hash
                                // it should only store the hash when that's not empty.
                                settingsStore.setLatestCommitHash(hash)
                            }
                            json
                        } else {
                            // Just read the file
                            val fileContent = uadList.readText()
                            if (fileContent.isBlank()) {
                                LogUtils.e(TAG, "Local uad_lists.json is blank. Retrying fetch.")
                                val (json, hash) =
                                        bloatFetcher.fetchBloatList(uadList) // Retry fetch
                                if (json.length() > 0 && hash.isNotEmpty()) {
                                    settingsStore.setLatestCommitHash(hash)
                                }
                                json
                            } else {
                                // reading the file
                                JSONObject(fileContent)
                            }
                        }
                    } catch (e: Exception) {
                        LogUtils.e(TAG, "Exception while reading uad_lists.json .", e)
                        JSONObject()
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
            apps =
                    apps.map { app ->
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

    /** Changes app status from installed to uninstalled or vice versa. */
    fun changeAppStatus(packageName: String) {
        apps =
                apps.map {
                    if (it.packageName == packageName) {
                        it.copy(isUninstalled = !it.isUninstalled)
                    } else {
                        it
                    }
                }
    }
}

private fun cantaBloatData(context: Context): BloatData {
    return BloatData(
            installData = null,
            description =
                    context.getString(
                            R.string.canta_description,
                            "Universal Debloater Alliance (https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)"
                    ),
            removal = null,
    )
}
