package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.exercise.ExerciseFilter
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {

    private fun ex(name: String, vararg primary: MuscleGroup, secondary: Set<MuscleGroup> = emptySet()) = Exercise(
        name = name,
        type = ExerciseType.STRENGTH,
        equipment = Equipment.BARBELL,
        primaryMuscles = primary.toSet(),
        secondaryMuscles = secondary,
    )

    private val bench = ex("Bankdrücken", MuscleGroup.CHEST, secondary = setOf(MuscleGroup.TRICEPS))
    private val row = ex("Rudern vorgebeugt", MuscleGroup.UPPER_BACK, MuscleGroup.LATS)
    private val curl = ex("Hammercurls", MuscleGroup.BICEPS)
    private val all = listOf(bench, row, curl)

    @Test
    fun emptyFilterKeepsEverything() {
        assertEquals(all, ExerciseFilter().apply(all))
    }

    @Test
    fun searchIgnoresCaseAndUmlauts() {
        assertEquals(listOf(bench), ExerciseFilter(query = "BANKDRUCKEN").apply(all))
        assertEquals(listOf(bench), ExerciseFilter(query = "drück").apply(all))
    }

    @Test
    fun allWordsMustMatchInAnyOrder() {
        assertEquals(listOf(row), ExerciseFilter(query = "vorgebeugt rud").apply(all))
        assertEquals(emptyList<Exercise>(), ExerciseFilter(query = "rudern curls").apply(all))
    }

    @Test
    fun regionUsesPrimaryMusclesOnly() {
        assertEquals(listOf(curl), ExerciseFilter(region = BodyRegion.ARMS).apply(all))
        assertEquals(listOf(row), ExerciseFilter(region = BodyRegion.BACK).apply(all))
    }

    @Test
    fun searchAndRegionCombine() {
        assertEquals(emptyList<Exercise>(), ExerciseFilter(query = "bank", region = BodyRegion.BACK).apply(all))
    }
}
