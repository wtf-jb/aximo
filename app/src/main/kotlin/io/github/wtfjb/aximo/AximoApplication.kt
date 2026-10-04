package io.github.wtfjb.aximo

import android.app.Application
import io.github.wtfjb.aximo.di.appModule
import io.github.wtfjb.aximo.domain.exercise.CatalogSeeder
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.rest.RestNotifications
import io.github.wtfjb.aximo.review.ReviewNotifications
import io.github.wtfjb.aximo.review.WeeklyReviewScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
        ReviewNotifications.createChannel(this)
        val seeder: CatalogSeeder = get()
        appScope.launch { seeder.seedIfEmpty() }
        syncWeeklyReview()
    }

    /** Schedules or cancels the weekly review whenever its setting or the AI profiles change (B-02). */
    private fun syncWeeklyReview() {
        val preferences: AiPreferences = get()
        val profiles: AiProfileRepository = get()
        val scheduler = WeeklyReviewScheduler(this)
        appScope.launch {
            combine(preferences.weeklyReview, profiles.observeProfiles().map { AiProfiles.isAvailable(it) }) { setting, available ->
                setting to available
            }.distinctUntilChanged().collect { (setting, available) -> scheduler.sync(setting, available) }
        }
    }
}
