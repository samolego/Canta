package io.github.samolego.canta.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ImportConfigurationDialog(
    onDismiss: () -> Unit,
    onImportFromClipboard: () -> Unit,
    onImportFromText: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var jsonText by remember { mutableStateOf("") }
    var textError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Configuration",
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
                    text = "Import a configuration from clipboard or paste JSON text directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Clipboard") },
                        icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Text") },
                        icon = { Icon(Icons.Default.Download, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // Clipboard import
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Import configuration from clipboard. Make sure you have copied a valid Canta configuration JSON.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onImportFromClipboard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import from Clipboard")
                            }
                        }
                    }
                    1 -> {
                        // Text import
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Paste configuration JSON:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedTextField(
                                value = jsonText,
                                onValueChange = {
                                    jsonText = it
                                    textError = false
                                },
                                placeholder = { Text("Paste JSON configuration here...") },
                                isError = textError,
                                supportingText = if (textError) {
                                    { Text("Please enter valid JSON") }
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
                0 -> {
                    // No confirm button for clipboard tab, handled by the button inside
                }
                1 -> {
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
                        Text("Import")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}