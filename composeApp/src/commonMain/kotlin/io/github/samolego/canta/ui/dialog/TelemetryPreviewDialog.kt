package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.close
import io.github.samolego.canta.generated.resources.whats_sent_description
import io.github.samolego.canta.generated.resources.whats_sent_title
import io.github.samolego.canta.ui.component.CantaDialog
import org.jetbrains.compose.resources.stringResource

/** Example of what package analytics send, so users can judge before opting in. */
const val EXAMPLE_TELEMETRY_JSON =
    """{"v":1,"platform":"android","device":{"brand":"xiaomi","rom":"hyperos","romVersion":1,"sdk":34},"events":[{"e":"uninstalled","p":"com.example.bloat","w":"2026-10-26","b":"11+"}]}"""

/** Shows an example analytics payload. */
@Composable
fun TelemetryPreviewDialog(onDismiss: () -> Unit) {
    CantaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.whats_sent_title),
        scrollable = true,
        buttons = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    ) {
        Text(text = stringResource(Res.string.whats_sent_description), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
            SelectionContainer {
                Text(
                    text = EXAMPLE_TELEMETRY_JSON,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
