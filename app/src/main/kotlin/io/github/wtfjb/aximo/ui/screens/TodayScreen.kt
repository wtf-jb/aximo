package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.BuildConfig
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.today.RecentList
import io.github.wtfjb.aximo.ui.today.TodayViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * "Heute" tab: resume the running workout, or start the next routine or a free workout,
 * and "Zuletzt". Hero card and week bar follow with the statistics.
 * Debug builds also show the entry to the theme showcase.
 */
@Composable
fun TodayScreen(
    onOpenWorkout: () -> Unit,
    onOpenThemeShowcase: () -> Unit,
    onOpenCardio: (Long) -> Unit,
    onOpenFinishedWorkout: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val next = state.nextRoutine

    TabScreen(
        title = stringResource(R.string.nav_today),
        modifier = modifier.verticalScroll(rememberScrollState()),
        action = { CircleIconButton(AppIcons.Settings, stringResource(R.string.settings_title), onOpenSettings) },
    ) {
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
        RecentList(items = state.recent, onOpenCardio = onOpenCardio, onOpenWorkout = onOpenFinishedWorkout)
        if (BuildConfig.DEBUG) {
            SecondaryButton(
                text = stringResource(R.string.showcase_open),
                onClick = onOpenThemeShowcase,
            )
        }
    }
}
