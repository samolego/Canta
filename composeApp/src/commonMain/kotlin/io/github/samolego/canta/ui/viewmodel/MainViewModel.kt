package io.github.samolego.canta.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.core.CantaDevice
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.core.DeviceDiscovery
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.auth_required
import io.github.samolego.canta.generated.resources.auth_required_description
import io.github.samolego.canta.generated.resources.cannot_uninstall_canta
import io.github.samolego.canta.generated.resources.device_unreachable
import io.github.samolego.canta.ui.AppAction
import io.github.samolego.canta.ui.AppsType
import io.github.samolego.canta.ui.dialog.message
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** The dialog the main screen is showing; at most one at a time. */
sealed interface MainDialog {
    data class ConfirmUninstall(
        val appCount: Int,
        val canResetToFactory: Boolean,
        val hasDisabledApp: Boolean = false,
        val hasEnabledApp: Boolean = true,
    ) : MainDialog
    data class Success(val count: Int, val action: AppAction = AppAction.UNINSTALL) : MainDialog
    data object ShizukuSetup : MainDialog
    data object ExplainBadges : MainDialog
    data class DeviceChooser(val devices: List<CantaDevice> = emptyList(), val isLoading: Boolean = false) : MainDialog
}

/**
 * Drives the main screen's flows: connecting to a device and the
 * authorize → confirm → authenticate → uninstall/reinstall sequence, exposing
 * the current step as [dialog].
 */
