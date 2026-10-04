package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.chat.ChatHistory
import io.github.wtfjb.aximo.domain.chat.ChatService
import io.github.wtfjb.aximo.domain.plan.PlanService
import io.github.wtfjb.aximo.domain.chat.GeneratedReply
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.review.ReviewException
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.ui.ai.FakeAiPreferences
import io.github.wtfjb.aximo.ui.ai.FakeAiProfileRepository
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.routine.FakeRoutineRepository
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import io.github.wtfjb.aximo.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val exercises = FakeExerciseRepository(listOf(bench))
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
    private val chats = FakeAiChatRepository()
    private val reviews = FakeAiReviewRepository(chats)
    private val preferences = FakeAiPreferences()

    private var calls = 0
    private var answer: suspend () -> GeneratedReply = { GeneratedReply("Antwort") }
    private val provider = { _: io.github.wtfjb.aximo.domain.ai.AiProviderProfile, _: String? ->
        object : AiProvider { override suspend fun complete(request: AiRequest) = "" }
    }

    private val chatService = ChatService(
        workouts, routines, exercises, FakeSettingsRepository(), profiles, provider,
        { _, _, _ ->
            calls++
            answer()
        },
        chats, { emptyList() }, reviews, PlanService(exercises, routines, profiles, provider, { _, _, _ -> error("not used") }),
        TimeSource { now }, { TimeZone.UTC },
    )
    private val reviewService = ReviewService(
        workouts, routines, exercises, FakeSettingsRepository(), profiles, provider,
        { _, _, _ -> error("not used") }, reviews, TimeSource { now }, { TimeZone.UTC },
    )

    private val vm by lazy {
        ChatViewModel(
            chatService, reviewService, routines, exercises, preferences,
            language = { "de" },
            formatPayload = { p -> "ctx ${p.context.review.sessions} msgs ${p.messages.map { it.text }}" },
        )
    }

    private suspend fun withProfile() = profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)

    @Test
    fun firstMessageAsksForTheChatNoticeOnce() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.accepted.value = true // review notice alone is not enough
        vm.uiState.launchIn(backgroundScope)

        vm.setDraft("Warum?")
        vm.send()
        assertTrue(vm.uiState.value.showNotice)
        assertEquals(0, calls)

        vm.acceptNotice()
        assertTrue(preferences.chatAccepted.value)
        assertEquals(1, calls)
        assertEquals(listOf("Warum?", "Antwort"), vm.uiState.value.entries.map { it.message.text })
        assertEquals("", vm.uiState.value.draft)

        vm.setDraft("Noch was")
        vm.send()
        assertFalse(vm.uiState.value.showNotice)
        assertEquals(2, calls)
    }

    @Test
    fun dismissingTheNoticeSendsNothing() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        vm.uiState.launchIn(backgroundScope)

        vm.setDraft("Warum?")
        vm.send()
        vm.dismissNotice()

        assertEquals(0, calls)
        assertEquals("Warum?", vm.uiState.value.draft)
    }

    @Test
    fun blankDraftIsNotSent() = runTest(UnconfinedTestDispatcher()) {
        preferences.chatAccepted.value = true
        vm.uiState.launchIn(backgroundScope)

        vm.setDraft("   ")
        assertFalse(vm.uiState.value.canSend)
        vm.send()

        assertEquals(0, calls)
    }

    @Test
    fun pendingQuestionIsShownWhileTheCoachThinks() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.chatAccepted.value = true
        val reply = CompletableDeferred<GeneratedReply>()
        answer = { reply.await() }
        vm.uiState.launchIn(backgroundScope)

        vm.setDraft("Warum?")
        vm.send()
        assertEquals("Warum?", vm.uiState.value.pending)
        assertTrue(vm.uiState.value.sending)
        assertFalse(vm.uiState.value.canSend)

        reply.complete(GeneratedReply("Darum."))
        assertNull(vm.uiState.value.pending)
        assertEquals("Darum.", vm.uiState.value.entries.last().message.text)
    }

    @Test
    fun errorPutsTheQuestionBack() = runTest(UnconfinedTestDispatcher()) {
        preferences.chatAccepted.value = true
        vm.uiState.launchIn(backgroundScope)

        vm.setDraft("Warum?")
        vm.send()
        assertEquals(CoachError.Review(ReviewException.Reason.NO_PROFILE), vm.uiState.value.error)
        assertEquals("Warum?", vm.uiState.value.draft)

        withProfile()
        answer = { throw AiException(AiException.Reason.NETWORK) }
        vm.send()
        assertEquals(CoachError.Ai(AiException.Reason.NETWORK, null, null), vm.uiState.value.error)
        assertEquals("Warum?", vm.uiState.value.draft)
        assertNull(vm.uiState.value.pending)
        assertTrue(vm.uiState.value.entries.isEmpty())

        answer = { GeneratedReply("Jetzt geht's.") }
        vm.send()
        assertNull(vm.uiState.value.error)
        assertEquals(2, vm.uiState.value.entries.size)
    }

    @Test
    fun suggestionsAreAppliedOrDiscardedOnTap() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.chatAccepted.value = true
        answer = {
            GeneratedReply(
                "Zwei Ideen.",
                listOf(
                    GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "9 Sätze"),
                    GeneratedSuggestion(SuggestionChange.RepRange(1, bench.id, 6, 8, 8, 10), "stagniert"),
                ),
            )
        }
        vm.uiState.launchIn(backgroundScope)
        vm.setDraft("Was ändern?")
        vm.send()

        val (sets, reps) = vm.uiState.value.entries.last().items
        assertEquals("Push A", sets.routineName)
        assertEquals("Bankdrücken", sets.exerciseName)
        assertEquals(3, routines.getRoutine(1)!!.exercises.single().targetSets)

        vm.apply(sets.suggestion.id)
        vm.discard(reps.suggestion.id)

        val items = vm.uiState.value.entries.last().items
        assertEquals(SuggestionStatus.APPLIED, items[0].suggestion.status)
        assertEquals(SuggestionStatus.DISCARDED, items[1].suggestion.status)
        assertEquals(4, routines.getRoutine(1)!!.exercises.single().targetSets)
        assertEquals(6, routines.getRoutine(1)!!.exercises.single().repMin)
    }

    @Test
    fun sentDataShowsContextHistoryAndDraft() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.chatAccepted.value = true
        vm.uiState.launchIn(backgroundScope)
        vm.setDraft("Erste")
        vm.send()

        vm.setDraft("Zweite")
        vm.showSentData()
        assertEquals("ctx 0 msgs [Erste, Antwort, Zweite]", vm.uiState.value.sentData)

        vm.dismissSentData()
        assertNull(vm.uiState.value.sentData)
    }

    @Test
    fun newConversationAsksFirst() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.chatAccepted.value = true
        vm.uiState.launchIn(backgroundScope)
        vm.setDraft("Frage")
        vm.send()

        vm.requestClear()
        assertTrue(vm.uiState.value.confirmClear)
        vm.dismissClear()
        assertEquals(2, vm.uiState.value.entries.size)

        vm.requestClear()
        vm.confirmClear()
        assertFalse(vm.uiState.value.confirmClear)
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun draftIsLimited() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)

        vm.setDraft("x".repeat(ChatHistory.MAX_QUESTION_CHARS + 10))

        assertEquals(ChatHistory.MAX_QUESTION_CHARS, vm.uiState.value.draft.length)
    }

    @Test
    fun planCardSavesTheRoutinesThatAreSwitchedOn() = runTest(UnconfinedTestDispatcher()) {
        withProfile()
        preferences.chatAccepted.value = true
        answer = {
            GeneratedReply(
                "Plan.",
                listOf(
                    GeneratedSuggestion(
                        SuggestionChange.CreatePlan(
                            listOf(
                                PlanRoutine("A", listOf(PlanEntry(PlanExerciseRef.Existing(bench.id), 3, 8, 12, 2))),
                                PlanRoutine("B", listOf(PlanEntry(PlanExerciseRef.New("Neu", ExerciseType.STRENGTH, Equipment.OTHER), 3, 8, 12, null))),
                            ),
                        ),
                        "2 Tage",
                    ),
                ),
            )
        }
        vm.uiState.launchIn(backgroundScope)
        vm.setDraft("Plan bitte")
        vm.send()

        val item = vm.uiState.value.entries.last().items.single()
        assertEquals(listOf("A", "B"), item.plan!!.map { it.name })
        assertEquals("Bankdrücken", item.plan!![0].exercises.single().name)
        assertTrue(item.plan!![1].exercises.single().isNew)

        vm.togglePlanRoutine(item.suggestion.id, 1)
        assertEquals(setOf(1), vm.uiState.value.excluded[item.suggestion.id])
        vm.applyPlan(item.suggestion.id)

        assertEquals(listOf("Push A", "A"), routines.observeRoutines().first().map { it.name })
        assertEquals(SuggestionStatus.APPLIED, vm.uiState.value.entries.last().items.single().suggestion.status)
    }
}
