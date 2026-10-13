package io.github.samolego.canta.ui.dialog

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.samolego.canta.core.CantaHandler
import io.github.samolego.canta.core.CantaPlatform
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.enable_usb_debugging
import io.github.samolego.canta.util.LogUtils
import org.jetbrains.compose.resources.stringResource

private const val TAG = "ShizukuRequirementDialog"

@Composable
actual fun ShizukuRequirementDialog(
    onClose: (shouldProceed: Boolean) -> Unit,
    handler: CantaHandler,
    platform: CantaPlatform,
) {
    val context = LocalContext.current
    val isUsbDebuggingEnabled = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    } catch (e: Exception) {
        false
    }
    ShizukuRequirementDialogContent(
        onClose = onClose,
        handler = handler,
        platform = platform,
        usbRequirementRow = {
            // Prompt the user to enable USB debugging just in case,
            // if they'll need to rescue their device with a PC if
            // something goes wrong (#284).
            RequirementItem(
                text = stringResource(Res.string.enable_usb_debugging),
                isCompleted = isUsbDebuggingEnabled,
                enabled = false,
                onActionClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    } catch (e: Exception) {
                        LogUtils.e(TAG, "Failed to open development settings", e)
                    }
                },
            )
        },
    )
}
