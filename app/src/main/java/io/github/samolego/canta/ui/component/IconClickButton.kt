package io.github.samolego.canta.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconClickButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    scale: Float = 1f
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size((48 * scale).dp),
    ) {
        Icon(
            modifier = modifier.size((24 * scale).dp).then(Modifier.size(48.dp)),
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}
