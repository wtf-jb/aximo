package io.github.wtfjb.aximo.domain.review

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiProviderFactory
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** What the AI answered, already parsed and checked against the schema. */
data class GeneratedReview(
    val summary: String,
    val suggestions: List<GeneratedSuggestion>,
    /** Entries that did not match the schema. */
    val dropped: Int = 0,
)

data class GeneratedSuggestion(val change: SuggestionChange, val rationale: String)

/** Builds the prompt, calls the provider and parses the JSON answer; lives in `:ai`. */
fun interface ReviewGenerator {
    /** [language] is the app language as a tag ("de", "en"); rationales are written in it. */
    suspend fun generate(provider: AiProvider, context: ReviewContext, language: String): GeneratedReview
}

/** Why a review could not be created, besides [AiException]. */
class ReviewException(val reason: Reason) : Exception("Review not possible: $reason") {
    enum class Reason { NO_PROFILE, NO_DATA }
}

/**
 * Weekly review (B-02): collects the context, asks the AI and stores the
 * answer as suggestions. Applying a suggestion is a separate step the user
 * confirms; the AI never writes data itself.
 */
class ReviewService(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val profiles: AiProfileRepository,
    private val factory: AiProviderFactory,
    private val generator: ReviewGenerator,
    private val reviews: AiReviewRepository,
    private val time: TimeSource,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

    /** Exactly what would be sent now. */
    suspend fun context(): ReviewContext = ReviewContextBuilder.build(
        workouts = workouts.observeFinished().first(),
        routines = routines.observeRoutinesWithExercises().first(),
        exercises = exercises.observeExercises(includeArchived = true).first(),
        weeklyGoal = settings.training.first().weeklyGoal,
        today = StatsCalendar.localDate(time.now(), zone()),
        zone = zone(),
    )

    /**
     * Creates and stores a review with the active profile. Suggestions that do
     * not fit the current routines are dropped. Throws [ReviewException] or
     * [AiException]; returns the review id.
     */
    suspend fun createReview(language: String): Long {
        val profile = AiProfiles.active(profiles.observeProfiles().first())
            ?: throw ReviewException(ReviewException.Reason.NO_PROFILE)
        val context = context()
        if (context.isEmpty) throw ReviewException(ReviewException.Reason.NO_DATA)

        val provider = factory.create(profile, profiles.apiKey(profile.id))
        val generated = generator.generate(provider, context, language)

        val valid = SuggestionApplier.applicable(
            generated.suggestions,
            routines.observeRoutinesWithExercises().first(),
            exercises.observeExercises(includeArchived = true).first(),
        )
        val review = AiReview(
            createdAt = time.now(),
            weeks = context.weeks,
            summary = generated.summary,
            suggestions = valid.map { AiSuggestion(change = it.change, rationale = it.rationale) },
            droppedSuggestions = generated.dropped + (generated.suggestions.size - valid.size),
        )
        return reviews.saveReview(review)
    }

    /** True if the suggestion still fits its routine (it may have changed since). */
    suspend fun isApplicable(suggestion: AiSuggestion): Boolean {
        val routine = routines.getRoutine(suggestion.change.routineId)
        return SuggestionApplier.isApplicable(suggestion.change, routine, exercises.observeExercises(includeArchived = true).first())
    }

    /**
     * Applies an open suggestion after the user confirmed it. Works for review
     * and chat suggestions alike (B-05). Returns false if it no longer fits.
     */
    suspend fun apply(suggestionId: Long): Boolean {
        val suggestion = reviews.getSuggestion(suggestionId) ?: return false
        if (suggestion.status != SuggestionStatus.OPEN) return false
        val routine = routines.getRoutine(suggestion.change.routineId) ?: return false
        if (!SuggestionApplier.isApplicable(suggestion.change, routine, exercises.observeExercises(includeArchived = true).first())) return false
        routines.saveRoutine(routine.routine, SuggestionApplier.apply(suggestion.change, routine))
        reviews.setStatus(suggestionId, SuggestionStatus.APPLIED)
        return true
    }

    suspend fun discard(suggestionId: Long) = reviews.setStatus(suggestionId, SuggestionStatus.DISCARDED)
}
