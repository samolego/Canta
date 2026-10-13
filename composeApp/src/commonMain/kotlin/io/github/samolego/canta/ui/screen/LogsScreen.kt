package io.github.samolego.canta.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.copy_logs
import io.github.samolego.canta.generated.resources.log_copied
import io.github.samolego.canta.generated.resources.logs
import io.github.samolego.canta.ui.component.ScreenTopBar
import io.github.samolego.canta.util.LogUtils

@Composable
fun LogsScreen(
    platform: CantaPlatform,
    onNavigateBack: () -> Unit,
) {
    val logs = LogUtils.getLogs()
    val logsCopiedText = stringResource(Res.string.log_copied)

    Scaffold(
        topBar = {
            ScreenTopBar(onNavigateBack = onNavigateBack, title = { Text(stringResource(Res.string.logs)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    platform.copyToClipboard(logs.joinToString("\n") { it.format() })
                    platform.showMessage(logsCopiedText)
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(Res.string.copy_logs)
                )
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(logs) { logEntry -> LogEntryChip(logEntry, platform) }
        }
    }
}

@Composable
private fun LogEntryChip(logEntry: LogUtils.LogEntry, platform: CantaPlatform) {
    var expanded by remember { mutableStateOf(false) }
    val logCopiedText = stringResource(Res.string.log_copied)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .padding(horizontal = 8.dp)
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    platform.copyToClipboard(logEntry.format())
                    platform.showMessage(logCopiedText)
                }
            ),
        color = logEntry.level.color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${logEntry.level} ${logEntry.tag}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = logEntry.getFormattedTime(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = logEntry.message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
