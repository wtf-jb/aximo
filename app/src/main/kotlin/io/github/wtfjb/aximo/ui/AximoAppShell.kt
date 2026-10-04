package io.github.wtfjb.aximo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.ui.navigation.AppNavigationBar
import io.github.wtfjb.aximo.ui.navigation.CoachRoute
import io.github.wtfjb.aximo.ui.navigation.PlansRoute
import io.github.wtfjb.aximo.ui.navigation.StatsRoute
import io.github.wtfjb.aximo.ui.navigation.ThemeShowcaseRoute
import io.github.wtfjb.aximo.ui.navigation.TodayRoute
import io.github.wtfjb.aximo.ui.navigation.TopLevelDestination
import io.github.wtfjb.aximo.ui.screens.PlaceholderTab
import io.github.wtfjb.aximo.ui.screens.ThemeShowcaseScreen
import io.github.wtfjb.aximo.ui.screens.TodayScreen

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
                TodayScreen(onOpenThemeShowcase = { navController.navigate(ThemeShowcaseRoute) })
            }
            composable<PlansRoute> {
                PlaceholderTab(stringResource(R.string.nav_plans), stringResource(R.string.placeholder_body))
            }
            composable<StatsRoute> {
                PlaceholderTab(stringResource(R.string.nav_stats), stringResource(R.string.placeholder_body))
            }
            composable<CoachRoute> {
                PlaceholderTab(stringResource(R.string.nav_coach), stringResource(R.string.placeholder_body))
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
