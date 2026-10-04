package io.github.wtfjb.aximo.domain.plan

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import kotlin.math.roundToInt

enum class PlanGoal { STRENGTH, HYPERTROPHY, GENERAL }

/** The choices offered on the "Mit KI erstellen" form (B-03). */
object PlanOptions {
    val days: List<Int> = (2..6).toList()
    val minutes: List<Int> = listOf(30, 45, 60, 90)

    /** "OTHER" has no chip: such exercises are always offered. */
    val equipment: List<Equipment> = listOf(
        Equipment.BARBELL,
        Equipment.DUMBBELL,
        Equipment.MACHINE,
        Equipment.CABLE,
        Equipment.KETTLEBELL,
        Equipment.BAND,
        Equipment.BODYWEIGHT,
    )
}

/** What the user wants from the plan (B-03): goal, days, duration, equipment, restrictions. */
data class PlanRequest(
    val goal: PlanGoal = PlanGoal.HYPERTROPHY,
    val daysPerWeek: Int = 3,
    val minutes: Int = 60,
    val equipment: Set<Equipment> = PlanOptions.equipment.toSet(),
    /** Free text: injuries, preferences. */
    val restrictions: String = "",
) {
    init {
        require(daysPerWeek in 1..7) { "daysPerWeek must be 1..7" }
        require(minutes in 15..180) { "minutes must be 15..180" }
    }

    /** Rough size of a session: about 10 minutes per exercise with warm-up and rest. */
    val exercisesPerRoutine: Int get() = (minutes / 10.0).roundToInt().coerceIn(2, 10)

    val cleanRestrictions: String get() = restrictions.trim().take(MAX_RESTRICTIONS)

    companion object {
        const val MAX_RESTRICTIONS = 300
    }
}

/** An exercise the AI may use in the plan. */
data class PlanExercise(
    val exerciseId: Long,
    val name: String,
    val equipment: Equipment,
    val regions: Set<BodyRegion>,
    val repMin: Int,
    val repMax: Int,
)

/** Exactly what is sent to the AI for B-03: the request plus the usable exercises. */
data class PlanInput(val request: PlanRequest, val exercises: List<PlanExercise>) {
    companion object {
        /** Too few exercises make no plan. */
        const val MIN_EXERCISES = 6

        /** Sent at most; the list is sorted by name. */
        const val MAX_EXERCISES = 120

        /** Strength and bodyweight exercises that are not archived and match the chosen equipment. */
        fun from(request: PlanRequest, all: List<Exercise>): PlanInput {
            val usable = all
                .filter { !it.archived && it.type != ExerciseType.CARDIO }
                .filter { it.equipment == Equipment.OTHER || it.equipment in request.equipment }
                .sortedBy { it.name.lowercase() }
                .take(MAX_EXERCISES)
                .map { PlanExercise(it.id, it.name, it.equipment, it.primaryMuscles.map { m -> m.region }.toSet(), it.repRangeMin, it.repRangeMax) }
            return PlanInput(request, usable)
        }
    }
}
