package io.github.samolego.canta.ui.dialog.preset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.create_preset
import io.github.samolego.canta.generated.resources.create_preset_description
import io.github.samolego.canta.generated.resources.edit_preset
import io.github.samolego.canta.generated.resources.edit_preset_description
import io.github.samolego.canta.generated.resources.files_access_body
import io.github.samolego.canta.generated.resources.files_access_title
import io.github.samolego.canta.generated.resources.folders_to_delete
import io.github.samolego.canta.generated.resources.folders_to_delete_hint
import io.github.samolego.canta.generated.resources.invalid_folder_paths
import io.github.samolego.canta.generated.resources.later
import io.github.samolego.canta.generated.resources.open_settings
import io.github.samolego.canta.generated.resources.optional_description
import io.github.samolego.canta.generated.resources.preset_description_placeholder
import io.github.samolego.canta.generated.resources.preset_name
import io.github.samolego.canta.generated.resources.preset_name_missing_error
import io.github.samolego.canta.generated.resources.preset_name_placeholder
import io.github.samolego.canta.generated.resources.preset_save_error
import io.github.samolego.canta.generated.resources.pick_folder
import io.github.samolego.canta.generated.resources.remove_canta_after_run
import io.github.samolego.canta.generated.resources.save
import io.github.samolego.canta.packages.isSafeDeletionPath
import io.github.samolego.canta.ui.component.CheckboxRow
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.data.preset.CantaPresetData

/** Edits preset metadata and deletion options, validating paths before calling [onConfirm]. */
@Composable
private fun PresetDialog(
    title: String,
    description: String,
    initialName: String,
    initialDescription: String,
    initialFolders: Set<String>,
    initialRemoveCanta: Boolean,
    platform: CantaPlatform,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, folders: Set<String>, removeCanta: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var presetDescription by remember { mutableStateOf(initialDescription) }
    var foldersText by remember { mutableStateOf(initialFolders.sorted().joinToString("\n")) }
    var removeCanta by remember { mutableStateOf(initialRemoveCanta) }
    var nameError by remember { mutableStateOf(false) }
    var invalidPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var filesPromptShown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(Res.string.preset_name)) },
                    placeholder = { Text(stringResource(Res.string.preset_name_placeholder)) },
                    isError = nameError,
                    supportingText =
                    if (nameError) {
                        { Text(stringResource(Res.string.preset_name_missing_error)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = presetDescription,
                    onValueChange = { presetDescription = it },
                    label = { Text(stringResource(Res.string.optional_description)) },
                    placeholder = { Text(stringResource(Res.string.preset_description_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = foldersText,
                    onValueChange = {
                        foldersText = it
                        invalidPaths = emptyList()
                    },
                    label = { Text(stringResource(Res.string.folders_to_delete)) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        // Asking for a folder for deletion needs "All files
                        // access": prompt the grant on the first attempt.
                        if (it.isFocused && !filesPromptShown && platform.needsAllFilesAccess) {
                            filesPromptShown = true
                        }
                    },
                    isError = invalidPaths.isNotEmpty(),
                    supportingText = {
                        Text(
                            if (invalidPaths.isNotEmpty()) {
                                stringResource(Res.string.invalid_folder_paths, invalidPaths.joinToString(", "))
                            } else {
                                stringResource(Res.string.folders_to_delete_hint)
                            }
                        )
                    },
                    minLines = 2,
                    maxLines = 4
                )

                if (platform.supportsFolderPicker) {
                    TextButton(
                        onClick = {
                            if (platform.needsAllFilesAccess) {
                                filesPromptShown = true
                            } else {
                                platform.pickFolder { picked ->
                                    picked?.let {
                                        foldersText = (foldersText.trimEnd() + "\n" + it).trimStart('\n')
                                    }
                                }
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.pick_folder))
                    }
                }

                CheckboxRow(
                    checked = removeCanta,
                    onCheckedChange = { removeCanta = it },
                    label = stringResource(Res.string.remove_canta_after_run),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lines = foldersText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    invalidPaths = lines.filterNot { isSafeDeletionPath(it) }
                    if (invalidPaths.isEmpty()) {
                        onConfirm(name.trim(), presetDescription.trim(), lines.toSet(), removeCanta)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )

    if (filesPromptShown && platform.needsAllFilesAccess) {
        AlertDialog(
            onDismissRequest = { filesPromptShown = false },
            title = { Text(stringResource(Res.string.files_access_title)) },
            text = { Text(stringResource(Res.string.files_access_body)) },
            confirmButton = {
                Button(onClick = {
                    filesPromptShown = false
                    platform.requestAllFilesAccess()
                }) {
                    Text(stringResource(Res.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { filesPromptShown = false }) {
                    Text(stringResource(Res.string.later))
                }
            }
        )
    }
}

/** Creates a preset from all currently uninstalled apps and the selected deletion options. */
@Composable
fun PresetCreateDialog(
    platform: CantaPlatform,
    appListViewModel: AppListViewModel,
    presetViewModel: PresetsViewModel,
    closeDialog: () -> Unit,
) {
    val presetSaveErrorText = stringResource(Res.string.preset_save_error)
    PresetDialog(
        title = stringResource(Res.string.create_preset),
        description = stringResource(Res.string.create_preset_description),
        initialName = "",
        initialDescription = "",
        initialFolders = emptySet(),
        initialRemoveCanta = false,
        platform = platform,
        onDismiss = closeDialog,
        onConfirm = { name, description, folders, removeCanta ->
            presetViewModel.savePreset(
                name = name,
                description = description,
                // All uninstalled apps, not just the ones the current search/filter shows.
                apps = appListViewModel.apps.filter { it.isUninstalled }.mapTo(mutableSetOf()) { it.packageName },
                folders = folders,
                removeCantaAfterRun = removeCanta,
                onSuccess = { closeDialog() },
                onError = { platform.showMessage(presetSaveErrorText) },
            )
        }
    )
}

/** Edits preset metadata and deletion options without replacing its app selection. */
@Composable
fun PresetEditDialog(
    platform: CantaPlatform,
    preset: CantaPresetData,
    presetViewModel: PresetsViewModel,
    closeDialog: () -> Unit,
) {
    val presetSaveErrorText = stringResource(Res.string.preset_save_error)
    PresetDialog(
        title = stringResource(Res.string.edit_preset),
        description = stringResource(Res.string.edit_preset_description),
        initialName = preset.name,
        initialDescription = preset.description,
        initialFolders = preset.folders,
        initialRemoveCanta = preset.removeCantaAfterRun,
        platform = platform,
        onDismiss = closeDialog,
        onConfirm = { name, description, folders, removeCanta ->
            presetViewModel.updatePreset(
                oldPreset = preset,
                newName = name,
                newDescription = description,
                newFolders = folders,
                newRemoveCantaAfterRun = removeCanta,
                onSuccess = { closeDialog() },
                onError = { platform.showMessage(presetSaveErrorText) },
            )
        }
    )
}
