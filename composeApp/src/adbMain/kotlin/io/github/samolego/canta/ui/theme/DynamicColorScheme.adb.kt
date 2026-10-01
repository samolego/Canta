package io.github.samolego.canta.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Desktop and web have no system dynamic colors. */
@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) StaticDarkColorScheme else StaticLightColorScheme
}