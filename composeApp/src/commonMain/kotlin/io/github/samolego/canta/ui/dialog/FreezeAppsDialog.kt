package io.github.samolego.canta.ui.dialog

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.are_you_sure_to_freeze_apps
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.ok
import io.github.samolego.canta.ui.component.CantaDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun FreezeAppsDialog(
    appCount: Int,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
) {
    CantaDialog(
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            TextButton(onClick = onAgree) { Text(stringResource(Res.string.ok)) }
        },
    ) {
        Text(stringResource(Res.string.are_you_sure_to_freeze_apps, appCount))
    }
}
