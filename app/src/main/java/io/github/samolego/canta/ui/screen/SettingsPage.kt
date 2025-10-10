package io.github.samolego.canta.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.samolego.canta.BuildConfig
import io.github.samolego.canta.R
import io.github.samolego.canta.ui.component.IconClickButton
import io.github.samolego.canta.ui.component.SettingsItem
import io.github.samolego.canta.ui.component.SettingsTextItem
import io.github.samolego.canta.ui.viewmodel.SettingsViewModel
import io.github.samolego.canta.util.DEFAULT_BLOAT_COMMITS_URL
import io.github.samolego.canta.util.DEFAULT_BLOAT_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onVersionTap: () -> Unit,
) {
    val context = LocalContext.current
    val autoUpdateBloatList by settingsViewModel.autoUpdateBloatList.collectAsStateWithLifecycle()
    val confirmBeforeUninstall by
            settingsViewModel.confirmBeforeUninstall.collectAsStateWithLifecycle()

    var advancedSettingsExpanded by remember { mutableStateOf(false) }
    var bloatListUrl by remember { mutableStateOf(settingsViewModel.bloatListUrl.value.let { if (it.isEmpty()) DEFAULT_BLOAT_URL else it }) }
    var commitsUrl by remember { mutableStateOf(settingsViewModel.commitsUrl.value.let { if (it.isEmpty()) DEFAULT_BLOAT_COMMITS_URL else it }) }
    val allowUnsafe by settingsViewModel.allowUnsafeUninstall.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconClickButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Auto-update bloat list
            SettingsItem(
                title = stringResource(R.string.auto_update_bloat_list),
                description = stringResource(R.string.auto_update_bloat_list_description),
                icon = Icons.Default.Update,
                isSwitch = true,
                checked = autoUpdateBloatList,
                onCheckedChange = {
                    settingsViewModel.saveAutoUpdateBloatList(it)
                }
            )

            // Confirm before uninstall
            SettingsItem(
                title = stringResource(R.string.confirm_uninstall),
                description = stringResource(R.string.confirm_uninstall_description),
                icon = Icons.Default.Delete,
                isSwitch = true,
                checked = confirmBeforeUninstall,
                onCheckedChange = {
                    settingsViewModel.saveConfirmBeforeUninstall(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(16.dp))

            // Advanced Settings Section
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable {
                                        advancedSettingsExpanded = !advancedSettingsExpanded
                                    }
                                    .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = stringResource(R.string.advanced_settings),
                            style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                            text = stringResource(R.string.click_to_expand),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                        imageVector =
                                if (advancedSettingsExpanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                        contentDescription = if (advancedSettingsExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                    visible = advancedSettingsExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column {
                    SettingsItem(
                        title = stringResource(R.string.allow_unsafe_selections),
                        description = stringResource(R.string.allow_unsafe_uninstalls_description),
                        icon = Icons.Default.Close,
                        isSwitch = true,
                        checked = allowUnsafe,
                        onCheckedChange = {
                            settingsViewModel.saveAllowUnsafeUninstalls(it)
                        }
                    )

                    // Bloat List URL
                    SettingsTextItem(
                            title = stringResource(R.string.bloat_list_url),
                            description = stringResource(R.string.bloat_list_url_description),
                            icon = Icons.Default.Link,
                            keyboardType = KeyboardType.Uri,
                            value = bloatListUrl,
                            onValueChange = {
                                bloatListUrl = it
                                settingsViewModel.saveBloatListUrl(it)
                            },
                    )

                    // Commits URL
                    SettingsTextItem(
                            title = stringResource(R.string.commits_url),
                            description = stringResource(R.string.commits_url_description),
                            icon = Icons.Default.Link,
                            keyboardType = KeyboardType.Uri,
                            value = commitsUrl,
                            onValueChange = {
                                commitsUrl = it
                                settingsViewModel.saveCommitsUrl(it)
                            },
                    )
                }
            }

            // Spacer to push footer to bottom
            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            // App info footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // App version
                Text(
                    text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        onVersionTap()
                    }
                )

                // App homepage
                Text(
                    text = "https://samolego.github.io/Canta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://samolego.github.io/Canta"))
                        context.startActivity(browserIntent)
                    }
                )
            }
        }
    }
}
