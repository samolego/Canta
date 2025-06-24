package io.github.samolego.canta.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import io.github.samolego.canta.data.proto.CantaPreset
import io.github.samolego.canta.data.proto.PresetsList
import io.github.samolego.canta.util.CantaPresetData
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object PresetsListSerializer : Serializer<PresetsList> {
    override val defaultValue: PresetsList = PresetsList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PresetsList {
        try {
            return PresetsList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read files.", exception)
        }
    }

    override suspend fun writeTo(t: PresetsList, output: OutputStream) = t.writeTo(output)
}

private val Context.presetDataStore: DataStore<PresetsList> by
        dataStore(fileName = "presets.pb", serializer = PresetsListSerializer)

class PresetStore(private val context: Context) {

    companion object {
        private const val TAG = "PresetStore"
    }

    val presetsFlow: Flow<List<CantaPresetData>> =
            context.presetDataStore.data
                    .catch { exception ->
                        if (exception is IOException) {
                            LogUtils.e(TAG, "Error reading presets.", exception)
                            emit(PresetsList.getDefaultInstance())
                        } else {
                            throw exception
                        }
                    }
                    .map { presetsList ->
                        presetsList.presetsList.map { protoPreset ->
                            CantaPresetData(
                                    name = protoPreset.name,
                                    description = protoPreset.description,
                                    createdDate = protoPreset.createdDate,
                                    apps = protoPreset.appsList.toSet(),
                                    version = protoPreset.version.ifEmpty { "1.0" }
                            )
                        }
                    }

    suspend fun savePreset(preset: CantaPresetData): Boolean {
        return try {
            context.presetDataStore.updateData { currentPresets ->
                val protoPreset =
                        CantaPreset.newBuilder()
                                .setName(preset.name)
                                .setDescription(preset.description)
                                .setCreatedDate(preset.createdDate)
                                .addAllApps(preset.apps)
                                .setVersion(preset.version)
                                .build()

                currentPresets.toBuilder().addPresets(protoPreset).build()
            }
            LogUtils.i(TAG, "Preset saved: ${preset.name}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to save preset: ${e.message}")
            false
        }
    }

    suspend fun deletePreset(preset: CantaPresetData): Boolean {
        return try {
            context.presetDataStore.updateData { currentPresets ->
                currentPresets
                        .toBuilder()
                        .clearPresets()
                        .addAllPresets(
                                currentPresets.presetsList.filter { protoPreset ->
                                    !(protoPreset.name == preset.name &&
                                            protoPreset.createdDate == preset.createdDate)
                                }
                        )
                        .build()
            }
            LogUtils.i(TAG, "Preset deleted: ${preset.name}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to delete preset: ${e.message}")
            false
        }
    }

    suspend fun updatePreset(
        oldPreset: CantaPresetData,
        newPreset: CantaPresetData
    ): Boolean {
        return try {
            context.presetDataStore.updateData { currentPresets ->
                val updatedPresets =
                        currentPresets.presetsList.map { protoPreset ->
                            if (protoPreset.name == oldPreset.name &&
                                            protoPreset.createdDate == oldPreset.createdDate
                            ) {
                                CantaPreset.newBuilder()
                                        .setName(newPreset.name)
                                        .setDescription(newPreset.description)
                                        .setCreatedDate(newPreset.createdDate)
                                        .addAllApps(newPreset.apps)
                                        .setVersion(newPreset.version)
                                        .build()
                            } else {
                                protoPreset
                            }
                        }

                currentPresets.toBuilder().clearPresets().addAllPresets(updatedPresets).build()
            }
            LogUtils.i(TAG, "Preset updated: ${newPreset.name}")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to update preset: ${e.message}")
            false
        }
    }

    suspend fun setPresetApps(
        preset: CantaPresetData,
        newApps: Set<String>
    ): Boolean {
        val updatedPreset = preset.copy(apps = newApps)
        return updatePreset(preset, updatedPreset)
    }

    fun exportToJson(preset: CantaPresetData): String {
        // Keep the same JSON format for compatibility with import/export
        val jsonObject =
                JSONObject().apply {
                    put("name", preset.name)
                    put("description", preset.description)
                    put("createdDate", preset.createdDate)
                    put("version", preset.version)

                    val appsArray = org.json.JSONArray()
                    preset.apps.forEach { app ->
                        val appJson = JSONObject()
                        appJson.put("packageName", app)
                        appsArray.put(appJson)
                    }
                    put("apps", appsArray)
                }
        return jsonObject.toString(2)
    }

    fun importFromJson(jsonString: String): CantaPresetData? {
        return try {
            val json = JSONObject(jsonString)
            val apps = mutableSetOf<String>()
            val appsArray = json.getJSONArray("apps")

            for (i in 0 until appsArray.length()) {
                val appJson = appsArray.getJSONObject(i)
                apps.add(appJson.getString("packageName"))
            }

            CantaPresetData(
                    name = json.getString("name"),
                    description = json.getString("description"),
                    createdDate = json.getLong("createdDate"),
                    apps = apps,
                    version = json.optString("version", "1.0")
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to import preset from JSON: ${e.message}")
            null
        }
    }

    fun createPresetFromUninstalledApps(
            apps: Set<String>,
            name: String,
            description: String
    ): CantaPresetData {
        return CantaPresetData(
                name = name,
                description = description,
                createdDate = System.currentTimeMillis(),
                apps = apps
        )
    }

    fun formatDate(timestamp: Long): String {
        val formatter =
                java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestamp))
    }
}
