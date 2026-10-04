package io.github.wtfjb.aximo.domain.model

/** Kind of exercise. Decides which fields are logged (sets vs. cardio). */
enum class ExerciseType { STRENGTH, BODYWEIGHT, CARDIO }

/** Equipment an exercise needs. */
enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, KETTLEBELL, BAND, BODYWEIGHT, OTHER }

/**
 * Fixed list of muscle groups. Used for filtering and for the weekly volume per
 * muscle group (A-07). Display names live in the string resources of :app.
 */
enum class MuscleGroup {
    CHEST,
    UPPER_BACK,
    LATS,
    LOWER_BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    ADDUCTORS,
}

/**
 * An exercise the user can log (A-01).
 *
 * All weights are in kg. [roundingStepKg] overrides the global plate step
 * (2.5 kg) for this exercise; null means "use the global setting".
 */
data class Exercise(
    val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val primaryMuscles: Set<MuscleGroup> = emptySet(),
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val note: String = "",
    val incrementKg: Double = DEFAULT_INCREMENT_KG,
    val repRangeMin: Int = DEFAULT_REP_RANGE_MIN,
    val repRangeMax: Int = DEFAULT_REP_RANGE_MAX,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val roundingStepKg: Double? = null,
    val catalogId: String? = null,
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(incrementKg >= 0) { "incrementKg must not be negative" }
        require(repRangeMin in 1..repRangeMax) { "rep range must be 1 ≤ min ≤ max" }
        require(restSeconds >= 0) { "restSeconds must not be negative" }
        require(roundingStepKg == null || roundingStepKg > 0) { "roundingStepKg must be positive" }
        require(primaryMuscles.intersect(secondaryMuscles).isEmpty()) {
            "a muscle group is either primary or secondary, not both"
        }
    }

    companion object {
        const val DEFAULT_INCREMENT_KG = 2.5
        const val DEFAULT_REP_RANGE_MIN = 8
        const val DEFAULT_REP_RANGE_MAX = 12
        const val DEFAULT_REST_SECONDS = 120
    }
}
