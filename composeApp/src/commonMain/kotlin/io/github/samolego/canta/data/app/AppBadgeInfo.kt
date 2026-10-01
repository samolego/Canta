package io.github.samolego.canta.data.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.samolego.canta.data.bloat.BloatData

/**
 * Badge info for an app, sourced from the UAD list ([BloatData.badgeInfo])
 * or local fallbacks. [SYSTEM] is a local badge (not a UAD removal level)
 * and the base for future badges.
 */
enum class AppBadgeInfo(
    val icon: ImageVector,
    val badgeColor: Color,
    val description: String
) {
    RECOMMENDED(
        Icons.Default.Check,
        Color.Green,
        "Pointless or outright negative packages, and/or apps available through Google Play."
    ),
    ADVANCED(
        Icons.Default.Settings,
        Color.Yellow,
        "Breaks obscure or minor parts of functionality, or apps that aren't easily enabled/installed through Settings/Google Play. This category is also used for apps that are useful (default keyboard/gallery/launcher/music app.) but that can easily be replaced by a better alternative."
    ),
    EXPERT(
        Icons.Default.Warning,
        Color.Red,
        "Breaks widespread and/or important functionality, but nothing important to the basic operation of the operating system. Removing an 'Expert' package should not bootloop the device (unless mentioned in the description) but we can't guarantee it 100%."
    ),
    UNSAFE(
        Icons.Default.Close,
        Color.Magenta,
        "Can break vital parts of the operating system. Removing an 'Unsafe' package have an extremely high risk of bootlooping your device."
    ),
    SYSTEM(
        Icons.Default.Android,
        Color.DarkGray,
        "System apps are apps that come pre-installed with your device."
    );

    companion object {
        fun byNameIgnoreCaseOrNull(input: String): AppBadgeInfo? {
            return entries.firstOrNull { it.name.equals(input, true) }
        }
    }
}
