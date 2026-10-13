package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.core.CantaDevice
import io.github.samolego.canta.core.DeviceConnectionFailure
import io.github.samolego.canta.core.DeviceDiscovery
import io.github.samolego.canta.core.UsbAccessHint
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.helper_notice
import io.github.samolego.canta.generated.resources.close
import io.github.samolego.canta.generated.resources.device_access_denied
import io.github.samolego.canta.generated.resources.device_access_denied_linux
import io.github.samolego.canta.generated.resources.device_access_denied_windows
import io.github.samolego.canta.generated.resources.device_android_too_old
import io.github.samolego.canta.generated.resources.device_busy
import io.github.samolego.canta.generated.resources.device_busy_linux
import io.github.samolego.canta.generated.resources.device_connection_failed
import io.github.samolego.canta.generated.resources.device_no_longer_available
import io.github.samolego.canta.generated.resources.helper_unavailable
import io.github.samolego.canta.generated.resources.no_devices_found
import io.github.samolego.canta.generated.resources.pick_device
import io.github.samolego.canta.generated.resources.pick_device_description
import io.github.samolego.canta.generated.resources.refresh
import io.github.samolego.canta.generated.resources.select_device
import io.github.samolego.canta.generated.resources.select_device_description
import io.github.samolego.canta.generated.resources.webusb_not_supported_description
import io.github.samolego.canta.generated.resources.webusb_not_supported_title
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.SettingsItem
import io.github.samolego.canta.ui.component.WIDE_DIALOG_WIDTH
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Localizes a connection failure for display. Access-denied gets actionable
 * guidance; anything else shows the technical detail for diagnosis.
 */
internal suspend fun DeviceConnectionFailure.message(): String =
    when (this) {
        is DeviceConnectionFailure.AccessDenied ->
            getString(
                when (hint) {
                    UsbAccessHint.LinuxUdev -> Res.string.device_access_denied_linux
                    UsbAccessHint.WindowsDriver -> Res.string.device_access_denied_windows
                    UsbAccessHint.Generic -> Res.string.device_access_denied
                }
            )
        DeviceConnectionFailure.DeviceUnavailable ->
            getString(Res.string.device_no_longer_available)
        is DeviceConnectionFailure.DeviceBusy ->
            getString(
                when (hint) {
                    UsbAccessHint.LinuxUdev -> Res.string.device_busy_linux
                    UsbAccessHint.WindowsDriver,
                    UsbAccessHint.Generic -> Res.string.device_busy
                }
            )
        DeviceConnectionFailure.HelperUnavailable ->
            getString(Res.string.helper_unavailable)
        DeviceConnectionFailure.UnsupportedAndroidVersion ->
            getString(Res.string.device_android_too_old)
        DeviceConnectionFailure.UnsupportedBrowser ->
            getString(Res.string.webusb_not_supported_description)
        is DeviceConnectionFailure.Unknown ->
            getString(Res.string.device_connection_failed, detail)
    }

/**
 * Lets the user pick which device Canta should manage (desktop ADB, web
 * WebUSB). Shown before apps load and whenever a privileged action needs a
 * connection that is not established yet.
 *
 * @param devices last discovery result.
 * @param isLoading true while discovering or connecting; rows are disabled.
 * @param discovery [DeviceDiscovery.List] shows discovered devices;
 * [DeviceDiscovery.SystemPicker] (web) shows a button for the browser's USB
 * chooser ([onSystemPick]), where granting access also selects the device.
 * @param connectionSupported false when the platform cannot reach devices at
 * all (web without WebUSB support); shows a warning instead of the picker.
 */
@Composable
fun DeviceChooserDialog(
    devices: List<CantaDevice>,
    isLoading: Boolean,
    discovery: DeviceDiscovery,
    connectionSupported: Boolean = true,
    onRefresh: () -> Unit,
    onSystemPick: () -> Unit,
    onSelect: (CantaDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val showDeviceList = discovery == DeviceDiscovery.List
    val showSystemPicker = discovery == DeviceDiscovery.SystemPicker

    CantaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.select_device),
        widthFraction = WIDE_DIALOG_WIDTH,
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
            if (showDeviceList) {
                TextButton(enabled = !isLoading, onClick = onRefresh) { Text(stringResource(Res.string.refresh)) }
            }
        },
    ) {
        Text(
            text = stringResource(
                if (showDeviceList) Res.string.select_device_description
                else Res.string.pick_device_description
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!connectionSupported) {
            Text(
                text = stringResource(Res.string.webusb_not_supported_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(Res.string.webusb_not_supported_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (showDeviceList) {
            if (isLoading && devices.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                )
            } else if (devices.isEmpty()) {
                Text(
                    text = stringResource(Res.string.no_devices_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    devices.forEach { device ->
                        DeviceRow(
                            device = device,
                            enabled = !isLoading,
                            onSelect = { onSelect(device) },
                        )
                    }
                }
            }

            if (isLoading && devices.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp)
                        .size(24.dp),
                    strokeWidth = 3.dp,
                )
            }
        }

        if (showSystemPicker && connectionSupported) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                onClick = onSystemPick,
            ) { Text(stringResource(Res.string.pick_device)) }
        }

        if (connectionSupported) {
            Spacer(modifier = Modifier.height(16.dp))
            // Tell the user up front what Canta places on their device.
            Text(
                text = stringResource(Res.string.helper_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: CantaDevice,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    SettingsItem(
        title = device.displayName,
        description = device.detail,
        icon = Icons.Default.PhoneAndroid,
        enabled = enabled,
        onClick = onSelect,
    )
}
