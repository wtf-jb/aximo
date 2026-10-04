package io.github.wtfjb.aximo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import io.github.wtfjb.aximo.ui.theme.Spacing

/**
 * Key figure: big value, small label below ("85 kg / Max. Gewicht").
 * On a white card the tile is [MaterialTheme.colorScheme.surfaceContainerLow], on the background white.
 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = color, modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.s12), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
