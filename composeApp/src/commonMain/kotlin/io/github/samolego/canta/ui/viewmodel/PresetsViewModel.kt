package io.github.samolego.canta.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.core.PresetRunner
import io.github.samolego.canta.core.describe
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.preset.CantaPresetStore
import io.github.samolego.canta.data.preset.exportPresetToJson
import io.github.samolego.canta.data.preset.importPresetFromJson
import io.github.samolego.canta.data.preset.newPreset
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.auth_required
import io.github.samolego.canta.generated.resources.auth_required_description
import io.github.samolego.canta.generated.resources.files_access_required
import io.github.samolego.canta.generated.resources.preset_run_done
import io.github.samolego.canta.generated.resources.preset_run_done_failures
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class PresetsViewModel(
    private val presetStore: CantaPresetStore,
    private val cantaHandler: CantaHandler,
    private val platform: CantaPlatform,
    private val settings: CantaSettings,
) : ViewModel() {

    companion object {
        private const val TAG = "PresetsViewModel"
    }

    /** The preset whose apps are being edited on the main screen, if any. */
    var editingPreset by mutableStateOf<CantaPresetData?>(null)

    /** Whether a preset run (uninstalls + folder deletions) is in progress. */
    var runningPreset by mutableStateOf(false)
        private set

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

    fun savePreset(
        name: String,
        description: String,
        apps: Set<String>,
        folders: Set<String>,
        removeCantaAfterRun: Boolean,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) = persist("Saving preset '$name'", onSuccess, onError) {
        savePreset(newPreset(name, description, apps).copy(folders = folders, removeCantaAfterRun = removeCantaAfterRun))
    }

    fun deletePreset(preset: CantaPresetData, onSuccess: () -> Unit, onError: () -> Unit) =
        persist("Deleting preset '${preset.name}'", onSuccess, onError) { deletePreset(preset) }

    fun updatePreset(
        oldPreset: CantaPresetData,
        newName: String,
        newDescription: String,
        newFolders: Set<String>,
        newRemoveCantaAfterRun: Boolean,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) = persist("Updating preset '${oldPreset.name}'", onSuccess, onError) {
        updatePreset(
            oldPreset,
            oldPreset.copy(
                name = newName,
                description = newDescription,
                folders = newFolders,
                removeCantaAfterRun = newRemoveCantaAfterRun,
            )
        )
    }

    fun setPresetApps(preset: CantaPresetData, newApps: Set<String>, onSuccess: () -> Unit, onError: () -> Unit) =
        persist("Updating apps of preset '${preset.name}'", onSuccess, onError) { setPresetApps(preset, newApps) }

    /**
     * Runs [preset] immediately — uninstalls its apps and deletes its folders
     * (and Canta itself, when the preset says so). The run happens fully in
     * the background; the "require authentication" setting gates it behind
     * the system credential prompt.
     */
    fun runPreset(preset: CantaPresetData) {
        if (runningPreset) return
        viewModelScope.launch {
            if (settings.authEnabledFlow.first()) {
                platform.requireBiometric(
                    title = getString(Res.string.auth_required),
                    subtitle = getString(Res.string.auth_required_description),
                ) { viewModelScope.launch { executeRun(preset) } }
            } else {
                executeRun(preset)
            }
        }
    }

    private suspend fun executeRun(preset: CantaPresetData) {
        if (preset.folders.isNotEmpty() && platform.needsAllFilesAccess) {
            platform.showMessage(getString(Res.string.files_access_required))
            return
        }
        runningPreset = true
        try {
            val result = PresetRunner(cantaHandler).run(preset)
            platform.showMessage(result.describe())
        } finally {
            runningPreset = false
        }
    }

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
