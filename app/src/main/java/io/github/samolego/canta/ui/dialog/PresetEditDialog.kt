package io.github.samolego.canta.ui.dialog

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.R
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
                text = stringResource(R.string.create_preset),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.create_preset_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.preset_name)) },
                    placeholder = { Text(stringResource(R.string.preset_name_placeholder)) },
                    isError = nameError,
                    supportingText =
                    if (nameError) {
                        { Text(stringResource(R.string.preset_name_missing_error)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.optional_description)) },
                    placeholder = { Text(stringResource(R.string.preset_description_placeholder)) },
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
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun PresetCreateDialog(
    appListViewModel: AppListViewModel,
    presetViewModel: PresetsViewModel,
    closeDialog: () -> Unit,
) {
    val context = LocalContext.current
    val presetSaveErrorText = stringResource(R.string.preset_save_error)
    PresetDialog(
        initialName = "",
        initialDescription = "",
        onDismiss = closeDialog,
        onConfirm = { name, description ->
            presetViewModel.savePreset(
                name = name,
                description = description,
                apps = appListViewModel.appList.filter { it.isUninstalled }.map { it.packageName }
                    .toSet(),
                onSuccess = { closeDialog() },
                onError = {
                    Toast.makeText(
                        context,
                        presetSaveErrorText,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                },
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
    val context = LocalContext.current
    val presetSaveErrorText = stringResource(R.string.preset_save_error)
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
                onError = {
                    Toast.makeText(
                        context,
                        presetSaveErrorText,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                },
            )
        }
    )
}
