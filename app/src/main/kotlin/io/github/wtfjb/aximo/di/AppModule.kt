package io.github.wtfjb.aximo.di

import io.github.wtfjb.aximo.MainViewModel
import io.github.wtfjb.aximo.data.settings.DataStoreSettingsRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Dependency injection: which implementation is used for which interface. */
val appModule = module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    viewModel { MainViewModel(get()) }
}
