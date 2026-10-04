package io.github.wtfjb.aximo.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.BuildConfig
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.components.SecondaryButton

/** "Heute" tab. Placeholder for now; debug builds show an entry to the theme showcase. */
@Composable
fun TodayScreen(onOpenThemeShowcase: () -> Unit, modifier: Modifier = Modifier) {
    TabScreen(title = stringResource(R.string.nav_today), modifier = modifier) {
        Text(
            text = stringResource(R.string.placeholder_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (BuildConfig.DEBUG) {
            SecondaryButton(
                text = stringResource(R.string.showcase_open),
                onClick = onOpenThemeShowcase,
            )
        }
    }
}
