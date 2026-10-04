package io.github.wtfjb.aximo.data

import org.robolectric.RuntimeEnvironment
import io.github.wtfjb.aximo.data.settings.DataStoreAiPreferences
import io.github.wtfjb.aximo.data.settings.DataStoreSettingsRepository
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import io.github.wtfjb.aximo.domain.review.WeeklyReviewSetting
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @Test
    fun trainingSettingsAreStoredAndReadBack() = runTest {
        val repository = DataStoreSettingsRepository(RuntimeEnvironment.getApplication())
        val settings = TrainingSettings(restSeconds = 150, steps = WeightSteps(1.25, 1.0), rating = SetRating.RPE, weeklyGoal = 3, bodyWeightKg = 82.5).withUnit(WeightUnit.LBS)

        repository.setTraining(settings)

        assertEquals(settings, repository.training.first())
    }

    @Test
    fun aiDataNoticeIsRemembered() = runTest {
        val preferences = DataStoreAiPreferences(RuntimeEnvironment.getApplication())
        assertEquals(false, preferences.dataNoticeAccepted.first())

        preferences.acceptDataNotice()

        assertEquals(true, preferences.dataNoticeAccepted.first())
        // The chat (B-05) has its own notice; accepting it also counts as the general one.
        assertEquals(false, preferences.chatNoticeAccepted.first())
        preferences.acceptChatNotice()
        assertEquals(true, preferences.chatNoticeAccepted.first())
    }

    @Test
    fun weeklyReviewSettingIsStored() = runTest {
        val preferences = DataStoreAiPreferences(RuntimeEnvironment.getApplication())
        assertEquals(WeeklyReviewSetting(), preferences.weeklyReview.first())

        val setting = WeeklyReviewSetting(enabled = true, day = DayOfWeek.MONDAY, hour = 7)
        preferences.setWeeklyReview(setting)

        assertEquals(setting, preferences.weeklyReview.first())
    }
}
