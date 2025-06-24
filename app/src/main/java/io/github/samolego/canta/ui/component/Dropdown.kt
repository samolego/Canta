package io.github.samolego.canta.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Dropdown(
    header: @Composable (Boolean) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    headerBackgroundColor: Color = MaterialTheme.colorScheme.background,
    contentBackgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val onExpansionChange = onExpandedChange ?: { internalExpanded = it }

    Column(modifier = modifier) {
        Card(
                modifier = Modifier.fillMaxWidth().clickable { onExpansionChange(!isExpanded) },
                colors = CardDefaults.cardColors(containerColor = headerBackgroundColor)
        ) { header(isExpanded) }

        AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = contentBackgroundColor)
            ) { Column { content() } }
        }
    }
}

@Composable
fun DropdownHeader(
        title: String,
        subtitle: String? = null,
        showExpandIcon: Boolean = true,
        expanded: Boolean = false,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit = {}
) {
    Row(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) {
                androidx.compose.material3.Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        content()

        if (showExpandIcon) {
            Icon(
                    imageVector =
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    }
}
