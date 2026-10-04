package io.github.wtfjb.aximo.domain.stats

import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlin.time.Instant

/** One session of an exercise: its sets from one finished workout, in logged order. */
data class ExerciseSession(
    val workoutId: Long,
    val routineId: Long?,
    val startedAt: Instant,
    val sets: List<SetEntry>,
)

/** What the progress chart shows: e1RM for weighted exercises, most reps in a set for bodyweight. */
enum class ProgressMetric { E1RM, REPS }

/** One value per session, e.g. the best e1RM of that day. */
data class ProgressPoint(val workoutId: Long, val startedAt: Instant, val value: Double)

/** Best values over a list of sessions. Warm-ups never count. */
data class ExerciseBests(
    val bestE1rm: Double?,
    /** Heaviest weight; for bodyweight exercises the heaviest extra load. */
    val maxWeightKg: Double?,
    /** Set with the best e1RM (weighted) or the most reps (bodyweight). */
    val bestSet: SetEntry?,
    val volumeKg: Double,
    val totalReps: Int,
    val sessions: Int,
)

/** Most reps done with [weightKg]. */
data class RepRecord(val weightKg: Double, val reps: Int)

/** Progress and best values per exercise (A-07). */
object ExerciseStats {

    fun metricFor(type: ExerciseType): ProgressMetric =
        if (type == ExerciseType.BODYWEIGHT) ProgressMetric.REPS else ProgressMetric.E1RM

    /**
     * Sessions of [exerciseId] in finished workouts, oldest first. Several entries
     * of the exercise in one workout are merged. Sessions without a completed
     * set are left out, running workouts too.
     */
    fun sessions(workouts: List<WorkoutDetail>, exerciseId: Long): List<ExerciseSession> =
        workouts
            .filter { it.workout.endedAt != null }
            .mapNotNull { detail ->
                val sets = detail.exercises
                    .filter { it.entry.exerciseId == exerciseId }
                    .flatMap { entry -> entry.sets.filter { it.completedAt != null } }
                if (sets.isEmpty()) {
                    null
                } else {
                    ExerciseSession(detail.workout.id, detail.workout.routineId, detail.workout.startedAt, sets)
                }
            }
            .sortedBy { it.startedAt }

    /** Value of one session for the chart; null if no set counts (e.g. only sets above 12 reps for e1RM). */
    fun sessionValue(sets: List<SetEntry>, metric: ProgressMetric): Double? = when (metric) {
        ProgressMetric.E1RM -> Records.bestE1rm(sets)
        ProgressMetric.REPS -> Records.countingSets(sets).maxOfOrNull { it.reps }?.toDouble()
    }

    fun points(sessions: List<ExerciseSession>, metric: ProgressMetric): List<ProgressPoint> =
        sessions.mapNotNull { session ->
            sessionValue(session.sets, metric)?.let { ProgressPoint(session.workoutId, session.startedAt, it) }
        }

    /** Change from the first to the last point; null with fewer than two points. */
    fun change(points: List<ProgressPoint>): Double? {
        if (points.size < 2) return null
        return StatsCalendar.relativeChange(points.first().value, points.last().value)
    }

    fun bests(sessions: List<ExerciseSession>, metric: ProgressMetric): ExerciseBests {
        val sets = Records.countingSets(sessions.flatMap { it.sets })
        val bestSet = when (metric) {
            ProgressMetric.E1RM -> sets
                .filter { Records.e1rm(it.weightKg, it.reps) != null }
                .maxByOrNull { Records.e1rm(it.weightKg, it.reps)!! }
                ?: sets.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
            ProgressMetric.REPS -> sets.maxWithOrNull(compareBy({ it.reps }, { it.weightKg }))
        }
        return ExerciseBests(
            bestE1rm = Records.bestE1rm(sets),
            maxWeightKg = sets.maxOfOrNull { it.weightKg },
            bestSet = bestSet,
            volumeKg = Records.volume(sets),
            totalReps = sets.sumOf { it.reps },
            sessions = sessions.size,
        )
    }

    /**
     * Workouts whose session value beat every earlier session ("PR" badge).
     * The first session is never a record, as on the finish screen.
     */
    fun recordSessions(sessions: List<ExerciseSession>, metric: ProgressMetric): Set<Long> {
        val records = mutableSetOf<Long>()
        var best: Double? = null
        for (session in sessions.sortedBy { it.startedAt }) {
            val value = sessionValue(session.sets, metric) ?: continue
            val previous = best
            if (previous != null && value > previous + EPSILON) records += session.workoutId
            if (previous == null || value > previous) best = value
        }
        return records
    }

    /**
     * Most reps per weight, heaviest first. A weight is left out if a heavier
     * one was done for at least as many reps, so the list stays short.
     */
    fun repRecords(sessions: List<ExerciseSession>): List<RepRecord> {
        val bestPerWeight = Records.countingSets(sessions.flatMap { it.sets })
            .groupBy { it.weightKg }
            .map { (weight, sets) -> RepRecord(weight, sets.maxOf { it.reps }) }
            .sortedByDescending { it.weightKg }
        val result = mutableListOf<RepRecord>()
        var mostReps = 0
        for (record in bestPerWeight) {
            if (record.reps > mostReps) {
                result += record
                mostReps = record.reps
            }
        }
        return result
    }

    /** Strength and bodyweight exercises with at least one finished session, most recently trained first. */
    fun trainedExercises(workouts: List<WorkoutDetail>): List<Exercise> =
        workouts
            .filter { it.workout.endedAt != null }
            .sortedByDescending { it.workout.startedAt }
            .flatMap { detail -> detail.exercises.filter { entry -> entry.sets.any { it.completedAt != null } } }
            .map { it.exercise }
            .filter { it.type != ExerciseType.CARDIO }
            .distinctBy { it.id }

    private const val EPSILON = 1e-6
}
