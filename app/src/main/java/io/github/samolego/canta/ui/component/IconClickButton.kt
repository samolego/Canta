package io.github.samolego.canta.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun IconClickButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            modifier = modifier,
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}
