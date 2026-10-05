package io.github.wtfjb.aximo.domain.cardio

import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType

/** How the tempo of an activity is shown first: pace per km, pace per 500 m or speed. */
enum class PaceStyle { PER_KM, PER_500M, SPEED }

/** Activities created once so the cardio screen isn't empty. Identified by [catalogId]. */
enum class DefaultActivity(val catalogId: String, val paceStyle: PaceStyle) {
    RUNNING("cardio_running", PaceStyle.PER_KM),
    CYCLING("cardio_cycling", PaceStyle.SPEED),
    ROWING("cardio_rowing", PaceStyle.PER_500M),
    OTHER("cardio_other", PaceStyle.SPEED),
}

/** Cardio activities are exercises of type CARDIO. */
object CardioActivities {

    /** The default activity [exercise] was created from, or null for the user's own. */
    fun defaultOf(exercise: Exercise): DefaultActivity? =
        DefaultActivity.entries.firstOrNull { it.catalogId == exercise.catalogId }

    /** Pace style of the activity; own activities show speed. */
    fun paceStyle(exercise: Exercise): PaceStyle = defaultOf(exercise)?.paceStyle ?: PaceStyle.SPEED

    /** Defaults are created only while no cardio exercise exists at all, archived ones included. */
    fun needsDefaults(allExercises: List<Exercise>): Boolean = allExercises.none { it.type == ExerciseType.CARDIO }

    /** The default activities with display names from [name] (localized by the caller). */
    fun defaults(name: (DefaultActivity) -> String): List<Exercise> = DefaultActivity.entries.map { activity ->
        Exercise(name = name(activity), type = ExerciseType.CARDIO, equipment = Equipment.OTHER, catalogId = activity.catalogId)
    }

    /**
     * Activities to choose from: non-archived cardio exercises, the defaults in
     * their fixed order first, then the user's own by name. [keepId] stays in the
     * list even if archived (editing an old entry).
     */
    fun choices(allExercises: List<Exercise>, keepId: Long? = null): List<Exercise> =
        allExercises
            .filter { it.type == ExerciseType.CARDIO && (!it.archived || it.id == keepId) }
            .sortedWith(compareBy({ defaultOf(it)?.ordinal ?: Int.MAX_VALUE }, { it.name.lowercase() }))

    /** Preselected activity for a new entry: the one of [latest] if still a choice, else the first. */
    fun preselect(choices: List<Exercise>, latest: CardioEntry?): Long? =
        choices.firstOrNull { it.id == latest?.exerciseId }?.id ?: choices.firstOrNull()?.id
}
