package org.samo_lego.canta.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.samo_lego.canta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallAppsDialog(
    appCount: Int,
    canResetToFactory: Boolean = false,
    onDismiss: () -> Unit,
    onAgree: (resetToFactory: Boolean) -> Unit,
) {
    var resetToFactory by remember { mutableStateOf(false) }

    BasicAlertDialog(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.are_you_sure_to_uninstall_apps, appCount))

            if (canResetToFactory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = resetToFactory,
                        onCheckedChange = { resetToFactory = it }
                    )
                    Text(
                        text = stringResource(R.string.reset_to_factory_version),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onAgree(resetToFactory) }
                ) {
                    Text(stringResource(R.string.ok))
                }
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Preview
@Composable
fun UninstallAppsDialogPreview() {
    UninstallAppsDialog(
        appCount = 5,
        canResetToFactory = true,
        onDismiss = {},
        onAgree = {}
    )
}

@Preview
@Composable
fun UninstallAppsDialogRegularAppPreview() {
    UninstallAppsDialog(
        appCount = 1,
        canResetToFactory = false,
        onDismiss = {},
        onAgree = {}
    )
}