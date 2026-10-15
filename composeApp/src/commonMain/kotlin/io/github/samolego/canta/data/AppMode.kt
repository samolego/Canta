package io.github.samolego.canta.data

/**
 * Operating mode for Canta:
 * - [UNINSTALLER]: Default uninstaller mode showing Installed and Uninstalled apps.
 * - [FREEZE]: Freeze mode showing Enabled and Frozen (disabled) apps.
 */
enum class AppMode(val id: Int) {
    UNINSTALLER(0),
    FREEZE(1);

    companion object {
        fun fromId(id: Int): AppMode = entries.firstOrNull { it.id == id } ?: UNINSTALLER
    }
}
