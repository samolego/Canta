package io.github.samolego.canta.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.samolego.canta.BuildInfo
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.advanced_settings
import io.github.samolego.canta.generated.resources.allow_unsafe_selections
import io.github.samolego.canta.generated.resources.allow_unsafe_uninstalls_description
import io.github.samolego.canta.generated.resources.app_version
import io.github.samolego.canta.generated.resources.auth_required
import io.github.samolego.canta.generated.resources.auth_required_description
import io.github.samolego.canta.generated.resources.auto_update_bloat_list
import io.github.samolego.canta.generated.resources.auto_update_bloat_list_description
import io.github.samolego.canta.generated.resources.bloat_list_url
import io.github.samolego.canta.generated.resources.bloat_list_url_description
import io.github.samolego.canta.generated.resources.click_to_expand
import io.github.samolego.canta.generated.resources.commits_url
import io.github.samolego.canta.generated.resources.commits_url_description
import io.github.samolego.canta.generated.resources.confirm_uninstall
import io.github.samolego.canta.generated.resources.confirm_uninstall_description
import io.github.samolego.canta.generated.resources.hide_success_dialog
import io.github.samolego.canta.generated.resources.hide_success_dialog_description
import io.github.samolego.canta.generated.resources.require_auth_setting
import io.github.samolego.canta.generated.resources.require_auth_setting_desc
import io.github.samolego.canta.generated.resources.settings
import io.github.samolego.canta.ui.component.SettingsItem
import io.github.samolego.canta.ui.component.SwitchSettingsItem
import io.github.samolego.canta.ui.component.ScreenTopBar
import io.github.samolego.canta.ui.component.ExpandIcon
import io.github.samolego.canta.ui.component.SettingsTextItem
import io.github.samolego.canta.ui.viewmodel.SettingsViewModel

private const val CANTA_WEBSITE = "https://samolego.github.io/Canta"

@Composable
fun SettingsScreen(
    platform: CantaPlatform,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onVersionTap: () -> Unit,
) {
    val autoUpdateBloatList by settingsViewModel.autoUpdateBloatList.collectAsStateWithLifecycle()
    val confirmBeforeUninstall by settingsViewModel.confirmBeforeUninstall.collectAsStateWithLifecycle()
    val allowUnsafe by settingsViewModel.allowUnsafeUninstalls.collectAsStateWithLifecycle()
    val hideSuccessDialog by settingsViewModel.hideSuccessDialog.collectAsStateWithLifecycle()
    val authEnabled by settingsViewModel.authEnabled.collectAsStateWithLifecycle()
    val bloatListUrl by settingsViewModel.bloatListUrl.collectAsStateWithLifecycle()
    val commitsUrl by settingsViewModel.commitsUrl.collectAsStateWithLifecycle()
    var advancedSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    val authRequiredTitle = stringResource(Res.string.auth_required)
    val authRequiredSubtitle = stringResource(Res.string.auth_required_description)

    Scaffold(
        topBar = {
            ScreenTopBar(onNavigateBack = onNavigateBack, title = { Text(stringResource(Res.string.settings)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SwitchSettingsItem(
                title = stringResource(Res.string.auto_update_bloat_list),
                description = stringResource(Res.string.auto_update_bloat_list_description),
                icon = Icons.Default.Update,
                checked = autoUpdateBloatList,
                onCheckedChange = settingsViewModel::saveAutoUpdateBloatList,
            )
            SwitchSettingsItem(
                title = stringResource(Res.string.confirm_uninstall),
                description = stringResource(Res.string.confirm_uninstall_description),
                icon = Icons.Default.Delete,
                checked = confirmBeforeUninstall,
                onCheckedChange = settingsViewModel::saveConfirmBeforeUninstall,
            )
            SwitchSettingsItem(
                title = stringResource(Res.string.hide_success_dialog),
                description = stringResource(Res.string.hide_success_dialog_description),
                icon = Icons.AutoMirrored.Default.Message,
                checked = hideSuccessDialog,
                onCheckedChange = settingsViewModel::saveHideSuccessDialog,
            )
            SwitchSettingsItem(
                title = stringResource(Res.string.require_auth_setting),
                description = stringResource(Res.string.require_auth_setting_desc),
                icon = Icons.Default.Lock,
                checked = authEnabled,
                onCheckedChange = { enabled ->
                    platform.requireBiometric(title = authRequiredTitle, subtitle = authRequiredSubtitle) {
                        settingsViewModel.saveAuthEnabled(enabled)
                    }
                },
            )
            HorizontalDivider(modifier = Modifier.padding(16.dp))

            SettingsItem(
                title = stringResource(Res.string.advanced_settings),
                description = stringResource(Res.string.click_to_expand),
                icon = Icons.Default.Settings,
                onClick = { advancedSettingsExpanded = !advancedSettingsExpanded },
                trailing = { ExpandIcon(advancedSettingsExpanded) },
            )

            AnimatedVisibility(
                visible = advancedSettingsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    SwitchSettingsItem(
                        title = stringResource(Res.string.allow_unsafe_selections),
                        description = stringResource(Res.string.allow_unsafe_uninstalls_description),
                        icon = Icons.Default.Close,
                        checked = allowUnsafe,
                        onCheckedChange = settingsViewModel::saveAllowUnsafeUninstalls,
                    )
                    SettingsTextItem(
                        title = stringResource(Res.string.bloat_list_url),
                        description = stringResource(Res.string.bloat_list_url_description),
                        icon = Icons.Default.Link,
                        keyboardType = KeyboardType.Uri,
                        storedValue = bloatListUrl,
                        onCommit = settingsViewModel::saveBloatListUrl,
                    )
                    SettingsTextItem(
                        title = stringResource(Res.string.commits_url),
                        description = stringResource(Res.string.commits_url_description),
                        icon = Icons.Default.Link,
                        keyboardType = KeyboardType.Uri,
                        storedValue = commitsUrl,
                        onCommit = settingsViewModel::saveCommitsUrl,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.app_version, BuildInfo.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onVersionTap)
                )
                Text(
                    text = CANTA_WEBSITE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { platform.openUrl(CANTA_WEBSITE) }
                )
            }
        }
    }
}
