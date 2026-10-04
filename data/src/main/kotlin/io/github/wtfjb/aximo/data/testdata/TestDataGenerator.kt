package io.github.wtfjb.aximo.data.testdata

import io.github.wtfjb.aximo.data.backup.BackupFile
import io.github.wtfjb.aximo.data.backup.CardioRow
import io.github.wtfjb.aximo.data.backup.ExerciseRow
import io.github.wtfjb.aximo.data.backup.ProgressionRow
import io.github.wtfjb.aximo.data.backup.RoutineExerciseRow
import io.github.wtfjb.aximo.data.backup.RoutineRow
import io.github.wtfjb.aximo.data.backup.SetRow
import io.github.wtfjb.aximo.data.backup.WorkoutExerciseRow
import io.github.wtfjb.aximo.data.backup.WorkoutRow
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.AB_WHEEL
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.BARBELL_CURL
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.BARBELL_ROW
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.BENCH_PRESS
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.CALF_RAISE
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.DIP
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.FACE_PULL
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.HAMMER_CURL
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.HANGING_LEG_RAISE
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.INCLINE_DUMBBELL_PRESS
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.LATERAL_RAISE
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.LEG_CURL
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.LEG_PRESS
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.OVERHEAD_PRESS
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.PULL_UP
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.ROMANIAN_DEADLIFT
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.SEATED_CABLE_ROW
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.SQUAT
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise.TRICEPS_PUSHDOWN
import io.github.wtfjb.aximo.domain.model.CardioSource
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.SetType
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Generates sample data for testing the app: a push/pull/legs routine set, about
 * [years] years of workouts (progress that flattens out, deload every fifth week,
 * skipped sessions, one break, notes, ratings, supersets, warm-up/drop/failure
 * sets, free workouts) and runs, rides and rows. The same [seed] and day give
 * the same data.
 *
 * Works on a [BackupFile] that already holds the exercises (the standard
 * exercises) and adds everything else, so [RoomDevDataRepository] can restore it
 * through the normal, validated import.
 */
class TestDataGenerator(private val seed: Long = 42, private val years: Int = 3) {

    private class Entry(
        val exercise: CatalogExercise,
        val sets: Int,
        val startKg: Double,
        val peakKg: Double,
        val step: Double = 2.5,
        val superset: String? = null,
    )

    private class Day(val name: String, val entries: List<Entry>)

    private val push = Day(
        "Push",
        listOf(
            Entry(BENCH_PRESS, 4, 60.0, 95.0),
            Entry(OVERHEAD_PRESS, 3, 35.0, 57.5),
            Entry(INCLINE_DUMBBELL_PRESS, 3, 20.0, 32.0, step = 2.0),
            Entry(LATERAL_RAISE, 3, 6.0, 12.0, step = 1.0, superset = "A"),
            Entry(TRICEPS_PUSHDOWN, 3, 25.0, 42.5, superset = "A"),
            Entry(DIP, 3, 0.0, 20.0),
        ),
    )
    private val pull = Day(
        "Pull",
        listOf(
            Entry(PULL_UP, 4, 0.0, 20.0),
            Entry(BARBELL_ROW, 4, 50.0, 85.0),
            Entry(SEATED_CABLE_ROW, 3, 45.0, 70.0),
            Entry(FACE_PULL, 3, 20.0, 35.0),
            Entry(BARBELL_CURL, 3, 25.0, 42.5, superset = "A"),
            Entry(HAMMER_CURL, 3, 10.0, 18.0, step = 2.0, superset = "A"),
        ),
    )
    private val legs = Day(
        "Legs",
        listOf(
            Entry(SQUAT, 4, 70.0, 130.0),
            Entry(ROMANIAN_DEADLIFT, 3, 70.0, 120.0),
            Entry(LEG_PRESS, 3, 120.0, 220.0, step = 5.0),
            Entry(LEG_CURL, 3, 35.0, 60.0),
            Entry(CALF_RAISE, 4, 60.0, 110.0, step = 5.0),
            Entry(HANGING_LEG_RAISE, 3, 0.0, 0.0),
        ),
    )

