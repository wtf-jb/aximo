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
