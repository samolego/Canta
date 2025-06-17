package io.github.samolego.canta.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    var editingPreset by mutableStateOf<CantaPreset?>(null)

    private lateinit var presetsStore: PresetManager

    fun initialize(context: Context) {
        presetsStore = PresetManager(context)
        loadPresets()
    }

    fun loadPresets() {
        viewModelScope.launch {
            isLoading = true
            presets = presetsStore.loadPresets()
            LogUtils.i(TAG, "Loaded ${presets.size} configurations")
            isLoading = false
        }
    }

    fun savePreset(
            name: String,
            description: String,
            apps: Set<String>,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val preset = presetsStore.createPresetFromUninstalledApps(apps, name, description)
            val success = presetsStore.savePreset(preset)
            if (success) {
                loadPresets()
                onSuccess()
            } else {
                onError("Failed to save preset ${preset.name}!")
                LogUtils.e(TAG, "Failed to save preset ${preset.name}!")
            }
        }
    }

    fun deletePreset(config: CantaPreset, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = presetsStore.deletePreset(config)
            if (success) {
                loadPresets()
                onSuccess()
            } else {
                onError("Failed to delete configuration")
            }
        }
    }

    fun exportToClipboard(
            context: Context,
            config: CantaPreset,
    ) {
        val jsonString = presetsStore.exportToJson(config)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Canta Preset", jsonString)
        clipboard.setPrimaryClip(clip)
    }

    fun importFromClipboard(
            context: Context,
            onSuccess: (CantaPreset) -> Unit,
            onError: (String) -> Unit
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val jsonString = clipData.getItemAt(0).text.toString()
            val config = presetsStore.importFromJson(jsonString)

            if (config != null) {
                onSuccess(config)
            } else {
                onError("Invalid configuration format")
            }
        } else {
            onError("No data found in clipboard")
        }
    }

    fun importFromJson(
            jsonString: String,
            onSuccess: (CantaPreset) -> Unit,
            onError: (String) -> Unit
    ) {
        val config = presetsStore.importFromJson(jsonString)
        if (config != null) {
            onSuccess(config)
        } else {
            onError("Invalid configuration format")
        }
    }

    fun formatDate(timestamp: Long): String {
        return presetsStore.formatDate(timestamp)
    }

    fun updatePreset(
            oldPreset: CantaPreset,
            newName: String,
            newDescription: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val updatedPreset =
                    oldPreset.copy(
                            name = newName,
                            description = newDescription,
                            apps = oldPreset.apps
                    )

            val success = presetsStore.updatePreset(oldPreset, updatedPreset)
            if (success) {
                loadPresets()
                onSuccess()
            } else {
                onError("Failed to update configuration")
            }
        }
    }

    fun setPresetApps(
            preset: CantaPreset,
            newApps: Set<String>,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = presetsStore.setPresetApps(preset, newApps)
            if (success) {
                loadPresets()
                onSuccess()
            } else {
                onError("Failed to update preset apps")
            }
        }
    }

    fun saveImportedPreset(config: CantaPreset, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = presetsStore.savePreset(config)
            if (success) {
                loadPresets()
                onSuccess()
            } else {
                LogUtils.e(TAG, "Failed to save imported configuration")
                onError("Failed to save imported configuration")
            }
        }
    }
}
