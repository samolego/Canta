package org.samo_lego.canta.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.samo_lego.canta.R
import org.samo_lego.canta.ui.component.AppIconImage
import org.samo_lego.canta.ui.component.IconClickButton
import org.samo_lego.canta.ui.viewmodel.ConfigurationViewModel
import org.samo_lego.canta.util.AppConfiguration
import org.samo_lego.canta.util.CantaConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationAppsScreen(
    configuration: CantaConfiguration,
    onNavigateBack: () -> Unit,
    onUninstallApps: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val configViewModel: ConfigurationViewModel = viewModel()
    
    var isEditMode by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(configuration) {
        configViewModel.loadConfigurationApps(configuration.apps)
    }
    
    val selectedCount = configViewModel.selectedAppsForConfig.size
    val totalCount = configuration.apps.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = { 
                    Column {
                        Text(
                            text = configuration.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEditMode) {
                                "$selectedCount selected for removal"
                            } else {
                                "$selectedCount of $totalCount selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconClickButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                },
                actions = {
                    if (isEditMode) {
                        IconClickButton(
                            onClick = { 
                                configViewModel.deselectAllApps()
                                isEditMode = false 
                            },
                            icon = Icons.Default.Close,
                            contentDescription = "Exit Edit Mode"
                        )
                        if (selectedCount > 0) {
                            IconClickButton(
                                onClick = { showRemoveDialog = true },
                                icon = Icons.Default.RemoveCircle,
                                contentDescription = "Remove Selected Apps"
                            )
                        }
                    } else {
                        IconClickButton(
                            onClick = { isEditMode = true },
                            icon = Icons.Default.Edit,
                            contentDescription = "Edit Configuration"
                        )
                        IconClickButton(
                            onClick = { configViewModel.selectAllApps() },
                            icon = Icons.Default.SelectAll,
                            contentDescription = "Select All"
                        )
                        IconClickButton(
                            onClick = { configViewModel.deselectAllApps() },
                            icon = Icons.Default.Deselect,
                            contentDescription = "Deselect All"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCount > 0 && !isEditMode) {
                FloatingActionButton(
                    onClick = {
                        val selectedPackages = configViewModel.getSelectedConfigApps().map { it.packageName }
                        onUninstallApps(selectedPackages)
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Uninstall Selected Apps"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Edit mode indicator
            if (isEditMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit mode - select apps to remove from configuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Configuration description and summary
            Column {
                if (configuration.description.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = configuration.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                // Summary of available vs unavailable apps
                val availableCount = remember(configuration.apps) {
                    configuration.apps.count { app ->
                        try {
                            context.packageManager.getPackageInfo(app.packageName, 0)
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
                val unavailableCount = configuration.apps.size - availableCount
                
                if (unavailableCount > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$unavailableCount of ${configuration.apps.size} apps are no longer available on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Apps list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(configuration.apps) { app ->
                    ConfigurationAppItem(
                        app = app,
                        isSelected = configViewModel.selectedAppsForConfig.contains(app.packageName),
                        onToggle = { configViewModel.toggleAppSelection(app.packageName) },
                        isEditMode = isEditMode
                    )
                }
            }
        }
    }
    
    // Remove apps dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Apps from Configuration") },
            text = { 
                Text("Are you sure you want to remove ${selectedCount} app(s) from '${configuration.name}'?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedPackages = configViewModel.getSelectedConfigApps().map { it.packageName }
                        configViewModel.removeAppsFromConfiguration(
                            config = configuration,
                            appsToRemove = selectedPackages,
                            onSuccess = {
                                showRemoveDialog = false
                                isEditMode = false
                                configViewModel.deselectAllApps()
                                Toast.makeText(context, "Apps removed from configuration", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Failed to remove apps: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConfigurationAppItem(
    app: AppConfiguration,
    isSelected: Boolean,
    onToggle: () -> Unit,
    isEditMode: Boolean
) {
    val context = LocalContext.current
    
    // Check if the app is still available on the device
    val isAppAvailable = remember(app.packageName) {
        try {
            context.packageManager.getPackageInfo(app.packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                onValueChange = { onToggle() },
                role = Role.Checkbox
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isEditMode) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            } else {
                if (!isAppAvailable) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        border = if (isEditMode && isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            val appIcon = remember(app.packageName) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (e: Exception) {
                    null
                }
            }
            
            Box {
                if (appIcon != null) {
                    AppIconImage(
                        appIconImage = appIcon,
                        contentDescription = app.appName
                    )
                } else {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = app.appName,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                // Show overlay if app is not available
                if (!isAppAvailable) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "App not available",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // App info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (!isAppAvailable) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!isAppAvailable) {
                        Text(
                            text = "Not Available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (!isAppAvailable) 0.5f else 1f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (app.description != null) {
                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (!isAppAvailable) 0.5f else 1f
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // System app indicator
                if (app.isSystemApp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (!isAppAvailable) 0.5f else 1f
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "System App",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (!isAppAvailable) 0.5f else 1f
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                enabled = isAppAvailable // Disable checkbox for unavailable apps
            )
        }
    }
} 