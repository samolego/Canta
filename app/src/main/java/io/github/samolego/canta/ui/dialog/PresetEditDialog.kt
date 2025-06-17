package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.util.CantaPreset

@Composable
private fun PresetDialog(
        initialName: String,
        initialDescription: String,
        onDismiss: () -> Unit,
        onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text = "Create Configuration",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                            text =
                                    "Create a configuration from currently uninstalled apps. This will save a list of apps that can be shared and applied on other devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = it.isBlank()
                            },
                            label = { Text("Configuration Name") },
                            placeholder = { Text("e.g., Samsung Bloatware") },
                            isError = nameError,
                            supportingText =
                                    if (nameError) {
                                        { Text("Name is required") }
                                    } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )

                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            placeholder = { Text("Describe what this configuration removes...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), description.trim())
                            } else {
                                nameError = true
                            }
                        },
                        enabled = name.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PresetCreateDialog(
        appListViewModel: AppListViewModel,
        presetViewModel: PresetsViewModel,
        closeDialog: () -> Unit,
        onError: (String) -> Unit,
) {
    PresetDialog(
            initialName = "",
            initialDescription = "",
            onDismiss = closeDialog,
            onConfirm = { name, description ->
                presetViewModel.savePreset(
                        name = name,
                        description = description,
                        apps = appListViewModel.selectedApps.keys,
                        onSuccess = { closeDialog() },
                        onError = onError,
                )
            }
    )
}

@Composable
fun PresetEditDialog(
        preset: CantaPreset,
        presetViewModel: PresetsViewModel,
        closeDialog: () -> Unit,
) {
    PresetDialog(
            initialName = preset.name,
            initialDescription = preset.description,
            onDismiss = closeDialog,
            onConfirm = { name, description ->
                presetViewModel.updatePreset(
                        oldPreset = preset,
                        newName = name,
                        newDescription = description,
                        onSuccess = { closeDialog() },
                        onError = { _ ->
                            // Error is already logged in the ViewModel
                        },
                )
            }
    )
}
