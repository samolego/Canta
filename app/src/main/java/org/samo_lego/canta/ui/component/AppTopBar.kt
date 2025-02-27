package org.samo_lego.canta.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import org.samo_lego.canta.APP_NAME
import org.samo_lego.canta.R
import org.samo_lego.canta.ui.menu.FiltersMenu
import org.samo_lego.canta.ui.menu.MoreOptionsMenu
import org.samo_lego.canta.ui.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantaTopBar(
    openBadgesInfoDialog: () -> Unit,
    openLogsScreen: () -> Unit,
) {
    var showMoreOptionsMenu by remember { mutableStateOf(false) }
    var showFiltersMenu by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val appListViewModel = viewModel<AppListViewModel>()

    TopAppBar(
        title = {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Regular title - only visible when search is not active
                AnimatedVisibility(
                    visible = !searchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(APP_NAME)
                }

                // Search bar - only visible when search is active
                AnimatedVisibility(
                    visible = searchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                searchActive = false
                                appListViewModel.searchQuery = ""
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            value = appListViewModel.searchQuery,
                            onValueChange = {
                                appListViewModel.searchQuery = it
                            },
                            placeholder = { Text(stringResource(R.string.search_apps)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                            )
                        )
                    }

                    // Focus the search field when it becomes visible
                    LaunchedEffect(searchActive) {
                        if (searchActive) {
                            searchFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (searchActive) {
                        // If search is already active, clear the search
                        appListViewModel.searchQuery = ""
                    } else {
                        // Enable search mode
                        searchActive = true
                    }
                }
            ) {
                Icon(
                    if (searchActive) Icons.Default.Clear else Icons.Default.Search,
                    contentDescription = if (searchActive) "Clear search" else "Search"
                )
            }

            IconButton(
                onClick = { showFiltersMenu = !showFiltersMenu },
            ) {
                Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
            }

            IconButton(
                onClick = { showMoreOptionsMenu = !showMoreOptionsMenu },
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }

            FiltersMenu(
                showMenu = showFiltersMenu,
                onDismiss = { showFiltersMenu = false },
            )

            MoreOptionsMenu(
                showMenu = showMoreOptionsMenu,
                showBadgeInfoDialog = {
                    openBadgesInfoDialog()
                },
                openLogsScreen = {
                    openLogsScreen()
                },
                onDismiss = { showMoreOptionsMenu = false },
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
}
