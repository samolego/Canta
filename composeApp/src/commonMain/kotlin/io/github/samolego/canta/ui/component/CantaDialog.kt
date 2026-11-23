package io.github.samolego.canta.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.close
import org.jetbrains.compose.resources.stringResource

/** Width of dialogs with rich content (lists, requirement steps), as a fraction of the window. */
const val WIDE_DIALOG_WIDTH = 0.9f

/**
 * The dialog scaffold every Canta dialog uses: a rounded surface with an
 * optional [title], the [content], and a right-aligned row of [buttons]
 * (dismissive action first, confirming action last).
 *
 * @param widthFraction fraction of the window width, e.g. [WIDE_DIALOG_WIDTH];
 *   null uses the platform's default dialog width.
 * @param dismissible whether back/outside taps dismiss the dialog.
 * @param scrollable makes [content] scroll when it's taller than the window.
 *   Leave false for content that scrolls on its own (lazy lists).
 */
/**
 * How far content at the top of a dialog with a close button ([CantaDialog]'s onClose) must stay
 * from the sides to not run under it: the button reaches 28 dp into the content area.
 */
val CLOSE_BUTTON_CLEARANCE = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantaDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    widthFraction: Float? = null,
    dismissible: Boolean = true,
    scrollable: Boolean = false,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    /** When set, an X in the top right corner that calls it. */
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = if (widthFraction != null) Modifier.fillMaxWidth(widthFraction) else Modifier,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = widthFraction == null,
        ),
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
          Box {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                content()
                if (buttons != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        content = buttons,
                    )
                }
            }
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
                }
            }
          }
        }
    }
}
