package io.github.wtfjb.aximo.domain.chat

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.review.ReviewContextBuilder
import io.github.wtfjb.aximo.domain.stats.ExerciseStats
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Builds the [ChatContext] (B-05): the review context (6 weeks) plus, for
 * questions like "Why is my bench press stalling?", the best value per
 * exercise in blocks of 4 weeks over the last 24 weeks.
 */
object ChatContextBuilder {
    const val BLOCKS = 6
    const val BLOCK_WEEKS = 4

    /** The most trained exercises; keeps the request small. */
    const val MAX_HISTORY_EXERCISES = 12

    fun build(
        workouts: List<WorkoutDetail>,
        routines: List<RoutineWithExercises>,
        exercises: List<Exercise>,
        weeklyGoal: Int?,
        today: LocalDate,
        zone: TimeZone,
    ): ChatContext = ChatContext(
        review = ReviewContextBuilder.build(workouts, routines, exercises, weeklyGoal, today, zone),
        history = history(workouts, today, zone),
    )

    fun history(workouts: List<WorkoutDetail>, today: LocalDate, zone: TimeZone): List<ExerciseHistory> {
        val blockDays = BLOCK_WEEKS * DAYS_PER_WEEK
        val start = today.minus(BLOCKS * blockDays - 1, DateTimeUnit.DAY)
        fun date(instant: kotlin.time.Instant) = StatsCalendar.localDate(instant, zone)

        val inPeriod = workouts.filter { it.workout.endedAt != null && date(it.workout.startedAt) in start..today }
        return ExerciseStats.trainedExercises(inPeriod)
            .filter { it.type != ExerciseType.CARDIO }
            .map { exercise ->
                val metric = ExerciseStats.metricFor(exercise.type)
                val sessions = ExerciseStats.sessions(inPeriod, exercise.id)
                val blocks = (0 until BLOCKS).map { index ->
                    val blockStart = start.plus(index * blockDays, DateTimeUnit.DAY)
                    val blockEnd = blockStart.plus(blockDays - 1, DateTimeUnit.DAY)
                    val inBlock = sessions.filter { date(it.startedAt) in blockStart..blockEnd }
                    val best = inBlock.mapNotNull { ExerciseStats.sessionValue(it.sets, metric) }.maxOrNull()
                    HistoryBlock(blockStart, inBlock.size, best?.let(::round1))
                }
                ExerciseHistory(exercise.id, exercise.name, metric, blocks) to sessions.size
            }
            .sortedByDescending { it.second }
            .take(MAX_HISTORY_EXERCISES)
            .map { it.first }
    }

    private fun round1(value: Double): Double = kotlin.math.round(value * 10) / 10

    private const val DAYS_PER_WEEK = 7
}
