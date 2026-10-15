package io.github.samolego.canta.ui.dialog

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.are_you_sure_to_uninstall_apps
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.ok
import io.github.samolego.canta.generated.resources.reset_to_factory_version
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.CheckboxRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun UninstallAppsDialog(
    appCount: Int,
    canResetToFactory: Boolean = false,
    onDismiss: () -> Unit,
    onAgree: (resetToFactory: Boolean) -> Unit,
) {
    var resetToFactory by rememberSaveable { mutableStateOf(false) }

    CantaDialog(
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            TextButton(onClick = { onAgree(resetToFactory) }) { Text(stringResource(Res.string.ok)) }
        },
    ) {
        Text(stringResource(Res.string.are_you_sure_to_uninstall_apps, appCount))
        if (canResetToFactory) {
            CheckboxRow(
                checked = resetToFactory,
                onCheckedChange = { resetToFactory = it },
                label = stringResource(Res.string.reset_to_factory_version),
            )
        }
    }
}
