package io.github.samolego.canta.core

import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * What the desktop and web [CantaPlatform]s share: messages are shown as
 * snackbars (there are no toasts), and there is no biometric prompt.
 */
abstract class AdbCantaPlatform(private val tag: String) : CantaPlatform {

    private val snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    final override val messages: Flow<String> = snackbarMessages

    final override fun showMessage(message: String) {
        LogUtils.i(tag, message)
        snackbarMessages.tryEmit(message)
    }

    /** No biometric prompt on these platforms: the action runs directly. */
    final override fun requireBiometric(title: String, subtitle: String, onSuccess: () -> Unit) = onSuccess()
}
