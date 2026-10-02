package io.github.samolego.canta.ui.dialog

import androidx.compose.runtime.Composable
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform

/**
 * Desktop and web manage a remote device instead of using Shizuku, so there
 * is no USB-debugging step. Unreachable in practice: the setup dialog is only
 * shown when no device selection is required (Android).
 */
@Composable
actual fun ShizukuRequirementDialog(
    onClose: (shouldProceed: Boolean) -> Unit,
    handler: CantaHandler,
    platform: CantaPlatform,
) {
    ShizukuRequirementDialogContent(
        onClose = onClose,
        handler = handler,
        platform = platform,
    )
}
