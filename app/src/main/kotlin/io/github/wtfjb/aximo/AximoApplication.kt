package io.github.wtfjb.aximo

import android.app.Application
import io.github.wtfjb.aximo.di.appModule
import io.github.wtfjb.aximo.rest.RestNotifications
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AximoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AximoApplication)
            modules(appModule)
        }
        RestNotifications.createChannels(this)
    }
}
