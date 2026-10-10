package io.github.samolego.canta.ui.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.data.app.Filter
import io.github.samolego.canta.data.bloat.BloatData
import io.github.samolego.canta.data.bloat.BloatRepository
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.canta_description
import io.github.samolego.canta.ui.AppAction
import io.github.samolego.canta.ui.AppsType
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** The app list: loading apps and their bloat info, filtering, selection and (un)installing. */
class AppListViewModel(
    private val cantaHandler: CantaHandler,
    private val bloatRepository: BloatRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "AppListViewModel"
        /** Apply streamed icons after this many arrive... */
        private const val ICON_APPLY_BATCH = 20
        /** ...or after this long, whichever comes first. */
        private const val ICON_APPLY_INTERVAL_MS = 100L
        private const val UAD_CREDIT =
            "Universal Debloater Alliance (https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)"
    }

    private val _apps = mutableStateListOf<AppInfo>()

    /** All apps, regardless of search and filters. */
    val apps: List<AppInfo> get() = _apps

    val selectedApps = mutableStateSetOf<String>()

    var searchQuery by mutableStateOf("")
    var onlySystem by mutableStateOf(true)
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingBadges by mutableStateOf(false)
        private set

    var selectedFilter by mutableStateOf(Filter.any)

    val selectedAppsSorted by derivedStateOf {
        sortedList.filter { it.packageName in selectedApps }
    }

    private val sortedList by derivedStateOf {
        _apps.filter { selectedFilter.shouldShow(it) }
            .sortedWith(compareBy { it.name.lowercase() })
    }

    /** The apps matching the search query and filters. */
    val appList by derivedStateOf {
        sortedList
            .filter {
                it.name.contains(searchQuery, true) ||
                    it.packageName.contains(searchQuery, true)
            }
            .filter { it.isSystemApp || !onlySystem }
    }

    private var loadJob: Job? = null

    /** Icons received so far; kept across reloads, since icons rarely change. */
    private val iconCache = mutableMapOf<String, ImageBitmap>()

    /**
     * (Re)loads all apps, cancelling a load that is still running. The
     * returned job completes once apps, badges and streamed icons are loaded.
     */
    fun loadApps(): Job {
        loadJob?.cancel()
        return viewModelScope.launch { load() }.also { loadJob = it }
    }

    private suspend fun load() = coroutineScope {
        isLoading = true
        isLoadingBadges = true
        try {
            val start = currentTimeMillis()
            val all = cantaHandler.loadApps().distinctBy { it.packageName }
            LogUtils.i(TAG, "Loaded ${all.size} apps (uninstalled=${all.count { it.isUninstalled }}) in ${currentTimeMillis() - start}ms")

            // Show the list right away; a reload keeps the previous badges until the new ones arrive.
            val previous = _apps.associateBy { it.packageName }
            _apps.clear()
            _apps.addAll(
                all.map { app ->
                    app.copy(
                        bloatData = app.bloatData ?: previous[app.packageName]?.bloatData,
                        icon = app.icon ?: iconCache[app.packageName],
                    )
                }
            )
            isLoading = false

            // Icons and badges load in parallel.
            if (all.any { it.icon == null && it.packageName !in iconCache }) launch { streamIcons() }
            val badgesStart = currentTimeMillis()
            applyBloatData(bloatRepository.load())
            LogUtils.i(TAG, "Loaded badges in ${currentTimeMillis() - badgesStart}ms")
        } finally {
            isLoading = false
            isLoadingBadges = false
        }
    }

    private suspend fun applyBloatData(bloat: Map<String, BloatData>) {
        val cantaBloat = BloatData(description = getString(Res.string.canta_description, UAD_CREDIT), badgeInfo = null)
        for (index in _apps.indices) {
            val app = _apps[index]
            val bloatData = if (app.packageName == CANTA_PACKAGE_NAME) cantaBloat else bloat[app.packageName]
            if (bloatData != null && bloatData != app.bloatData) _apps[index] = app.copy(bloatData = bloatData)
        }
    }

    /**
     * Applies icons the handler streams (ADB targets load them lazily through
     * the on-device helper) in small chunks, so the list fills in top-down
     * without recomposing for every single icon.
     */
    private suspend fun streamIcons() {
        val start = currentTimeMillis()
        val chunk = mutableMapOf<String, ImageBitmap>()
        var lastApplied = start
        cantaHandler.loadIcons().collect { (packageName, icon) ->
            iconCache[packageName] = icon
            chunk[packageName] = icon
            if (chunk.size >= ICON_APPLY_BATCH || currentTimeMillis() - lastApplied >= ICON_APPLY_INTERVAL_MS) {
                applyIcons(chunk)
                chunk.clear()
                lastApplied = currentTimeMillis()
            }
        }
        applyIcons(chunk)
        LogUtils.i(TAG, "Loaded ${iconCache.size} icons in ${currentTimeMillis() - start}ms")
    }

    private fun applyIcons(icons: Map<String, ImageBitmap>) {
        if (icons.isEmpty()) return
        for (index in _apps.indices) {
            val app = _apps[index]
            if (app.icon == null) icons[app.packageName]?.let { _apps[index] = app.withIcon(it) }
        }
    }

    fun changeAppEnabledStatus(packageName: String, enabled: Boolean) {
        val index = _apps.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            val current = _apps[index]
            _apps[index] = current.withDisabled(!enabled)
        }
    }

    /**
     * Uninstalls ([AppsType.INSTALLED]) or reinstalls ([AppsType.UNINSTALLED])
     * every selected app, alongside optional disable/enable operations.
     * Returns a map of [AppAction] to the number of apps that succeeded.
     */
    suspend fun applyToSelected(
        type: AppsType,
        resetToFactory: Boolean = false,
        disableApp: Boolean = false,
        enableApp: Boolean = false,
        uninstallApp: Boolean = true,
    ): Map<AppAction, Int> {
        val packageNames = selectedApps.toList()
        val actionCounts = mutableMapOf<AppAction, Int>()

        when (type) {
            AppsType.INSTALLED -> {
                val enableSuccess = mutableSetOf<String>()
                val disableSuccess = mutableSetOf<String>()
                val uninstallSuccess = mutableSetOf<String>()

                if (enableApp) {
                    cantaHandler.enableApps(packageNames).collect { result ->
                        if (result.success) {
                            enableSuccess.add(result.packageName)
                            changeAppEnabledStatus(result.packageName, true)
                        } else {
                            LogUtils.w(TAG, "enable '${result.packageName}' failed: ${result.message} (${result.status})")
                        }
                    }
                }

                if (disableApp) {
                    cantaHandler.disableApps(packageNames).collect { result ->
                        if (result.success) {
                            disableSuccess.add(result.packageName)
                            changeAppEnabledStatus(result.packageName, false)
                        } else {
                            LogUtils.w(TAG, "disable '${result.packageName}' failed: ${result.message} (${result.status})")
                        }
                    }
                }

                if (uninstallApp) {
                    cantaHandler.uninstallApps(packageNames, resetToFactory).collect { result ->
                        if (result.success) {
                            uninstallSuccess.add(result.packageName)
                            toggleUninstalled(result.packageName)
                        } else {
                            LogUtils.w(TAG, "uninstall '${result.packageName}' failed: ${result.message} (${result.status})")
                        }
                    }
                }

                // Track action outcomes independently and clear selection only for successful outcomes
                packageNames.forEach { pkg ->
                    val uninstalled = uninstallSuccess.contains(pkg)
                    val disabled = disableSuccess.contains(pkg)
                    val enabled = enableSuccess.contains(pkg)

                    if (uninstallApp) {
                        if (uninstalled) {
                            selectedApps.remove(pkg)
                            actionCounts[AppAction.UNINSTALL] = (actionCounts[AppAction.UNINSTALL] ?: 0) + 1
                        } else if (disabled) {
                            // Uninstall failed, but disable succeeded
                            actionCounts[AppAction.DISABLE] = (actionCounts[AppAction.DISABLE] ?: 0) + 1
                        }
                    } else if (disabled) {
                        selectedApps.remove(pkg)
                        actionCounts[AppAction.DISABLE] = (actionCounts[AppAction.DISABLE] ?: 0) + 1
                    } else if (enabled) {
                        selectedApps.remove(pkg)
                        actionCounts[AppAction.ENABLE] = (actionCounts[AppAction.ENABLE] ?: 0) + 1
                    }
                }
            }

            AppsType.UNINSTALLED -> {
                cantaHandler.reinstallApps(packageNames).collect { result ->
                    if (result.success) {
                        actionCounts[AppAction.REINSTALL] = (actionCounts[AppAction.REINSTALL] ?: 0) + 1
                        toggleUninstalled(result.packageName)
                        selectedApps.remove(result.packageName)
                    } else {
                        LogUtils.w(TAG, "reinstall '${result.packageName}' failed: ${result.message} (${result.status})")
                    }
                }
            }
        }

        return actionCounts
    }

    private fun toggleUninstalled(packageName: String) {
        val index = _apps.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            val current = _apps[index]
            _apps[index] = current.withUninstalled(!current.isUninstalled)
        }
    }
}
