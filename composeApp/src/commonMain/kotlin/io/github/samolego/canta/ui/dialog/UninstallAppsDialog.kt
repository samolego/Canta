package io.github.samolego.canta.ui.dialog

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.choose_action_for_apps
import io.github.samolego.canta.generated.resources.disable_app
import io.github.samolego.canta.generated.resources.enable_app
import io.github.samolego.canta.generated.resources.ok
import io.github.samolego.canta.generated.resources.reset_to_factory_version
import io.github.samolego.canta.generated.resources.uninstall_app
import io.github.samolego.canta.ui.component.CantaDialog
import io.github.samolego.canta.ui.component.CheckboxRow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun UninstallAppsDialog(
    appCount: Int,
    canResetToFactory: Boolean = false,
    hasDisabledApp: Boolean = false,
    hasEnabledApp: Boolean = true,
    onDismiss: () -> Unit,
    onAgree: (resetToFactory: Boolean, disableApp: Boolean, enableApp: Boolean, uninstallApp: Boolean) -> Unit,
) {
    val onlyDisabled = hasDisabledApp && !hasEnabledApp
    var resetToFactory by rememberSaveable { mutableStateOf(false) }
    var disableApp by rememberSaveable { mutableStateOf(false) }
    var enableApp by rememberSaveable { mutableStateOf(onlyDisabled) }
    var uninstallApp by rememberSaveable { mutableStateOf(!onlyDisabled) }

    CantaDialog(
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            TextButton(
                onClick = { onAgree(resetToFactory && uninstallApp, disableApp, enableApp, uninstallApp) },
                enabled = disableApp || enableApp || uninstallApp
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
    ) {
        Text(pluralStringResource(Res.plurals.choose_action_for_apps, appCount, appCount))

        if (hasDisabledApp) {
            CheckboxRow(
                checked = enableApp,
                onCheckedChange = {
                    enableApp = it
                    if (it) {
                        disableApp = false
                        uninstallApp = false
                        resetToFactory = false
                    }
                },
                label = stringResource(Res.string.enable_app),
            )
        }

        if (hasEnabledApp) {
            CheckboxRow(
                checked = disableApp,
                onCheckedChange = {
                    disableApp = it
                    if (it) {
                        enableApp = false
                        uninstallApp = false
                        resetToFactory = false
                    }
                },
                label = stringResource(Res.string.disable_app),
            )
        }

        CheckboxRow(
            checked = uninstallApp,
            onCheckedChange = {
                uninstallApp = it
                if (it) {
                    enableApp = false
                } else {
                    resetToFactory = false
                }
            },
            label = stringResource(Res.string.uninstall_app),
        )

        if (canResetToFactory && uninstallApp) {
            CheckboxRow(
                checked = resetToFactory,
                onCheckedChange = { resetToFactory = it },
                label = stringResource(Res.string.reset_to_factory_version),
            )
        }
    }
}
