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

    fun copyToClipboard(text: String)
    fun readClipboard(): String?
}
