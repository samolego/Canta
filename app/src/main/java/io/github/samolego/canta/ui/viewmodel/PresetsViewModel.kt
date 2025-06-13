package io.github.samolego.canta.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.extension.add
import io.github.samolego.canta.extension.addAll
import io.github.samolego.canta.extension.mutableStateSetOf
import io.github.samolego.canta.util.AppInfo
import io.github.samolego.canta.util.AppPresetEntry
import io.github.samolego.canta.util.CantaPreset
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.PresetManager
import kotlinx.coroutines.launch

class PresetsViewModel : ViewModel() {

    companion object {
        private const val TAG = "PresetsViewModel"
    }

    var presets by mutableStateOf<List<CantaPreset>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var selectedAppsForConfig = mutableStateSetOf<String>()

    var currentConfigApps by mutableStateOf<List<AppPresetEntry>>(emptyList())
        private set

    var isAutoSyncEnabled by mutableStateOf(true)

    var editingPreset by mutableStateOf<CantaPreset?>(null)
        private set

    var availableAppsForAdding by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    private lateinit var configManager: PresetManager

    fun initialize(context: Context) {
        configManager = PresetManager(context)
        loadPresets()
    }

    fun loadPresets() {
        viewModelScope.launch {
            isLoading = true
            try {
                presets = configManager.loadPresets()
                LogUtils.i(TAG, "Loaded ${presets.size} configurations")
            } catch (e: Exception) {
                LogUtils.e(TAG, "Failed to load configurations: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun savePreset(
        name: String,
        description: String,
        apps: List<AppInfo>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val preset = configManager.createPresetFromUninstalledApps(apps, name, description)
                val success = configManager.savePreset(preset)
                if (success) {
                    loadPresets() // Refresh the list
                    onSuccess()
                } else {
                    onError("Failed to save preset ${preset.name}!")
                    LogUtils.e(TAG, "Failed to save preset ${preset.name}!")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error saving configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun deletePreset(
        config: CantaPreset,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.deletePreset(config)
                if (success) {
                    loadPresets() // Refresh the list
                    onSuccess()
                } else {
                    onError("Failed to delete configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error deleting configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun exportToClipboard(
        context: Context,
        config: CantaPreset,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val jsonString = configManager.exportToJson(config)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Canta Preset", jsonString)
            clipboard.setPrimaryClip(clip)
            onSuccess()
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error exporting to clipboard: ${e.message}")
            onError(e.message ?: "Unknown error")
        }
    }

    fun importFromClipboard(
        context: Context,
        onSuccess: (CantaPreset) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip

            if (clipData != null && clipData.itemCount > 0) {
                val jsonString = clipData.getItemAt(0).text.toString()
                val config = configManager.importFromJson(jsonString)

                if (config != null) {
                    onSuccess(config)
                } else {
                    onError("Invalid configuration format")
                }
            } else {
                onError("No data found in clipboard")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error importing from clipboard: ${e.message}")
            onError(e.message ?: "Unknown error")
        }
    }

    fun importFromJson(
        jsonString: String,
        onSuccess: (CantaPreset) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val config = configManager.importFromJson(jsonString)
            if (config != null) {
                onSuccess(config)
            } else {
                onError("Invalid configuration format")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error importing from JSON: ${e.message}")
            onError(e.message ?: "Unknown error")
        }
    }

    fun loadPresetApps(apps: List<AppPresetEntry>) {
        currentConfigApps = apps
        selectedAppsForConfig.clear()
        selectedAppsForConfig.addAll(apps.map { it.packageName })
    }

    fun toggleAppSelection(packageName: String) {
        if (selectedAppsForConfig.contains(packageName)) {
            selectedAppsForConfig.remove(packageName)
        } else {
            selectedAppsForConfig.add(packageName)
        }
    }

    fun selectAllApps() {
        selectedAppsForConfig.clear()
        selectedAppsForConfig.addAll(currentConfigApps.map { it.packageName })
    }

    fun deselectAllApps() {
        selectedAppsForConfig.clear()
    }

    fun getSelectedConfigApps(): List<AppPresetEntry> {
        return currentConfigApps.filter { selectedAppsForConfig.contains(it.packageName) }
    }

    fun formatDate(timestamp: Long): String {
        return configManager.formatDate(timestamp)
    }

    fun enableAutoSync(enabled: Boolean) {
        isAutoSyncEnabled = enabled
    }

    fun startEditingPreset(config: CantaPreset) {
        editingPreset = config
        loadPresetApps(config.apps)
    }

    fun stopEditingPreset() {
        editingPreset = null
        currentConfigApps = emptyList()
        selectedAppsForConfig.clear()
    }

    fun updatePreset(
        oldConfig: CantaPreset,
        newName: String,
        newDescription: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val selectedApps = getSelectedConfigApps()
                val updatedConfig = oldConfig.copy(
                    name = newName,
                    description = newDescription,
                    apps = selectedApps
                )

                val success = configManager.updatePreset(oldConfig, updatedConfig)
                if (success) {
                    loadPresets() // Refresh the list
                    stopEditingPreset()
                    onSuccess()
                } else {
                    onError("Failed to update configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error updating configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun addAppsToPreset(
        config: CantaPreset,
        appsToAdd: List<AppPresetEntry>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.addAppsToPreset(config, appsToAdd)
                if (success) {
                    loadPresets() // Refresh the list
                    onSuccess()
                } else {
                    LogUtils.e(TAG, "Failed to add apps to configuration")
                    onError("Failed to add apps to configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error adding apps to configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun removeAppsFromPreset(
        config: CantaPreset,
        appsToRemove: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.removeAppsFromPreset(config, appsToRemove)
                if (success) {
                    loadPresets() // Refresh the list
                    onSuccess()
                } else {
                    onError("Failed to remove apps from configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error removing apps from configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun syncPresetsWithUninstalledApps(
        uninstalledApps: List<AppInfo>,
        onSuccess: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!isAutoSyncEnabled) return

        viewModelScope.launch {
            try {
                val updated = configManager.syncPresetsWithUninstalledApps(uninstalledApps)
                if (updated) {
                    loadPresets() // Refresh the list
                }
                onSuccess(updated)
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error syncing configurations: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun loadAvailableAppsForAdding(allApps: List<AppInfo>) {
        availableAppsForAdding = allApps.filter { app ->
            // Show installed apps that are not already in the current configuration
            !app.isUninstalled && !currentConfigApps.any { it.packageName == app.packageName }
        }
    }

    fun createAppPresetFromAppInfo(appInfo: AppInfo): AppPresetEntry {
        return AppPresetEntry(
            packageName = appInfo.packageName,
            appName = appInfo.name,
            uninstallDate = System.currentTimeMillis(),
            isSystemApp = appInfo.isSystemApp,
            description = appInfo.bloatData?.description
        )
    }

    fun saveImportedPreset(
        config: CantaPreset,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.savePreset(config)
                if (success) {
                    loadPresets() // Refresh the list
                    onSuccess()
                } else {
                    LogUtils.e(TAG, "Failed to save imported configuration")
                    onError("Failed to save imported configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error saving imported configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }
}