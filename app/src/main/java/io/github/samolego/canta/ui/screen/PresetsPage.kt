package io.github.samolego.canta.ui.screen

import ScreenTopBar
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.samolego.canta.ui.component.IconClickButton
import io.github.samolego.canta.ui.dialog.ImportConfigurationDialog
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.util.CantaConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsPage(
    onNavigateBack: () -> Unit,
    appListViewModel: AppListViewModel
) {
    val context = LocalContext.current
    val configViewModel: PresetsViewModel = viewModel()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configViewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                onNavigateBack =  onNavigateBack,
                title = {
                    Text("Presets")
                },
                actions = {
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
        },
        floatingActionButton = {
            // Todo - once clicked, expand for import / create
            FloatingActionButton(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add preset"
                )
            }
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
                            onAddApps = {
                                configViewModel.loadAvailableAppsForAdding(appListViewModel.appList)
                            },
                            formatDate = configViewModel::formatDate,
                            onEdit = {},
                            onApply = {},
                            onDelete = {},
                            onExport = {
                                configViewModel.exportToClipboard(
                                    context = context,
                                    config = config,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Configuration copied to clipboard",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(
                                            context,
                                            "Export failed: $error",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            },
                        )
                    }
                }
            }
        }
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
