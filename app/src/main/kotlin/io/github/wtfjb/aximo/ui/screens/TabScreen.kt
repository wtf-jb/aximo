package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.wtfjb.aximo.ui.theme.Spacing

/** Layout of a main tab: optional overline (e.g. the date), screen title in display-l, optional action on the right, then the content. */
@Composable
fun TabScreen(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = Spacing.s20, end = Spacing.s20, top = Spacing.s28),
        verticalArrangement = Arrangement.spacedBy(Spacing.s18),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (overline != null) {
                    Text(text = overline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = title, style = MaterialTheme.typography.displayMedium)
            }
            action?.invoke()
        }
        content()
    }
}
