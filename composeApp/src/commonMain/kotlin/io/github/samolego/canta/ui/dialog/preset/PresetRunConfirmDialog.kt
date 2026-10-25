package io.github.samolego.canta.ui.dialog.preset

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.run_preset_confirm
import io.github.samolego.canta.generated.resources.run_preset_confirm_self
import io.github.samolego.canta.generated.resources.run_preset_title
import io.github.samolego.canta.generated.resources.slide_to_confirm
import io.github.samolego.canta.ui.component.CantaDialog
import org.jetbrains.compose.resources.stringResource

private const val CONFIRM_THRESHOLD = 0.95f

/**
 * Confirmation shown before a preset runs from a shortcut. Confirming
 * requires dragging the slider all the way right, so a stray tap on the
 * launcher icon can't start a destructive run.
 */
@Composable
fun PresetRunConfirmDialog(
    preset: CantaPresetData,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var slide by remember { mutableFloatStateOf(0f) }
    CantaDialog(
        onDismissRequest = onCancel,
        title = stringResource(Res.string.run_preset_title, preset.name),
        buttons = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.cancel))
            }
        },
    ) {
        Text(
            text = stringResource(Res.string.run_preset_confirm, preset.apps.size, preset.folders.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (preset.folders.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                preset.folders.sorted().forEach { path ->
                    Text(text = path, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (preset.removeCantaAfterRun) {
            Text(
                text = stringResource(Res.string.run_preset_confirm_self),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            text = stringResource(Res.string.slide_to_confirm),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Slider(
            value = slide,
            onValueChange = { slide = it },
            onValueChangeFinished = {
                if (slide >= CONFIRM_THRESHOLD) onConfirm() else slide = 0f
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
