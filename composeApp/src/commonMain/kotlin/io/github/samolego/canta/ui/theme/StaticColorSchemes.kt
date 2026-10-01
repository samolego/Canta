package io.github.samolego.canta.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light palette
private val CantaRed40 = Color(0xFFC0161B)
private val CantaRedContainer90 = Color(0xFFFFDAD5)
private val CantaRedOnContainer10 = Color(0xFF410003)
private val CantaRose40 = Color(0xFF775652)
private val CantaRoseContainer90 = Color(0xFFF7DDDA)
private val CantaRoseOnContainer10 = Color(0xFF2C1512)
private val CantaGold40 = Color(0xFF705C0C)
private val CantaGoldContainer90 = Color(0xFFFBE087)
private val CantaGoldOnContainer10 = Color(0xFF231B00)
private val CantaLightSurface = Color(0xFFFFF8F7)
private val CantaLightOnSurface = Color(0xFF221918)

// Dark palette
private val CantaRed80 = Color(0xFFFFB3AC)
private val CantaOnRed20 = Color(0xFF68000A)
private val CantaRedContainer30 = Color(0xFF93000F)
private val CantaRose80 = Color(0xFFE7BDB8)
private val CantaOnRose20 = Color(0xFF442926)
private val CantaRoseContainer30 = Color(0xFF5D3F3C)
private val CantaGold80 = Color(0xFFDEC46E)
private val CantaOnGold20 = Color(0xFF3B2F00)
private val CantaGoldContainer30 = Color(0xFF554500)
private val CantaDarkSurface = Color(0xFF1A1110)
private val CantaDarkOnSurface = Color(0xFFF1DEDC)

internal val StaticDarkColorScheme = darkColorScheme(
    primary = CantaRed80,
    onPrimary = CantaOnRed20,
    primaryContainer = CantaRedContainer30,
    onPrimaryContainer = CantaRedContainer90,
    secondary = CantaRose80,
    onSecondary = CantaOnRose20,
    secondaryContainer = CantaRoseContainer30,
    onSecondaryContainer = CantaRoseContainer90,
    tertiary = CantaGold80,
    onTertiary = CantaOnGold20,
    tertiaryContainer = CantaGoldContainer30,
    onTertiaryContainer = CantaGoldContainer90,
    background = CantaDarkSurface,
    onBackground = CantaDarkOnSurface,
    surface = CantaDarkSurface,
    onSurface = CantaDarkOnSurface,
)

internal val StaticLightColorScheme = lightColorScheme(
    primary = CantaRed40,
    onPrimary = Color.White,
    primaryContainer = CantaRedContainer90,
    onPrimaryContainer = CantaRedOnContainer10,
    secondary = CantaRose40,
    onSecondary = Color.White,
    secondaryContainer = CantaRoseContainer90,
    onSecondaryContainer = CantaRoseOnContainer10,
    tertiary = CantaGold40,
    onTertiary = Color.White,
    tertiaryContainer = CantaGoldContainer90,
    onTertiaryContainer = CantaGoldOnContainer10,
    background = CantaLightSurface,
    onBackground = CantaLightOnSurface,
    surface = CantaLightSurface,
    onSurface = CantaLightOnSurface,
)
