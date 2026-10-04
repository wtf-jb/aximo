package io.github.wtfjb.aximo.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.ui.theme.AppTextStyles
import io.github.wtfjb.aximo.ui.theme.Elevation
import io.github.wtfjb.aximo.ui.theme.Sizes

/** Bottom navigation: Heute · Pläne · Statistik · Coach (Coach only with an AI profile). */
@Composable
fun AppNavigationBar(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    Column {
        HorizontalDivider(thickness = Sizes.hairline, color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = Elevation.none,
        ) {
            destinations.forEach { destination ->
                val isSelected = destination == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(Sizes.icon),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            style = if (isSelected) AppTextStyles.navLabelActive else AppTextStyles.navLabel,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
