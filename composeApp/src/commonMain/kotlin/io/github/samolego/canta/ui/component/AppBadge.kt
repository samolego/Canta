package io.github.samolego.canta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisabledByDefault
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import io.github.samolego.canta.APP_NAME
import io.github.samolego.canta.data.app.AppBadgeInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.badge_disabled
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBadge(type: AppBadgeInfo) {
    AppBadgeContent(
        label = type.name,
        icon = type.icon,
        color = type.badgeColor
    )
}

@Composable
fun SystemBadge() {
    AppBadge(type = AppBadgeInfo.SYSTEM)
}

@Composable
fun DisabledBadge() {
    AppBadgeContent(
        label = stringResource(Res.string.badge_disabled),
        icon = Icons.Default.DisabledByDefault,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
fun CantaBadge() {
    AppBadgeContent(
        label = APP_NAME.uppercase(),
        icon = Icons.Default.RestoreFromTrash,
        color = Color.Red.copy(alpha = 0.7f),
    )
}

@Composable
private fun AppBadgeContent(
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

private fun Color.getContrastColor(): Color = if (luminance() > 0.5f) Color.Black else Color.White
