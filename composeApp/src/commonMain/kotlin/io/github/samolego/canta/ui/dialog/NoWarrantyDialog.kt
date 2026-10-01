package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.disclaimer
import io.github.samolego.canta.generated.resources.never_show_again
import io.github.samolego.canta.generated.resources.no_warranty_content
import io.github.samolego.canta.generated.resources.proceed
import io.github.samolego.canta.generated.resources.proceed_at_own_risk
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.CheckboxRow
import org.jetbrains.compose.resources.stringResource

/** Startup disclaimer; cancelling closes the app. */
@Composable
fun NoWarrantyDialog(
    onProceed: (neverShowAgain: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var neverShowAgain by rememberSaveable { mutableStateOf(false) }

    CantaDialog(
        onDismissRequest = onCancel,
        title = stringResource(Res.string.disclaimer),
        dismissible = false,
        scrollable = true,
        buttons = {
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            TextButton(onClick = { onProceed(neverShowAgain) }) { Text(stringResource(Res.string.proceed)) }
        },
    ) {
        Text(text = stringResource(Res.string.no_warranty_content), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.proceed_at_own_risk),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        CheckboxRow(
            checked = neverShowAgain,
            onCheckedChange = { neverShowAgain = it },
            label = stringResource(Res.string.never_show_again),
        )
    }
}
