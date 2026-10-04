package io.github.wtfjb.aximo.domain.today

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.stats.Consistency
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import kotlin.math.ceil
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/** One line of the "next workout" card: an exercise whose progression changes something. */
data class PreviewItem(
    val exerciseId: Long,
    val name: String,
    val reason: ProgressionReason,
    val nextWeightKg: Double,
    val nextRepTarget: Int,
    /** Change against the heaviest working set of the last session, null if unknown. */
    val deltaKg: Double?,
)

/** The "Heute" hero card (mockup Heute.html): length estimate and progression preview. */
object NextWorkout {
    /** Rough time to perform one set, on top of the rest after it. */
    const val SECONDS_PER_SET = 40
    const val PREVIEW_LIMIT = 3

    /**
     * Estimated duration in minutes: every target set takes [SECONDS_PER_SET] plus
     * the rest of its exercise, rounded up to five minutes. 0 for an empty routine.
     */
    fun estimatedMinutes(targets: List<RoutineExercise>, exercises: Map<Long, Exercise>): Int {
        val seconds = targets.sumOf { target ->
            val rest = exercises[target.exerciseId]?.restSeconds ?: Exercise.DEFAULT_REST_SECONDS
            target.targetSets * (SECONDS_PER_SET + rest)
        }
        if (seconds == 0) return 0
        return (ceil(seconds / 300.0) * 5).toInt()
    }

    /**
     * Exercises of the routine, in routine order, whose suggestion changes weight or
     * reps ("Gewicht bleibt" is left out), at most [limit]. [lastTopKg] is the
     * heaviest working set of the last session per exercise.
     */
    fun preview(
        targets: List<RoutineExercise>,
        exercises: Map<Long, Exercise>,
        suggestions: Map<Long, ProgressionState>,
        lastTopKg: Map<Long, Double>,
        limit: Int = PREVIEW_LIMIT,
    ): List<PreviewItem> = targets
        .distinctBy { it.exerciseId }
        .mapNotNull { target ->
            val suggestion = suggestions[target.exerciseId]?.takeIf { it.reason != ProgressionReason.HOLD } ?: return@mapNotNull null
            val exercise = exercises[target.exerciseId] ?: return@mapNotNull null
            PreviewItem(
                exerciseId = exercise.id,
                name = exercise.name,
                reason = suggestion.reason,
                nextWeightKg = suggestion.nextWeightKg,
                nextRepTarget = suggestion.nextRepTarget,
                deltaKg = lastTopKg[exercise.id]?.let { suggestion.nextWeightKg - it },
            )
        }
        .take(limit)
}

/** One day of the week bar on "Heute". */
data class WeekDay(val date: LocalDate, val kind: DayKind, val isToday: Boolean)

/** "Diese Woche": Monday to Sunday of the current week and the number of sessions. */
data class WeekOverview(val days: List<WeekDay>, val sessions: Int)

object WeekBar {
    /** [strength] and [cardio] are start times of finished workouts and cardio entries. */
    fun overview(strength: List<Instant>, cardio: List<Instant>, today: LocalDate, zone: TimeZone): WeekOverview {
        val start = StatsCalendar.weekStart(today)
        val kinds = Consistency.heatmap(Consistency.activeDays(strength, cardio, zone), today, weeks = 1).single()
        val days = kinds.mapIndexed { index, kind ->
            val date = start.plus(index, DateTimeUnit.DAY)
            WeekDay(date, kind, date == today)
        }
        val sessions = (strength + cardio).count { StatsCalendar.localDate(it, zone) in start..today }
        return WeekOverview(days, sessions)
    }
}
