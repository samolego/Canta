package io.github.samolego.canta.ui.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.ui.viewmodel.AppListViewModel

data class SortOption(
    val field: SortField,
    val direction: SortDirection,
    val label: String
)

enum class SortField {
    NAME,
    PACKAGE_NAME,
    VERSION,
    SIZE
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

val sortOptions = listOf(
    SortOption(SortField.NAME, SortDirection.ASCENDING, "Name: A → Z"),
    SortOption(SortField.NAME, SortDirection.DESCENDING, "Name: Z → A"),
    SortOption(SortField.PACKAGE_NAME, SortDirection.ASCENDING, "Package: A → Z"),
    SortOption(SortField.PACKAGE_NAME, SortDirection.DESCENDING, "Package: Z → A"),
    SortOption(SortField.SIZE, SortDirection.ASCENDING, "Size: Low → High"),
    SortOption(SortField.SIZE, SortDirection.DESCENDING, "Size: High → Low"),
    SortOption(SortField.VERSION, SortDirection.ASCENDING, "Version: Low → High"),
    SortOption(SortField.VERSION, SortDirection.DESCENDING, "Version: High → Low")
)

@Composable
fun SortByOptions(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    appListViewModel: AppListViewModel
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(180.dp)
    ) {
        sortOptions.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                onClick = {
                    appListViewModel.sortedBy = option
                    onDismiss()
                },
                trailingIcon = {
                    if (appListViewModel.sortedBy == option) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    }
}
