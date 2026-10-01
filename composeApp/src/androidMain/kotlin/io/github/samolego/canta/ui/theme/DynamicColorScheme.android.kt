package io.github.samolego.canta.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    // Material You dynamic color exists only on Android 12+ (API 31).
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) StaticDarkColorScheme else StaticLightColorScheme
    }
}