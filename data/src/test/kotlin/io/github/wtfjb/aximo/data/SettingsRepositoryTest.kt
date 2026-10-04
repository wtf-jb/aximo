package io.github.wtfjb.aximo.data

import org.robolectric.RuntimeEnvironment
import io.github.wtfjb.aximo.data.settings.DataStoreAiPreferences
import io.github.wtfjb.aximo.data.settings.DataStoreSettingsRepository
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.settings.WeightSteps
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import kotlinx.coroutines.flow.first
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
        val settings = TrainingSettings(restSeconds = 150, steps = WeightSteps(1.25, 1.0), rating = SetRating.RPE, weeklyGoal = 3).withUnit(WeightUnit.LBS)

        repository.setTraining(settings)

        assertEquals(settings, repository.training.first())
    }

    @Test
    fun aiDataNoticeIsRemembered() = runTest {
        val preferences = DataStoreAiPreferences(RuntimeEnvironment.getApplication())
        assertEquals(false, preferences.dataNoticeAccepted.first())

        preferences.acceptDataNotice()

        assertEquals(true, preferences.dataNoticeAccepted.first())
    }
}
