package io.github.samolego.canta.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.samolego.canta.R
import io.github.samolego.canta.extension.addAll
import io.github.samolego.canta.extension.showFor
import io.github.samolego.canta.packageName
import io.github.samolego.canta.ui.component.AppIconImage
import io.github.samolego.canta.ui.component.AppList
import io.github.samolego.canta.ui.component.CantaTopBar
import io.github.samolego.canta.ui.component.fab.PresetEditFAB
import io.github.samolego.canta.ui.dialog.ExplainBadgesDialog
import io.github.samolego.canta.ui.dialog.NoWarrantyDialog
import io.github.samolego.canta.ui.dialog.ShizukuRequirementDialog
import io.github.samolego.canta.ui.dialog.UninstallAppsDialog
import io.github.samolego.canta.ui.navigation.Screen
import io.github.samolego.canta.ui.screen.LogsPage
import io.github.samolego.canta.ui.screen.PresetsPage
import io.github.samolego.canta.ui.screen.SettingsScreen
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.ui.viewmodel.SettingsViewModel
import io.github.samolego.canta.ui.viewmodel.SettingsViewModelFactory
import io.github.samolego.canta.util.Filter
import io.github.samolego.canta.util.ShizukuPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val secretTaps = 12

@Composable
fun CantaApp(
    canResetAppToFactory: (String) -> Boolean,
    uninstallApp: (String, Boolean) -> Boolean,
    reinstallApp: (String) -> Boolean,
    closeApp: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val appListViewModel = viewModel<AppListViewModel>()
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory())
    val presetViewModel = viewModel<PresetsViewModel>()
    var versionTapCounter by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainContent(
                canResetAppToFactory = canResetAppToFactory,
                uninstallApp = uninstallApp,
                reinstallApp = reinstallApp,
                navigateToPage = { navController.navigate(it) },
                closeApp = closeApp,
                presetEditMode = presetViewModel.editingPreset != null,
                onPresetEditFinish = {
                    // Save all the selected apps to the preset
                    presetViewModel.editingPreset?.let { preset ->
                        presetViewModel.setPresetApps(
                            preset = preset,
                            newApps = appListViewModel.selectedApps.keys,
                            onSuccess = {
                                appListViewModel.selectedApps.clear()
                                presetViewModel.editingPreset = null
                                navController.navigate(Screen.Presets.route)
                            },
                            onError = {
                                // Show error message to user
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.preset_save_error
                                    ),
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        )
                    }
                },
                enableSelectAll = versionTapCounter >= secretTaps,
                appListViewModel = appListViewModel,
                settingsViewModel = settingsViewModel,
            )
        }
        composable(route = Screen.Logs.route) {
            LogsPage(onNavigateBack = { navController.navigateUp() })
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                settingsViewModel = settingsViewModel,
                onVersionTap = {
                    versionTapCounter += 1
                    coroutineScope.launch {
                        if (versionTapCounter > 6 && versionTapCounter < secretTaps) {
                            // Show quick toast
                            val remainingTaps = secretTaps - versionTapCounter
                            val message =
                                context.getString(R.string.select_all_tip, remainingTaps)
                            val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                            toast.showFor(500)
                        } else if (versionTapCounter >= secretTaps) {
                            // Enable select all functionality with quick toast
                            val toast =
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.select_all_enabled),
                                    Toast.LENGTH_SHORT
                                )
                            toast.showFor(500)
                        }
                    }
                }
            )
        }

        composable(route = Screen.Presets.route) {
            PresetsPage(
                presetViewModel = presetViewModel,
                onNavigateBack = { preset ->
                    preset?.let {
                        appListViewModel.selectedApps.clear()
                        // Select apps in appListViewModel according to preset
                        appListViewModel.selectedApps.addAll(it.apps)
                    }

                    navController.navigateUp()
                },
                appListViewModel = appListViewModel,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    canResetAppToFactory: (String) -> Boolean,
    uninstallApp: (String, Boolean) -> Boolean,
    reinstallApp: (String) -> Boolean,
    onPresetEditFinish: () -> Unit,
    navigateToPage: (route: String) -> Unit,
    closeApp: () -> Unit,
    enableSelectAll: Boolean,
    presetEditMode: Boolean,
    appListViewModel: AppListViewModel,
    settingsViewModel: SettingsViewModel,
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Selected tab
    var selectedAppsType by remember { mutableStateOf(AppsType.INSTALLED) }

    val disableRiskDialog by settingsViewModel.disableRiskDialog.collectAsStateWithLifecycle()
    val confirmBeforeUninstall by settingsViewModel.confirmBeforeUninstall.collectAsStateWithLifecycle()

    // Current active dialog
    var currentDialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    if (!disableRiskDialog) {
        NoWarrantyDialog(
            onProceed = { neverShowAgain ->
                settingsViewModel.saveDisableRiskDialog(neverShowAgain)
            },
            onCancel = { closeApp() }
        )
    }

    val pagerState = rememberPagerState(pageCount = { AppsType.entries.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedAppsType = AppsType.entries[page]
            appListViewModel.selectedFilter = Filter.any
        }
    }

    val cantaIcon = remember(context) { context.packageManager.getApplicationIcon(packageName) }

    var showExplainBadgeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CantaTopBar(
                openBadgesInfoDialog = { showExplainBadgeDialog = true },
                navigateToPage = navigateToPage,
                appListViewModel = appListViewModel,
            )

            if (showExplainBadgeDialog) {
                ExplainBadgesDialog(onDismissRequest = { showExplainBadgeDialog = false })
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                // Make the FAB hidden if no apps are selected
                visible = appListViewModel.selectedApps.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                if (presetEditMode) {
                    PresetEditFAB(
                        onPresetEditFinish = onPresetEditFinish,
                    )
                } else {
                    FloatingActionButton(
                        containerColor =
                        if (selectedAppsType == AppsType.UNINSTALLED) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {

                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        onClick = {
                            // Check if Canta is selected too
                            // Super secret don't tell anyone you saw this
                            // since this is an easter egg :P
                            if (appListViewModel.selectedApps.contains(packageName)) {
                                // Show easter egg toast
                                Toast.makeText(
                                    context,
                                    "Can'ta ouch this!",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                return@FloatingActionButton
                            }

                            val uninstallApps = {
                                val uninstall = { resetToFactory: Boolean ->
                                    coroutineScope.launch {
                                        uninstallOrReinstall(
                                            uninstallApp = uninstallApp,
                                            reinstallApp = reinstallApp,
                                            selectedAppsType = selectedAppsType,
                                            appListViewModel = appListViewModel,
                                            resetToFactory = resetToFactory
                                        )
                                    }
                                }

                                // Show confirmation dialog.
                                if (selectedAppsType == AppsType.INSTALLED && confirmBeforeUninstall) {
                                    if (appListViewModel.selectedApps.isNotEmpty()) {
                                        // TO-Do Consider refactoring dialog management.
                                        // This `currentDialog`
                                        // currentDialog(@Composable) could potentially be
                                        // replaced by a simpler state
                                        // Haven't touched it as for now due to its role in
                                        // core uninstall flow.
                                        currentDialog = {
                                            val canResetAny =
                                                appListViewModel.selectedApps.keys.any {
                                                    canResetAppToFactory(it)
                                                }

                                            UninstallAppsDialog(
                                                appCount =
                                                appListViewModel
                                                    .selectedApps
                                                    .size,
                                                canResetToFactory = canResetAny,
                                                onDismiss = { currentDialog = null },
                                                onAgree = { resetToFactory ->
                                                    currentDialog = null
                                                    uninstall(resetToFactory)
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Trigger uninstall
                                    uninstall(false)
                                }
                            }

                            // Show dialog before uninstalling if we are on the "installed"
                            // tab
                            // However, do not show it if user has disabled the dialog in
                            // settings
                            // or if we are on the "uninstalled" tab
                            if (!ShizukuPermission.isCantaAuthorized()) {
                                currentDialog = {
                                    ShizukuRequirementDialog(
                                        shizukuStatus =
                                        ShizukuPermission.checkShizukuActive(
                                            context.packageManager
                                        ),
                                        onClose = { proceed ->
                                            currentDialog = null

                                            if (proceed) {
                                                uninstallApps()
                                            }
                                        }
                                    )
                                }
                            } else {
                                uninstallApps()
                            }
                        },
                    ) {
                        when (selectedAppsType) {
                            AppsType.INSTALLED ->
                                // Show Canta icon if only Canta is selected
                                if (appListViewModel.selectedApps.contains(packageName)) {
                                    AppIconImage(
                                        appIconImage = cantaIcon,
                                        contentDescription =
                                        stringResource(R.string.app_name)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription =
                                        stringResource(R.string.uninstall)
                                    )
                                }

                            AppsType.UNINSTALLED ->
                                Icon(
                                    Icons.Default.InstallMobile,
                                    contentDescription =
                                    stringResource(R.string.reinstall)
                                )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TabRow(
                selectedTabIndex = selectedAppsType.ordinal,
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                AppsType.entries.forEach { currentTab ->
                    Tab(
                        selected = selectedAppsType == currentTab,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentTab.ordinal)
                                appListViewModel.selectedFilter = Filter.any
                            }
                            selectedAppsType = currentTab
                        },
                        icon = {
                            Icon(currentTab.icon, contentDescription = currentTab.toString())
                        },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                // Show active dialog
                currentDialog?.let { it() }

                var isRefreshing by remember { mutableStateOf(false) }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            isRefreshing = true
                            appListViewModel.loadInstalled(
                                packageManager = context.packageManager,
                                context = context,
                            )
                            isRefreshing = false
                        }
                    },
                ) {
                    AppList(
                        appType = AppsType.entries[page],
                        appListModel = appListViewModel,
                        enableSelectAll = enableSelectAll,
                    )
                }
            }
        }
    }
}

suspend fun uninstallOrReinstall(
    uninstallApp: (String, Boolean) -> Boolean,
    reinstallApp: (String) -> Boolean,
    selectedAppsType: AppsType,
    appListViewModel: AppListViewModel,
    resetToFactory: Boolean = false,
) {
    val appsToProcess = appListViewModel.selectedApps.keys.toList()
    withContext(Dispatchers.IO) {
        when (selectedAppsType) {
            AppsType.INSTALLED -> {
                appsToProcess.forEach { app ->
                    val uninstalled = uninstallApp(app, resetToFactory)
                    if (uninstalled) {
                        with(Dispatchers.Main) {
                            appListViewModel.changeAppStatus(app)
                            appListViewModel.selectedApps.remove(app)
                        }
                    }
                }
            }

            AppsType.UNINSTALLED -> {
                appsToProcess.forEach { app ->
                    val installed = reinstallApp(app)
                    if (installed) {
                        with(Dispatchers.Main) {
                            appListViewModel.changeAppStatus(app)
                            appListViewModel.selectedApps.remove(app)
                        }
                    }
                }
            }
        }
    }
}

enum class AppsType(val icon: ImageVector) {
    INSTALLED(Icons.Default.AutoDelete),
    UNINSTALLED(Icons.Default.DeleteForever),
}
