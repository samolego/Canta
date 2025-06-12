package io.github.samolego.canta.util

import android.content.Context
import android.os.Parcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data class AppConfiguration(
    val packageName: String,
    val appName: String,
    val uninstallDate: Long,
    val isSystemApp: Boolean,
    val description: String? = null
) : Parcelable

@Parcelize
data class CantaConfiguration(
    val name: String,
    val description: String,
    val createdDate: Long,
    val apps: List<AppConfiguration>,
    val version: String = "1.0"
) : Parcelable

class PresetManager(private val context: Context) {

    companion object {
        private const val PRESETS_DIR = "presets"
        private const val TAG = "PresetManager"
    }

    private val configDir: File by lazy {
        File(context.filesDir, PRESETS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Saves a configuration to local storage
     */
    suspend fun saveConfiguration(config: CantaConfiguration): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${config.name.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${config.createdDate}.json"
            val file = File(configDir, fileName)
            file.writeText(configurationToJson(config).toString(2))
            LogUtils.i(TAG, "Configuration saved: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to save configuration: ${e.message}")
            false
        }
    }

    /**
     * Loads all saved configurations
     */
    suspend fun loadConfigurations(): List<CantaConfiguration> = withContext(Dispatchers.IO) {
        try {
            configDir.listFiles { _, name -> name.endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        val json = JSONObject(file.readText())
                        jsonToConfiguration(json)
                    } catch (e: Exception) {
                        LogUtils.e(TAG, "Failed to parse configuration file ${file.name}: ${e.message}")
                        null
                    }
                } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to load configurations: ${e.message}")
            emptyList()
        }
    }

    /**
     * Deletes a configuration file
     */
    suspend fun deleteConfiguration(config: CantaConfiguration): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${config.name.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${config.createdDate}.json"
            val file = File(configDir, fileName)
            val deleted = file.delete()
            LogUtils.i(TAG, "Configuration deleted: $deleted")
            deleted
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to delete configuration: ${e.message}")
            false
        }
    }

    /**
     * Exports configuration to JSON string
     */
    fun exportToJson(config: CantaConfiguration): String {
        return configurationToJson(config).toString(2)
    }

    /**
     * Imports configuration from JSON string
     */
    fun importFromJson(jsonString: String): CantaConfiguration? {
        return try {
            val json = JSONObject(jsonString)
            jsonToConfiguration(json)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to import configuration from JSON: ${e.message}")
            null
        }
    }

    /**
     * Creates a configuration from currently uninstalled apps
     */
    fun createConfigurationFromUninstalledApps(
        apps: List<AppInfo>,
        name: String,
        description: String
    ): CantaConfiguration {
        val uninstalledApps = apps.filter { it.isUninstalled }
            .map { app ->
                AppConfiguration(
                    packageName = app.packageName,
                    appName = app.name,
                    uninstallDate = System.currentTimeMillis(),
                    isSystemApp = app.isSystemApp,
                    description = app.bloatData?.description
                )
            }

        return CantaConfiguration(
            name = name,
            description = description,
            createdDate = System.currentTimeMillis(),
            apps = uninstalledApps
        )
    }

    private fun configurationToJson(config: CantaConfiguration): JSONObject {
        val json = JSONObject()
        json.put("name", config.name)
        json.put("description", config.description)
        json.put("createdDate", config.createdDate)
        json.put("version", config.version)

        val appsArray = JSONArray()
        config.apps.forEach { app ->
            val appJson = JSONObject()
            appJson.put("packageName", app.packageName)
            appJson.put("appName", app.appName)
            appJson.put("uninstallDate", app.uninstallDate)
            appJson.put("isSystemApp", app.isSystemApp)
            app.description?.let { appJson.put("description", it) }
            appsArray.put(appJson)
        }
        json.put("apps", appsArray)

        return json
    }

    private fun jsonToConfiguration(json: JSONObject): CantaConfiguration {
        val apps = mutableListOf<AppConfiguration>()
        val appsArray = json.getJSONArray("apps")

        for (i in 0 until appsArray.length()) {
            val appJson = appsArray.getJSONObject(i)
            apps.add(
                AppConfiguration(
                    packageName = appJson.getString("packageName"),
                    appName = appJson.getString("appName"),
                    uninstallDate = appJson.getLong("uninstallDate"),
                    isSystemApp = appJson.getBoolean("isSystemApp"),
                    description = appJson.optString("description").takeIf { it.isNotEmpty() }
                )
            )
        }

        return CantaConfiguration(
            name = json.getString("name"),
            description = json.getString("description"),
            createdDate = json.getLong("createdDate"),
            apps = apps,
            version = json.optString("version", "1.0")
        )
    }

    /**
     * Formats date for display
     */
    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Updates all configurations by adding newly uninstalled apps
     */
    suspend fun syncConfigurationsWithUninstalledApps(
        uninstalledApps: List<AppInfo>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val configurations = loadConfigurations()
            var updated = false

            configurations.forEach { config ->
                val existingPackages = config.apps.map { it.packageName }.toSet()
                val newUninstalledApps = uninstalledApps.filter { app ->
                    app.isUninstalled && !existingPackages.contains(app.packageName)
                }

                if (newUninstalledApps.isNotEmpty()) {
                    val newAppConfigs = newUninstalledApps.map { app ->
                        AppConfiguration(
                            packageName = app.packageName,
                            appName = app.name,
                            uninstallDate = System.currentTimeMillis(),
                            isSystemApp = app.isSystemApp,
                            description = app.bloatData?.description
                        )
                    }

                    val updatedConfig = config.copy(
                        apps = config.apps + newAppConfigs
                    )

                    saveConfiguration(updatedConfig)
                    updated = true
                    LogUtils.i(TAG, "Auto-synced ${newAppConfigs.size} apps to configuration '${config.name}'")
                }
            }

            updated
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to sync configurations: ${e.message}")
            false
        }
    }

    /**
     * Updates a specific configuration
     */
    suspend fun updateConfiguration(
        oldConfig: CantaConfiguration,
        newConfig: CantaConfiguration
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete old configuration
            deleteConfiguration(oldConfig)
            // Save new configuration
            saveConfiguration(newConfig)
            LogUtils.i(TAG, "Configuration updated: ${newConfig.name}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to update configuration: ${e.message}")
            false
        }
    }

    /**
     * Adds apps to an existing configuration
     */
    suspend fun addAppsToConfiguration(
        config: CantaConfiguration,
        newApps: List<AppConfiguration>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingPackages = config.apps.map { it.packageName }.toSet()
            val uniqueNewApps = newApps.filter { !existingPackages.contains(it.packageName) }

            if (uniqueNewApps.isNotEmpty()) {
                val updatedConfig = config.copy(
                    apps = config.apps + uniqueNewApps
                )
                updateConfiguration(config, updatedConfig)
            } else {
                true // No new apps to add
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to add apps to configuration: ${e.message}")
            false
        }
    }

    /**
     * Removes apps from an existing configuration
     */
    suspend fun removeAppsFromConfiguration(
        config: CantaConfiguration,
        appsToRemove: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updatedApps = config.apps.filter { !appsToRemove.contains(it.packageName) }
            val updatedConfig = config.copy(apps = updatedApps)
            updateConfiguration(config, updatedConfig)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to remove apps from configuration: ${e.message}")
            false
        }
    }
}
