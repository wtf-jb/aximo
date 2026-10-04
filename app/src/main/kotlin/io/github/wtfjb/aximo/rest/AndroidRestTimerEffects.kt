package io.github.wtfjb.aximo.rest

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.wtfjb.aximo.domain.rest.NextSet
import io.github.wtfjb.aximo.domain.rest.RestTimerEffects

/** Android side of the rest timer: starts the service and shows the alert. */
class AndroidRestTimerEffects(private val context: Context) : RestTimerEffects {

    override fun started() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, RestTimerService::class.java))
        } catch (e: IllegalStateException) {
            // Not allowed from the background (Android 12+). The bar in the app still works.
            Log.w(TAG, "Could not start the rest timer service", e)
        }
    }

    override fun finished(next: NextSet?) {
        RestNotifications.showFinished(context, next)
    }

    private companion object {
        const val TAG = "RestTimer"
    }
}
