package io.github.samolego.canta.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Button content with a leading icon, sized and spaced per Material. The icon
 * is decorative: the [text] already labels the button for screen readers.
 */
@Composable
fun RowScope.IconText(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
    Text(text)
}
