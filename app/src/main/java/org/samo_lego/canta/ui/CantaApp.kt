package org.samo_lego.canta.ui

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.samo_lego.canta.APP_NAME
import org.samo_lego.canta.R
import org.samo_lego.canta.packageName
import org.samo_lego.canta.ui.component.AppIconImage
import org.samo_lego.canta.ui.component.AppList
import org.samo_lego.canta.ui.component.CantaTopBar
import org.samo_lego.canta.ui.dialog.ExplainBadgesDialog
import org.samo_lego.canta.ui.dialog.NoWarrantyDialog
import org.samo_lego.canta.ui.dialog.UninstallAppsDialog
import org.samo_lego.canta.ui.navigation.Screen
import org.samo_lego.canta.ui.screen.ConfigurationAppsScreen
import org.samo_lego.canta.ui.screen.ConfigurationScreen
import org.samo_lego.canta.ui.screen.LogsPage
import org.samo_lego.canta.ui.screen.SettingsScreen
import org.samo_lego.canta.ui.viewmodel.AppListViewModel
import org.samo_lego.canta.ui.viewmodel.SettingsViewModel
import org.samo_lego.canta.ui.viewmodel.ConfigurationViewModel
import org.samo_lego.canta.util.CantaConfiguration
import org.samo_lego.canta.util.Filter
import org.samo_lego.canta.util.SettingsStore
import org.samo_lego.canta.util.ShizukuData
import org.samo_lego.canta.util.ShizukuInfo
import org.samo_lego.canta.util.showFor

private const val secretTaps = 12

