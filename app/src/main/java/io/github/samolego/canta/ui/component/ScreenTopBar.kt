package io.github.samolego.canta.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.samolego.canta.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
        onNavigateBack: () -> Unit,
        title: @Composable () -> Unit,
        actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
            colors =
                    TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
            title = title,
            navigationIcon = {
                IconClickButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                )
            },
            actions = actions,
    )
}
