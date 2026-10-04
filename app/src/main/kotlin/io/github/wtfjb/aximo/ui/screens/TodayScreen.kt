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
 * "Heute" tab. For now: resume the running workout, or start the next routine or a free workout.
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val next = state.nextRoutine

    TabScreen(title = stringResource(R.string.nav_today), modifier = modifier) {
        when {
            state.hasActiveWorkout -> PrimaryButton(
                text = stringResource(R.string.today_resume_workout),
                onClick = { viewModel.startOrResume(null, onOpenWorkout) },
                modifier = Modifier.fillMaxWidth(),
            )
            next != null -> {
                PrimaryButton(
                    text = stringResource(R.string.today_start_routine, next.name),
                    onClick = { viewModel.startOrResume(next.id, onOpenWorkout) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(R.string.plans_free_training),
                    onClick = { viewModel.startOrResume(null, onOpenWorkout) },
                )
            }
            else -> PrimaryButton(
                text = stringResource(R.string.today_start_workout),
                onClick = { viewModel.startOrResume(null, onOpenWorkout) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (BuildConfig.DEBUG) {
            SecondaryButton(
                text = stringResource(R.string.showcase_open),
                onClick = onOpenThemeShowcase,
            )
        }
    }
}
