package io.github.samolego.canta.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns a platform dynamic color scheme (Material You on Android 12+),
 * or the static fallback schemes on platforms without dynamic color.
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme