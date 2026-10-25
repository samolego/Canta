package io.github.samolego.canta.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Abstraction over small, UI-facing platform capabilities so the shared UI
 * can remain target-agnostic. Each platform provides its own implementation.
 */
interface CantaPlatform {
    /**
     * Show a transient short message. Android shows a Toast; platforms without
     * one emit it on [messages] instead, which the app shows as a snackbar.
     */
    fun showMessage(message: String)

    /** Messages to show as snackbars; empty where [showMessage] shows them itself. */
    val messages: Flow<String>
        get() = emptyFlow()

    /** Open a URL in the platform's default browser. */
    fun openUrl(url: String)

    /**
     * Request biometric/device-credential authentication before an action.
     * [title] and [subtitle] are shown by the system prompt; platforms
     * without biometrics should invoke [onSuccess] immediately.
     */
    fun requireBiometric(title: String, subtitle: String, onSuccess: () -> Unit)

    /** Copies plain text to the platform clipboard. */
    fun copyToClipboard(text: String)
    /** Returns clipboard text, or null when clipboard access is unavailable. */
    fun readClipboard(): String?

    /**
     * Whether the platform can install a shortcut that runs a preset without
     * opening the UI — a pinned launcher shortcut on Android, a `.desktop`
     * entry on desktop.
     */
    val supportsPresetShortcuts: Boolean
        get() = false

    /**
     * Installs a shortcut that runs the preset identified by [presetUuid]
     * headlessly. Returns whether the shortcut was created/requested.
     */
    fun installPresetShortcut(presetName: String, presetUuid: String): Boolean = false

    /**
     * Whether preset folder deletion still needs a storage permission the
     * user has not granted ("All files access" on Android). False everywhere
     * else — desktops delete through ADB, which needs no such grant.
     */
    val needsAllFilesAccess: Boolean
        get() = false

    /** Opens the system flow where the user grants the storage permission. */
    fun requestAllFilesAccess() {}

    /** Whether [pickFolder] can show a system folder chooser. */
    val supportsFolderPicker: Boolean
        get() = false

    /**
     * Opens the platform folder chooser; [onPicked] receives a device path
     * suitable for a preset's folder list, or null if cancelled/unavailable.
     */
    fun pickFolder(onPicked: (String?) -> Unit) {
        onPicked(null)
    }
}
