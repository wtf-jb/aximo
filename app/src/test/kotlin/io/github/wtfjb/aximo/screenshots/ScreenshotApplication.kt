package io.github.wtfjb.aximo.screenshots

import android.app.Application
import io.github.wtfjb.aximo.di.appModule
import io.github.wtfjb.aximo.rest.RestNotifications
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Like AximoApplication, but without the weekly-review scheduler: WorkManager is not
 * set up under Robolectric. The screenshots don't need it.
 */
class ScreenshotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ScreenshotApplication)
            modules(appModule)
        }
        RestNotifications.createChannels(this)
    }
}
