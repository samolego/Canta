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
data class CantaPreset(
    val name: String,
    val description: String,
    val createdDate: Long,
    val apps: Set<String>,
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
    suspend fun savePreset(config: CantaPreset): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${config.name.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${config.createdDate}.json"
            val file = File(configDir, fileName)
            file.writeText(configurationToJson(config).toString(2))
            LogUtils.i(TAG, "Preset saved: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to save configuration: ${e.message}")
            false
        }
    }

    /**
     * Loads all saved configurations
     */
    suspend fun loadPresets(): List<CantaPreset> = withContext(Dispatchers.IO) {
        try {
            configDir.listFiles { _, name -> name.endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        val json = JSONObject(file.readText())
                        jsonToPreset(json)
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
    suspend fun deletePreset(config: CantaPreset): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${config.name.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${config.createdDate}.json"
            val file = File(configDir, fileName)
            val deleted = file.delete()
            LogUtils.i(TAG, "Preset deleted: $deleted")
            deleted
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to delete configuration: ${e.message}")
            false
        }
    }

    /**
     * Exports configuration to JSON string
     */
    fun exportToJson(config: CantaPreset): String {
        return configurationToJson(config).toString(2)
    }

    /**
     * Imports configuration from JSON string
     */
    fun importFromJson(jsonString: String): CantaPreset? {
        return try {
            val json = JSONObject(jsonString)
            jsonToPreset(json)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to import configuration from JSON: ${e.message}")
            null
        }
    }

    /**
     * Creates a configuration from currently uninstalled apps
     */
    fun createPresetFromUninstalledApps(
        apps: Set<String>,
        name: String,
        description: String
    ): CantaPreset {
        return CantaPreset(
            name = name,
            description = description,
            createdDate = System.currentTimeMillis(),
            apps = apps
        )
    }

    private fun configurationToJson(config: CantaPreset): JSONObject {
        val json = JSONObject()
        json.put("name", config.name)
        json.put("description", config.description)
        json.put("createdDate", config.createdDate)
        json.put("version", config.version)

        val appsArray = JSONArray()
        config.apps.forEach { app ->
            val appJson = JSONObject()
            appJson.put("packageName", app)
            appsArray.put(appJson)
        }
        json.put("apps", appsArray)

        return json
    }

    private fun jsonToPreset(json: JSONObject): CantaPreset {
        val apps = mutableSetOf<String>()
        val appsArray = json.getJSONArray("apps")

        for (i in 0 until appsArray.length()) {
            val appJson = appsArray.getJSONObject(i)
            apps.add(appJson.getString("packageName"))
        }

        return CantaPreset(
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
     * Updates a specific configuration
     */
    suspend fun updatePreset(
        oldConfig: CantaPreset,
        newConfig: CantaPreset
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete old configuration
            deletePreset(oldConfig)
            // Save new configuration
            savePreset(newConfig)
            LogUtils.i(TAG, "Preset updated: ${newConfig.name}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to update configuration: ${e.message}")
            false
        }
    }

    /**
     * Adds apps to an existing configuration
     */
    suspend fun setPresetApps(
        config: CantaPreset,
        newApps: Set<String>
    ) = withContext(Dispatchers.IO) {
        val updatedConfig = config.copy(
            apps = newApps
        )
        updatePreset(config, updatedConfig)
    }

    /**
     * Removes apps from an existing configuration
     */
    suspend fun removeAppsFromPreset(
        config: CantaPreset,
        appsToRemove: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val updatedApps = config.apps.filter { !appsToRemove.contains(it) }.toSet()
            val updatedConfig = config.copy(apps = updatedApps)
            updatePreset(config, updatedConfig)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to remove apps from configuration: ${e.message}")
            false
        }
    }
}
