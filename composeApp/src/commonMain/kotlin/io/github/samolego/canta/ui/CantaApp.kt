package io.github.samolego.canta.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.samolego.canta.core.CantaAppDependencies
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.preset_save_error
import io.github.samolego.canta.generated.resources.select_all_enabled
import io.github.samolego.canta.generated.resources.select_all_tip
import io.github.samolego.canta.ui.navigation.Screen
import io.github.samolego.canta.ui.screen.LogsScreen
import io.github.samolego.canta.ui.screen.MainScreen
import io.github.samolego.canta.ui.screen.PresetsScreen
import io.github.samolego.canta.ui.screen.SettingsScreen
import io.github.samolego.canta.ui.theme.CantaTheme
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.MainViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** Taps on the version in settings that unlock "select all" for recommended apps. */
private const val SECRET_TAPS = 6

/** Taps after which the remaining count is hinted. */
private const val SECRET_TAPS_HINT_AFTER = 3

/** The app's root composable, shared by the Android, desktop and web entry points. */
@Composable
fun CantaApp(deps: CantaAppDependencies, closeApp: () -> Unit) {
    CantaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CantaNavigation(deps, closeApp)
        }
    }
}

@Composable
private fun CantaNavigation(deps: CantaAppDependencies, closeApp: () -> Unit) {
    val handler = deps.handler
    val platform = deps.platform

    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val appListViewModel: AppListViewModel = viewModel { AppListViewModel(handler, deps.bloatRepository) }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(deps.settings) }
    val presetViewModel: PresetsViewModel = viewModel { PresetsViewModel(deps.presetStore, handler, platform) }
    val mainViewModel: MainViewModel = viewModel { MainViewModel(handler, platform, deps.settings, appListViewModel) }

    var versionTapCounter by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(platform) {
        platform.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Screen.Main.route) {
            composable(Screen.Main.route) {
                MainScreen(
                    handler = handler,
                    platform = platform,
                    mainViewModel = mainViewModel,
                    appListViewModel = appListViewModel,
                    settingsViewModel = settingsViewModel,
                    presetEditMode = presetViewModel.editingPreset != null,
                    onPresetEditFinish = {
                        val preset = presetViewModel.editingPreset ?: return@MainScreen
                        presetViewModel.setPresetApps(
                            preset = preset,
                            // Snapshot: the selection is cleared right after saving.
                            newApps = appListViewModel.selectedApps.toSet(),
                            onSuccess = {
                                appListViewModel.selectedApps.clear()
                                presetViewModel.editingPreset = null
                                navController.navigate(Screen.Presets.route)
                            },
                            onError = {
                                coroutineScope.launch { platform.showMessage(getString(Res.string.preset_save_error)) }
                            },
                        )
                    },
                    navigateToPage = { navController.navigate(it) },
                    closeApp = closeApp,
                    enableSelectAll = versionTapCounter >= SECRET_TAPS,
                )
            }
            composable(route = Screen.Logs.route) {
                LogsScreen(
                    platform = platform,
                    onNavigateBack = { navController.navigateUp() },
                )
            }
            composable(route = Screen.Settings.route) {
                SettingsScreen(
                    platform = platform,
                    onNavigateBack = { navController.navigateUp() },
                    settingsViewModel = settingsViewModel,
                    onVersionTap = {
                        versionTapCounter += 1
                        coroutineScope.launch {
                            when {
                                versionTapCounter == SECRET_TAPS ->
                                    platform.showMessage(getString(Res.string.select_all_enabled))
                                versionTapCounter in (SECRET_TAPS_HINT_AFTER + 1)..<SECRET_TAPS ->
                                    platform.showMessage(getString(Res.string.select_all_tip, SECRET_TAPS - versionTapCounter))
                            }
                        }
                    },
                )
            }
            composable(route = Screen.Presets.route) {
                PresetsScreen(
                    platform = platform,
                    presetViewModel = presetViewModel,
                    onNavigateBack = { appliedPreset ->
                        appliedPreset?.let {
                            appListViewModel.selectedApps.clear()
                            appListViewModel.selectedApps.addAll(it.apps)
                        }
                        navController.navigateUp()
                    },
                    appListViewModel = appListViewModel,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
