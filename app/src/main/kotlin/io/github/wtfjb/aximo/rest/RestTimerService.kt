package io.github.wtfjb.aximo.rest

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import io.github.wtfjb.aximo.domain.rest.RestTimer
import io.github.wtfjb.aximo.domain.rest.RestTimerController
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Keeps the app alive while a rest runs and shows the countdown notification
 * (A-03). The timer itself lives in [RestTimerController]; this service only
 * mirrors it and stops as soon as no rest is running.
 *
 * A partial wake lock keeps the CPU awake for the length of the rest, so the
 * alert comes on time even with the screen off.
 */
class RestTimerService : Service() {

    private val controller: RestTimerController by inject()
    private val time: TimeSource by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXTEND -> controller.extend()
            ACTION_SKIP -> controller.stop()
        }
        // Must be called right away after startForegroundService, even if the rest is already over.
        ServiceCompat.startForeground(
            this,
            RestNotifications.ID_RUNNING,
            RestNotifications.running(this, controller.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (observing == null) {
            observing = scope.launch { controller.state.collect(::show) }
        }
        return START_NOT_STICKY
    }

    private fun show(timer: RestTimer?) {
        if (timer == null) {
            releaseWakeLock()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        RestNotifications.cancelFinished(this)
        RestNotifications.updateRunning(this, timer)
        holdWakeLock(timer)
    }

    private fun holdWakeLock(timer: RestTimer) {
        val lock = wakeLock ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .also {
                it.setReferenceCounted(false)
                wakeLock = it
            }
        // Timeout as a safety net: the lock never outlives the rest by much.
        val millis = (timer.endsAt - time.now()).inWholeMilliseconds.coerceAtLeast(0) + WAKE_LOCK_SLACK_MS
        lock.acquire(millis)
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_EXTEND = "io.github.wtfjb.aximo.rest.EXTEND"
        const val ACTION_SKIP = "io.github.wtfjb.aximo.rest.SKIP"
        private const val WAKE_LOCK_TAG = "aximo:rest"
        private const val WAKE_LOCK_SLACK_MS = 10_000L
    }
}
