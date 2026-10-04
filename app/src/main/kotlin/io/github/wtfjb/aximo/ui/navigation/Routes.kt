package io.github.wtfjb.aximo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.icons.AppIcons
import kotlinx.serialization.Serializable

// Type-safe navigation destinations.

@Serializable data object TodayRoute

@Serializable data object PlansRoute

/** Calendar of training days, opened from "Heute". */
@Serializable data object CalendarRoute

@Serializable data object StatsRoute

/** Coach tab (B-02), only shown with an AI provider profile. */
@Serializable data object CoachRoute

@Serializable data object ThemeShowcaseRoute

@Serializable data object SettingsRoute

/** AI provider profiles (B-01). */
@Serializable data object AiProfilesRoute

/** profileId = 0 creates a new profile. */
@Serializable data class AiProfileEditRoute(val profileId: Long = 0)

/** "Mit KI erstellen": generate routines (B-03). */
@Serializable data object PlanGeneratorRoute

/** Bundled exercise library (B-06). */
@Serializable data object CatalogRoute

@Serializable data object ExerciseListRoute

@Serializable data object WorkoutRoute

/** Log sets by text or voice into the running workout (B-04). */
@Serializable data object LoggingRoute

/** Summary after finishing a workout. */
@Serializable data class SummaryRoute(val workoutId: Long)

/** Log (entryId = 0) or edit a cardio session. */
@Serializable data class CardioRoute(val entryId: Long = 0)

/** routineId = 0 creates a new routine. */
@Serializable data class RoutineEditRoute(val routineId: Long = 0)

/** Exercise list in selection mode, returns the picked ids to the screen that opened it. */
@Serializable data object ExercisePickerRoute

/** Progress, history and records of one exercise (A-07). */
@Serializable data class ExerciseDetailRoute(val exerciseId: Long)

/** A finished workout, opened from "Zuletzt". */
@Serializable data class WorkoutDetailRoute(val workoutId: Long)

/** exerciseId = 0 creates a new exercise. */
@Serializable data class ExerciseEditRoute(val exerciseId: Long = 0)

/** The tabs of the bottom navigation, in display order. Coach only shows with an AI profile. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    TODAY(TodayRoute, R.string.nav_today, AppIcons.Today),
    PLANS(PlansRoute, R.string.nav_plans, AppIcons.Plans),
    STATS(StatsRoute, R.string.nav_stats, AppIcons.Stats),
    COACH(CoachRoute, R.string.nav_coach, AppIcons.Coach),
    ;

    companion object {
        /** Tabs to show: Coach only if AI features are available (B-01). */
        fun visible(aiAvailable: Boolean): List<TopLevelDestination> =
            if (aiAvailable) entries else entries.filter { it != COACH }
    }
}
