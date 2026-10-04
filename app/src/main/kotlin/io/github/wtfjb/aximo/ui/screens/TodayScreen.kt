package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.BuildConfig
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.today.TodayViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * "Heute" tab. For now: start a free workout or resume the running one.
 * Hero card, week bar and history follow with routines and statistics.
 * Debug builds also show the entry to the theme showcase.
 */
@Composable
fun TodayScreen(
    onOpenWorkout: () -> Unit,
    onOpenThemeShowcase: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val hasActiveWorkout by viewModel.hasActiveWorkout.collectAsStateWithLifecycle()

    TabScreen(title = stringResource(R.string.nav_today), modifier = modifier) {
        PrimaryButton(
            text = stringResource(if (hasActiveWorkout) R.string.today_resume_workout else R.string.today_start_workout),
            onClick = { viewModel.startOrResume(onOpenWorkout) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (BuildConfig.DEBUG) {
            SecondaryButton(
                text = stringResource(R.string.showcase_open),
                onClick = onOpenThemeShowcase,
            )
        }
    }
}
