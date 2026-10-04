package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.progression.ProgressionRules
import io.github.wtfjb.aximo.domain.stats.ExerciseStats
import io.github.wtfjb.aximo.domain.stats.MuscleVolume
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.workout.Effort
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

/** Builds the [ReviewContext] from finished workouts and the current routines (B-02). */
object ReviewContextBuilder {
    /** Requirements: 4–8 weeks. Six is the middle and matches the mockup. */
    const val DEFAULT_WEEKS = 6

    /** Keeps the request small; the most trained exercises matter most. */
    const val MAX_EXERCISES = 20

    /** Exercises offered for additions. */
    const val MAX_AVAILABLE = 80

    fun build(
        workouts: List<WorkoutDetail>,
        routines: List<RoutineWithExercises>,
        exercises: List<Exercise>,
        weeklyGoal: Int?,
        today: LocalDate,
        zone: TimeZone,
        weeks: Int = DEFAULT_WEEKS,
    ): ReviewContext {
        // Period: the last [weeks] full weeks up to today, e.g. 6 × 7 days.
        val start = today.minus(weeks * DAYS_PER_WEEK - 1, DateTimeUnit.DAY)
        val half = today.minus(weeks * DAYS_PER_WEEK / 2 - 1, DateTimeUnit.DAY)
        fun date(detail: WorkoutDetail) = StatsCalendar.localDate(detail.workout.startedAt, zone)

        val inPeriod = workouts
            .filter { it.workout.endedAt != null && date(it) in start..today }
            .sortedBy { it.workout.startedAt }
        val names = exercises.associate { it.id to it.name }

        val routineInfos = routines.map { r ->
            RoutineInfo(
                routineId = r.routine.id,
                name = r.routine.name,
                sessions = inPeriod.count { it.workout.routineId == r.routine.id },
                exercises = r.exercises.map { e ->
                    RoutineExerciseInfo(e.exerciseId, names[e.exerciseId] ?: "?", e.targetSets, e.repMin, e.repMax, e.targetRir)
                },
            )
        }

        val trends = ExerciseStats.trainedExercises(inPeriod)
            .filter { it.type != ExerciseType.CARDIO }
            .mapNotNull { trend(inPeriod, it) }
            .sortedByDescending { it.sessions }
            .take(MAX_EXERCISES)

        val volume = MuscleVolume.perWeek(MuscleVolume.setsPerRegion(inPeriod), weeks).map { (region, sets) ->
            RegionVolume(region, round1(sets), MuscleVolume.TARGET_MIN, MuscleVolume.TARGET_MAX)
        }

        val effort = EffortTrend(
            avgRirFirstHalf = averageRir(inPeriod.filter { date(it) < half }),
            avgRirSecondHalf = averageRir(inPeriod.filter { date(it) >= half }),
        )

        return ReviewContext(
            today = today,
            weeks = weeks,
            sessions = inPeriod.size,
            sessionsPerWeek = round1(inPeriod.size.toDouble() / weeks),
            weeklyGoal = weeklyGoal,
            routines = routineInfos,
            exercises = trends,
            volume = volume,
            effort = effort,
            available = exercises
                .filter { !it.archived && it.type != ExerciseType.CARDIO }
                .take(MAX_AVAILABLE)
                .map { e -> AvailableExercise(e.id, e.name, e.primaryMuscles.map { it.region }.toSet()) },
        )
    }

    private fun trend(workouts: List<WorkoutDetail>, exercise: Exercise): ExerciseTrend? {
        val metric = ExerciseStats.metricFor(exercise.type)
        val sessions = ExerciseStats.sessions(workouts, exercise.id)
        val points = ExerciseStats.points(sessions, metric)
        if (points.isEmpty()) return null
        val values = points.map { it.value }
        val lastSets = ProgressionRules.workingSets(sessions.last().sets)
        val top = lastSets.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
        val rirs = sessions.flatMap { ProgressionRules.workingSets(it.sets) }.mapNotNull { Effort.rir(it) }
        return ExerciseTrend(
            exerciseId = exercise.id,
            name = exercise.name,
            sessions = sessions.size,
            metric = metric,
            first = round1(values.first()),
            last = round1(values.last()),
            best = round1(values.max()),
            sessionsSinceBest = sessionsSinceBest(values),
            lastTopWeightKg = top?.weightKg ?: 0.0,
            lastTopReps = top?.reps ?: 0,
            avgRir = rirs.takeIf { it.isNotEmpty() }?.let { round1(it.average()) },
        )
    }

    /**
     * Sessions after the last new high. The first session sets the bar;
     * [4, 5, 5, 5] → 2 (no new high in the last two).
     */
    fun sessionsSinceBest(values: List<Double>): Int {
        var best = Double.NEGATIVE_INFINITY
        var bestIndex = 0
        values.forEachIndexed { index, value ->
            if (value > best) {
                best = value
                bestIndex = index
            }
        }
        return values.lastIndex - bestIndex
    }

    private fun averageRir(workouts: List<WorkoutDetail>): Double? {
        val rirs = workouts.flatMap { w ->
            w.exercises.filter { it.exercise.type != ExerciseType.CARDIO }.flatMap { ProgressionRules.workingSets(it.sets) }
        }.mapNotNull { Effort.rir(it) }
        return rirs.takeIf { it.isNotEmpty() }?.let { round1(it.average()) }
    }

    private fun round1(value: Double): Double = kotlin.math.round(value * 10) / 10

    private const val DAYS_PER_WEEK = 7
}
