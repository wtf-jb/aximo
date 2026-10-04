package io.github.wtfjb.aximo.ui.catalog

import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.ui.exercises.FakeExerciseRepository
import io.github.wtfjb.aximo.ui.exercises.MainDispatcherRule
import io.github.wtfjb.aximo.ui.settings.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun entry(id: String, name: String, muscle: MuscleGroup) = CatalogEntry(
        id = id, name = name, type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL,
        primary = setOf(muscle), secondary = emptySet(), repMin = 6, repMax = 10, instructions = listOf("Do it."),
    )

    private val curl = entry("Barbell_Curl", "Barbell Curl", MuscleGroup.BICEPS)
    private val bench = entry("Bench", "Barbell Bench Press", MuscleGroup.CHEST)
    private val exercises = FakeExerciseRepository()
    private val repository = CatalogRepository { listOf(curl, bench) }
    private val vm by lazy { CatalogViewModel(repository, exercises, FakeSettingsRepository(TrainingSettings(restSeconds = 120))) }

    @Test
    fun listsFiltersAndSearches() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)
        assertEquals(listOf("Barbell Bench Press", "Barbell Curl"), vm.uiState.value.entries.map { it.name })

        vm.onQueryChange("curl")
        assertEquals(listOf("Barbell Curl"), vm.uiState.value.entries.map { it.name })

        vm.onQueryChange("")
        vm.onRegionSelect(BodyRegion.CHEST)
        assertEquals(listOf("Barbell Bench Press"), vm.uiState.value.entries.map { it.name })
    }

    @Test
    fun addingCreatesAnExerciseOnceAndMarksIt() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)
        vm.open(curl)
        assertEquals(curl, vm.uiState.value.selected)

        vm.add(curl)
        vm.add(curl)

        val saved = exercises.observeExercises(includeArchived = true).first()
        assertEquals(1, saved.size)
        assertEquals("fed_Barbell_Curl", saved.single().catalogId)
        assertEquals(120, saved.single().restSeconds)
        assertEquals(setOf("fed_Barbell_Curl"), vm.uiState.value.addedIds)
        assertNull(vm.uiState.value.selected)
    }

    @Test
    fun closeClearsTheSelection() = runTest(UnconfinedTestDispatcher()) {
        vm.uiState.launchIn(backgroundScope)
        vm.open(bench)
        vm.close()

        assertTrue(vm.uiState.value.selected == null)
    }
}
