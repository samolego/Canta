package io.github.samolego.canta.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.samolego.canta.data.AppMode
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.tab_disabled
import io.github.samolego.canta.generated.resources.tab_enabled
import io.github.samolego.canta.generated.resources.tab_installed
import io.github.samolego.canta.generated.resources.tab_uninstalled
import org.jetbrains.compose.resources.StringResource

/** The app tabs: installed/uninstalled in uninstaller mode, enabled/disabled in freeze mode. */
enum class AppsType(val icon: ImageVector, val label: StringResource) {
    INSTALLED(Icons.Default.AutoDelete, Res.string.tab_installed),
    UNINSTALLED(Icons.Default.DeleteForever, Res.string.tab_uninstalled),
    ENABLED(Icons.Default.CheckCircle, Res.string.tab_enabled),
    DISABLED(Icons.Default.AcUnit, Res.string.tab_disabled);

    fun matches(app: AppInfo): Boolean = when (this) {
        INSTALLED -> !app.isUninstalled
        UNINSTALLED -> app.isUninstalled
        ENABLED -> !app.isUninstalled && !app.isDisabled
        DISABLED -> !app.isUninstalled && app.isDisabled
    }
}

/** Returns the active tabs for the given [AppMode]. */
fun AppMode.tabs(): List<AppsType> = when (this) {
    AppMode.UNINSTALLER -> listOf(AppsType.INSTALLED, AppsType.UNINSTALLED)
    AppMode.FREEZE -> listOf(AppsType.ENABLED, AppsType.DISABLED)
}