@Composable
fun CantaApp(
        launchShizuku: () -> Unit,
        canResetAppToFactory: (String) -> Boolean,
        uninstallApp: (String, Boolean) -> Boolean,
        reinstallApp: (String) -> Boolean,
        closeApp: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsStore = remember { SettingsStore(context) }
    val showDisclaimerWarning = remember { mutableStateOf(true) }
    var versionTapCounter by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Load settings when app starts and update the disclaimer warning state
    LaunchedEffect(Unit) {
        // First load settings
        settingsViewModel.loadSettings(settingsStore)

        // Create a collector for the disableRiskDialog flow
        settingsStore.disableRiskDialogFlow.collect { disableRiskDialog ->
            // Update the warning state based on the loaded setting
            showDisclaimerWarning.value = !disableRiskDialog
        }
    }

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainContent(
                    launchShizuku = launchShizuku,
                    canResetAppToFactory = canResetAppToFactory,
                    uninstallApp = uninstallApp,
                    reinstallApp = reinstallApp,
                    navigateToLogs = { navController.navigate(Screen.Logs.route) },
                    navigateToSettings = { navController.navigate(Screen.Settings.route) },
                    navigateToConfigurations = { navController.navigate(Screen.Configurations.route) },
                    closeApp = closeApp,
                    settingsStore = settingsStore,
                    showWarning = showDisclaimerWarning,
                    enableSelectAll = versionTapCounter >= secretTaps,
            )
        }
        composable(route = Screen.Logs.route) {
            LogsPage(onNavigateBack = { navController.navigateUp() })
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                    onNavigateBack = {
                        // Save settings when navigating back
                        settingsViewModel.saveSettings(settingsStore)
                        navController.navigateUp()
                    },
                    settingsViewModel = settingsViewModel,
                    onVersionTap = {
                        versionTapCounter += 1
                        coroutineScope.launch {
                            if (versionTapCounter > 6 && versionTapCounter < secretTaps) {
                                // Show quick toast
                                val remainingTaps = secretTaps - versionTapCounter
                                val message = context.getString(R.string.select_all_tip, remainingTaps)
                                val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                                toast.showFor(500)
                            } else if (versionTapCounter >= secretTaps) {
                                // Enable select all functionality with quick toast
                                val toast = Toast.makeText(context, context.getString(R.string.select_all_enabled), Toast.LENGTH_SHORT)
                                toast.showFor(500)
                            }
                        }
                    }
            )
        }
        composable(route = Screen.Configurations.route) {
            val appListViewModel = viewModel<AppListViewModel>()
            ConfigurationScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToConfigApps = { config ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("configuration", config)
                    navController.navigate(Screen.ConfigurationApps.route)
                },
                appListViewModel = appListViewModel
            )
        }
        composable(route = Screen.ConfigurationApps.route) {
            val configuration = navController.previousBackStackEntry?.savedStateHandle?.get<CantaConfiguration>("configuration")
            if (configuration != null) {
                ConfigurationAppsScreen(
                    configuration = configuration,
                    onNavigateBack = { navController.navigateUp() },
                    onUninstallApps = { packageNames ->
                        // Handle uninstalling the selected apps with error handling
                        var successCount = 0
                        var errorCount = 0
                        
                        packageNames.forEach { packageName ->
                            try {
                                // Check if package exists before trying to uninstall
                                context.packageManager.getPackageInfo(packageName, 0)
                                val success = uninstallApp(packageName, false)
                                if (success) {
                                    successCount++
                                } else {
                                    errorCount++
                                }
                            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                                // Package doesn't exist, skip it
                                errorCount++
                                android.util.Log.w("CantaApp", "Package $packageName not found, skipping uninstall")
                            } catch (e: Exception) {
                                // Other errors
                                errorCount++
                                android.util.Log.e("CantaApp", "Error uninstalling $packageName: ${e.message}")
                            }
                        }
                        
                        navController.navigateUp()
                        
                        // Show appropriate message based on results
                        val message = when {
                            successCount > 0 && errorCount == 0 -> "Successfully uninstalled $successCount app(s)"
                            successCount > 0 && errorCount > 0 -> "Uninstalled $successCount app(s), $errorCount failed or not found"
                            errorCount > 0 -> "$errorCount app(s) failed to uninstall or not found"
                            else -> "No apps were uninstalled"
                        }
                        
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent(
        launchShizuku: () -> Unit,
        canResetAppToFactory: (String) -> Boolean,
        uninstallApp: (String, Boolean) -> Boolean,
        reinstallApp: (String) -> Boolean,
        navigateToLogs: () -> Unit,
        navigateToSettings: () -> Unit,
        navigateToConfigurations: () -> Unit,
        closeApp: () -> Unit,
        settingsStore: SettingsStore,
        showWarning: MutableState<Boolean>,
        enableSelectAll: Boolean,
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Selected tab
    var selectedAppsType by remember { mutableStateOf(AppsType.INSTALLED) }

    val appListViewModel = viewModel<AppListViewModel>()
    val settingsViewModel = viewModel<SettingsViewModel>()
    val configurationViewModel = viewModel<ConfigurationViewModel>()

    var showBadgeInfoDialog by remember { mutableStateOf(false) }
    var showUninstallConfirmDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { AppsType.entries.size })

    // Set up auto-sync callback
    LaunchedEffect(Unit) {
        configurationViewModel.initialize(context)
        appListViewModel.onAppStatusChanged = { apps ->
            if (configurationViewModel.isAutoSyncEnabled) {
                configurationViewModel.syncConfigurationsWithUninstalledApps(
                    uninstalledApps = apps,
                    onSuccess = { updated ->
                        if (updated) {
                            Toast.makeText(context, "Configurations auto-synced", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedAppsType = AppsType.entries[page]
            appListViewModel.selectedFilter = Filter.any
        }
    }

    val cantaIcon = remember(context) { context.packageManager.getApplicationIcon(packageName) }

    Scaffold(
            topBar = {
                CantaTopBar(
                        openBadgesInfoDialog = { showBadgeInfoDialog = true },
                        openLogsScreen = navigateToLogs,
                        openSettingsScreen = navigateToSettings,
                        openConfigurationsScreen = navigateToConfigurations
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                        // Make the FAB hidden if no apps are selected
                        visible = appListViewModel.selectedApps.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                            containerColor =
                                    when (selectedAppsType) {
                                        AppsType.INSTALLED ->
                                                MaterialTheme.colorScheme.errorContainer
                                        AppsType.UNINSTALLED ->
                                                MaterialTheme.colorScheme.tertiaryContainer
                                    },
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                            onClick = {
                                // Check if only Canta is selected
                                // Super secret don't tell anyone you saw this
                                // since this is an easter egg :P
                                if (appListViewModel.selectedApps.contains(packageName)) {
                                    // Show easter egg toast
                                    Toast.makeText(context, "Can'ta ouch this!", Toast.LENGTH_SHORT)
                                            .show()

                                    return@FloatingActionButton
                                }

                                // Show dialog before uninstalling if we are on the "instelled" tab
                                // However, do not show it if user has disabled the dialog in
                                // settings
                                // or if we are on the "uninstalled" tab
                                if (selectedAppsType == AppsType.INSTALLED &&
                                                settingsViewModel.confirmBeforeUninstall
                                ) {
                                    showUninstallConfirmDialog =
                                            appListViewModel.selectedApps.isNotEmpty()
                                    return@FloatingActionButton
                                }
                                // Trigger uninstall
                                uninstallOrReinstall(
                                        context = context,
                                        coroutineScope = coroutineScope,
                                        launchShizuku = launchShizuku,
                                        uninstallApp = uninstallApp,
                                        reinstallApp = reinstallApp,
                                        selectedAppsType = selectedAppsType,
                                        appListViewModel = appListViewModel,
                                )
                            },
                    ) {
                        when (selectedAppsType) {
                            AppsType.INSTALLED ->
                                // Show Canta icon if only Canta is selected
                                if (appListViewModel.selectedApps.contains(packageName)) {
                                    AppIconImage(
                                        appIconImage = cantaIcon,
                                        contentDescription = stringResource(R.string.app_name)
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
            },
            floatingActionButtonPosition = FabPosition.End,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                    modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                if (showBadgeInfoDialog) {
                    ExplainBadgesDialog(onDismissRequest = { showBadgeInfoDialog = false })
                } else if (showUninstallConfirmDialog) {

                    val canResetAny = appListViewModel.selectedApps.keys
                        .any { pkg -> canResetAppToFactory(pkg) }

                    UninstallAppsDialog(
                        appCount = appListViewModel.selectedApps.size,
                        canResetToFactory = canResetAny,
                        onDismiss = { showUninstallConfirmDialog = false },
                        onAgree = { resetToFactory ->
                            showUninstallConfirmDialog = false

                            uninstallOrReinstall(
                                context = context,
                                coroutineScope = coroutineScope,
                                launchShizuku = launchShizuku,
                                uninstallApp = uninstallApp,
                                reinstallApp = reinstallApp,
                                selectedAppsType = selectedAppsType,
                                appListViewModel = appListViewModel,
                                resetToFactory = resetToFactory
                            )
                        }
                    )
                } else if (showWarning.value) {
                    NoWarrantyDialog(
                            onProceed = { neverShowAgain ->
                                showWarning.value = false
                                if (neverShowAgain) {
                                    settingsViewModel.disableRiskDialog = true
                                    settingsViewModel.saveDisableRiskDialog(settingsStore)
                                }
                            },
                            onCancel = {
                                // Close the app
                                closeApp()
                            }
                    )
                }
                AppList(
                    appType = AppsType.entries[page],
                    appListModel = appListViewModel,
                    enableSelectAll = enableSelectAll,
                )
            }
        }
    }
}

fun uninstallOrReinstall(
        context: Context,
        coroutineScope: CoroutineScope,
        launchShizuku: () -> Unit,
        uninstallApp: (String, Boolean) -> Boolean,
        reinstallApp: (String) -> Boolean,
        selectedAppsType: AppsType,
        appListViewModel: AppListViewModel,
        resetToFactory: Boolean = false,
) {
    coroutineScope.launch {
        when (ShizukuData.checkShizukuActive(context.packageManager)) {
            ShizukuInfo.NOT_INSTALLED -> {
                Toast.makeText(
                                context,
                                context.getString(
                                        R.string.please_install_shizuku_and_authorise_canta
                                ),
                                Toast.LENGTH_SHORT
                        )
                        .show()
                return@launch
            }
            ShizukuInfo.NOT_ACTIVE -> {
                Toast.makeText(
                                context,
                                context.getString(R.string.please_start_shizuku),
                                Toast.LENGTH_SHORT
                        )
                        .show()
                launchShizuku()
                return@launch
            }
            ShizukuInfo.ACTIVE -> {
                // Check shizuku permission
                ShizukuData.checkShizukuPermission { permResult ->
                    val permission = permResult == PackageManager.PERMISSION_GRANTED

                    if (!permission) {
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.please_allow_shizuku_access_for_canta
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    } else {
                        // Proceed with the action
                        when (selectedAppsType) {
                            AppsType.INSTALLED -> {
                                appListViewModel.selectedApps.forEach {
                                    val uninstalled = uninstallApp(it.key, resetToFactory)
                                    if (uninstalled) {
                                        appListViewModel.changeAppStatus(it.key)
                                        appListViewModel.selectedApps.remove(it.key)
                                    }
                                }
                            }
                            AppsType.UNINSTALLED -> {
                                appListViewModel.selectedApps.forEach {
                                    val installed = reinstallApp(it.key)
                                    if (installed) {
                                        appListViewModel.changeAppStatus(it.key)
                                        appListViewModel.selectedApps.remove(it.key)
                                    }
                                }
                            }
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
