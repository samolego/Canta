package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.data.app.AppBadgeInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.got_it
import io.github.samolego.canta.ui.component.AppBadge
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.WIDE_DIALOG_WIDTH
import org.jetbrains.compose.resources.stringResource

/** Explains what each bloat-list badge means. */
@Composable
fun ExplainBadgesDialog(
    onDismissRequest: () -> Unit
) {
    CantaDialog(
        onDismissRequest = onDismissRequest,
        widthFraction = WIDE_DIALOG_WIDTH,
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.got_it)) }
        },
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            items(AppBadgeInfo.entries) { badgeInfo ->
                Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
                    Box(modifier = Modifier.weight(2f)) { AppBadge(type = badgeInfo) }
                    Text(
                        badgeInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(3f)
                    )
                }
            }
        }
    }
}
