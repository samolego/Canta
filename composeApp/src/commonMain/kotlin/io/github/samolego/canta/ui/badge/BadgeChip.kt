package io.github.samolego.canta.ui.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

@Composable
fun BadgeChip(badge: Badge, modifier: Modifier = Modifier) {
    val color = badge.color()
    val label = stringResource(badge.label)
    val contrastColor = color.getContrastColor()
    Row(
        modifier = modifier
            .padding(all = 4.dp)
            .background(color, shape = RoundedCornerShape(16.dp))
    ) {
        Icon(
            badge.icon,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeRow(badges: List<Badge>) {
    FlowRow {
        badges.forEach { BadgeChip(it) }
    }
}

/** Black text on light colors (yellows, greens), white on dark ones (reds, magentas, grays). */
private fun Color.getContrastColor(): Color = if (luminance() > 0.4f) Color.Black else Color.White