    /** Extra sessions without a routine (free training). */
    private val free = Day(
        "",
        listOf(
            Entry(BARBELL_CURL, 3, 25.0, 42.5),
            Entry(TRICEPS_PUSHDOWN, 3, 25.0, 42.5),
            Entry(AB_WHEEL, 3, 0.0, 0.0),
        ),
    )

    private val notes = listOf(
        "Gutes Training", "Schulter zwickt leicht", "Müde, kurze Einheit", "Wenig Schlaf", "Voller Fokus", "Neuer Bestwert",
    )

    /** Last working set of an exercise, for the progression suggestion. */
    private class LastSet(val weightKg: Double, val firstReps: Int)

    fun generate(base: BackupFile, now: Instant, zone: TimeZone): BackupFile {
        val random = Random(seed)
        val today = now.toLocalDateTime(zone).date
        val firstMonday = (today.minus(DatePeriod(years = years))).let { it.minus(DatePeriod(days = it.dayOfWeek.ordinal)) }
        val weekCount = firstMonday.daysUntil(today) / 7 + 1

        var nextExerciseId = (base.exercises.maxOfOrNull { it.id } ?: 0L) + 1
        val cardioExercises = listOf("Laufen", "Radfahren", "Rudern").map { name ->
            ExerciseRow(
                id = nextExerciseId++, name = name, type = ExerciseType.CARDIO.name, equipment = Equipment.OTHER.name,
                note = "", incrementKg = 0.0, repRangeMin = 1, repRangeMax = 1, restSeconds = 0, archived = false,
            )
        }
        val running = cardioExercises[0].id
        val cycling = cardioExercises[1].id
        val rowing = cardioExercises[2].id

        val exerciseIds = base.exercises.mapNotNull { row -> row.catalogId?.let { it to row.id } }.toMap()
        fun idOf(entry: Entry): Long? = exerciseIds[entry.exercise.catalogId]

        // Routines: ids 1..3, in the order of the week.
        val days = listOf(push, pull, legs)
        val routines = days.mapIndexed { index, day -> RoutineRow(id = index + 1L, name = day.name, position = index) }
        var routineExerciseId = 1L
        val routineExercises = days.flatMapIndexed { dayIndex, day ->
            day.entries.mapNotNull { entry ->
                val exerciseId = idOf(entry) ?: return@mapNotNull null
                RoutineExerciseRow(
                    id = routineExerciseId++, routineId = dayIndex + 1L, exerciseId = exerciseId, position = 0,
                    targetSets = entry.sets, repMin = entry.exercise.repMin, repMax = entry.exercise.repMax,
                    targetRir = 2, supersetGroup = entry.superset,
                )
            }.mapIndexed { position, row -> row.copy(position = position) }
        }

        val workouts = mutableListOf<WorkoutRow>()
        val workoutExercises = mutableListOf<WorkoutExerciseRow>()
        val sets = mutableListOf<SetRow>()
        val cardio = mutableListOf<CardioRow>()
        val lastSets = mutableMapOf<Long, LastSet>()
        var workoutId = 1L
        var workoutExerciseId = 1L
        var setId = 1L
        var cardioId = 1L

        // One break of three weeks, about 55 % into the timeline.
        val breakStart = (weekCount * 0.55).toInt()
        var trainedWeeks = 0
        var weeksSinceBreak = 99

        fun at(date: LocalDate, hour: Int, minute: Int): Instant = date.atTime(hour, minute).toInstant(zone)

        fun writeWorkout(day: Day, routineId: Long?, date: LocalDate, factor: Double, deload: Boolean): Long? {
            val start = at(date, 17, 30) + random.nextInt(0, 60).minutes
            if (start >= now) return null
            val id = workoutId++
            var time = start + 4.minutes
            var position = 0
            val usable = day.entries.filter { idOf(it) != null }
            usable.forEach { entry ->
                val exerciseId = idOf(entry)!!
                val weId = workoutExerciseId++
                workoutExercises += WorkoutExerciseRow(weId, id, exerciseId, position++, entry.superset, note = "")
                val progress = 1 - exp(-trainedWeeks / 60.0)
                val raw = entry.startKg + (entry.peakKg - entry.startKg) * progress
                val working = if (entry.peakKg == 0.0) 0.0 else roundTo(raw * factor, entry.step)
                val setCount = if (deload) maxOf(2, entry.sets - 1) else entry.sets
                var setPosition = 0

                fun add(weight: Double, reps: Int, rir: Int?, type: SetType) {
                    time += (150 + random.nextInt(0, 90)).seconds
                    sets += SetRow(
                        id = setId++, workoutExerciseId = weId, position = setPosition++, weightKg = weight, reps = reps,
                        rpe = null, rir = rir, setType = type.name, completedAtMillis = time.toEpochMilliseconds(),
                    )
                }

                // Warm-up for the first heavy exercise.
                if (position == 1 && working >= 40) {
                    add(roundTo(working * 0.5, 2.5), 8, null, SetType.WARM_UP)
                    add(roundTo(working * 0.75, 2.5), 4, null, SetType.WARM_UP)
                }
                var reps = 0
                for (index in 0 until setCount) {
                    val last = index == setCount - 1
                    reps = if (index == 0) {
                        (entry.exercise.repMax - random.nextInt(0, 3)).coerceAtLeast(entry.exercise.repMin)
                    } else {
                        (reps - random.nextInt(0, 2)).coerceAtLeast(entry.exercise.repMin - 2).coerceAtLeast(1)
                    }
                    val rir = when {
                        deload -> 3
                        last -> random.nextInt(0, 2)
                        index == 0 -> random.nextInt(2, 4)
                        else -> random.nextInt(1, 3)
                    }
                    if (index == 0) lastSets[exerciseId] = LastSet(working, reps)
                    val roll = random.nextDouble()
                    when {
                        last && !deload && roll < 0.06 -> add(working, reps, 0, SetType.FAILURE)
                        last && !deload && roll < 0.12 && working > 0 -> add(roundTo(working * 0.75, entry.step), reps + 3, 1, SetType.DROP)
                        else -> add(working, reps, rir, SetType.WORKING)
                    }
                }
                time += 90.seconds
            }
            val rating = if (random.nextDouble() < 0.7) random.nextInt(2, 6) else null
            val note = if (random.nextDouble() < 0.12) notes.random(random) else ""
            workouts += WorkoutRow(id, start.toEpochMilliseconds(), (time + 3.minutes).toEpochMilliseconds(), routineId, note, rating)
            return id
        }

        fun writeCardio(date: LocalDate, workoutRef: Long?, startAt: Instant?) {
            val progress = trainedWeeks / (weekCount * 1.0)
            val roll = random.nextDouble()
            val (exerciseId, distanceM, seconds, heartRate, elevation) = when {
                workoutRef != null -> CardioDraft(rowing, 2000.0 + random.nextInt(0, 500), 600, 138 + random.nextInt(0, 10), null)
                roll < 0.6 -> {
                    val km = 4.5 + progress * 2.5 + random.nextDouble() * 3.0
                    val pace = 360 - progress * 40 + random.nextInt(-20, 21)
                    CardioDraft(running, km * 1000, (km * pace).roundToInt(), 148 + random.nextInt(0, 18), 20.0 + random.nextInt(0, 100))
                }
                roll < 0.9 -> {
                    val km = 15.0 + random.nextDouble() * 25.0
                    CardioDraft(cycling, km * 1000, (km / 24.0 * 3600).roundToInt(), 125 + random.nextInt(0, 20), 80.0 + random.nextInt(0, 270))
                }
                else -> CardioDraft(rowing, 4000.0 + random.nextInt(0, 1500), 1200 + random.nextInt(0, 300), 140 + random.nextInt(0, 15), null)
            }
            val start = startAt ?: at(date, if (random.nextBoolean()) 7 else 18, random.nextInt(0, 45))
            if (start >= now) return
            cardio += CardioRow(
                id = cardioId++, workoutId = workoutRef, exerciseId = exerciseId, startedAtMillis = start.toEpochMilliseconds(),
                durationSec = seconds, distanceM = (distanceM / 10).roundToInt() * 10.0, avgHeartRate = heartRate, elevationM = elevation,
                note = if (random.nextDouble() < 0.1) "Locker" else "", source = CardioSource.MANUAL.name,
            )
        }

        for (week in 0 until weekCount) {
            val monday = firstMonday.plus(DatePeriod(days = week * 7))
            val inBreak = week in breakStart until breakStart + 3
            if (inBreak || random.nextDouble() < 0.05) {
                weeksSinceBreak = 0
                continue
            }
            trainedWeeks++
            val deload = trainedWeeks % 5 == 0
            // Fewer weights right after a break.
            val factor = (if (deload) 0.9 else 1.0) * (if (weeksSinceBreak < 2) 0.92 else 1.0)
            weeksSinceBreak++

            days.forEachIndexed { index, day ->
                if (random.nextDouble() < 0.1) return@forEachIndexed
                val shift = if (random.nextDouble() < 0.25) 1 else 0
                val date = monday.plus(DatePeriod(days = index * 2 + shift))
                if (date >= today) return@forEachIndexed
                val id = writeWorkout(day, index + 1L, date, factor, deload)
                if (id != null && day === legs && random.nextDouble() < 0.25) {
                    writeCardio(date, id, Instant.fromEpochMilliseconds(workouts.last().endedAtMillis!!) + 5.minutes)
                }
            }
            if (random.nextDouble() < 0.12) {
                val date = monday.plus(DatePeriod(days = 5))
                if (date < today) writeWorkout(free, null, date, factor, deload)
            }
            val cardioDays = listOf(1, 3, 6).shuffled(random)
            if (random.nextDouble() < 0.8) monday.plus(DatePeriod(days = cardioDays[0])).takeIf { it < today }?.let { writeCardio(it, null, null) }
            if (random.nextDouble() < 0.35) monday.plus(DatePeriod(days = cardioDays[1])).takeIf { it < today }?.let { writeCardio(it, null, null) }
        }

        val progression = days.flatMap { it.entries }.distinctBy { it.exercise }.mapNotNull { entry ->
            val exerciseId = idOf(entry) ?: return@mapNotNull null
            val last = lastSets[exerciseId] ?: return@mapNotNull null
            when {
                last.firstReps >= entry.exercise.repMax ->
                    ProgressionRow(exerciseId, last.weightKg + entry.step, entry.exercise.repMin, ProgressionReason.INCREASE_WEIGHT.name)
                last.firstReps <= entry.exercise.repMin ->
                    ProgressionRow(exerciseId, last.weightKg, last.firstReps, ProgressionReason.HOLD.name)
                else -> ProgressionRow(exerciseId, last.weightKg, last.firstReps + 1, ProgressionReason.INCREASE_REPS.name)
            }
        }

        return base.copy(
            exercises = base.exercises + cardioExercises,
            routines = routines,
            routineExercises = routineExercises,
            workouts = workouts,
            workoutExercises = workoutExercises,
            sets = sets,
            cardioEntries = cardio,
            progression = progression,
        )
    }

    private data class CardioDraft(val exerciseId: Long, val distanceM: Double, val seconds: Int, val heartRate: Int, val elevation: Double?)

    private fun roundTo(value: Double, step: Double): Double = (value / step).roundToInt() * step
}
