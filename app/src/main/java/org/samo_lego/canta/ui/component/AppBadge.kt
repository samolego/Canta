package org.samo_lego.canta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisabledByDefault
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.samo_lego.canta.util.RemovalRecommendation

@Composable
fun RemovalBadge(type: RemovalRecommendation) {
    AppBadge(
        label = type.name,
        icon = type.icon,
        color = type.badgeColor
    )
}

@Composable
fun SystemBadge() {
    RemovalBadge(type = RemovalRecommendation.SYSTEM)
}

@Composable
fun DisabledBadge() {
    AppBadge(
        label = "DISABLED",
        icon = Icons.Default.DisabledByDefault,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun AppBadge(
    label: String,
    icon: ImageVector,
    color: Color,
) {
    val contrastColor = color.getContrastColor()
    Row(
        modifier = Modifier
            .padding(all = 4.dp)
            .background(
                color,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Icon(
            icon,
            tint = contrastColor,
            modifier = Modifier
                .padding(start = 4.dp)
                .padding(vertical = 2.dp)
                .size(16.dp)
                .align(alignment = Alignment.CenterVertically),
            contentDescription = label,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(alignment = Alignment.CenterVertically),
            style = TextStyle(
                fontSize = 8.sp,
                color = contrastColor,
            )
        )
    }
}

private fun Color.getContrastColor(): Color {
    val luminance = (0.113 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

@Preview
@Composable
fun BadgePreviews() {
    Column {
        for (removal in RemovalRecommendation.entries) {
            RemovalBadge(removal)
        }
        DisabledBadge()
    }
}