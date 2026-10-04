package io.github.wtfjb.aximo.ui.stats

import kotlin.math.abs
import kotlin.math.max

/** Y range of a trend chart: the values plus some air above and below. */
object ChartScale {
    private const val PADDING = 0.15

    fun range(values: List<Double>): Pair<Double, Double> {
        if (values.isEmpty()) return 0.0 to 1.0
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: max(abs(max) * PADDING, 1.0)
        val pad = span * PADDING
        return (min - pad).coerceAtLeast(if (min >= 0) 0.0 else Double.NEGATIVE_INFINITY) to max + pad
    }
}
