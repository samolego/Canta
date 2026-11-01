package io.github.samolego.canta

/**
 * Contract between [io.github.samolego.canta.core.CantaPlatform.installPresetShortcut]
 * and `PresetRunActivity`, which lives in the `androidApp` module and is
 * therefore referenced by class name, not by type.
 */
object PresetRunContract {
    const val ACTION_RUN_PRESET = "io.github.samolego.canta.action.RUN_PRESET"
    const val EXTRA_PRESET_UUID = "io.github.samolego.canta.extra.PRESET_UUID"
    const val RUN_ACTIVITY_CLASS = "io.github.samolego.canta.PresetRunActivity"
    const val SHORTCUT_ID_PREFIX = "run-preset-"
}
