package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
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
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
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
    private val workouts = FakeWorkoutRepository(mapOf(bench.id to bench)).apply {
        allFinished.value = listOf(
            WorkoutDetail(
                Workout(id = 1, startedAt = now - 2.days, endedAt = now - 2.days + 1.hours, routineId = 1),
                listOf(
                    WorkoutExerciseDetail(
                        WorkoutExercise(10, 1, bench.id, 0),
                        bench,
                        listOf(SetEntry(workoutExerciseId = 10, position = 0, weightKg = 80.0, reps = 8, completedAt = now - 2.days)),
                    ),
                ),
            ),
        )
    }
    private val profiles = FakeAiProfileRepository()
    private val reviews = FakeAiReviewRepository()
    private val preferences = object : AiPreferences {
        val accepted = MutableStateFlow(false)
        override val dataNoticeAccepted = accepted
        override suspend fun acceptDataNotice() {
            accepted.value = true
        }
    }

    private var calls = 0
    private var answer: () -> GeneratedReview = {
        GeneratedReview(
            "Gut.",
            listOf(
                GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "9 Sätze"),
                GeneratedSuggestion(SuggestionChange.RepRange(1, bench.id, 6, 8, 8, 10), "stagniert"),
                GeneratedSuggestion(SuggestionChange.AddExercise(1, dips.id, 3, 8, 12, null), "Trizeps"),
            ),
        )
    }

    private val service = ReviewService(
        workouts, routines, exercises, FakeSettingsRepository(), profiles,
        { _, _ -> object : AiProvider { override suspend fun complete(request: AiRequest) = "" } },
        { _, _, _ ->
            calls++
            answer()
        },
        reviews, TimeSource { now }, { TimeZone.UTC },
    )

    private val vm by lazy {
        CoachViewModel(service, reviews, routines, exercises, preferences, language = { "de" }, formatContext = { "ctx ${it.sessions}" })
    }

    @Test
    fun firstReviewAsksForConsentOnce() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        vm.uiState.launchIn(backgroundScope)
        assertNull(vm.uiState.value.review)

        vm.requestReview()
        assertTrue(vm.uiState.value.showNotice)
        assertEquals(0, calls)

        vm.acceptNotice()
        assertFalse(vm.uiState.value.showNotice)
        assertEquals(1, calls)
        assertEquals("Gut.", vm.uiState.value.review?.summary)
        assertEquals(listOf("Push A"), vm.uiState.value.items.map { it.routineName }.distinct())
        assertEquals("Dips", vm.uiState.value.items[2].exerciseName)

        vm.requestReview()
        assertFalse(vm.uiState.value.showNotice)
        assertEquals(2, calls)
    }

    @Test
    fun dismissingNoticeSendsNothing() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        vm.uiState.launchIn(backgroundScope)

        vm.requestReview()
        vm.dismissNotice()

        assertEquals(0, calls)
        assertFalse(preferences.accepted.value)
    }

    @Test
    fun applyMarksOthersNotApplicableWhenTheyClash() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        preferences.accepted.value = true
        vm.uiState.launchIn(backgroundScope)
        vm.requestReview()
        val (sets, reps, add) = vm.uiState.value.items

        vm.apply(sets.suggestion.id)
        vm.discard(add.suggestion.id)

        val items = vm.uiState.value.items
        assertEquals(SuggestionStatus.APPLIED, items[0].suggestion.status)
        assertTrue("rep range still fits", items[1].applicable)
        assertEquals(SuggestionStatus.DISCARDED, items[2].suggestion.status)
        assertEquals(listOf(reps.suggestion.id), vm.uiState.value.openApplicable.map { it.suggestion.id })
    }

    @Test
    fun applyAll() = runTest(UnconfinedTestDispatcher()) {
        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        preferences.accepted.value = true
        vm.uiState.launchIn(backgroundScope)
        vm.requestReview()

        vm.applyAll()

        assertTrue(vm.uiState.value.items.all { it.suggestion.status == SuggestionStatus.APPLIED })
        val routine = routines.getRoutine(1)!!
        assertEquals(listOf(bench.id, dips.id), routine.exercises.map { it.exerciseId })
        assertEquals(4, routine.exercises[0].targetSets)
        assertEquals(8, routine.exercises[0].repMin)
    }

    @Test
    fun errorsAreShown() = runTest(UnconfinedTestDispatcher()) {
        preferences.accepted.value = true
        vm.uiState.launchIn(backgroundScope)

        vm.requestReview()
        assertEquals(CoachError.Review(ReviewException.Reason.NO_PROFILE), vm.uiState.value.error)

        profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)
        answer = { throw AiException(AiException.Reason.UNAUTHORIZED, 401, "bad key") }
        vm.requestReview()
        assertEquals(CoachError.Ai(AiException.Reason.UNAUTHORIZED, 401, "bad key"), vm.uiState.value.error)
        assertFalse(vm.uiState.value.running)
    }

    @Test
    fun sentDataShowsContext() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.showSentData()
        assertEquals("ctx 1", vm.uiState.value.sentData)

        vm.dismissSentData()
        assertNull(vm.uiState.value.sentData)
    }
}
