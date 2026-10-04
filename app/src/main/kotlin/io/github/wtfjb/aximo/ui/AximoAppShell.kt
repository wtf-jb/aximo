package io.github.wtfjb.aximo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.ui.navigation.AppNavigationBar
import io.github.wtfjb.aximo.ui.exercisedetail.ExerciseDetailScreen
import io.github.wtfjb.aximo.ui.exercises.ExerciseEditScreen
import io.github.wtfjb.aximo.ui.exercises.ExerciseListScreen
import io.github.wtfjb.aximo.ui.cardio.CardioScreen
import io.github.wtfjb.aximo.ui.navigation.CardioRoute
import io.github.wtfjb.aximo.ui.navigation.ExerciseEditRoute
import io.github.wtfjb.aximo.ui.navigation.ExerciseListRoute
import io.github.wtfjb.aximo.ui.navigation.ExercisePickerRoute
import io.github.wtfjb.aximo.ui.navigation.RoutineEditRoute
import io.github.wtfjb.aximo.ui.navigation.SettingsRoute
import io.github.wtfjb.aximo.ui.navigation.SummaryRoute
import io.github.wtfjb.aximo.ui.settings.SettingsScreen
import io.github.wtfjb.aximo.ui.summary.SummaryScreen
import io.github.wtfjb.aximo.ui.routine.RoutineEditScreen
import io.github.wtfjb.aximo.ui.navigation.WorkoutRoute
import io.github.wtfjb.aximo.ui.workout.PickedExercises
import io.github.wtfjb.aximo.ui.workout.WorkoutScreen
import io.github.wtfjb.aximo.ui.navigation.PlansRoute
import io.github.wtfjb.aximo.ui.navigation.StatsRoute
import io.github.wtfjb.aximo.ui.navigation.ThemeShowcaseRoute
import io.github.wtfjb.aximo.ui.navigation.TodayRoute
import io.github.wtfjb.aximo.ui.navigation.TopLevelDestination
import io.github.wtfjb.aximo.ui.screens.PlansScreen
import io.github.wtfjb.aximo.ui.screens.ThemeShowcaseScreen
import io.github.wtfjb.aximo.ui.screens.TodayScreen
import io.github.wtfjb.aximo.ui.stats.StatsScreen
import io.github.wtfjb.aximo.ui.navigation.ExerciseDetailRoute

/** App frame: bottom navigation plus the screen of the selected tab. */
@Composable
fun AximoAppShell(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hasRoute(tab.route::class) == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Hidden on detail screens, as in the design.
            if (currentTab != null) {
                AppNavigationBar(
                    selected = currentTab,
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<TodayRoute> {
                TodayScreen(
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    onOpenThemeShowcase = { navController.navigate(ThemeShowcaseRoute) },
                    onOpenCardio = { id -> navController.navigate(CardioRoute(id)) },
                    onOpenFinishedWorkout = { id -> navController.navigate(SummaryRoute(id)) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<PlansRoute> {
                PlansScreen(
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    onOpenExercises = { navController.navigate(ExerciseListRoute) },
                    onLogCardio = { navController.navigate(CardioRoute()) },
                    onOpenRoutine = { id -> navController.navigate(RoutineEditRoute(id)) },
                    onNewRoutine = { navController.navigate(RoutineEditRoute()) },
                )
            }
            composable<StatsRoute> {
                StatsScreen(onOpenExercise = { id -> navController.navigate(ExerciseDetailRoute(id)) })
            }
            composable<ExerciseDetailRoute> { entry ->
                ExerciseDetailScreen(
                    exerciseId = entry.toRoute<ExerciseDetailRoute>().exerciseId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(ExerciseEditRoute(id)) },
                )
            }
            composable<ExerciseListRoute> {
                ExerciseListScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(ExerciseEditRoute()) },
                    onOpen = { id -> navController.navigate(ExerciseDetailRoute(id)) },
                )
            }
            composable<WorkoutRoute> { entry ->
                // Result of the exercise picker, handed over via the saved state of this entry.
                val handle = entry.savedStateHandle
                val ids by handle.getStateFlow<LongArray?>(PICKED_IDS, null).collectAsStateWithLifecycle()
                val superset by handle.getStateFlow(PICKED_SUPERSET, false).collectAsStateWithLifecycle()
                WorkoutScreen(
                    picked = ids?.let { PickedExercises(it.toList(), superset) },
                    onPickedConsumed = {
                        handle.remove<LongArray>(PICKED_IDS)
                        handle.remove<Boolean>(PICKED_SUPERSET)
                    },
                    onAddExercise = { navController.navigate(ExercisePickerRoute) },
                    onClose = { navController.popBackStack() },
                    onFinished = { workoutId ->
                        navController.navigate(SummaryRoute(workoutId)) {
                            popUpTo<WorkoutRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<CardioRoute> { entry ->
                CardioScreen(
                    entryId = entry.toRoute<CardioRoute>().entryId,
                    onDone = { navController.popBackStack() },
                )
            }
            composable<SummaryRoute> { entry ->
                SummaryScreen(
                    workoutId = entry.toRoute<SummaryRoute>().workoutId,
                    onDone = { navController.popBackStack() },
                )
            }
            composable<RoutineEditRoute> { entry ->
                val handle = entry.savedStateHandle
                val ids by handle.getStateFlow<LongArray?>(PICKED_IDS, null).collectAsStateWithLifecycle()
                val superset by handle.getStateFlow(PICKED_SUPERSET, false).collectAsStateWithLifecycle()
                RoutineEditScreen(
                    routineId = entry.toRoute<RoutineEditRoute>().routineId,
                    picked = ids?.let { PickedExercises(it.toList(), superset) },
                    onPickedConsumed = {
                        handle.remove<LongArray>(PICKED_IDS)
                        handle.remove<Boolean>(PICKED_SUPERSET)
                    },
                    onAddExercise = { navController.navigate(ExercisePickerRoute) },
                    onDone = { navController.popBackStack() },
                )
            }
            composable<ExercisePickerRoute> {
                ExerciseListScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(ExerciseEditRoute()) },
                    onOpen = {},
                    selectionMode = true,
                    onPicked = { ids, superset ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set(PICKED_IDS, ids.toLongArray())
                            set(PICKED_SUPERSET, superset)
                        }
                        navController.popBackStack()
                    },
                )
            }
            composable<ExerciseEditRoute> { entry ->
                ExerciseEditScreen(
                    exerciseId = entry.toRoute<ExerciseEditRoute>().exerciseId,
                    onDone = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<ThemeShowcaseRoute> {
                ThemeShowcaseScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private const val PICKED_IDS = "picked_ids"
private const val PICKED_SUPERSET = "picked_superset"
