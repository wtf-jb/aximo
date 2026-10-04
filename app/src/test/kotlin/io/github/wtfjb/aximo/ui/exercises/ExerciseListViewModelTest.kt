package io.github.wtfjb.aximo.ui.exercises

import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ExerciseListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun ex(id: Long, name: String, muscle: MuscleGroup, archived: Boolean = false) = Exercise(
        id = id,
        name = name,
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        primaryMuscles = setOf(muscle),
        archived = archived,
    )

    private val repo = FakeExerciseRepository(
        listOf(
            ex(1, "Bankdrücken", MuscleGroup.CHEST),
            ex(2, "Kniebeuge", MuscleGroup.QUADS),
            ex(3, "Butterfly", MuscleGroup.CHEST, archived = true),
        ),
    )

    /** stateIn(WhileSubscribed) only runs while someone collects. */
    private fun TestScope.subscribed(vm: ExerciseListViewModel) =
        vm.uiState.launchIn(backgroundScope)

    @Test
    fun showsActiveExercisesSortedByName() = runTest(UnconfinedTestDispatcher()) {
        val vm = ExerciseListViewModel(repo)
        subscribed(vm)

        assertEquals(listOf("Bankdrücken", "Kniebeuge"), vm.uiState.value.exercises.map { it.name })
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun filtersByQueryAndRegion() = runTest(UnconfinedTestDispatcher()) {
        val vm = ExerciseListViewModel(repo)
        subscribed(vm)

        vm.onRegionSelect(BodyRegion.LEGS)
        assertEquals(listOf("Kniebeuge"), vm.uiState.value.exercises.map { it.name })

        vm.onRegionSelect(null)
        vm.onQueryChange("bank")
        assertEquals(listOf("Bankdrücken"), vm.uiState.value.exercises.map { it.name })
    }

    @Test
    fun archiveShowsOnlyArchivedExercises() = runTest(UnconfinedTestDispatcher()) {
        val vm = ExerciseListViewModel(repo)
        subscribed(vm)

        vm.onShowArchivedToggle()

        assertEquals(listOf("Butterfly"), vm.uiState.value.exercises.map { it.name })
    }
}
