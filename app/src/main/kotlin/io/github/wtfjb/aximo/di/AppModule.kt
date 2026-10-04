package io.github.wtfjb.aximo.di

import io.github.wtfjb.aximo.MainViewModel
import io.github.wtfjb.aximo.data.db.AximoDatabase
import io.github.wtfjb.aximo.data.repository.RoomExerciseRepository
import io.github.wtfjb.aximo.data.repository.RoomRoutineRepository
import io.github.wtfjb.aximo.data.settings.DataStoreSettingsRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Dependency injection: which implementation is used for which interface. */
val appModule = module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single { AximoDatabase.build(androidContext()) }
    single<ExerciseRepository> { RoomExerciseRepository(get<AximoDatabase>().exerciseDao()) }
    single<RoutineRepository> { RoomRoutineRepository(get<AximoDatabase>().routineDao()) }
    viewModel { MainViewModel(get()) }
}
