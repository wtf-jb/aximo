package io.github.wtfjb.aximo.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp), // radius-xs
    small = RoundedCornerShape(14.dp), // radius-md
    medium = RoundedCornerShape(18.dp), // radius-lg
    large = RoundedCornerShape(22.dp), // radius-xl
    extraLarge = RoundedCornerShape(28.dp), // radius-3xl
)

/** Radii that are not Material 3 shape steps. */
object Radii {
    val sm = RoundedCornerShape(10.dp) // radius-sm
    val xxl = RoundedCornerShape(26.dp) // radius-2xl
    val full = CircleShape // radius-full
}
