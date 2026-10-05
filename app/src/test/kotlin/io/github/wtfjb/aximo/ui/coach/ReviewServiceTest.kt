package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.review.GeneratedReview
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.ReviewException
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionReason
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ReviewServiceTest {

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL, primaryMuscles = setOf(MuscleGroup.CHEST))
    private val dips = bench.copy(id = 2, name = "Dips")
    private val exercises = FakeExerciseRepository(listOf(bench, dips))
    private val routines = FakeRoutineRepository(
        listOf(
            RoutineWithExercises(
                Routine(id = 1, name = "Push A"),
                listOf(RoutineExercise(routineId = 1, exerciseId = bench.id, position = 0, targetSets = 3, repMin = 6, repMax = 8)),
            ),
        ),
    )
    private val workouts = FakeWorkoutRepository(mapOf(bench.id to bench))
    private val profiles = FakeAiProfileRepository()
    private val reviews = FakeAiReviewRepository()

    private var answer: GeneratedReview = GeneratedReview("ok", emptyList())
    private var sentLanguage: String? = null
    private var usedKey: String? = null

    private val service = ReviewService(
        workouts = workouts,
        routines = routines,
        exercises = exercises,
        settings = FakeSettingsRepository(),
        profiles = profiles,
        factory = { _, key ->
            usedKey = key
            object : AiProvider {
                override suspend fun complete(request: AiRequest): String = error("not used, the generator is faked")
            }
        },
        generator = { _, _, language ->
            sentLanguage = language
            answer
        },
        reviews = reviews,
        time = TimeSource { now },
        zone = { TimeZone.UTC },
    )

    private fun finishedBench(daysAgo: Int) = WorkoutDetail(
        Workout(id = daysAgo.toLong(), startedAt = now - daysAgo.days, endedAt = now - daysAgo.days + 1.hours, routineId = 1),
        listOf(
            WorkoutExerciseDetail(
                WorkoutExercise(id = daysAgo * 10L, workoutId = daysAgo.toLong(), exerciseId = bench.id, position = 0),
                bench,
                listOf(SetEntry(workoutExerciseId = daysAgo * 10L, position = 0, weightKg = 80.0, reps = 8, completedAt = now - daysAgo.days)),
            ),
        ),
    )

    private suspend fun expectReviewError(): ReviewException.Reason {
        try {
            service.createReview("de")
        } catch (e: ReviewException) {
            return e.reason
        }
        fail("Expected ReviewException")
        throw AssertionError()
    }

    @Test
    fun needsProfileAndData() = runTest {
        assertEquals(ReviewException.Reason.NO_PROFILE, expectReviewError())

        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        assertEquals(ReviewException.Reason.NO_DATA, expectReviewError())
    }

    @Test
    fun storesOnlyApplicableSuggestions() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Set("k"))
        workouts.allFinished.value = listOf(finishedBench(3), finishedBench(10))
        answer = GeneratedReview(
            summary = "2 Einheiten.",
            suggestions = listOf(
                GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "mehr"),
                GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "duplicate"),
                GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 2, 4), "stale from"),
                GeneratedSuggestion(SuggestionChange.AddExercise(1, dips.id, 3, 8, 12, 2), "dips"),
                GeneratedSuggestion(SuggestionChange.RemoveExercise(99, bench.id), "unknown routine"),
            ),
            dropped = 1,
        )

        service.createReview("de")

        val review = reviews.latest.value!!
        assertEquals("de", sentLanguage)
        assertEquals("k", usedKey)
        assertEquals("2 Einheiten.", review.summary)
        assertEquals(listOf("mehr", "dips"), review.suggestions.map { it.rationale })
        assertEquals(4, review.droppedSuggestions)
        assertEquals(now, review.createdAt)
    }

    @Test
    fun dropsImplausibleSuggestionsAndKeepsTheReason() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        // Two sessions with one set each: chest is far below 10–20 sets per week.
        workouts.allFinished.value = listOf(finishedBench(3), finishedBench(10))
        answer = GeneratedReview(
            "s",
            listOf(
                GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 2), "weniger", SuggestionReason.STAGNATION),
                GeneratedSuggestion(SuggestionChange.RepRange(1, bench.id, 6, 8, 8, 10), "mehr Wdh.", SuggestionReason.REP_CEILING),
            ),
        )

        service.createReview("de")

        val review = reviews.latest.value!!
        assertEquals(listOf("mehr Wdh."), review.suggestions.map { it.rationale })
        assertEquals(listOf(SuggestionReason.REP_CEILING), review.suggestions.map { it.reason })
        assertEquals(1, review.droppedSuggestions)
    }

    @Test
    fun aiErrorsPassThroughAndNothingIsStored() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        workouts.allFinished.value = listOf(finishedBench(3))
        val failing = ReviewService(
            workouts, routines, exercises, FakeSettingsRepository(), profiles,
            { _, _ -> object : AiProvider { override suspend fun complete(request: AiRequest) = "" } },
            { _, _, _ -> throw AiException(AiException.Reason.TIMEOUT) },
            reviews, TimeSource { now }, { TimeZone.UTC },
        )

        val reason = try {
            failing.createReview("en")
            null
        } catch (e: AiException) {
            e.reason
        }

        assertEquals(AiException.Reason.TIMEOUT, reason)
        assertEquals(null, reviews.latest.value)
    }

    @Test
    fun applyChangesRoutineOnlyOnceAndDiscard() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        workouts.allFinished.value = listOf(finishedBench(3))
        answer = GeneratedReview(
            "s",
            listOf(
                GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "a"),
                GeneratedSuggestion(SuggestionChange.RepRange(1, bench.id, 6, 8, 8, 10), "b"),
                GeneratedSuggestion(SuggestionChange.AddExercise(1, dips.id, 3, 8, 12, null), "c"),
            ),
        )
        service.createReview("de")
        val (sets, reps, add) = reviews.latest.value!!.suggestions

        assertTrue(service.apply(sets.id))
        assertFalse("already applied", service.apply(sets.id))
        assertTrue(service.apply(reps.id))
        service.discard(add.id)

        val routine = routines.getRoutine(1)!!
        assertEquals(4, routine.exercises.single().targetSets)
        assertEquals(8, routine.exercises.single().repMin)
        assertEquals(
            listOf(SuggestionStatus.APPLIED, SuggestionStatus.APPLIED, SuggestionStatus.DISCARDED),
            reviews.latest.value!!.suggestions.map { it.status },
        )
        assertFalse("discarded", service.apply(add.id))
    }

    @Test
    fun suggestionNoLongerFitsAfterUserEdit() = runTest {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        workouts.allFinished.value = listOf(finishedBench(3))
        answer = GeneratedReview("s", listOf(GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "a")))
        service.createReview("de")
        val suggestion = reviews.latest.value!!.suggestions.single()

        val routine = routines.getRoutine(1)!!
        routines.saveRoutine(routine.routine, routine.exercises.map { it.copy(targetSets = 5) })

        assertFalse(service.isApplicable(suggestion))
        assertFalse(service.apply(suggestion.id))
        assertEquals(SuggestionStatus.OPEN, reviews.latest.value!!.suggestions.single().status)
        assertEquals(5, routines.observeRoutinesWithExercises().first().single().exercises.single().targetSets)
    }
}