class MainViewModel(
    private val handler: CantaHandler,
    private val platform: CantaPlatform,
    private val settings: CantaSettings,
    private val appList: AppListViewModel,
) : ViewModel() {

    var dialog by mutableStateOf<MainDialog?>(null)
        private set

    /** What to do once the pending connection/authorization succeeds. */
    private var afterConnect: (suspend () -> Unit)? = null

    init {
        if (handler.requiresDeviceSelection && !handler.isAuthorized) {
            openDeviceChooser { appList.loadApps() }
        } else {
            appList.loadApps()
        }
    }

    fun dismissDialog() {
        dialog = null
        afterConnect = null
    }

    fun showBadgesExplanation() {
        dialog = MainDialog.ExplainBadges
    }

    /** The FAB: uninstall ([AppsType.INSTALLED]) or reinstall the selected apps. */
    fun applyToSelected(type: AppsType) {
        viewModelScope.launch {
            if (CANTA_PACKAGE_NAME in appList.selectedApps) {
                platform.showMessage(getString(Res.string.cannot_uninstall_canta))
                return@launch
            }
            whenAuthorized { confirm(type) }
        }
    }

    private suspend fun whenAuthorized(action: suspend () -> Unit) {
        when {
            handler.isAuthorized -> action()
            handler.requiresDeviceSelection -> openDeviceChooser(action)
            else -> {
                afterConnect = action
                dialog = MainDialog.ShizukuSetup
            }
        }
    }

    /** [MainDialog.ShizukuSetup] closed; [proceed] requests the Shizuku permission. */
    fun onShizukuSetupClosed(proceed: Boolean) {
        val action = afterConnect
        dismissDialog()
        if (!proceed || action == null) return
        viewModelScope.launch {
            handler.requestAuthorization()
            if (handler.isAuthorized) action() else platform.showMessage(getString(Res.string.device_unreachable))
        }
    }

    private suspend fun confirm(type: AppsType) {
        if (type == AppsType.INSTALLED && settings.confirmBeforeUninstallFlow.first()) {
            val selected = appList.selectedApps.toList()
            if (selected.isEmpty()) return
            val selectedAppsInfo = appList.selectedAppsSorted
            dialog = MainDialog.ConfirmUninstall(
                appCount = selected.size,
                canResetToFactory = selected.any { handler.canResetToFactory(it) },
                hasDisabledApp = selectedAppsInfo.any { it.isDisabled },
                hasEnabledApp = selectedAppsInfo.any { !it.isDisabled },
            )
        } else {
            authenticateAndApply(type, resetToFactory = false)
        }
    }

    fun onUninstallConfirmed(
        resetToFactory: Boolean = false,
        disableApp: Boolean = false,
        enableApp: Boolean = false,
        uninstallApp: Boolean = true,
    ) {
        dialog = null
        viewModelScope.launch {
            authenticateAndApply(
                AppsType.INSTALLED,
                resetToFactory = resetToFactory,
                disableApp = disableApp,
                enableApp = enableApp,
                uninstallApp = uninstallApp
            )
        }
    }

    private suspend fun authenticateAndApply(
        type: AppsType,
        resetToFactory: Boolean,
        disableApp: Boolean = false,
        enableApp: Boolean = false,
        uninstallApp: Boolean = true,
    ) {
        if (settings.authEnabledFlow.first()) {
            platform.requireBiometric(
                title = getString(Res.string.auth_required),
                subtitle = getString(Res.string.auth_required_description),
            ) {
                viewModelScope.launch {
                    apply(
                        type,
                        resetToFactory = resetToFactory,
                        disableApp = disableApp,
                        enableApp = enableApp,
                        uninstallApp = uninstallApp
                    )
                }
            }
        } else {
            apply(
                type,
                resetToFactory = resetToFactory,
                disableApp = disableApp,
                enableApp = enableApp,
                uninstallApp = uninstallApp
            )
        }
    }

    private suspend fun apply(
        type: AppsType,
        resetToFactory: Boolean,
        disableApp: Boolean = false,
        enableApp: Boolean = false,
        uninstallApp: Boolean = true,
    ) {
        val actionCounts = appList.applyToSelected(
            type,
            resetToFactory = resetToFactory,
            disableApp = disableApp,
            enableApp = enableApp,
            uninstallApp = uninstallApp
        )
        if (actionCounts.isNotEmpty() && !settings.hideSuccessDialogFlow.first()) {
            val primaryAction = when {
                type == AppsType.UNINSTALLED -> AppAction.REINSTALL
                actionCounts.containsKey(AppAction.UNINSTALL) -> AppAction.UNINSTALL
                actionCounts.containsKey(AppAction.DISABLE) -> AppAction.DISABLE
                actionCounts.containsKey(AppAction.ENABLE) -> AppAction.ENABLE
                else -> actionCounts.keys.first()
            }
            val count = actionCounts[primaryAction] ?: 0
            if (count > 0) {
                dialog = MainDialog.Success(count = count, action = primaryAction)
            }
        }
    }

    // Device selection (desktop, web).

    private fun openDeviceChooser(then: suspend () -> Unit) {
        afterConnect = then
        dialog = MainDialog.DeviceChooser()
        if (handler.deviceDiscovery == DeviceDiscovery.List) refreshDevices()
    }

    fun refreshDevices() {
        viewModelScope.launch {
            updateChooser { copy(isLoading = true) }
            val devices = handler.availableDevices()
            updateChooser { copy(devices = devices, isLoading = false) }
        }
    }

    fun selectDevice(device: CantaDevice) = connect {
        handler.selectDevice(device)
        handler.isAuthorized
    }

    fun pickSystemDevice() = connect { handler.pickSystemDevice() != null }

    private fun connect(attempt: suspend () -> Boolean) {
        viewModelScope.launch {
            updateChooser { copy(isLoading = true) }
            if (attempt()) {
                val action = afterConnect
                dismissDialog()
                action?.invoke()
            } else {
                updateChooser { copy(isLoading = false) }
                handler.lastConnectionFailure?.let { platform.showMessage(it.message()) }
                if (handler.deviceDiscovery == DeviceDiscovery.List) refreshDevices()
            }
        }
    }

    private fun updateChooser(update: MainDialog.DeviceChooser.() -> MainDialog.DeviceChooser) {
        (dialog as? MainDialog.DeviceChooser)?.let { dialog = it.update() }
    }
}
