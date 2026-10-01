package io.github.samolego.canta.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A card with a [title]/[subtitle] header that expands to show [content]. */
@Composable
fun Dropdown(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colors = CardDefaults.cardColors(containerColor = backgroundColor)

    Column(modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = colors) {
            SettingsItem(title = title, description = subtitle, trailing = { ExpandIcon(expanded) })
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = colors) {
                Column { content() }
            }
        }
    }
}
