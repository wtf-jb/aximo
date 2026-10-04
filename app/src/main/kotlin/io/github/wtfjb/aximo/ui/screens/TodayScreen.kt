package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.BuildConfig
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.format.formatLongDate
import io.github.wtfjb.aximo.ui.today.NextWorkoutCard
import io.github.wtfjb.aximo.ui.today.RecentList
import io.github.wtfjb.aximo.ui.today.WeekCard
import io.github.wtfjb.aximo.ui.today.TodayViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.androidx.compose.koinViewModel

/**
 * "Heute" tab (mockup Heute.html): resume the running workout, or the next routine
 * as hero card with a free workout below; the current week and "Zuletzt".
 * The weekly AI review card is Prio B and left out.
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
    val next = state.next
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    TabScreen(
        title = stringResource(R.string.nav_today),
        modifier = modifier.verticalScroll(rememberScrollState()),
        overline = formatLongDate(today),
        action = { CircleIconButton(AppIcons.Settings, stringResource(R.string.settings_title), onOpenSettings) },
    ) {
        when {
            state.hasActiveWorkout -> PrimaryButton(
                text = stringResource(R.string.today_resume_workout),
                onClick = { viewModel.startOrResume(null, onOpenWorkout) },
                modifier = Modifier.fillMaxWidth(),
            )
            next != null -> {
                NextWorkoutCard(next = next, onStart = { viewModel.startOrResume(next.routine.id, onOpenWorkout) })
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
        state.week?.let { WeekCard(it) }
        RecentList(items = state.recent, onOpenCardio = onOpenCardio, onOpenWorkout = onOpenFinishedWorkout)
        if (BuildConfig.DEBUG) {
            SecondaryButton(
                text = stringResource(R.string.showcase_open),
                onClick = onOpenThemeShowcase,
            )
        }
    }
}
