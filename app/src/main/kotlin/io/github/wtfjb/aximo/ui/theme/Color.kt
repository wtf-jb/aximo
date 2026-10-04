package io.github.wtfjb.aximo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The only place with hex colors. Values come from docs/design/tokens.json,
// the mapping from docs/design/compose.md.

val LightColors = lightColorScheme(
    background = Color(0xFFF3F3F1),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F1),
    surfaceContainerHigh = Color(0xFFEDEDE9),
    surfaceVariant = Color(0xFFEDEDE9),
    onBackground = Color(0xFF17181C),
    onSurface = Color(0xFF17181C),
    onSurfaceVariant = Color(0xFF5D626C),
    primary = Color(0xFFF0643F),
    onPrimary = Color(0xFF17181C),
    primaryContainer = Color(0xFFFBE3DB),
    onPrimaryContainer = Color(0xFF9E2F18),
    inverseSurface = Color(0xFF17181C),
    inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFD6D6D1),
    outlineVariant = Color(0xFFE4E4E0),
)

val DarkColors = darkColorScheme(
    background = Color(0xFF0F1013),
    surface = Color(0xFF1B1D22),
    surfaceContainer = Color(0xFF1B1D22),
    surfaceContainerLow = Color(0xFF24262C),
    surfaceContainerHigh = Color(0xFF24262C),
    surfaceVariant = Color(0xFF24262C),
    onBackground = Color(0xFFF2F2EF),
    onSurface = Color(0xFFF2F2EF),
    onSurfaceVariant = Color(0xFFA3A8B2),
    primary = Color(0xFFF0643F),
    onPrimary = Color(0xFF17181C),
    primaryContainer = Color(0xFF3A2620),
    onPrimaryContainer = Color(0xFFFFB39F),
    inverseSurface = Color(0xFFF2F2EF),
    inverseOnSurface = Color(0xFF17181C),
    outline = Color(0xFF3A3D45),
    outlineVariant = Color(0xFF2C2F36),
)

/** Color roles that Material 3 has no slot for. */
@Immutable
data class ExtendedColors(
    val inkPlaceholder: Color,
    val onInverseMuted: Color,
    val inverseRaised: Color,
    val inverseAccentContainer: Color,
    val onInverseAccentContainer: Color,
    val accentChart: Color,
    val chartGrid: Color,
    val chartBand: Color,
    val chartArea: Color,
    val segmentActive: Color,
    val track: Color,
)

val LightExtendedColors = ExtendedColors(
    inkPlaceholder = Color(0xFF6B707A),
    onInverseMuted = Color(0xFFB9BCC4),
    inverseRaised = Color(0xFF2C2E35),
    inverseAccentContainer = Color(0xFF3A2620),
    onInverseAccentContainer = Color(0xFFFFB39F),
    accentChart = Color(0xFFE0502C),
    chartGrid = Color(0xFFEDEDE9),
    chartBand = Color(0xFFDCDCD6),
    chartArea = Color(0xFFFBE3DB),
    segmentActive = Color(0xFFFFFFFF),
    track = Color(0xFFE6E6E2),
)

val DarkExtendedColors = ExtendedColors(
    inkPlaceholder = Color(0xFF8A8F98),
    onInverseMuted = Color(0xFF4A4E57),
    inverseRaised = Color(0xFFDDDDD8),
    inverseAccentContainer = Color(0xFFFBE3DB),
    onInverseAccentContainer = Color(0xFF9E2F18),
    accentChart = Color(0xFFF0643F),
    chartGrid = Color(0xFF2C2F36),
    chartBand = Color(0xFF3A3D45),
    chartArea = Color(0xFF3A2620),
    segmentActive = Color(0xFF3A3D45),
    track = Color(0xFF1B1D22),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
