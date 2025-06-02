package org.samo_lego.canta.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.samo_lego.canta.extension.add
import org.samo_lego.canta.extension.addAll
import org.samo_lego.canta.extension.mutableStateSetOf
import org.samo_lego.canta.util.AppConfiguration
import org.samo_lego.canta.util.AppInfo
import org.samo_lego.canta.util.CantaConfiguration
import org.samo_lego.canta.util.ConfigurationManager
import org.samo_lego.canta.util.LogUtils

class ConfigurationViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "ConfigurationViewModel"
    }
    
    var configurations by mutableStateOf<List<CantaConfiguration>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var selectedAppsForConfig = mutableStateSetOf<String>()
    
    var currentConfigApps by mutableStateOf<List<AppConfiguration>>(emptyList())
        private set
    
    var isAutoSyncEnabled by mutableStateOf(true)
    
    var editingConfiguration by mutableStateOf<CantaConfiguration?>(null)
        private set
    
    var availableAppsForAdding by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    
    private lateinit var configManager: ConfigurationManager
    
    fun initialize(context: Context) {
        configManager = ConfigurationManager(context)
        loadConfigurations()
    }
    
    fun loadConfigurations() {
        viewModelScope.launch {
            isLoading = true
            try {
                configurations = configManager.loadConfigurations()
                LogUtils.i(TAG, "Loaded ${configurations.size} configurations")
            } catch (e: Exception) {
                LogUtils.e(TAG, "Failed to load configurations: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    fun saveConfiguration(
        name: String,
        description: String,
        apps: List<AppInfo>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val config = configManager.createConfigurationFromUninstalledApps(apps, name, description)
                val success = configManager.saveConfiguration(config)
                if (success) {
                    loadConfigurations() // Refresh the list
                    onSuccess()
                } else {
                    onError("Failed to save configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error saving configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }
    
    fun deleteConfiguration(
        config: CantaConfiguration,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.deleteConfiguration(config)
                if (success) {
                    loadConfigurations() // Refresh the list
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
        config: CantaConfiguration,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val jsonString = configManager.exportToJson(config)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Canta Configuration", jsonString)
            clipboard.setPrimaryClip(clip)
            onSuccess()
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error exporting to clipboard: ${e.message}")
            onError(e.message ?: "Unknown error")
        }
    }
    
    fun importFromClipboard(
        context: Context,
        onSuccess: (CantaConfiguration) -> Unit,
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
        onSuccess: (CantaConfiguration) -> Unit,
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
    
    fun loadConfigurationApps(apps: List<AppConfiguration>) {
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
    
    fun getSelectedConfigApps(): List<AppConfiguration> {
        return currentConfigApps.filter { selectedAppsForConfig.contains(it.packageName) }
    }
    
    fun formatDate(timestamp: Long): String {
        return configManager.formatDate(timestamp)
    }
    
    fun enableAutoSync(enabled: Boolean) {
        isAutoSyncEnabled = enabled
    }
    
    fun startEditingConfiguration(config: CantaConfiguration) {
        editingConfiguration = config
        loadConfigurationApps(config.apps)
    }
    
    fun stopEditingConfiguration() {
        editingConfiguration = null
        currentConfigApps = emptyList()
        selectedAppsForConfig.clear()
    }
    
    fun updateConfiguration(
        oldConfig: CantaConfiguration,
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
                
                val success = configManager.updateConfiguration(oldConfig, updatedConfig)
                if (success) {
                    loadConfigurations() // Refresh the list
                    stopEditingConfiguration()
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
    
    fun addAppsToConfiguration(
        config: CantaConfiguration,
        appsToAdd: List<AppConfiguration>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.addAppsToConfiguration(config, appsToAdd)
                if (success) {
                    loadConfigurations() // Refresh the list
                    onSuccess()
                } else {
                    onError("Failed to add apps to configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error adding apps to configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }
    
    fun removeAppsFromConfiguration(
        config: CantaConfiguration,
        appsToRemove: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.removeAppsFromConfiguration(config, appsToRemove)
                if (success) {
                    loadConfigurations() // Refresh the list
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
    
    fun syncConfigurationsWithUninstalledApps(
        uninstalledApps: List<AppInfo>,
        onSuccess: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!isAutoSyncEnabled) return
        
        viewModelScope.launch {
            try {
                val updated = configManager.syncConfigurationsWithUninstalledApps(uninstalledApps)
                if (updated) {
                    loadConfigurations() // Refresh the list
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
    
    fun createAppConfigurationFromAppInfo(appInfo: AppInfo): AppConfiguration {
        return AppConfiguration(
            packageName = appInfo.packageName,
            appName = appInfo.name,
            uninstallDate = System.currentTimeMillis(),
            isSystemApp = appInfo.isSystemApp,
            description = appInfo.bloatData?.description
        )
    }
    
    fun saveImportedConfiguration(
        config: CantaConfiguration,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = configManager.saveConfiguration(config)
                if (success) {
                    loadConfigurations() // Refresh the list
                    onSuccess()
                } else {
                    onError("Failed to save imported configuration")
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error saving imported configuration: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }
} 