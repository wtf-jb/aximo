package io.github.wtfjb.aximo.review

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.wtfjb.aximo.MainActivity
import io.github.wtfjb.aximo.R

/** "Wochen-Review bereit" after the automatic weekly review (B-02). */
object ReviewNotifications {
    private const val CHANNEL = "weekly_review"
    private const val ID_READY = 10

    fun createChannel(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.review_channel))
                .setDescription(context.getString(R.string.review_channel_description))
                .build(),
        )
    }

    /** Shows the notice, if notifications are allowed; otherwise "Heute" shows the card anyway. */
    fun showReady(context: Context, suggestions: Int) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val text = context.resources.getQuantityString(R.plurals.review_notification_text, suggestions, suggestions)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_coach)
            .setContentTitle(context.getString(R.string.review_notification_title))
            .setContentText(text)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    ID_READY,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_READY, notification)
    }
}
