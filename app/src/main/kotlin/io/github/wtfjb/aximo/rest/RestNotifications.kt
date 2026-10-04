package io.github.wtfjb.aximo.rest

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.wtfjb.aximo.MainActivity
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.rest.NextSet
import io.github.wtfjb.aximo.domain.rest.RestTimer

/**
 * Notifications of the rest timer (A-03): a silent one with the countdown while
 * the rest runs, and a loud one with sound and vibration when it is over.
 */
object RestNotifications {

    private const val TAG = "RestNotifications"
    private const val CHANNEL_RUNNING = "rest_running"
    private const val CHANNEL_FINISHED = "rest_finished"
    const val ID_RUNNING = 1
    private const val ID_FINISHED = 2

    /** The "rest over" notification disappears by itself after this time. */
    private const val FINISHED_TIMEOUT_MS = 60_000L

    /** Vibration when notifications are turned off: three short pulses. */
    private val FALLBACK_VIBRATION = longArrayOf(0, 300, 150, 300, 150, 300)

    /** Creates both channels. Safe to call more than once. */
    fun createChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_RUNNING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.rest_channel_running))
                .setDescription(context.getString(R.string.rest_channel_running_description))
                .setShowBadge(false)
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_FINISHED, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.rest_channel_finished))
                .setDescription(context.getString(R.string.rest_channel_finished_description))
                .setVibrationEnabled(true)
                .setShowBadge(false)
                .build(),
        )
    }

    /** Ongoing notification with a countdown. The system counts down itself, no updates per second. */
    fun running(context: Context, timer: RestTimer?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_rest)
            .setContentTitle(context.getString(R.string.rest_notification_title))
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (timer != null) {
            builder
                .setContentText(context.getString(R.string.rest_notification_next, nextSetText(context.resources, timer.next)))
                .setWhen(timer.endsAt.toEpochMilliseconds())
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.rest_extend), serviceAction(context, RestTimerService.ACTION_EXTEND))
                .addAction(0, context.getString(R.string.rest_skip), serviceAction(context, RestTimerService.ACTION_SKIP))
        }
        return builder.build()
    }

    /** New end time or next set after "+15 s" or a new rest. */
    fun updateRunning(context: Context, timer: RestTimer) {
        if (!hasPermission(context)) return
        post(context, ID_RUNNING, running(context, timer))
    }

    /** Rest is over: sound and vibration through the channel, or at least a vibration without notifications. */
    fun showFinished(context: Context, next: NextSet?) {
        NotificationManagerCompat.from(context).cancel(ID_RUNNING)
        val manager = NotificationManagerCompat.from(context)
        if (!hasPermission(context) || !manager.areNotificationsEnabled()) {
            vibrate(context)
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_FINISHED)
            .setSmallIcon(R.drawable.ic_notification_rest)
            .setContentTitle(context.getString(R.string.rest_finished_title))
            .setContentText(context.getString(R.string.rest_notification_next, nextSetText(context.resources, next)))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setTimeoutAfter(FINISHED_TIMEOUT_MS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        if (!post(context, ID_FINISHED, notification)) vibrate(context)
    }

    /** Removes an old "rest over" notification when the next rest starts. */
    fun cancelFinished(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_FINISHED)
    }

    /** Shows a notification; false if the permission was taken away in the meantime. */
    private fun post(context: Context, id: Int, notification: Notification): Boolean = try {
        NotificationManagerCompat.from(context).notify(id, notification)
        true
    } catch (e: SecurityException) {
        Log.w(TAG, "Notification permission missing", e)
        false
    }

    /** POST_NOTIFICATIONS is a runtime permission from Android 13 on; before that it is always granted. */
    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(FALLBACK_VIBRATION, -1))
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun serviceAction(context: Context, action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, RestTimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
