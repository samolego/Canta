package io.github.samolego.canta.ui.component.fab

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.ui.component.IconClickButton

@Composable
fun ExpandableFAB(
    onBottomClick: () -> Unit,
    onTopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val rotation by
            animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "rotation"
            )

    val spacingAnimation by
            animateDpAsState(
                    targetValue = if (isExpanded) 8.dp else 0.dp,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "spacing"
            )

    val actionButtonScale by
            animateFloatAsState(
                    targetValue = if (isExpanded) 1f else 0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "actionButtonScale"
            )

    Box(
        modifier = modifier.clip(
            shape = RoundedCornerShape(16.dp),
        ),
        contentAlignment = Alignment.BottomEnd,
        ) {
        Column(
                modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacingAnimation)
        ) {
            // Import button
            if (isExpanded) {
                IconClickButton(
                    onClick = {
                        onTopClick()
                        isExpanded = false
                    },
                    icon = Icons.Default.Download,
                    contentDescription = "Top click",
                    scale = actionButtonScale
                )
            }
            // Main FAB
            FloatingActionButton(
                modifier = Modifier.rotate(rotation),
                    onClick = {
                        if (isExpanded) {
                            onBottomClick()
                        }
                        isExpanded = !isExpanded
                    },
            ) {
                Icon(
                        Icons.Default.Add,
                        contentDescription = if (isExpanded) "Bottom click" else "More actions",
                )
            }
        }
    }
}
