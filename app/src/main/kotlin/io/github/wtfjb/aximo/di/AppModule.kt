package io.github.wtfjb.aximo.di

import io.github.wtfjb.aximo.MainViewModel
import io.github.wtfjb.aximo.ai.ChatPrompt
import io.github.wtfjb.aximo.ai.KtorChatGenerator
import io.github.wtfjb.aximo.data.repository.RoomAiChatRepository
import io.github.wtfjb.aximo.domain.chat.AiChatRepository
import io.github.wtfjb.aximo.domain.chat.ChatGenerator
import io.github.wtfjb.aximo.domain.chat.ChatService
import io.github.wtfjb.aximo.ui.coach.ChatViewModel
import io.github.wtfjb.aximo.ai.AiHttp
import io.github.wtfjb.aximo.ai.KtorAiProviderFactory
import io.github.wtfjb.aximo.ai.KtorModelLister
import io.github.wtfjb.aximo.domain.ai.AiModelLister
import io.github.wtfjb.aximo.ai.KtorLoggingGenerator
import io.github.wtfjb.aximo.ai.KtorPlanGenerator
import io.github.wtfjb.aximo.ai.LoggingPrompt
import io.github.wtfjb.aximo.domain.logging.LoggingGenerator
import io.github.wtfjb.aximo.domain.logging.LoggingService
import io.github.wtfjb.aximo.domain.logging.SpeechInput
import io.github.wtfjb.aximo.speech.AndroidSpeechInput
import io.github.wtfjb.aximo.ui.logging.LoggingViewModel
import io.github.wtfjb.aximo.catalog.AssetCatalogRepository
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import io.github.wtfjb.aximo.ui.catalog.CatalogViewModel
import io.github.wtfjb.aximo.ai.KtorReviewGenerator
import io.github.wtfjb.aximo.ai.PlanPrompt
import io.github.wtfjb.aximo.domain.plan.PlanGenerator
import io.github.wtfjb.aximo.domain.plan.PlanService
import io.github.wtfjb.aximo.ui.plangen.PlanGeneratorViewModel
import io.github.wtfjb.aximo.ai.ReviewPrompt
import io.github.wtfjb.aximo.data.settings.DataStoreAiPreferences
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.ui.coach.CoachViewModel
import io.github.wtfjb.aximo.ui.coach.ReviewHintViewModel
import io.github.wtfjb.aximo.data.repository.RoomAiReviewRepository
import io.github.wtfjb.aximo.domain.review.AiReviewRepository
import io.github.wtfjb.aximo.domain.review.ReviewGenerator
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.data.ai.KeystoreAiKeyStore
import io.github.wtfjb.aximo.data.repository.RoomAiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiConnectionTester
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProviderFactory
import io.github.wtfjb.aximo.ui.ai.AiProfileEditViewModel
import io.github.wtfjb.aximo.ui.ai.AiProfilesViewModel
import io.ktor.client.engine.okhttp.OkHttp
import io.github.wtfjb.aximo.data.backup.RoomBackupRepository
import io.github.wtfjb.aximo.data.db.AximoDatabase
import io.github.wtfjb.aximo.data.testdata.RoomDevDataRepository
import io.github.wtfjb.aximo.domain.devdata.DevDataRepository
import io.github.wtfjb.aximo.data.repository.RoomCardioRepository
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomProgressionRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.data.repository.RoomWorkoutRepository
import io.github.wtfjb.aximo.data.settings.DataStoreSettingsRepository
import io.github.wtfjb.aximo.domain.backup.BackupRepository
import io.github.wtfjb.aximo.domain.cardio.DefaultActivity
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository
import io.github.wtfjb.aximo.domain.rest.RestTimerController
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutFinisher
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import io.github.wtfjb.aximo.rest.AndroidRestTimerEffects
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.cardio.CardioViewModel
import io.github.wtfjb.aximo.ui.exercisedetail.DetailTab
import io.github.wtfjb.aximo.ui.exercisedetail.ExerciseDetailViewModel
import io.github.wtfjb.aximo.ui.exercises.ExerciseEditViewModel
import io.github.wtfjb.aximo.ui.exercises.ExerciseListViewModel
import io.github.wtfjb.aximo.ui.exercises.nameRes
import io.github.wtfjb.aximo.ui.plans.PlansViewModel
import io.github.wtfjb.aximo.ui.routine.RoutineEditViewModel
import io.github.wtfjb.aximo.ui.settings.ContentResolverDocumentStore
import io.github.wtfjb.aximo.ui.settings.DocumentStore
import io.github.wtfjb.aximo.ui.settings.SettingsViewModel
import io.github.wtfjb.aximo.ui.calendar.CalendarViewModel
import io.github.wtfjb.aximo.ui.stats.StatsViewModel
import io.github.wtfjb.aximo.ui.summary.SummaryViewModel
import io.github.wtfjb.aximo.ui.today.TodayViewModel
import io.github.wtfjb.aximo.ui.workout.WorkoutViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Dependency injection: which implementation is used for which interface. */
val appModule = module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single { AximoDatabase.build(androidContext()) }
    single<ExerciseRepository> { RoomExerciseRepository(get<AximoDatabase>().exerciseDao()) }
    single<RoutineRepository> { RoomRoutineRepository(get<AximoDatabase>().routineDao()) }
    single<WorkoutRepository> { RoomWorkoutRepository(get<AximoDatabase>().workoutDao()) }
    single<CardioRepository> { RoomCardioRepository(get<AximoDatabase>().cardioDao()) }
    single { TimeSource.System }
    single { RoomBackupRepository(get(), get(), get()) }
    single<BackupRepository> { get<RoomBackupRepository>() }
    single<DocumentStore> { ContentResolverDocumentStore(androidContext()) }
    // AI (B-01): one HTTP client for all providers; keys encrypted via the Android Keystore.
    single<AiProfileRepository> { RoomAiProfileRepository(get<AximoDatabase>().aiProfileDao(), KeystoreAiKeyStore(androidContext())) }
    single { AiHttp.client(OkHttp.create()) }
    single<AiProviderFactory> { KtorAiProviderFactory(get()) }
    single { AiConnectionTester(get()) }
    single<AiModelLister> { KtorModelLister(get()) }
    single<AiReviewRepository> { RoomAiReviewRepository(get<AximoDatabase>().aiReviewDao()) }
    single<ReviewGenerator> { KtorReviewGenerator() }
    single<AiPreferences> { DataStoreAiPreferences(androidContext()) }
    single { ReviewService(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<AiChatRepository> { RoomAiChatRepository(get<AximoDatabase>().aiChatDao()) }
    single<ChatGenerator> { KtorChatGenerator() }
    single { ChatService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PlanGenerator> { KtorPlanGenerator() }
    single<CatalogRepository> { AssetCatalogRepository(androidContext()) }
    single { PlanService(get(), get(), get(), get(), get()) }
    single<LoggingGenerator> { KtorLoggingGenerator() }
    single<SpeechInput> { AndroidSpeechInput(androidContext()) }
    single { LoggingService(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<ProgressionRepository> { RoomProgressionRepository(get<AximoDatabase>().progressionDao()) }
    single { WorkoutStarter(get(), get(), get(), get()) }
    single {
        val context = androidContext()
        CatalogSeeder(get(), get()) { entry -> context.getString(entry.nameRes()) }
    }
    single<DevDataRepository> { RoomDevDataRepository(get(), get(), get(), get()) }
    single { WorkoutFinisher(get(), get(), get(), get(), get()) }
    // One rest timer for the whole app; its scope lives as long as the app.
    single {
        RestTimerController(
            time = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            effects = AndroidRestTimerEffects(androidContext()),
        )
    }
    viewModel { MainViewModel(get(), get()) }
    viewModel { ReviewHintViewModel(get(), get()) }
    viewModel {
        val context = androidContext()
        CoachViewModel(
            service = get(),
            reviews = get(),
            routines = get(),
            exercises = get(),
            preferences = get(),
            language = { context.resources.configuration.locales[0].language },
            formatContext = { ReviewPrompt.prettyContext(it) },
        )
    }
    viewModel {
        val context = androidContext()
        ChatViewModel(
            chat = get(),
            review = get(),
            routines = get(),
            exercises = get(),
            preferences = get(),
            language = { context.resources.configuration.locales[0].language },
            formatPayload = { ChatPrompt.prettyPayload(it) },
        )
    }
    viewModel { (selectionMode: Boolean) -> ExerciseListViewModel(get(), selectionMode) }
    viewModel { (exerciseId: Long) -> ExerciseEditViewModel(get(), get(), exerciseId) }
    viewModel { (exerciseId: Long, initialTab: DetailTab) -> ExerciseDetailViewModel(get(), get(), get(), get(), get(), exerciseId, initialTab) }
    viewModel { CatalogViewModel(get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get(), get(), get()) }
    viewModel { TodayViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (entryId: Long) ->
        val context = androidContext()
        CardioViewModel(get(), get(), get(), get(), { activity -> context.getString(activity.nameRes()) }, entryId)
    }
    viewModel { PlansViewModel(get(), get(), get(), get(), get()) }
    viewModel {
        val context = androidContext()
        PlanGeneratorViewModel(
            service = get(),
            preferences = get(),
            language = { context.resources.configuration.locales[0].language },
            formatInput = { PlanPrompt.prettyInput(it) },
        )
    }
    viewModel {
        val context = androidContext()
        LoggingViewModel(
            service = get(),
            preferences = get(),
            speech = get(),
            language = { context.resources.configuration.locales[0].toLanguageTag() },
            formatInput = { LoggingPrompt.prettyInput(it) },
        )
    }
    viewModel { (routineId: Long) -> RoutineEditViewModel(get(), get(), routineId) }
    viewModel { WorkoutViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (workoutId: Long) -> SummaryViewModel(get(), get(), get(), get(), workoutId) }
    viewModel { StatsViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) { ReviewPrompt.prettyContext(it) } }
    viewModel { AiProfilesViewModel(get()) }
    viewModel { (profileId: Long) -> AiProfileEditViewModel(get(), get(), get(), profileId) }
}

/** Display name of a default cardio activity, stored once when the activities are created. */
private fun DefaultActivity.nameRes(): Int = when (this) {
    DefaultActivity.RUNNING -> R.string.cardio_activity_running
    DefaultActivity.CYCLING -> R.string.cardio_activity_cycling
    DefaultActivity.ROWING -> R.string.cardio_activity_rowing
    DefaultActivity.OTHER -> R.string.cardio_activity_other
}
