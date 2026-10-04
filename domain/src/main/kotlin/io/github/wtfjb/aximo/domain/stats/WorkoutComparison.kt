package io.github.wtfjb.aximo.domain.stats

import io.github.wtfjb.aximo.domain.workout.WorkoutDetail

/** Comparison with the previous workout of the same routine ("+4 % ggü. letzter Push A"). */
object WorkoutComparison {

    fun volume(detail: WorkoutDetail): Double = Records.volume(detail.exercises.flatMap { it.sets })

    /** Relative volume change; null without a previous workout or if it had no volume. */
    fun volumeChange(current: WorkoutDetail, previous: WorkoutDetail?): Double? {
        previous ?: return null
        return StatsCalendar.relativeChange(volume(previous), volume(current))
    }
}
