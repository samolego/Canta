package org.samo_lego.canta.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun IconClickButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}
