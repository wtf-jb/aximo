package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiProvider
import io.github.wtfjb.aximo.domain.ai.AiRequest
import io.github.wtfjb.aximo.domain.ai.ApiKeyChange
import io.github.wtfjb.aximo.domain.chat.ChatException
import io.github.wtfjb.aximo.domain.chat.ChatHistory
import io.github.wtfjb.aximo.domain.chat.ChatMessage
import io.github.wtfjb.aximo.domain.chat.ChatPayload
import io.github.wtfjb.aximo.domain.chat.ChatRole
import io.github.wtfjb.aximo.domain.chat.ChatService
import io.github.wtfjb.aximo.domain.plan.PlanService
import io.github.wtfjb.aximo.domain.chat.GeneratedReply
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.review.PlanEntry
import io.github.wtfjb.aximo.domain.review.PlanExerciseRef
import io.github.wtfjb.aximo.domain.review.PlanRoutine
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.Routine
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.RoutineWithExercises
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.Workout
import io.github.wtfjb.aximo.domain.model.WorkoutExercise
import io.github.wtfjb.aximo.domain.review.GeneratedSuggestion
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.domain.review.SuggestionChange
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ChatServiceTest {

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
    private val chats = FakeAiChatRepository()
    private val reviews = FakeAiReviewRepository(chats)

    private var answer: () -> GeneratedReply = { GeneratedReply("Antwort") }
    private val sent = mutableListOf<ChatPayload>()
    private var sentLanguage: String? = null

    private val crossover = CatalogEntry(
        id = "Cable_Crossover", name = "Cable Crossover", type = ExerciseType.STRENGTH, equipment = Equipment.CABLE,
        primary = setOf(MuscleGroup.CHEST), secondary = emptySet(), repMin = 10, repMax = 15, instructions = listOf("Pull."),
    )
    private val catalog = listOf(crossover)

    private val factory = { _: io.github.wtfjb.aximo.domain.ai.AiProviderProfile, _: String? ->
        object : AiProvider {
            override suspend fun complete(request: AiRequest): String = error("not used, the generator is faked")
        }
    }

    private val service = ChatService(
        workouts, routines, exercises, FakeSettingsRepository(), profiles, factory,
        { _, payload, language ->
            sent += payload
            sentLanguage = language
            answer()
        },
        chats, { catalog }, reviews, PlanService(exercises, routines, profiles, factory, { _, _, _ -> error("not used") }),
        TimeSource { now }, { TimeZone.UTC },
    )

    private val reviewService = ReviewService(
        workouts, routines, exercises, FakeSettingsRepository(), profiles, factory,
        { _, _, _ -> error("not used") }, reviews, TimeSource { now }, { TimeZone.UTC },
    )

    private suspend fun withProfile() = profiles.saveProfile(FakeAiProfileRepository.profile("Ollama"), ApiKeyChange.Keep)

    private suspend fun expectChatError(question: String): ChatException.Reason {
        try {
            service.send(question, "de")
        } catch (e: ChatException) {
            return e.reason
        }
        fail("Expected ChatException")
        throw AssertionError()
    }

    @Test
    fun needsProfileAndText() = runTest {
        assertEquals(ChatException.Reason.EMPTY, expectChatError("  "))
        assertEquals(ChatException.Reason.NO_PROFILE, expectChatError("Warum?"))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun sendsContextAndConversationAndStoresTheExchange() = runTest {
        withProfile()

        service.send("  Warum stagniert mein Bankdrücken? ", "de")
        service.send("Und jetzt?", "de")

        val second = sent.last()
        assertEquals(1, second.context.review.sessions)
        assertEquals("Bankdrücken", second.context.history.single().name)
        assertEquals(listOf("Warum stagniert mein Bankdrücken?", "Antwort", "Und jetzt?"), second.messages.map { it.text })
        assertEquals(listOf(ChatRole.USER, ChatRole.COACH, ChatRole.USER), second.messages.map { it.role })
        assertEquals("de", sentLanguage)
        assertEquals(4, chats.messages.value.size)
    }

    @Test
    fun onlyTheLastMessagesAreSent() = runTest {
        withProfile()
        repeat(10) { service.send("Frage $it", "de") }

        val messages = sent.last().messages
        assertEquals(ChatHistory.MAX_MESSAGES + 1, messages.size) // 6 earlier exchanges + the new question
        assertEquals(ChatRole.USER, messages.first().role)
        assertEquals("Frage 9", messages.last().text)
    }

    @Test
    fun nothingIsStoredOnFailure() = runTest {
        withProfile()
        answer = { throw AiException(AiException.Reason.TIMEOUT) }

        try {
            service.send("Warum?", "de")
            fail("Expected AiException")
        } catch (e: AiException) {
            assertEquals(AiException.Reason.TIMEOUT, e.reason)
        }
        assertTrue(chats.messages.value.isEmpty())
    }

    @Test
    fun suggestionsAreCheckedAndAppliedOnlyAfterConfirming() = runTest {
        withProfile()
        answer = {
            GeneratedReply(
                "Mehr Volumen.",
                listOf(
                    GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 3, 4), "9 Sätze"),
                    GeneratedSuggestion(SuggestionChange.SetCount(1, bench.id, 5, 6), "falscher Ausgangswert"),
                    GeneratedSuggestion(SuggestionChange.AddExercise(9, dips.id, 3, 8, 12, null), "unbekannte Routine"),
                ),
                dropped = 1,
            )
        }

        service.send("Mehr Sätze?", "de")

        val answerMessage = chats.messages.value.last()
        val suggestion = answerMessage.suggestions.single()
        assertEquals(3, answerMessage.droppedSuggestions)
        assertEquals(SuggestionStatus.OPEN, suggestion.status)
        assertEquals(3, routines.getRoutine(1)!!.exercises.single().targetSets) // nothing changed yet

        assertTrue(reviewService.apply(suggestion.id))

        assertEquals(4, routines.getRoutine(1)!!.exercises.single().targetSets)
        assertEquals(SuggestionStatus.APPLIED, chats.messages.value.last().suggestions.single().status)
    }

    @Test
    fun payloadIsWhatSendSends() = runTest {
        withProfile()
        service.send("Erste Frage", "de")

        val preview = service.payload("")
        assertEquals(listOf("Erste Frage", "Antwort"), preview.messages.map { it.text })

        val withDraft = service.payload("Zweite")
        service.send("Zweite", "de")
        assertEquals(withDraft.messages.map(ChatMessage::text), sent.last().messages.map(ChatMessage::text))
    }

    @Test
    fun clearDeletesTheConversation() = runTest {
        withProfile()
        service.send("Frage", "de")

        service.clear()

        assertTrue(service.observeMessages().first().isEmpty())
    }

    private fun planAnswer() = GeneratedReply(
        "Zwei Ganzkörper-Einheiten.",
        listOf(
            GeneratedSuggestion(
                SuggestionChange.CreatePlan(
                    listOf(
                        PlanRoutine(
                            "Ganzkörper A",
                            listOf(
                                PlanEntry(PlanExerciseRef.Existing(bench.id), 3, 8, 12, 2),
                                PlanEntry(PlanExerciseRef.New("Kabelzug-Fliegende", ExerciseType.STRENGTH, Equipment.OTHER, catalogName = "Cable Crossover"), 3, 10, 15, null),
                            ),
                        ),
                        PlanRoutine(
                            "Ganzkörper B",
                            listOf(
                                PlanEntry(PlanExerciseRef.Existing(dips.id), 3, 8, 12, 2),
                                PlanEntry(PlanExerciseRef.New("Kabelzug-Fliegende", ExerciseType.STRENGTH, Equipment.OTHER, catalogName = "Cable Crossover"), 2, 10, 15, null),
                                PlanEntry(PlanExerciseRef.Existing(99), 3, 8, 12, null),
                            ),
                        ),
                    ),
                ),
                "3 Tage pro Woche",
            ),
        ),
    )

    @Test
    fun planIsResolvedButNothingIsSavedBeforeConfirming() = runTest {
        withProfile()
        answer = { planAnswer() }

        service.send("Erstelle mir einen Plan", "de")

        val answerMessage = chats.messages.value.last()
        val plan = answerMessage.suggestions.single().change as SuggestionChange.CreatePlan
        val new = plan.routines[0].exercises[1].exercise as PlanExerciseRef.New
        assertEquals(crossover.catalogId, new.catalogId)
        assertEquals(1, answerMessage.droppedSuggestions) // the unknown exercise id
        assertEquals(1, routines.observeRoutines().first().size)
        assertEquals(2, exercises.observeExercises(true).first().size)
    }

    @Test
    fun applyingAPlanAddsRoutinesAndCreatesNewExercisesOnce() = runTest {
        withProfile()
        answer = { planAnswer() }
        service.send("Erstelle mir einen Plan", "de")
        val id = chats.messages.value.last().suggestions.single().id

        assertEquals(2, service.applyPlan(id))

        val all = routines.observeRoutinesWithExercises().first()
        assertEquals(listOf("Push A", "Ganzkörper A", "Ganzkörper B"), all.map { it.routine.name })
        val created = exercises.observeExercises(true).first().filter { it.name == "Kabelzug-Fliegende" }
        assertEquals(1, created.size)
        assertEquals(crossover.catalogId, created.single().catalogId)
        assertEquals(listOf(bench.id, created.single().id), all[1].exercises.map { it.exerciseId })
        assertEquals(2, all[2].exercises[1].targetSets)
        assertEquals(SuggestionStatus.APPLIED, chats.messages.value.last().suggestions.single().status)

        assertEquals("only once", 0, service.applyPlan(id))
    }

    @Test
    fun switchedOffRoutinesAreNotSaved() = runTest {
        withProfile()
        answer = { planAnswer() }
        service.send("Plan", "de")
        val id = chats.messages.value.last().suggestions.single().id

        assertEquals(1, service.applyPlan(id, excluded = setOf(0)))

        assertEquals(listOf("Push A", "Ganzkörper B"), routines.observeRoutines().first().map { it.name })
        assertEquals(0, service.applyPlan(id))
    }

    @Test
    fun renameAndDeleteGoThroughTheReviewFlow() = runTest {
        withProfile()
        answer = {
            GeneratedReply(
                "Umbenennen.",
                listOf(GeneratedSuggestion(SuggestionChange.RenameRoutine(1, "Push A", "Oberkörper A"), "klarer")),
            )
        }
        service.send("Benenne Push A um", "de")
        assertTrue(reviewService.apply(chats.messages.value.last().suggestions.single().id))
        assertEquals("Oberkörper A", routines.getRoutine(1)!!.routine.name)

        answer = { GeneratedReply("Weg damit.", listOf(GeneratedSuggestion(SuggestionChange.DeleteRoutine(1, "Oberkörper A"), "doppelt"))) }
        service.send("Lösche die Routine", "de")
        assertTrue(reviewService.apply(chats.messages.value.last().suggestions.single().id))
        assertNull(routines.getRoutine(1))
    }
}
