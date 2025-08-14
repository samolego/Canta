package io.github.samolego.canta.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.samolego.canta.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    // here we are injecting preferences dependencies directly into the viewModel
    // now no need to manually inject the preferences every time in viewModel.
    private val settingsStore: SettingsStore,
    // savedStateHandle to (trying to) preserve the riskDialog visibility state across -
    // configuration changes and system initiated process death
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _autoUpdateBloatList = MutableStateFlow<Boolean>(true)
    val autoUpdateBloatList = _autoUpdateBloatList.asStateFlow()

    private val _confirmBeforeUninstall = MutableStateFlow<Boolean>(true)
    val confirmBeforeUninstall = _confirmBeforeUninstall.asStateFlow()

    private val _disableRiskDialog = MutableStateFlow<Boolean>(true)
    val disableRiskDialog = _disableRiskDialog.asStateFlow()

    private var _latestCommitHash = MutableStateFlow<String>("")
    val latestCommitHashFlow = _latestCommitHash.asStateFlow()


    init {
        // here this will automatically starts observing and collecting values
        // upon viewModel initialization from the preferences
        // so no need to manually check the state
        // also trying to maintain single source of truth

        // you could also use something like this in other parts of the code for easy management.
        observeSettings()
        observeLatestCommitHash()
        observeAutoUpdateBloatList()
        observeConfirmBeforeUninstall()
    }

    private fun observeSettings() {
        settingsStore.disableRiskDialogFlow
            .onEach { neverShowRiskDialog ->
                if (!neverShowRiskDialog) {
                    val riskDialogSavedState =
                        savedStateHandle.get<Boolean>(DISABLE_RISK_DIALOG_KEY) ?: false
                    if (!riskDialogSavedState)
                        _disableRiskDialog.value = false
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeAutoUpdateBloatList() {
        settingsStore.autoUpdateBloatListFlow
            .onEach {
                // preferred way to update the value is using .update { it } as it is thread safe
                // but
                // this is also correct/acceptable using .value = it
                _autoUpdateBloatList.value = it
            }
            .launchIn(viewModelScope)
    }

    private fun observeConfirmBeforeUninstall() {
        settingsStore.confirmBeforeUninstallFlow
            .onEach {
                _confirmBeforeUninstall.value = it
            }
            .launchIn(viewModelScope)
    }

    private fun observeLatestCommitHash() {
        settingsStore.latestCommitHashFlow
            .onEach {
                _latestCommitHash.value = it
            }
            .launchIn(viewModelScope)
    }

    fun saveAutoUpdateBloatList(autoupdate: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoUpdateBloatList(autoupdate)
        }
    }

    fun saveConfirmBeforeUninstall(confirmBeforeUninstall: Boolean) {
        viewModelScope.launch {
            settingsStore.setConfirmBeforeUninstall(confirmBeforeUninstall)
        }
    }

    fun saveLatestCommitHash(latestCommitHash: String) {
        viewModelScope.launch {
            settingsStore.setLatestCommitHash(latestCommitHash)
        }
    }

    fun saveDisableRiskDialog(permanentlyHide: Boolean) {
        viewModelScope.launch {
            if (permanentlyHide) {
                settingsStore.setDisableRiskDialog(true)
            }
            _disableRiskDialog.value = true
            savedStateHandle[DISABLE_RISK_DIALOG_KEY] = true
        }
    }

    private companion object {
        // this is a key to store the value in the savedStateHandle
        const val DISABLE_RISK_DIALOG_KEY = "disable_risk_dialog"
    }
}

// Factory to create SettingsViewModel with required dependencies using manual dependency injection
// as this app opt no to choose any other DI framework so i tried manual DI
class SettingsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            val settingsStore = SettingsStore.getInstance()
            return SettingsViewModel(settingsStore, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
