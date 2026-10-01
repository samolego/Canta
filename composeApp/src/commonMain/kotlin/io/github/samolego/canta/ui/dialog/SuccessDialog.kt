package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.canta_donate_request
import io.github.samolego.canta.generated.resources.donate
import io.github.samolego.canta.generated.resources.success
import io.github.samolego.canta.generated.resources.success_reinstalled
import io.github.samolego.canta.generated.resources.success_uninstalled
import io.github.samolego.canta.ui.component.CantaDialog
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val DONATE_URL = "https://www.paypal.com/donate/?hosted_button_id=FD4R46ZZ5EWME"

@Composable
fun SuccessDialog(
    platform: CantaPlatform,
    count: Int,
    isReinstall: Boolean = false,
    onDismissRequest: () -> Unit
) {
    CantaDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.success, "🎉"),
        dismissible = false,
        scrollable = true,
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
            Button(
                onClick = {
                    onDismissRequest()
                    platform.openUrl(DONATE_URL)
                }
            ) {
                Text(stringResource(Res.string.donate))
                // Decorative: the button's text already says "Donate".
                Icon(Icons.Default.Euro, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(12.dp))
            }
        },
    ) {
        Text(
            text = pluralStringResource(
                if (isReinstall) Res.plurals.success_reinstalled else Res.plurals.success_uninstalled,
                count,
                count
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(Res.string.canta_donate_request), style = MaterialTheme.typography.bodyMedium)
    }
}
