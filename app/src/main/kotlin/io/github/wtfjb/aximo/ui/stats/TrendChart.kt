package io.github.wtfjb.aximo.ui.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors

/**
 * Line chart of one value per session (e1RM, reps, pace …), drawn with Vico:
 * accent line with area, grid lines with labels on the right, the last point
 * large and filled, record points as rings. Points are evenly spaced; the
 * dates are shown by the caller below the chart. Needs at least two values.
 */
@Composable
fun TrendChart(
    values: List<Double>,
    highlighted: Set<Int>,
    formatValue: (Double) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.extendedColors.accentChart
    val area = MaterialTheme.extendedColors.chartArea
    val grid = MaterialTheme.extendedColors.chartGrid
    val surface = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lastIndex = values.lastIndex

    val model = remember(values) { CartesianChartModel(LineCartesianLayerModel.build { series(values) }) }
    val range = remember(values) { ChartScale.range(values) }

    val pointProvider = remember(highlighted, lastIndex, accent, surface) {
        ChartPoints(
            last = LineCartesianLayer.Point(
                ShapeComponent(Fill(accent), CircleShape, strokeFill = Fill(surface), strokeThickness = Sizes.chartPointStroke),
                Sizes.chartPointLast,
            ),
            record = LineCartesianLayer.Point(
                ShapeComponent(Fill(surface), CircleShape, strokeFill = Fill(accent), strokeThickness = Sizes.chartPointStroke),
                Sizes.chartPoint,
            ),
            highlighted = highlighted,
            lastIndex = lastIndex,
        )
    }
    val line = LineCartesianLayer.rememberLine(
        fill = remember(accent) { LineCartesianLayer.LineFill.single(Fill(accent)) },
        stroke = remember { LineCartesianLayer.LineStroke.Continuous(thickness = Sizes.chartLine, cap = StrokeCap.Round) },
        areaFill = remember(area) { LineCartesianLayer.AreaFill.single(Fill(area)) },
        pointProvider = pointProvider,
    )
    val layer = rememberLineCartesianLayer(
        lineProvider = remember(line) { LineCartesianLayer.LineProvider.series(line) },
        rangeProvider = remember(range) { CartesianLayerRangeProvider.fixed(minY = range.first, maxY = range.second) },
    )
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val endAxis = VerticalAxis.rememberEnd(
        line = null,
        label = rememberAxisLabelComponent(style = labelStyle),
        valueFormatter = remember(formatValue) { CartesianValueFormatter { _, value, _ -> formatValue(value) } },
        tick = null,
        guideline = rememberAxisGuidelineComponent(fill = Fill(grid), shape = RectangleShape),
        itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { AXIS_LABELS }) },
    )
    val chart = rememberCartesianChart(
        layer,
        endAxis = endAxis,
        layerPadding = { CartesianLayerPadding(unscalableStart = Spacing.s8, unscalableEnd = Spacing.s8) },
    )
    CartesianChartHost(
        chart = chart,
        model = model,
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.chartHeight)
            .semantics { this.contentDescription = contentDescription },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        chartAreaHeight = Sizes.chartHeight,
    )
}

/** Last point filled and large, record points as rings, all others without a point. */
private class ChartPoints(
    private val last: LineCartesianLayer.Point,
    private val record: LineCartesianLayer.Point,
    private val highlighted: Set<Int>,
    private val lastIndex: Int,
) : LineCartesianLayer.PointProvider {
    override fun getPoint(entry: LineCartesianLayerModel.Entry, extraStore: ExtraStore): LineCartesianLayer.Point? {
        val index = entry.x.toInt()
        return when {
            index == lastIndex -> last
            index in highlighted -> record
            else -> null
        }
    }

    override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = last
}

private const val AXIS_LABELS = 3
