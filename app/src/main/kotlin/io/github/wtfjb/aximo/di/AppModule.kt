package io.github.wtfjb.aximo.di

import io.github.wtfjb.aximo.MainViewModel
import io.github.wtfjb.aximo.data.db.AximoDatabase
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.data.repository.RoomWorkoutRepository
import io.github.wtfjb.aximo.data.settings.DataStoreSettingsRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.rest.RestTimerController
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutStarter
import io.github.wtfjb.aximo.rest.AndroidRestTimerEffects
import io.github.wtfjb.aximo.ui.exercises.ExerciseEditViewModel
import io.github.wtfjb.aximo.ui.exercises.ExerciseListViewModel
import io.github.wtfjb.aximo.ui.plans.PlansViewModel
import io.github.wtfjb.aximo.ui.routine.RoutineEditViewModel
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
    single { TimeSource.System }
    single { WorkoutStarter(get(), get(), get()) }
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
    viewModel { (exerciseId: Long) -> ExerciseEditViewModel(get(), exerciseId) }
    viewModel { TodayViewModel(get(), get(), get()) }
    viewModel { PlansViewModel(get(), get(), get(), get()) }
    viewModel { (routineId: Long) -> RoutineEditViewModel(get(), get(), routineId) }
    viewModel { WorkoutViewModel(get(), get(), get(), get(), get()) }
}
