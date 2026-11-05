package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.contribute
import io.github.samolego.canta.generated.resources.onboarding_contribute_intro
import io.github.samolego.canta.generated.resources.onboarding_ip_note
import io.github.samolego.canta.generated.resources.onboarding_never_sent
import io.github.samolego.canta.generated.resources.onboarding_never_sent_title
import io.github.samolego.canta.generated.resources.share_package_data_confirm_title
import io.github.samolego.canta.ui.component.CantaDialog
import org.jetbrains.compose.resources.stringResource

/** Confirms turning package analytics on, repeating what is and isn't sent. */
@Composable
fun ShareDataConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CantaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.share_package_data_confirm_title),
        scrollable = true,
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.contribute)) }
        },
    ) {
        Text(text = stringResource(Res.string.onboarding_contribute_intro), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.onboarding_never_sent_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(text = stringResource(Res.string.onboarding_never_sent), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(Res.string.onboarding_ip_note), style = MaterialTheme.typography.bodyMedium)
    }
}
