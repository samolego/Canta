package io.github.samolego.canta.ui.component.fab

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.R

@Composable
fun PresetEditFAB(
    onPresetEditFinish: () -> Unit,
) {
    ExtendedFloatingActionButton(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(16.dp).navigationBarsPadding(),
        icon = {
            Icon(
                Icons.Default.Save,
                contentDescription = stringResource(R.string.save),
            )
        },
        text = {
            Text(stringResource(R.string.save))
        },
        onClick = onPresetEditFinish,
    )
}