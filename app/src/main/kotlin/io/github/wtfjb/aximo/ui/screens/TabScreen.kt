package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.wtfjb.aximo.ui.theme.Spacing

/** Layout of a main tab: screen title in display-l, then the content. */
@Composable
fun TabScreen(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = Spacing.s20, end = Spacing.s20, top = Spacing.s28),
        verticalArrangement = Arrangement.spacedBy(Spacing.s18),
    ) {
        Text(text = title, style = MaterialTheme.typography.displayMedium)
        content()
    }
}

/** Placeholder for tabs that are built in later steps. */
@Composable
fun PlaceholderTab(title: String, body: String, modifier: Modifier = Modifier) {
    TabScreen(title = title, modifier = modifier) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
