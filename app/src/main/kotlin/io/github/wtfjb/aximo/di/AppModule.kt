package io.github.wtfjb.aximo.di

import io.github.wtfjb.aximo.MainViewModel
import io.github.wtfjb.aximo.data.backup.RoomBackupRepository
import io.github.wtfjb.aximo.data.db.AximoDatabase
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
import io.github.wtfjb.aximo.ui.exercisedetail.ExerciseDetailViewModel
import io.github.wtfjb.aximo.ui.exercises.ExerciseEditViewModel
import io.github.wtfjb.aximo.ui.exercises.ExerciseListViewModel
import io.github.wtfjb.aximo.ui.exercises.nameRes
import io.github.wtfjb.aximo.ui.plans.PlansViewModel
import io.github.wtfjb.aximo.ui.routine.RoutineEditViewModel
import io.github.wtfjb.aximo.ui.settings.ContentResolverDocumentStore
import io.github.wtfjb.aximo.ui.settings.DocumentStore
import io.github.wtfjb.aximo.ui.settings.SettingsViewModel
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
    single<BackupRepository> { RoomBackupRepository(get(), get(), get()) }
    single<DocumentStore> { ContentResolverDocumentStore(androidContext()) }
    single<ProgressionRepository> { RoomProgressionRepository(get<AximoDatabase>().progressionDao()) }
    single { WorkoutStarter(get(), get(), get(), get()) }
    single {
        val context = androidContext()
        CatalogSeeder(get(), get()) { entry -> context.getString(entry.nameRes()) }
    }
    single { WorkoutFinisher(get(), get(), get(), get(), get()) }
    // One rest timer for the whole app; its scope lives as long as the app.
    single {
        RestTimerController(
            time = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            effects = AndroidRestTimerEffects(androidContext()),
        )
    }
    viewModel { MainViewModel(get()) }
    viewModel { (selectionMode: Boolean) -> ExerciseListViewModel(get(), selectionMode) }
    viewModel { (exerciseId: Long) -> ExerciseEditViewModel(get(), get(), exerciseId) }
    viewModel { (exerciseId: Long) -> ExerciseDetailViewModel(get(), get(), get(), get(), exerciseId) }
    viewModel { TodayViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (entryId: Long) ->
        val context = androidContext()
        CardioViewModel(get(), get(), get(), { activity -> context.getString(activity.nameRes()) }, entryId)
    }
    viewModel { PlansViewModel(get(), get(), get(), get()) }
    viewModel { (routineId: Long) -> RoutineEditViewModel(get(), get(), routineId) }
    viewModel { WorkoutViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (workoutId: Long) -> SummaryViewModel(get(), get(), get(), workoutId) }
    viewModel { StatsViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get()) }
}

/** Display name of a default cardio activity, stored once when the activities are created. */
private fun DefaultActivity.nameRes(): Int = when (this) {
    DefaultActivity.RUNNING -> R.string.cardio_activity_running
    DefaultActivity.CYCLING -> R.string.cardio_activity_cycling
    DefaultActivity.ROWING -> R.string.cardio_activity_rowing
    DefaultActivity.OTHER -> R.string.cardio_activity_other
}
