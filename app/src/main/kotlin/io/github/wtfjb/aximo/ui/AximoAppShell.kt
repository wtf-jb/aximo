package io.github.wtfjb.aximo.ui

import androidx.compose.foundation.layout.consumeWindowInsets
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
import io.github.wtfjb.aximo.ui.navigation.AiProfileEditRoute
import io.github.wtfjb.aximo.ui.navigation.AiProfilesRoute
import io.github.wtfjb.aximo.ui.navigation.SettingsRoute
import io.github.wtfjb.aximo.ui.navigation.SummaryRoute
import io.github.wtfjb.aximo.ui.ai.AiProfileEditScreen
import io.github.wtfjb.aximo.ui.ai.AiProfilesScreen
import io.github.wtfjb.aximo.ui.settings.SettingsScreen
import io.github.wtfjb.aximo.ui.summary.SummaryScreen
import io.github.wtfjb.aximo.ui.routine.RoutineEditScreen
import io.github.wtfjb.aximo.ui.navigation.WorkoutRoute
import io.github.wtfjb.aximo.ui.navigation.LoggingRoute
import io.github.wtfjb.aximo.ui.logging.LoggingScreen
import io.github.wtfjb.aximo.ui.workout.PickedExercises
import io.github.wtfjb.aximo.ui.workout.WorkoutScreen
import io.github.wtfjb.aximo.ui.navigation.PlansRoute
import io.github.wtfjb.aximo.ui.navigation.CatalogRoute
import io.github.wtfjb.aximo.ui.catalog.CatalogScreen
import io.github.wtfjb.aximo.ui.navigation.PlanGeneratorRoute
import io.github.wtfjb.aximo.ui.plangen.PlanGeneratorScreen
import io.github.wtfjb.aximo.ui.navigation.StatsRoute
import io.github.wtfjb.aximo.ui.navigation.CoachRoute
import io.github.wtfjb.aximo.ui.coach.CoachScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
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
    aiAvailable: Boolean,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val tabs = TopLevelDestination.visible(aiAvailable)
    val currentTab = tabs.firstOrNull { tab ->
        currentDestination?.hasRoute(tab.route::class) == true
    }

    // The last profile is gone while the Coach tab is open: back to Heute.
    LaunchedEffect(aiAvailable, currentDestination) {
        if (!aiAvailable && currentDestination?.hasRoute(CoachRoute::class) == true) {
            navController.navigate(TodayRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Hidden on detail screens, as in the design.
            if (currentTab != null) {
                AppNavigationBar(
                    destinations = tabs,
                    selected = currentTab,
                    onSelect = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            // Consumed, so imePadding() on a screen (coach chat) adds only what the keyboard covers beyond the bars.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<TodayRoute> {
                TodayScreen(
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    onOpenThemeShowcase = { navController.navigate(ThemeShowcaseRoute) },
                    onOpenCardio = { id -> navController.navigate(CardioRoute(id)) },
                    onOpenFinishedWorkout = { id -> navController.navigate(SummaryRoute(id)) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenCoach = { navController.navigateToTab(TopLevelDestination.COACH) },
                )
            }
            composable<PlansRoute> {
                PlansScreen(
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    onOpenExercises = { navController.navigate(ExerciseListRoute) },
                    onLogCardio = { navController.navigate(CardioRoute()) },
                    onOpenRoutine = { id -> navController.navigate(RoutineEditRoute(id)) },
                    onNewRoutine = { navController.navigate(RoutineEditRoute()) },
                    onGeneratePlan = { navController.navigate(PlanGeneratorRoute) },
                )
            }
            composable<PlanGeneratorRoute> {
                PlanGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable<CoachRoute> {
                CoachScreen()
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
                    onOpenCatalog = { navController.navigate(CatalogRoute) },
                )
            }
            composable<CatalogRoute> {
                CatalogScreen(onBack = { navController.popBackStack() })
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
                    onLogText = { navController.navigate(LoggingRoute) },
                    onClose = { navController.popBackStack() },
                    onFinished = { workoutId ->
                        navController.navigate(SummaryRoute(workoutId)) {
                            popUpTo<WorkoutRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<LoggingRoute> {
                LoggingScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
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
                    onOpenCatalog = { navController.navigate(CatalogRoute) },
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
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAiProfiles = { navController.navigate(AiProfilesRoute) },
                )
            }
            composable<AiProfilesRoute> {
                AiProfilesScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(AiProfileEditRoute(id)) },
                )
            }
            composable<AiProfileEditRoute> { entry ->
                AiProfileEditScreen(
                    profileId = entry.toRoute<AiProfileEditRoute>().profileId,
                    onDone = { navController.popBackStack() },
                )
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

/** Switches tabs like the bottom navigation: one copy per tab, state kept. */
private fun NavController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
