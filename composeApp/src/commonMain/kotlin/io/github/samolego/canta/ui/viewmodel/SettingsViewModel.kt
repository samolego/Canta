package io.github.samolego.canta.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.util.PackageTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bump to show the onboarding again to everyone (existing users have 0). */
const val CURRENT_ONBOARDING_VERSION = 1

/**
 * Exposes [CantaSettings] as [StateFlow]s for the UI. Every value is derived
 * from the store, so the UI can never drift from what is persisted.
 */
class SettingsViewModel(
    private val settingsStore: CantaSettings,
    private val packageTelemetry: PackageTelemetry? = null,
) : ViewModel() {

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    private fun save(block: suspend CantaSettings.() -> Unit) {
        viewModelScope.launch { settingsStore.block() }
    }

    val autoUpdateBloatList = settingsStore.autoUpdateBloatListFlow.state(true)
    val confirmBeforeUninstall = settingsStore.confirmBeforeUninstallFlow.state(true)
    val bloatListUrl = settingsStore.bloatListUrlFlow.state("")
    val commitsUrl = settingsStore.commitsUrlFlow.state("")
    val allowUnsafeUninstalls = settingsStore.allowUnsafeUninstallsFlow.state(false)
    val hideSuccessDialog = settingsStore.hideSuccessDialogFlow.state(false)
    val authEnabled = settingsStore.authEnabledFlow.state(false)

    val analyticsEnabled = settingsStore.analyticsEnabledFlow.state(false)

    /** Null until the stored settings have loaded, so the UI doesn't flash the wrong screen. */
    val onboardingVersion: StateFlow<Int?> = settingsStore.onboardingVersionFlow.state<Int?>(null)

    /** Records the onboarding choice; [contribute] is the analytics opt-in. */
    fun completeOnboarding(contribute: Boolean) = save {
        setAnalyticsEnabled(contribute)
        setOnboardingVersion(CURRENT_ONBOARDING_VERSION)
    }

    fun setAnalyticsEnabled(enabled: Boolean) = save {
        setAnalyticsEnabled(enabled)
        if (!enabled) packageTelemetry?.clearPending() ?: setPendingReboot(emptyList())
    }

    fun saveAutoUpdateBloatList(autoUpdate: Boolean) = save { setAutoUpdateBloatList(autoUpdate) }
    fun saveConfirmBeforeUninstall(confirm: Boolean) = save { setConfirmBeforeUninstall(confirm) }
    fun saveBloatListUrl(url: String) = save { setBloatListUrl(url) }
    fun saveCommitsUrl(url: String) = save { setCommitsUrl(url) }
    fun saveAllowUnsafeUninstalls(allow: Boolean) = save { setAllowUnsafeUninstalls(allow) }
    fun saveHideSuccessDialog(hide: Boolean) = save { setHideSuccessDialog(hide) }
    fun saveAuthEnabled(enabled: Boolean) = save { setAuthEnabled(enabled) }

}
