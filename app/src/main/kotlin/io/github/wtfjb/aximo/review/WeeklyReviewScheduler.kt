package io.github.wtfjb.aximo.review

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import io.github.wtfjb.aximo.domain.review.WeeklySchedule
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Keeps the WorkManager job in line with the setting: one periodic job every
 * 7 days, first run at the chosen day and hour. Android may shift it a bit
 * (Doze); it waits for a network connection.
 */
class WeeklyReviewScheduler(private val context: Context) {

    suspend fun sync(setting: WeeklyReviewSetting, aiAvailable: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!setting.enabled || !aiAvailable) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        // The tag remembers day and hour, so an app start does not move an existing job.
        val tag = "$TAG_PREFIX${setting.day.name}_${setting.hour}"
        val existing = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).first()
        if (existing.any { !it.state.isFinished && tag in it.tags }) return

        val now = Clock.System.now()
        val firstRun = WeeklySchedule.nextRun(now, setting.day, setting.hour, TimeZone.currentSystemDefault())
        val request = PeriodicWorkRequestBuilder<WeeklyReviewWorker>(DAYS_PER_WEEK, TimeUnit.DAYS)
            .setInitialDelay((firstRun - now).inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(tag)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    private companion object {
        const val WORK_NAME = "weekly_review"
        const val TAG_PREFIX = "weekly_review_"
        const val DAYS_PER_WEEK = 7L
    }
}
