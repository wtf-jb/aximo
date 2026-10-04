package io.github.wtfjb.aximo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.ui.AximoAppShell
import io.github.wtfjb.aximo.ui.format.LocalSetRating
import io.github.wtfjb.aximo.ui.format.LocalWeightUnit
import io.github.wtfjb.aximo.ui.theme.AximoTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = koinViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
            val setRating by viewModel.setRating.collectAsStateWithLifecycle()
            val aiAvailable by viewModel.aiAvailable.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDark(systemIsDark = isSystemInDarkTheme())

            // Status bar icons follow the app theme, not only the system setting.
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            AximoTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalWeightUnit provides weightUnit, LocalSetRating provides setRating) {
                    AximoAppShell(
                        themeMode = themeMode,
                        onThemeModeChange = viewModel::setThemeMode,
                        aiAvailable = aiAvailable,
                    )
                }
            }
        }
    }
}
