package io.github.samolego.canta.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.data.PresetStore
import io.github.samolego.canta.util.CantaPreset
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel : ViewModel() {

    companion object {
        private const val TAG = "PresetsViewModel"
    }

    var editingPreset by mutableStateOf<CantaPreset?>(null)

    private lateinit var presetStore: PresetStore

    private val _presets = mutableStateOf<List<CantaPreset>>(emptyList())
    val presets: List<CantaPreset>
        get() = _presets.value

    var isLoading by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        presetStore = PresetStore(context)
        // Collect presets flow and update state
        viewModelScope.launch {
            presetStore.presetsFlow.stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = emptyList()
                    )
                    .collect { presetsList ->
                        _presets.value = presetsList
                        LogUtils.i(TAG, "Loaded ${presetsList.size} presets")
                    }
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
            val preset = presetStore.createPresetFromUninstalledApps(apps, name, description)
            val success = presetStore.savePreset(preset)
            if (success) {
                onSuccess()
            } else {
                onError("Failed to save preset ${preset.name}!")
                LogUtils.e(TAG, "Failed to save preset ${preset.name}!")
            }
        }
    }

    fun deletePreset(config: CantaPreset, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = presetStore.deletePreset(config)
            if (success) {
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
        val jsonString = presetStore.exportToJson(config)
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
            val config = presetStore.importFromJson(jsonString)

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
        val config = presetStore.importFromJson(jsonString)
        if (config != null) {
            onSuccess(config)
        } else {
            onError("Invalid configuration format")
        }
    }

    fun formatDate(timestamp: Long): String {
        return presetStore.formatDate(timestamp)
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

            val success = presetStore.updatePreset(oldPreset, updatedPreset)
            if (success) {
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
            val success = presetStore.setPresetApps(preset, newApps)
            if (success) {
                onSuccess()
            } else {
                onError("Failed to update preset apps")
            }
        }
    }

    fun saveImportedPreset(config: CantaPreset, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = presetStore.savePreset(config)
            if (success) {
                onSuccess()
            } else {
                LogUtils.e(TAG, "Failed to save imported configuration")
                onError("Failed to save imported configuration")
            }
        }
    }
}
