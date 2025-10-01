package io.github.samolego.canta.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    isSwitch: Boolean = false,
    checked: Boolean = false,
    onClick: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = onClick != null || (isSwitch && onCheckedChange != null),
                onClick = {
                    onClick?.invoke()
                    if (isSwitch) {
                        onCheckedChange?.invoke(!checked)
                    }
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSwitch) {
            Box(modifier = Modifier.padding(4.dp)) {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}

@Composable
fun SettingsTextItem(
        title: String,
        description: String? = null,
        icon: ImageVector? = null,
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
        if (icon != null) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp, top = 12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
            )
            if (description != null) {
                Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = placeholder?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
            )
        }
    }
}
