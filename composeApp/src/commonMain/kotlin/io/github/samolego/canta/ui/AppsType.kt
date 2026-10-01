package io.github.samolego.canta.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.samolego.canta.data.app.AppInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.tab_installed
import io.github.samolego.canta.generated.resources.tab_uninstalled
import org.jetbrains.compose.resources.StringResource

/** The two app tabs: installed apps (to uninstall) and uninstalled ones (to reinstall). */
enum class AppsType(val icon: ImageVector, val label: StringResource) {
    INSTALLED(Icons.Default.AutoDelete, Res.string.tab_installed),
    UNINSTALLED(Icons.Default.DeleteForever, Res.string.tab_uninstalled);

    fun matches(app: AppInfo): Boolean = when (this) {
        INSTALLED -> !app.isUninstalled
        UNINSTALLED -> app.isUninstalled
    }
}
