package io.github.samolego.canta.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.data.app.Filter
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.reinstall
import io.github.samolego.canta.generated.resources.uninstall
import io.github.samolego.canta.ui.AppsType
import io.github.samolego.canta.ui.component.AppList
import io.github.samolego.canta.ui.component.CantaTopBar
import io.github.samolego.canta.ui.component.fab.PresetEditFAB
import io.github.samolego.canta.ui.dialog.DeviceChooserDialog
import io.github.samolego.canta.ui.dialog.ExplainBadgesDialog
import io.github.samolego.canta.ui.dialog.NoWarrantyDialog
import io.github.samolego.canta.ui.dialog.ShizukuRequirementDialog
import io.github.samolego.canta.ui.dialog.SuccessDialog
import io.github.samolego.canta.ui.dialog.UninstallAppsDialog
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.MainDialog
import io.github.samolego.canta.ui.viewmodel.MainViewModel
import io.github.samolego.canta.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The app list screen: installed/uninstalled tabs, the action FAB and its dialogs. */
@Composable
fun MainScreen(
    handler: CantaHandler,
    platform: CantaPlatform,
    pagerState: PagerState,
    mainViewModel: MainViewModel,
    appListViewModel: AppListViewModel,
    settingsViewModel: SettingsViewModel,
    presetEditMode: Boolean,
    onPresetEditFinish: () -> Unit,
    navigateToPage: (route: String) -> Unit,
    closeApp: () -> Unit,
    enableSelectAll: Boolean,
) {
    // Pager state is hoisted to CantaNavigation so preset-applying knows the tab.
    val selectedAppsType = AppsType.entries[pagerState.currentPage]

    LaunchedEffect(pagerState) {
        // Filters and selection are per tab: reset them when the user switches
        // tabs. The first emission is skipped: returning from the Presets screen
        // recomposes MainScreen, and an applied preset populates the selection
        // just before that navigation.
        snapshotFlow { pagerState.currentPage }.drop(1).collect {
            appListViewModel.selectedFilter = Filter.any
            appListViewModel.selectedApps.clear()
        }
    }

    Scaffold(
        topBar = {
            CantaTopBar(
                openBadgesInfoDialog = mainViewModel::showBadgesExplanation,
                navigateToPage = navigateToPage,
                appListViewModel = appListViewModel,
            )
        },
        floatingActionButton = {
            MainFab(
                visible = appListViewModel.selectedApps.isNotEmpty(),
                presetEditMode = presetEditMode,
                appsType = selectedAppsType,
                onPresetEditFinish = onPresetEditFinish,
                onClick = { mainViewModel.applyToSelected(selectedAppsType) },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AppsTabs(pagerState)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                PullToRefreshBox(
                    isRefreshing = appListViewModel.isLoading,
                    onRefresh = appListViewModel::loadApps,
                ) {
                    AppList(
                        appType = AppsType.entries[page],
                        appListModel = appListViewModel,
                        settingsViewModel = settingsViewModel,
                        platform = platform,
                        enableSelectAll = enableSelectAll,
                    )
                }
            }
        }
    }

    MainDialogs(handler, platform, mainViewModel, settingsViewModel, closeApp)
}

@Composable
private fun MainFab(
    visible: Boolean,
    presetEditMode: Boolean,
    appsType: AppsType,
    onPresetEditFinish: () -> Unit,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        if (presetEditMode) {
            PresetEditFAB(onPresetEditFinish = onPresetEditFinish)
            return@AnimatedVisibility
        }
        FloatingActionButton(
            containerColor = when (appsType) {
                AppsType.INSTALLED -> MaterialTheme.colorScheme.errorContainer
                AppsType.UNINSTALLED -> MaterialTheme.colorScheme.tertiaryContainer
            },
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
            onClick = onClick,
        ) {
            when (appsType) {
                AppsType.INSTALLED -> Icon(Icons.Default.Delete, stringResource(Res.string.uninstall))
                AppsType.UNINSTALLED -> Icon(Icons.Default.InstallMobile, stringResource(Res.string.reinstall))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsTabs(pagerState: PagerState) {
    val coroutineScope = rememberCoroutineScope()
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        contentColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        AppsType.entries.forEach { tab ->
            Tab(
                selected = pagerState.currentPage == tab.ordinal,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
            )
        }
    }
}

/** Renders [MainViewModel.dialog] (once, outside the pager) and the startup disclaimer. */
@Composable
private fun MainDialogs(
    handler: CantaHandler,
    platform: CantaPlatform,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    closeApp: () -> Unit,
) {
    val showRiskDialog by settingsViewModel.showRiskDialog.collectAsStateWithLifecycle()

    if (showRiskDialog) {
        NoWarrantyDialog(onProceed = settingsViewModel::acceptRiskDialog, onCancel = closeApp)
    }

    when (val dialog = mainViewModel.dialog) {
        null -> {}
        is MainDialog.ConfirmUninstall -> UninstallAppsDialog(
            appCount = dialog.appCount,
            canResetToFactory = dialog.canResetToFactory,
            onDismiss = mainViewModel::dismissDialog,
            onAgree = mainViewModel::onUninstallConfirmed,
        )
        is MainDialog.Success -> SuccessDialog(
            platform = platform,
            count = dialog.count,
            isReinstall = dialog.isReinstall,
            onDismissRequest = mainViewModel::dismissDialog,
        )
        MainDialog.ShizukuSetup -> ShizukuRequirementDialog(
            onClose = mainViewModel::onShizukuSetupClosed,
            handler = handler,
            platform = platform,
        )
        MainDialog.ExplainBadges -> ExplainBadgesDialog(onDismissRequest = mainViewModel::dismissDialog)
        is MainDialog.DeviceChooser -> DeviceChooserDialog(
            devices = dialog.devices,
            isLoading = dialog.isLoading,
            discovery = handler.deviceDiscovery,
            connectionSupported = handler.connectionSupported,
            onRefresh = mainViewModel::refreshDevices,
            onSystemPick = mainViewModel::pickSystemDevice,
            onSelect = mainViewModel::selectDevice,
            onDismiss = mainViewModel::dismissDialog,
        )
    }
}
