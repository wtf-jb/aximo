package io.github.wtfjb.aximo.domain.logging

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderFactory
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.PlannedSet
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import kotlinx.coroutines.flow.first

/** What [LoggingService.save] did. */
data class LoggingResult(val exercises: Int, val sets: Int, val created: Int)

/**
 * Logging by text or voice (B-04): asks the AI to read the text and returns a preview.
 * Nothing is stored until the user confirms and [save] is called; the AI never writes data.
 */
class LoggingService(
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository,
    private val starter: WorkoutStarter,
    private val settings: SettingsRepository,
    private val catalog: CatalogRepository,
    private val profiles: AiProfileRepository,
    private val factory: AiProviderFactory,
    private val generator: LoggingGenerator,
    private val time: TimeSource,
) {

    /** Exactly what would be sent for [text]. */
    suspend fun input(text: String): LoggingInput =
        LoggingInput.from(text, exercises.observeExercises(includeArchived = false).first(), settings.training.first())

    /** Asks the active profile to read [text]. Throws [LoggingException] or [AiException]. */
    suspend fun parse(text: String, language: String): LoggingProposal {
        if (text.isBlank()) throw LoggingException(LoggingException.Reason.EMPTY_TEXT)
        val profile = AiProfiles.active(profiles.observeProfiles().first())
            ?: throw LoggingException(LoggingException.Reason.NO_PROFILE)
        val input = input(text)
        val provider = factory.create(profile, profiles.apiKey(profile.id))
        val parsed = generator.parse(provider, input, language)
        val own = exercises.observeExercises(includeArchived = false).first().filter { it.type != ExerciseType.CARDIO }
        return LoggingBuilder.build(parsed, own, catalog.entries(), settings.training.first())
    }

    /**
     * Logs the confirmed entries as done sets in the running workout (a free one is
     * started if none is running) and creates the new exercises among them.
     * Every entry becomes its own exercise block at the end of the workout.
     */
    suspend fun save(proposal: LoggingProposal): LoggingResult {
        val confirmed = proposal.confirmed
        if (confirmed.isEmpty()) return LoggingResult(0, 0, 0)
        val workoutId = starter.startOrResume()
        val now = time.now()
        val training = settings.training.first()
        // The same new name twice in one text creates one exercise.
        val created = mutableMapOf<String, Long>()
        var sets = 0
        for (entry in confirmed) {
            val exerciseId = when (val choice = entry.choice) {
                is ExerciseChoice.Existing -> choice.exercise.id
                is ExerciseChoice.Create -> created.getOrPut(choice.exercise.name.lowercase()) {
                    exercises.saveExercise(choice.exercise.toExercise(training))
                }
                null -> continue
            }
            val planned = entry.sets.map { PlannedSet(it.weightKg ?: 0.0, it.reps, it.rir, it.setType, it.rpe, now) }
            workouts.addExercise(workoutId, exerciseId, null, planned)
            sets += planned.size
        }
        return LoggingResult(exercises = confirmed.size, sets = sets, created = created.size)
    }
}
