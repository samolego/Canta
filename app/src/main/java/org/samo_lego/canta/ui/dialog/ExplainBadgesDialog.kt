package org.samo_lego.canta.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.samo_lego.canta.R
import org.samo_lego.canta.ui.component.RemovalBadge
import org.samo_lego.canta.util.RemovalRecommendation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainBadgesDialog(
    onDismissRequest: () -> Unit
) {
    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large),
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        ),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                items(RemovalRecommendation.entries) { removalRecommendation ->
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            modifier = Modifier.weight(2f),
                        ) {
                            RemovalBadge(
                                type = removalRecommendation
                            )
                        }
                        Text(
                            removalRecommendation.description,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(3f)
                        )
                    }
                }
            }
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = onDismissRequest,
            ) {
                Text(stringResource(R.string.got_it))
            }
        }
    }
}