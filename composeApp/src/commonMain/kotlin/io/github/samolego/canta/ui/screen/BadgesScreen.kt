package io.github.samolego.canta.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.badge_info
import io.github.samolego.canta.ui.badge.Badge
import io.github.samolego.canta.ui.badge.BadgeChip
import io.github.samolego.canta.ui.badge.BadgeGroup
import io.github.samolego.canta.ui.component.ScreenTopBar
import org.jetbrains.compose.resources.stringResource

/** Explains what each badge means, grouped by where it comes from. */
@Composable
fun BadgesScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            ScreenTopBar(onNavigateBack = onNavigateBack, title = { Text(stringResource(Res.string.badge_info)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            BadgeGroup.entries.forEach { group ->
                item(key = group.name) {
                    Text(
                        stringResource(group.title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                    group.description?.let {
                        Text(stringResource(it), style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(Badge.entries.filter { it.group == group }, key = { it.name }) { badge ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                            BadgeChip(
                                modifier = Modifier.scale(1.2f),
                                badge = badge
                            )
                        }
                        Text(
                            stringResource(badge.description),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(3f),
                        )
                    }
                }
            }
        }
    }
}
