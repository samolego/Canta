package io.github.samolego.canta.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.data.preset.CantaPresetStore
import io.github.samolego.canta.data.preset.exportPresetToJson
import io.github.samolego.canta.data.preset.importPresetFromJson
import io.github.samolego.canta.data.preset.newPreset
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel(
    private val presetStore: CantaPresetStore,
    private val cantaHandler: CantaHandler,
    private val platform: CantaPlatform
) : ViewModel() {

    companion object {
        private const val TAG = "PresetsViewModel"
    }

    /** The preset whose apps are being edited on the main screen, if any. */
    var editingPreset by mutableStateOf<CantaPresetData?>(null)

    val presets: StateFlow<List<CantaPresetData>> =
        presetStore.presetsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Runs a store [operation], then reports its outcome to [onSuccess] / [onError]. */
    private fun persist(
        label: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        operation: suspend CantaPresetStore.() -> Boolean,
    ) {
        viewModelScope.launch {
            if (presetStore.operation()) {
                onSuccess()
            } else {
                LogUtils.e(TAG, "$label failed")
                onError()
            }
        }
    }

    fun savePreset(name: String, description: String, apps: Set<String>, onSuccess: () -> Unit, onError: () -> Unit) =
        persist("Saving preset '$name'", onSuccess, onError) { savePreset(newPreset(name, description, apps)) }

    fun deletePreset(preset: CantaPresetData, onSuccess: () -> Unit, onError: () -> Unit) =
        persist("Deleting preset '${preset.name}'", onSuccess, onError) { deletePreset(preset) }

    fun updatePreset(
        oldPreset: CantaPresetData,
        newName: String,
        newDescription: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) = persist("Updating preset '${oldPreset.name}'", onSuccess, onError) {
        updatePreset(oldPreset, oldPreset.copy(name = newName, description = newDescription))
    }

    fun setPresetApps(preset: CantaPresetData, newApps: Set<String>, onSuccess: () -> Unit, onError: () -> Unit) =
        persist("Updating apps of preset '${preset.name}'", onSuccess, onError) { setPresetApps(preset, newApps) }

    fun exportToClipboard(preset: CantaPresetData) {
        platform.copyToClipboard(exportPresetToJson(preset))
    }

    fun importFromClipboard(onSuccess: () -> Unit, onError: () -> Unit) =
        importFromJson(platform.readClipboard().orEmpty(), onSuccess, onError)

    /**
     * Imports and saves a shared preset, keeping only apps that exist on this
     * device. [onError] runs if it can't be parsed or saved.
     */
    fun importFromJson(jsonString: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val preset = jsonString.takeIf { it.isNotBlank() }
                ?.let { json -> importPresetFromJson(json) { cantaHandler.packageExists(it) } }
            when {
                preset == null -> onError()
                presetStore.savePreset(preset) -> onSuccess()
                else -> {
                    LogUtils.e(TAG, "Saving imported preset '${preset.name}' failed")
                    onError()
                }
            }
        }
    }
}
