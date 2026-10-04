package io.github.wtfjb.aximo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.icons.AppIcons
import kotlinx.serialization.Serializable

// Type-safe navigation destinations.

@Serializable data object TodayRoute

@Serializable data object PlansRoute

@Serializable data object StatsRoute

@Serializable data object CoachRoute

@Serializable data object ThemeShowcaseRoute

@Serializable data object ExerciseListRoute

/** exerciseId = 0 creates a new exercise. */
@Serializable data class ExerciseEditRoute(val exerciseId: Long = 0)

/** The four tabs of the bottom navigation, in display order. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    TODAY(TodayRoute, R.string.nav_today, AppIcons.Today),
    PLANS(PlansRoute, R.string.nav_plans, AppIcons.Plans),
    STATS(StatsRoute, R.string.nav_stats, AppIcons.Stats),
    COACH(CoachRoute, R.string.nav_coach, AppIcons.Coach),
}
