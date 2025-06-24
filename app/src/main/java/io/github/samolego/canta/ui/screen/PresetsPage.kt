package io.github.samolego.canta.ui.screen

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.R
import io.github.samolego.canta.ui.component.ScreenTopBar
import io.github.samolego.canta.ui.component.fab.ExpandableFAB
import io.github.samolego.canta.ui.dialog.ImportPresetDialog
import io.github.samolego.canta.ui.dialog.PresetCreateDialog
import io.github.samolego.canta.ui.dialog.PresetEditDialog
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.util.CantaPresetData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsPage(
    presetViewModel: PresetsViewModel,
    onNavigateBack: (appliedPreset: CantaPresetData?) -> Unit,
    appListViewModel: AppListViewModel,
) {
    val context = LocalContext.current

    var currentDialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val createConfigDialog =
            @Composable
            {
                PresetCreateDialog(
                        appListViewModel = appListViewModel,
                        presetViewModel = presetViewModel,
                        closeDialog = { currentDialog = null }
                )
            }

    LaunchedEffect(Unit) { presetViewModel.initialize(context) }
    Scaffold(
            topBar = {
                ScreenTopBar(
                        onNavigateBack = { onNavigateBack(null) },
                        title = { Text(stringResource(R.string.presets)) },
                )
            },
            floatingActionButton = {
                if (presetViewModel.presets.isNotEmpty()) {
                    ExpandableFAB(
                            onBottomClick = { currentDialog = { createConfigDialog() } },
                            onTopClick = {
                                currentDialog = {
                                    ImportDialog(
                                            presetViewModel = presetViewModel,
                                            hideDialog = { currentDialog = null },
                                            context = context,
                                    )
                                }
                            }
                    )
                }
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (presetViewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (presetViewModel.presets.isEmpty()) {
                EmptyPresetsState(
                        onCreateClick = { currentDialog = { createConfigDialog() } },
                        onImportClick = {
                            currentDialog = {
                                ImportDialog(
                                        presetViewModel = presetViewModel,
                                        hideDialog = { currentDialog = null },
                                        context = context,
                                )
                            }
                        }
                )
            } else {
                val presetDeletedText = stringResource(R.string.preset_deleted)
                val presetDeleteErrorText = stringResource(R.string.preset_delete_error)
                val presetCopiedText = stringResource(R.string.preset_copied)
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(presetViewModel.presets) { preset ->
                        PresetCard(
                                preset = preset,
                                onAddApps = {
                                    presetViewModel.editingPreset = preset
                                    onNavigateBack(preset)
                                },
                                formatDate = presetViewModel::formatDate,
                                onEdit = {
                                    currentDialog = {
                                        PresetEditDialog(
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
                                            onSuccess = {
                                                Toast.makeText(
                                                                context,
                                                                presetDeletedText,
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            },
                                            onError = {
                                                Toast.makeText(
                                                                context,
                                                                presetDeleteErrorText,
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            },
                                    )
                                },
                                onExport = {
                                    presetViewModel.exportToClipboard(
                                            context = context,
                                            preset = preset,
                                    )
                                    Toast.makeText(
                                                    context,
                                                    presetCopiedText,
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
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
        hideDialog: () -> Unit,
        presetViewModel: PresetsViewModel,
        context: Context
) {
    val importFailedText = stringResource(R.string.import_failed)
    ImportPresetDialog(
            onDismiss = hideDialog,
            onImportFromClipboard = {
                presetViewModel.importFromClipboard(
                        context = context,
                        onSuccess = { preset ->
                            hideDialog()
                            presetViewModel.saveImportedPreset(
                                    preset = preset,
                                    onError = {
                                        Toast.makeText(context, importFailedText, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            )
                        },
                        onError = {
                            Toast.makeText(context, importFailedText, Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
            },
            onImportFromText = { jsonText ->
                presetViewModel.importFromJson(
                        jsonString = jsonText,
                        onSuccess = { preset ->
                            hideDialog()
                            presetViewModel.saveImportedPreset(
                                preset = preset,
                                onError = {
                                    Toast.makeText(context, importFailedText, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            )
                        },
                        onError = {
                            Toast.makeText(context, importFailedText, Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
            }
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
                        contentDescription = stringResource(R.string.presets),
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
                text = stringResource(R.string.no_presets),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
        )

        Text(
                text = stringResource(R.string.presets_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_preset))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_preset))
            }

            OutlinedButton(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.import_preset))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.import_preset))
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
                                contentDescription = stringResource(R.string.selected_apps),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = pluralStringResource(R.plurals.num_selected_apps, preset.apps.size, preset.apps.size),
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

                // Action menu button
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = {
                                    showMenu = false
                                    onEdit(preset)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                                }
                        )
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_apps)) },
                                onClick = {
                                    showMenu = false
                                    onAddApps(preset)
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_apps)) }
                        )
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
                                onClick = {
                                    showMenu = false
                                    onExport()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                                }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
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

            // Primary action button
            Button(
                    onClick = { onApply() },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                            )
            ) {
                Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.apply_preset),
                        modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.apply_preset))
            }
        }
    }
}
