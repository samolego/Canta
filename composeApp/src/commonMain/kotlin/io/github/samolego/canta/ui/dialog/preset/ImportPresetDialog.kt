package io.github.samolego.canta.ui.dialog.preset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.cancel
import io.github.samolego.canta.generated.resources.clipboard
import io.github.samolego.canta.generated.resources.import_button
import io.github.samolego.canta.generated.resources.import_preset
import io.github.samolego.canta.generated.resources.import_preset_clipboard
import io.github.samolego.canta.generated.resources.import_preset_clipboard_description
import io.github.samolego.canta.generated.resources.import_preset_description
import io.github.samolego.canta.generated.resources.paste_preset_json
import io.github.samolego.canta.generated.resources.paste_preset_json_here
import io.github.samolego.canta.generated.resources.text
import io.github.samolego.canta.ui.component.IconText

/** Fixed height of the import tabs' content, so switching tabs doesn't resize the dialog. */
private val IMPORT_CONTENT_HEIGHT = 400.dp

@Composable
fun ImportPresetDialog(
    onDismiss: () -> Unit,
    onImportFromClipboard: () -> Unit,
    onImportFromText: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(ImportSource.CLIPBOARD) }
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.import_preset),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IMPORT_CONTENT_HEIGHT)
            ) {
                Text(
                    text = stringResource(Res.string.import_preset_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SecondaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == ImportSource.CLIPBOARD,
                        onClick = { selectedTab = ImportSource.CLIPBOARD },
                        text = { Text(stringResource(Res.string.clipboard)) },
                        icon = { Icon(Icons.Default.ContentPaste, contentDescription = stringResource(Res.string.clipboard)) }
                    )
                    Tab(
                        selected = selectedTab == ImportSource.TEXT,
                        onClick = { selectedTab = ImportSource.TEXT },
                        text = { Text(stringResource(Res.string.text)) },
                        icon = { Icon(Icons.Default.Download, contentDescription = stringResource(Res.string.text)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    ImportSource.CLIPBOARD -> {
                        // Clipboard import
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.import_preset_clipboard_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onImportFromClipboard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconText(Icons.Default.ContentPaste, stringResource(Res.string.import_preset_clipboard))
                            }
                        }
                    }
                    ImportSource.TEXT -> {
                        // Text import
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.paste_preset_json),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedTextField(
                                value = jsonText,
                                onValueChange = { jsonText = it },
                                placeholder = { Text(stringResource(Res.string.paste_preset_json_here)) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                maxLines = Int.MAX_VALUE
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (selectedTab) {
                ImportSource.CLIPBOARD -> {
                    // No confirm button for clipboard tab, handled by the button inside
                }
                ImportSource.TEXT -> {
                    Button(
                        onClick = { onImportFromText(jsonText.trim()) },
                        enabled = jsonText.isNotBlank()
                    ) {
                        Text(stringResource(Res.string.import_button))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/** Where an imported preset comes from. */
private enum class ImportSource {
    CLIPBOARD,
    TEXT,
}
