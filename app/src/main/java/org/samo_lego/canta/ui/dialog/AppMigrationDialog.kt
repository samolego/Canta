package org.samo_lego.canta.ui.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CantaMigrationDialog(onClose: () -> Unit, uninstallCanta: () -> Unit) {
    val context = LocalContext.current
    BasicAlertDialog(
            onDismissRequest = onClose,
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                        text = "Canta package name migration",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text =
                                "Hi there!\nI'd like to inform you that Canta has changed its package name from " +
                                        "org.samo_lego.canta to io.github.samolego.canta.\n" +
                                        "You are currently using the old app.\n" +
                                        "You should install new one from FDroid, GitHub or PlayStore.",
                        style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = uninstallCanta) { Text("Uninstall current Canta version") }
                    TextButton(onClick = {
                        // Open F-Droid link
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/io.github.samolego.canta"))
                        context.startActivity(intent)
                    }) { Text("Install from FDroid") }

                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/samolego/Canta/releases"))
                        context.startActivity(intent)
                    }) { Text("See GitHub releases") }
                    TextButton(onClick = {
                        // Open Play Store link
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=io.github.samolego.canta"))
                        context.startActivity(intent)
                    }) { Text("Install from Play Store") }

                    TextButton(onClick = onClose) { Text("Got it") }
                }
            }
        }
    }
}
