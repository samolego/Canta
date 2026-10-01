package io.github.samolego.canta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CantaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        // Material You colors where the platform has them (Android 12+).
        colorScheme = dynamicColorScheme(darkTheme = darkTheme),
        typography = Typography,
        content = content
    )
}
