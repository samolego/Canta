package io.github.samolego.canta.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.R
import io.github.samolego.canta.ui.viewmodel.AppListViewModel
import io.github.samolego.canta.util.apps.Filter

@Composable
fun FiltersMenu(
        showMenu: Boolean,
        onDismiss: () -> Unit,
        appListViewModel: AppListViewModel,
) {
    var filtersMenu by remember { mutableStateOf(false) }

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(180.dp)
    ) {
        // System apps toggle
        FilterChip(
                text = stringResource(R.string.only_system),
                isSelected = appListViewModel.showSystem,
                onClick = { appListViewModel.showSystem = !appListViewModel.showSystem },
                trailingContent = {
                    Checkbox(
                            checked = appListViewModel.showSystem,
                            onCheckedChange = { appListViewModel.showSystem = it }
                    )
                }
        )

        // Filter submenu trigger
        FilterChip(
                text = appListViewModel.selectedFilter.name,
                isSelected = filtersMenu,
                onClick = { filtersMenu = !filtersMenu },
                trailingContent = {
                    Icon(
                            if (filtersMenu) Icons.Default.ArrowDropUp
                            else Icons.Default.ArrowDropDown,
                            contentDescription = null
                    )
                }
        )

        if (filtersMenu) {
            Filter.availableFilters.forEach { filter ->
                FilterChip(
                        text = filter.name,
                        isSelected = appListViewModel.selectedFilter == filter,
                        onClick = {
                            appListViewModel.selectedFilter = filter
                            filtersMenu = false
                        }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable(onClick = onClick),
            color =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
            )
            trailingContent?.invoke()
        }
    }
}
