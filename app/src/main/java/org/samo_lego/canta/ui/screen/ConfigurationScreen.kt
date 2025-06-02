package org.samo_lego.canta.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.samo_lego.canta.R
import org.samo_lego.canta.ui.component.IconClickButton
import org.samo_lego.canta.ui.dialog.CreateConfigurationDialog
import org.samo_lego.canta.ui.dialog.ImportConfigurationDialog
import org.samo_lego.canta.ui.screen.configuration.EditConfigurationDialog
import org.samo_lego.canta.ui.screen.configuration.AddAppsDialog
import org.samo_lego.canta.ui.viewmodel.AppListViewModel
import org.samo_lego.canta.ui.viewmodel.ConfigurationViewModel
import org.samo_lego.canta.util.CantaConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfigApps: (CantaConfiguration) -> Unit,
    appListViewModel: AppListViewModel
) {
    val context = LocalContext.current
    val configViewModel: ConfigurationViewModel = viewModel()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<CantaConfiguration?>(null) }
    var showEditDialog by remember { mutableStateOf<CantaConfiguration?>(null) }
    var showAddAppsDialog by remember { mutableStateOf<CantaConfiguration?>(null) }
    
    LaunchedEffect(Unit) {
        configViewModel.initialize(context)
    }
    
    // Auto-sync when apps are uninstalled
    LaunchedEffect(appListViewModel.appList) {
        if (configViewModel.isAutoSyncEnabled) {
            configViewModel.syncConfigurationsWithUninstalledApps(
                uninstalledApps = appListViewModel.appList,
                onSuccess = { updated ->
                    if (updated) {
                        Toast.makeText(context, "Configurations auto-synced", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = { 
                    Text(
                        "Configurations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconClickButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                },
                actions = {
                    // Auto-sync toggle with better visual feedback
                    Surface(
                        onClick = { 
                            configViewModel.enableAutoSync(!configViewModel.isAutoSyncEnabled)
                            Toast.makeText(
                                context, 
                                if (configViewModel.isAutoSyncEnabled) "Auto-sync enabled" else "Auto-sync disabled", 
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (configViewModel.isAutoSyncEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (configViewModel.isAutoSyncEnabled) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                contentDescription = if (configViewModel.isAutoSyncEnabled) "Disable Auto-sync" else "Enable Auto-sync",
                                modifier = Modifier.size(16.dp),
                                tint = if (configViewModel.isAutoSyncEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (configViewModel.isAutoSyncEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    
                    IconClickButton(
                        onClick = { showImportDialog = true },
                        icon = Icons.Default.Download,
                        contentDescription = "Import Configuration"
                    )
                    IconClickButton(
                        onClick = { showCreateDialog = true },
                        icon = Icons.Default.Add,
                        contentDescription = "Create Configuration"
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (configViewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (configViewModel.configurations.isEmpty()) {
                EmptyConfigurationsState(
                    onCreateClick = { showCreateDialog = true },
                    onImportClick = { showImportDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(configViewModel.configurations) { config ->
                        ConfigurationCard(
                            configuration = config,
                            onEdit = { showEditDialog = config },
                            onAddApps = { 
                                configViewModel.loadAvailableAppsForAdding(appListViewModel.appList)
                                showAddAppsDialog = config 
                            },
                            onExport = { 
                                configViewModel.exportToClipboard(
                                    context = context,
                                    config = config,
                                    onSuccess = {
                                        Toast.makeText(context, "Configuration copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Export failed: $error", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onDelete = { showDeleteDialog = config },
                            onApply = { onNavigateToConfigApps(config) },
                            formatDate = configViewModel::formatDate
                        )
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showCreateDialog) {
        CreateConfigurationDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                configViewModel.saveConfiguration(
                    name = name,
                    description = description,
                    apps = appListViewModel.appList,
                    onSuccess = {
                        showCreateDialog = false
                        Toast.makeText(context, "Configuration saved", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Save failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    
    if (showImportDialog) {
        ImportConfigurationDialog(
            onDismiss = { showImportDialog = false },
            onImportFromClipboard = {
                configViewModel.importFromClipboard(
                    context = context,
                    onSuccess = { config ->
                        showImportDialog = false
                        configViewModel.saveImportedConfiguration(
                            config = config,
                            onSuccess = {
                                Toast.makeText(context, "Configuration imported and saved", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Failed to save imported configuration: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onError = { error ->
                        Toast.makeText(context, "Import failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onImportFromText = { jsonText ->
                configViewModel.importFromJson(
                    jsonString = jsonText,
                    onSuccess = { config ->
                        showImportDialog = false
                        configViewModel.saveImportedConfiguration(
                            config = config,
                            onSuccess = {
                                Toast.makeText(context, "Configuration imported and saved", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Failed to save imported configuration: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onError = { error ->
                        Toast.makeText(context, "Import failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    
    showEditDialog?.let { config ->
        EditConfigurationDialog(
            configuration = config,
            onDismiss = { showEditDialog = null },
            onConfirm = { newName, newDescription ->
                configViewModel.updateConfiguration(
                    oldConfig = config,
                    newName = newName,
                    newDescription = newDescription,
                    onSuccess = {
                        showEditDialog = null
                        Toast.makeText(context, "Configuration updated", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Update failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    showAddAppsDialog?.let { config ->
        AddAppsDialog(
            availableApps = configViewModel.availableAppsForAdding,
            onDismiss = { showAddAppsDialog = null },
            onConfirm = { selectedApps ->
                val appConfigs = selectedApps.map { app ->
                    configViewModel.createAppConfigurationFromAppInfo(app)
                }
                configViewModel.addAppsToConfiguration(
                    config = config,
                    appsToAdd = appConfigs,
                    onSuccess = {
                        showAddAppsDialog = null
                        Toast.makeText(context, "Apps added to configuration", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Failed to add apps: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    showDeleteDialog?.let { config ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Configuration") },
            text = { Text("Are you sure you want to delete '${config.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        configViewModel.deleteConfiguration(
                            config = config,
                            onSuccess = {
                                showDeleteDialog = null
                                Toast.makeText(context, "Configuration deleted", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Delete failed: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyConfigurationsState(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Configurations",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Create or import configurations to manage\napp uninstall lists across devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Configuration")
            }
            
            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Configuration")
            }
        }
    }
}

@Composable
private fun ConfigurationCard(
    configuration: CantaConfiguration,
    onEdit: (CantaConfiguration) -> Unit,
    onAddApps: (CantaConfiguration) -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    formatDate: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = configuration.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (configuration.description.isNotEmpty()) {
                        Text(
                            text = configuration.description,
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
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${configuration.apps.size} apps",
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
                            text = formatDate(configuration.createdDate),
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
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit(configuration)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Apps") },
                            onClick = {
                                showMenu = false
                                onAddApps(configuration)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export") },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Configuration")
            }
        }
    }
} 