package io.github.samolego.canta.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.data.preset.formatPresetDate
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.add_apps
import io.github.samolego.canta.generated.resources.apply_preset
import io.github.samolego.canta.generated.resources.create_preset
import io.github.samolego.canta.generated.resources.delete
import io.github.samolego.canta.generated.resources.edit
import io.github.samolego.canta.generated.resources.import_failed
import io.github.samolego.canta.generated.resources.import_preset
import io.github.samolego.canta.generated.resources.more_options
import io.github.samolego.canta.generated.resources.no_presets
import io.github.samolego.canta.generated.resources.num_selected_apps
import io.github.samolego.canta.generated.resources.preset_copied
import io.github.samolego.canta.generated.resources.preset_delete_error
import io.github.samolego.canta.generated.resources.preset_deleted
import io.github.samolego.canta.generated.resources.presets
import io.github.samolego.canta.generated.resources.presets_description
import io.github.samolego.canta.generated.resources.selected_apps
import io.github.samolego.canta.generated.resources.share
import io.github.samolego.canta.ui.component.text.IconText
import io.github.samolego.canta.ui.component.ScreenTopBar
import io.github.samolego.canta.ui.component.fab.ExpandableFAB
import io.github.samolego.canta.ui.dialog.preset.ImportPresetDialog
import io.github.samolego.canta.ui.dialog.preset.PresetCreateDialog
import io.github.samolego.canta.ui.dialog.preset.PresetEditDialog
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(
    platform: CantaPlatform,
    presetViewModel: PresetsViewModel,
    onNavigateBack: (appliedPreset: CantaPresetData?) -> Unit,
    appListViewModel: AppListViewModel,
) {
    var currentDialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val presets by presetViewModel.presets.collectAsStateWithLifecycle()

    val createConfigDialog =
        @Composable {
            PresetCreateDialog(
                platform = platform,
                appListViewModel = appListViewModel,
                presetViewModel = presetViewModel,
                closeDialog = { currentDialog = null }
            )
        }

    val importDialog =
        @Composable {
            ImportDialog(
                platform = platform,
                presetViewModel = presetViewModel,
                hideDialog = { currentDialog = null },
            )
        }

    Scaffold(
        topBar = {
            ScreenTopBar(
                onNavigateBack = { onNavigateBack(null) },
                title = { Text(stringResource(Res.string.presets)) },
            )
        },
        floatingActionButton = {
            if (presets.isNotEmpty()) {
                ExpandableFAB(
                    onBottomClick = { currentDialog = createConfigDialog },
                    bottomDescription = stringResource(Res.string.create_preset),
                    topDescription = stringResource(Res.string.import_preset),
                    expandDescription = stringResource(Res.string.more_options),
                    onTopClick = { currentDialog = importDialog }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (presets.isEmpty()) {
                EmptyPresetsState(
                    onCreateClick = { currentDialog = createConfigDialog },
                    onImportClick = { currentDialog = importDialog }
                )
            } else {
                val presetDeletedText = stringResource(Res.string.preset_deleted)
                val presetDeleteErrorText = stringResource(Res.string.preset_delete_error)
                val presetCopiedText = stringResource(Res.string.preset_copied)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(presets, key = { it.uuid }) { preset ->
                        PresetCard(
                            preset = preset,
                            onAddApps = {
                                presetViewModel.editingPreset = preset
                                onNavigateBack(preset)
                            },
                            formatDate = ::formatPresetDate,
                            onEdit = {
                                currentDialog = {
                                    PresetEditDialog(
                                        platform = platform,
                                        preset = preset,
                                        presetViewModel = presetViewModel,
                                        closeDialog = { currentDialog = null }
                                    )
                                }
                            },
                            onApply = { onNavigateBack(preset) },
                            onDelete = {
                                presetViewModel.deletePreset(
                                    preset = preset,
                                    onSuccess = { platform.showMessage(presetDeletedText) },
                                    onError = { platform.showMessage(presetDeleteErrorText) },
                                )
                            },
                            onExport = {
                                presetViewModel.exportToClipboard(preset)
                                platform.showMessage(presetCopiedText)
                            },
                        )
                    }
                }
            }
        }
    }

    currentDialog?.let { it() }
}

@Composable
private fun ImportDialog(
    platform: CantaPlatform,
    hideDialog: () -> Unit,
    presetViewModel: PresetsViewModel,
) {
    val importFailedText = stringResource(Res.string.import_failed)
    val showError = { platform.showMessage(importFailedText) }
    ImportPresetDialog(
        onDismiss = hideDialog,
        onImportFromClipboard = { presetViewModel.importFromClipboard(onSuccess = hideDialog, onError = showError) },
        onImportFromText = { json -> presetViewModel.importFromJson(json, onSuccess = hideDialog, onError = showError) },
    )
}

@Composable
private fun EmptyPresetsState(onCreateClick: () -> Unit, onImportClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(Res.string.presets),
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.no_presets),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(Res.string.presets_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
                IconText(Icons.Default.Add, stringResource(Res.string.create_preset))
            }

            OutlinedButton(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                IconText(Icons.Default.Download, stringResource(Res.string.import_preset))
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: CantaPresetData,
    onEdit: (CantaPresetData) -> Unit,
    onAddApps: (CantaPresetData) -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    formatDate: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (preset.description.isNotEmpty()) {
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = stringResource(Res.string.selected_apps),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pluralStringResource(Res.plurals.num_selected_apps, preset.apps.size, preset.apps.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(preset.createdDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit(preset)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.add_apps)) },
                            onClick = {
                                showMenu = false
                                onAddApps(preset)
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_apps)) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.share)) },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.share))
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors =
                                MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onApply() },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
            ) {
                IconText(Icons.Default.PlayArrow, stringResource(Res.string.apply_preset))
            }
        }
    }
}
