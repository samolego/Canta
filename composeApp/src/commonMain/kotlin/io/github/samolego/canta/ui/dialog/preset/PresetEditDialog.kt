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
import io.github.samolego.canta.generated.resources.optional_description
import io.github.samolego.canta.generated.resources.preset_description_placeholder
import io.github.samolego.canta.generated.resources.preset_name
import io.github.samolego.canta.generated.resources.preset_name_missing_error
import io.github.samolego.canta.generated.resources.preset_name_placeholder
import io.github.samolego.canta.generated.resources.preset_save_error
import io.github.samolego.canta.generated.resources.save
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.ui.viewmodel.PresetsViewModel
import io.github.samolego.canta.data.preset.CantaPresetData

@Composable
private fun PresetDialog(
    title: String,
    description: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var presetDescription by remember { mutableStateOf(initialDescription) }
    var nameError by remember { mutableStateOf(false) }

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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), presetDescription.trim()) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

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
        onDismiss = closeDialog,
        onConfirm = { name, description ->
            presetViewModel.savePreset(
                name = name,
                description = description,
                // All uninstalled apps, not just the ones the current search/filter shows.
                apps = appListViewModel.apps.filter { it.isUninstalled }.mapTo(mutableSetOf()) { it.packageName },
                onSuccess = { closeDialog() },
                onError = { platform.showMessage(presetSaveErrorText) },
            )
        }
    )
}

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
        onDismiss = closeDialog,
        onConfirm = { name, description ->
            presetViewModel.updatePreset(
                oldPreset = preset,
                newName = name,
                newDescription = description,
                onSuccess = { closeDialog() },
                onError = { platform.showMessage(presetSaveErrorText) },
            )
        }
    )
}
