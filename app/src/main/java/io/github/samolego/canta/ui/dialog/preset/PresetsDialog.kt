package io.github.samolego.canta.ui.dialog.preset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.R

@Composable
fun ImportPresetDialog(
    onDismiss: () -> Unit,
    onImportFromClipboard: () -> Unit,
    onImportFromText: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.CLIPBOARD) }
    var jsonText by remember { mutableStateOf("") }
    var textError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.import_preset),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_preset_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == Tab.CLIPBOARD,
                        onClick = { selectedTab = Tab.CLIPBOARD },
                        text = { Text(stringResource(R.string.clipboard)) },
                        icon = { Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.clipboard)) }
                    )
                    Tab(
                        selected = selectedTab == Tab.TEXT,
                        onClick = { selectedTab = Tab.TEXT },
                        text = { Text(stringResource(R.string.text)) },
                        icon = { Icon(Icons.Default.Download, contentDescription = stringResource(R.string.text)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    Tab.CLIPBOARD -> {
                        // Clipboard import
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_preset_clipboard_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onImportFromClipboard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.import_preset_clipboard))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.import_preset_clipboard))
                            }
                        }
                    }
                    Tab.TEXT -> {
                        // Text import
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.paste_preset_json),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedTextField(
                                value = jsonText,
                                onValueChange = {
                                    jsonText = it
                                    textError = false
                                },
                                placeholder = { Text(stringResource(R.string.paste_preset_json_here)) },
                                isError = textError,
                                supportingText = if (textError) {
                                    { Text(stringResource(R.string.enter_valid_json)) }
                                } else null,
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
                Tab.CLIPBOARD -> {
                    // No confirm button for clipboard tab, handled by the button inside
                }
                Tab.TEXT -> {
                    Button(
                        onClick = {
                            if (jsonText.isNotBlank()) {
                                onImportFromText(jsonText.trim())
                            } else {
                                textError = true
                            }
                        },
                        enabled = jsonText.isNotBlank()
                    ) {
                        Text(stringResource(R.string.import_button))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

enum class Tab {
    CLIPBOARD,
    TEXT,
}
