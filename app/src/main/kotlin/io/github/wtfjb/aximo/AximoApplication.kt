package io.github.wtfjb.aximo

import android.app.Application
import io.github.wtfjb.aximo.di.appModule
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.rest.RestNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AximoApplication : Application() {

    /** Work that outlives a screen, such as creating the exercise catalog on first start. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AximoApplication)
            modules(appModule)
        }
        RestNotifications.createChannels(this)
        val seeder: CatalogSeeder = get()
        appScope.launch { seeder.seedIfEmpty() }
    }
}
