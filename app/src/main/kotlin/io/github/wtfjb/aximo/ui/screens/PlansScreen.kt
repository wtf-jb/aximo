package io.github.wtfjb.aximo.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.components.SecondaryButton

/**
 * "Pläne" tab. Routines follow in step 6 (A-05); until then it only links to the
 * exercise list.
 */
@Composable
fun PlansScreen(onOpenExercises: () -> Unit, modifier: Modifier = Modifier) {
    TabScreen(title = stringResource(R.string.nav_plans), modifier = modifier) {
        Text(
            text = stringResource(R.string.placeholder_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(text = stringResource(R.string.exercises_open), onClick = onOpenExercises)
    }
}
