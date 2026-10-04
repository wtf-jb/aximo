package io.github.wtfjb.aximo.review

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.wtfjb.aximo.domain.ai.AiException
import io.github.wtfjb.aximo.domain.ai.AiPreferences
import io.github.wtfjb.aximo.domain.review.AiReviewRepository
import io.github.wtfjb.aximo.domain.review.ReviewException
import io.github.wtfjb.aximo.domain.review.ReviewService
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runs the weekly review in the background (B-02) and posts "Wochen-Review
 * bereit". Only with the user's consent from the data notice; the suggestions
 * still wait for confirmation in the Coach tab.
 */
class WeeklyReviewWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    private val service: ReviewService by inject()
    private val reviews: AiReviewRepository by inject()
    private val preferences: AiPreferences by inject()

    override suspend fun doWork(): Result {
        if (!preferences.weeklyReview.first().enabled || !preferences.dataNoticeAccepted.first()) return Result.success()
        return try {
            service.createReview(applicationContext.resources.configuration.locales[0].language)
            val open = reviews.observeLatest().first()?.suggestions?.count { it.status == SuggestionStatus.OPEN } ?: 0
            ReviewNotifications.showReady(applicationContext, open)
            Result.success()
        } catch (e: ReviewException) {
            Result.success() // no profile or no training: nothing to do this week
        } catch (e: AiException) {
            if (e.reason in TRANSIENT && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    private companion object {
        val TRANSIENT = setOf(
            AiException.Reason.NETWORK,
            AiException.Reason.TIMEOUT,
            AiException.Reason.SERVER,
            AiException.Reason.RATE_LIMITED,
        )
        const val MAX_ATTEMPTS = 3
    }
}
