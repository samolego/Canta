package org.samo_lego.canta.ui

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.samo_lego.canta.APP_NAME
import org.samo_lego.canta.ui.component.AppList
import org.samo_lego.canta.ui.dialog.ExplainBadgesDialog
import org.samo_lego.canta.ui.viewmodel.AppListViewModel
import org.samo_lego.canta.util.Filter
import org.samo_lego.canta.util.ShizukuData
import org.samo_lego.canta.util.ShizukuInfo

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CantaApp(
    launchShizuku: () -> Unit,
    uninstallApp: (String) -> Boolean,
    reinstallApp: (String) -> Boolean,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Selected tab
    var selectedAppsType by remember { mutableStateOf(AppsType.INSTALLED) }

    val appListViewModel = viewModel<AppListViewModel>()

    var showMoreOptionsPanel by remember { mutableStateOf(false) }
    var showBadgeInfoDialog by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val pagerState = rememberPagerState(pageCount = { AppsType.entries.size })


    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedAppsType = AppsType.entries[page]
            appListViewModel.selectedFilter = Filter.any
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                    ) {

                        TextField(
                            modifier = Modifier.focusRequester(searchFocusRequester),
                            value = appListViewModel.searchQuery,
                            onValueChange = {
                                appListViewModel.searchQuery = it
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboardController?.hide() }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                            ),
                        )
                    }

                    if (!searchActive) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(APP_NAME)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchActive = !searchActive
                            appListViewModel.searchQuery = ""

                            if (searchActive) {
                                searchFocusRequester.requestFocus()
                                keyboardController?.show()
                            } else {
                                searchFocusRequester.freeFocus()
                                keyboardController?.hide()
                            }
                        }
                    ) {
                        Icon(
                            if (searchActive) Icons.Default.Clear
                            else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(
                        onClick = { showMoreOptionsPanel = !showMoreOptionsPanel },
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    MoreOptionsMenu(
                        showMoreOptionsPanel = showMoreOptionsPanel,
                        showBadgeInfoDialog = {
                            showBadgeInfoDialog = true
                            showMoreOptionsPanel = false
                        },
                        onDismiss = { showMoreOptionsPanel = false },
                    )
                },
                colors = TopAppBarColors(
                    containerColor = if (!searchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            // Add floating action button
            FloatingActionButton(
                containerColor = when (selectedAppsType) {
                    AppsType.INSTALLED -> MaterialTheme.colorScheme.errorContainer
                    AppsType.UNINSTALLED -> MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(16.dp),
                onClick = {
                    coroutineScope.launch {
                        when (ShizukuData.checkShizukuActive(context.packageManager)) {
                            ShizukuInfo.NOT_INSTALLED -> {
                                Toast.makeText(
                                    context,
                                    "Please install Shizuku and authorise Canta.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            ShizukuInfo.NOT_ACTIVE -> {
                                Toast.makeText(
                                    context,
                                    "Please start Shizuku.",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                                            "Please allow Shizuku access for Canta.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // Proceed with the action
                                        when (selectedAppsType) {
                                            AppsType.INSTALLED -> {
                                                appListViewModel.selectedApps.forEach {
                                                    val uninstalled = uninstallApp(it.key)
                                                    if (uninstalled) {
                                                        appListViewModel.changeAppStatus(it.key)
                                                    }
                                                }
                                            }

                                            AppsType.UNINSTALLED -> {
                                                appListViewModel.selectedApps.forEach {
                                                    val installed = reinstallApp(it.key)
                                                    if (installed) {
                                                        appListViewModel.changeAppStatus(it.key)
                                                    }
                                                }
                                            }
                                        }
                                        appListViewModel.resetSelectedApps()
                                    }
                                }
                            }
                        }
                    }
                },
            ) {
                when (selectedAppsType) {
                    AppsType.INSTALLED -> Icon(
                        Icons.Default.Delete,
                        contentDescription = "Uninstall"
                    )

                    AppsType.UNINSTALLED -> Icon(
                        Icons.Default.InstallMobile,
                        contentDescription = "ReInstall"
                    )
                }
            }

        },
        floatingActionButtonPosition = FabPosition.End,
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
                                selectedAppsType = currentTab
                                appListViewModel.selectedFilter = Filter.any
                            }
                        },
                        icon = {
                            Icon(
                                currentTab.icon,
                                contentDescription = currentTab.toString()
                            )
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
                if (showBadgeInfoDialog) {
                    ExplainBadgesDialog(
                        onDismissRequest = { showBadgeInfoDialog = false }
                    )
                }
                AppList(
                    appType = AppsType.entries[page]
                )
            }
        }
    }
}

@Composable
fun MoreOptionsMenu(
    showMoreOptionsPanel: Boolean,
    showBadgeInfoDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appListViewModel = viewModel<AppListViewModel>()
    DropdownMenu(
        expanded = showMoreOptionsPanel,
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    appListViewModel.showSystem = !appListViewModel.showSystem
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Only system")
            Checkbox(
                checked = appListViewModel.showSystem,
                onCheckedChange = {
                    appListViewModel.showSystem = it
                },
            )
        }
        // Filters
        var filtersMenu by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { filtersMenu = !filtersMenu }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(appListViewModel.selectedFilter.name)
            Icon(
                if (filtersMenu) {
                    Icons.Default.ArrowDropUp
                } else {
                    Icons.Default.ArrowDropDown
                },
                contentDescription = "Filters",
            )
        }

        if (filtersMenu) {
            Filter.availableFilters.forEach { filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appListViewModel.selectedFilter = filter
                            filtersMenu = false
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(filter.name)
                }
            }
        }

        // Badge info dialog
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showBadgeInfoDialog()
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Badge info")
        }
    }
}

enum class AppsType(val icon: ImageVector) {
    INSTALLED(Icons.Default.AutoDelete),
    UNINSTALLED(Icons.Default.DeleteForever),
}