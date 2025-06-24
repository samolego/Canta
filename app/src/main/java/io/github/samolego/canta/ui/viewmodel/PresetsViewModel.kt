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
import io.github.samolego.canta.util.CantaPresetData
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel : ViewModel() {

    companion object {
        private const val TAG = "PresetsViewModel"
    }

    var editingPreset by mutableStateOf<CantaPresetData?>(null)

    private lateinit var presetStore: PresetStore

    private val _presets = mutableStateOf<List<CantaPresetData>>(emptyList())
    val presets: List<CantaPresetData>
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
            onError: () -> Unit
    ) {
        viewModelScope.launch {
            val preset = presetStore.createPresetFromUninstalledApps(apps, name, description)
            val success = presetStore.savePreset(preset)
            if (success) {
                onSuccess()
            } else {
                onError()
                LogUtils.e(TAG, "Failed to save preset ${preset.name}!")
            }
        }
    }

    fun deletePreset(preset: CantaPresetData, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = presetStore.deletePreset(preset)
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun exportToClipboard(
        context: Context,
        preset: CantaPresetData,
    ) {
        val jsonString = presetStore.exportToJson(preset)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Canta Preset", jsonString)
        clipboard.setPrimaryClip(clip)
    }

    fun importFromClipboard(
        context: Context,
        onSuccess: (CantaPresetData) -> Unit,
        onError: () -> Unit
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val jsonString = clipData.getItemAt(0).text.toString()
            val preset = presetStore.importFromJson(jsonString)

            if (preset != null) {
                onSuccess(preset)
            } else {
                onError()
            }
        } else {
            onError()
        }
    }

    fun importFromJson(
        jsonString: String,
        onSuccess: (CantaPresetData) -> Unit,
        onError: () -> Unit
    ) {
        val preset = presetStore.importFromJson(jsonString)
        if (preset != null) {
            onSuccess(preset)
        } else {
            onError()
        }
    }

    fun formatDate(timestamp: Long): String {
        return presetStore.formatDate(timestamp)
    }

    fun updatePreset(
        oldPreset: CantaPresetData,
        newName: String,
        newDescription: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
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
                onError()
            }
        }
    }

    fun setPresetApps(
        preset: CantaPresetData,
        newApps: Set<String>,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            val success = presetStore.setPresetApps(preset, newApps)
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun saveImportedPreset(preset: CantaPresetData, onError: (() -> Unit)? = null) {
        viewModelScope.launch {
            val success = presetStore.savePreset(preset)
            if (!success) {
                LogUtils.e(TAG, "Failed to save imported preset")
                onError?.invoke()
            }
        }
    }
}
