package io.github.wtfjb.aximo.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons in Lucide style (24 grid, 2 px stroke, round caps), taken from the
 * mockups. The stroke color is a placeholder: Icon() tints it with the text color.
 */
object AppIcons {
    val Today by lazy { lineIcon("Today", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z") }
    val Plans by lazy {
        lineIcon(
            "Plans",
            "M7 4h10a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
            "M9 4V3h6v1M9 10h6M9 14h6M9 18h3",
        )
    }
    val Stats by lazy { lineIcon("Stats", "M5 20V11M12 20V4M19 20v-6") }
    val Coach by lazy {
        lineIcon(
            "Coach",
            "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z",
            "M19 15l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7z",
        )
    }
    val Back by lazy { lineIcon("Back", "M19 12H5M12 19l-7-7 7-7") }
    val TrendUp by lazy { lineIcon("TrendUp", "M3 17l6-6 4 4 8-8", "M15 7h6v6") }
    val Close by lazy { lineIcon("Close", "M18 6 6 18M6 6l12 12") }
    val Search by lazy { lineIcon("Search", "M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16z", "M21 21l-4.3-4.3") }
    val Check by lazy { lineIcon("Check", "M20 6 9 17l-5-5") }
    /** Three dots, drawn as round-capped dots (zero-length strokes, 3 px wide look). */
    val More by lazy { lineIcon("More", "M5 12h.01M12 12h.01M19 12h.01") }
    val ArrowRight by lazy { lineIcon("ArrowRight", "M5 12h14M13 6l6 6-6 6") }
    val Minus by lazy { lineIcon("Minus", "M5 12h14") }
    val Minimize by lazy { lineIcon("Minimize", "M6 9l6 6 6-6") }
    val ChevronDown by lazy { lineIcon("ChevronDown", "M6 9l6 6 6-6") }
    val Superset by lazy { lineIcon("Superset", "M7 7h10M7 12h10M7 17h10") }
    val Plus by lazy { lineIcon("Plus", "M12 5v14M5 12h14") }
    val Dumbbell by lazy { lineIcon("Dumbbell", "M6 7v10M3 9.5v5M18 7v10M21 9.5v5M6 12h12") }
    val Bodyweight by lazy {
        lineIcon("Bodyweight", "M12 3a2 2 0 1 0 0 4 2 2 0 0 0 0-4z", "M5 9l7 2 7-2M12 11v4M9 21l3-6 3 6")
    }
    val SkipForward by lazy { lineIcon("SkipForward", "M6 5l9 7-9 7z", "M18 5v14") }
    val Cardio by lazy { lineIcon("Cardio", "M3 12h4l3-8 4 16 3-8h4") }
    val Calendar by lazy { lineIcon("Calendar", "M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M4 10h16M9 3v4M15 3v4") }
    val Heart by lazy { lineIcon("Heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z") }
    val Mountain by lazy { lineIcon("Mountain", "M3 19l6-10 4 6 3-4 5 8z") }
    val Settings by lazy {
        lineIcon(
            "Settings",
            "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12",
            "M14 6a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
            "M8 12a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
            "M16 18a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
        )
    }
    val ChevronRight by lazy { lineIcon("ChevronRight", "M9 6l6 6-6 6") }
    val Note by lazy { lineIcon("Note", "M5 4h14v16H5z", "M9 9h6M9 13h6M9 17h3") }

    /** Filled play triangle (design system: filled icons are only play, check circle and dots). */
    val Play by lazy {
        ImageVector.Builder(name = "Play", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes("M8 5.5v13a1 1 0 0 0 1.5.86l10.5-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5z"),
                fill = SolidColor(Color.Black),
            )
            .build()
    }

    private fun lineIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { path ->
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
