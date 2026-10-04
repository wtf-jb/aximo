package io.github.wtfjb.aximo.domain.logging

import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LoggingBuilderTest {

    private val bench = Exercise(id = 1, name = "Bankdrücken", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val pullUp = Exercise(id = 2, name = "Klimmzug", type = ExerciseType.BODYWEIGHT, equipment = Equipment.BODYWEIGHT)
    private val squat = Exercise(id = 3, name = "Kniebeuge", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)
    private val exercises = listOf(bench, pullUp, squat)
    private val settings = TrainingSettings()

    private fun group(count: Int = 1, weight: Double? = 80.0, unit: WeightUnit? = null, reps: Int = 8, rir: Int? = null, rpe: Double? = null, type: SetType = SetType.WORKING) =
        ParsedSetGroup(count, weight, unit, reps, rir, rpe, type)

    private fun parsed(name: String, vararg groups: ParsedSetGroup, type: ExerciseType? = null, equipment: Equipment? = null) =
        ParsedExercise(name, type, equipment, groups.toList())

    private fun build(vararg entries: ParsedExercise, dropped: Int = 0, catalog: List<CatalogEntry> = emptyList(), settings: TrainingSettings = this.settings) =
        LoggingBuilder.build(ParsedLog(entries.toList(), dropped), exercises, catalog, settings)

    @Test
    fun threeTimesEightBecomesThreeSets() {
        val proposal = build(parsed("Bankdrücken", group(count = 3, reps = 8, rir = 2)))

        val entry = proposal.entries.single()
        assertEquals(ExerciseChoice.Existing(bench), entry.choice)
        assertEquals(List(3) { ProposedSet(80.0, 8, 2, null, SetType.WORKING) }, entry.sets)
        assertEquals(0, proposal.dropped)
        assertTrue(proposal.canSave)
    }

    @Test
    fun separateGroupsKeepTheirOrder() {
        val proposal = build(parsed("Kniebeuge", group(weight = 100.0, reps = 5), group(weight = 105.0, reps = 3)))

        assertEquals(listOf(100.0 to 5, 105.0 to 3), proposal.entries.single().sets.map { it.weightKg to it.reps })
    }

    @Test
    fun severalExercisesInOneText() {
        val proposal = build(parsed("Bankdrücken", group()), parsed("Klimmzüge", group(count = 4, weight = null, reps = 10)))

        assertEquals(listOf(bench, pullUp), proposal.entries.map { (it.choice as ExerciseChoice.Existing).exercise })
        assertEquals(4, proposal.entries[1].sets.size)
        assertFalse(proposal.entries[1].weightMissing)
    }

    @Test
    fun warmUpAndOtherTypesAreKept() {
        val proposal = build(parsed("Bankdrücken", group(weight = 40.0, reps = 10, type = SetType.WARM_UP), group()))

        assertEquals(listOf(SetType.WARM_UP, SetType.WORKING), proposal.entries.single().sets.map { it.setType })
    }

    @Test
    fun weightIsConvertedFromTheDisplayUnitToKg() {
        val lbs = TrainingSettings().withUnit(WeightUnit.LBS)

        val proposal = build(parsed("Bankdrücken", group(weight = 100.0)), settings = lbs)

        assertEquals(45.359237, proposal.entries.single().sets.single().weightKg!!, 1e-9)
    }

    @Test
    fun namedUnitWinsOverTheSetting() {
        val lbs = TrainingSettings().withUnit(WeightUnit.LBS)

        val proposal = build(parsed("Bankdrücken", group(weight = 80.0, unit = WeightUnit.KG), group(weight = 100.0, unit = WeightUnit.LBS)), settings = lbs)

        assertEquals(80.0, proposal.entries.single().sets[0].weightKg!!, 1e-9)
        assertEquals(45.359237, proposal.entries.single().sets[1].weightKg!!, 1e-9)
    }

    @Test
    fun missingWeightIsFlaggedForWeightedExercises() {
        val entry = build(parsed("Bankdrücken", group(weight = null))).entries.single()

        assertNull(entry.sets.single().weightKg)
        assertTrue(entry.weightMissing)
    }

    @Test
    fun rirAndRpeAreStoredAsTyped() {
        val proposal = build(parsed("Bankdrücken", group(rir = 2), group(rpe = 8.0), group(rpe = 8.3), group()))

        val sets = proposal.entries.single().sets
        assertEquals(2, sets[0].rir)
        assertNull(sets[0].rpe)
        assertEquals(8.0, sets[1].rpe!!, 0.0)
        assertNull(sets[1].rir)
        assertEquals(8.5, sets[2].rpe!!, 0.0) // half points
        assertNull(sets[3].rir)
        assertNull(sets[3].rpe)
    }

    @Test
    fun bothScalesKeepOnlyTheOneFromTheSettings() {
        val both = group(rir = 2, rpe = 8.0)

        val rir = build(parsed("Bankdrücken", both)).entries.single().sets.single()
        val rpe = build(parsed("Bankdrücken", both), settings = TrainingSettings(rating = SetRating.RPE)).entries.single().sets.single()

        assertEquals(2, rir.rir)
        assertNull(rir.rpe)
        assertEquals(8.0, rpe.rpe!!, 0.0)
        assertNull(rpe.rir)
    }

    @Test
    fun valuesOutOfRangeDropTheGroupAndCountIt() {
        val proposal = build(
            parsed(
                "Bankdrücken",
                group(),
                group(reps = 0),
                group(reps = 101),
                group(count = 0),
                group(count = 11),
                group(weight = 1_001.0),
                group(rir = -1),
                group(rir = 11),
                group(rpe = 0.5),
                group(rpe = 11.0),
            ),
        )

        assertEquals(1, proposal.entries.single().sets.size)
        assertEquals(9, proposal.dropped)
    }

    @Test
    fun negativeWeightIsAssistance() {
        val entry = build(parsed("Klimmzug", group(weight = -20.0))).entries.single()

        assertEquals(-20.0, entry.sets.single().weightKg!!, 0.0)
    }

    @Test
    fun entryWithoutUsableSetsOrNameIsDropped() {
        val proposal = build(parsed("Bankdrücken", group()), parsed("Kniebeuge", group(reps = 0)), parsed("  ", group()), dropped = 2)

        assertEquals(1, proposal.entries.size)
        assertEquals(2 + 1 + 1 + 1, proposal.dropped) // 2 from the parser, the bad set, its empty entry, the blank name
    }

    @Test
    fun nothingUsableThrows() {
        try {
            build(parsed("Bankdrücken", group(reps = 0)))
            fail()
        } catch (e: LoggingException) {
            assertEquals(LoggingException.Reason.NOTHING_USABLE, e.reason)
        }
        try {
            LoggingBuilder.build(ParsedLog(emptyList()), exercises, emptyList(), settings)
            fail()
        } catch (e: LoggingException) {
            assertEquals(LoggingException.Reason.NOTHING_USABLE, e.reason)
        }
    }

    @Test
    fun setsPerExerciseAndExercisesPerTextAreLimited() {
        val many = parsed("Bankdrücken", group(count = 10), group(count = 10), group(count = 10))
        val tooMany = List(12) { parsed("Neue Übung $it", group()) }

        val proposal = build(many, *tooMany.toTypedArray())

        assertEquals(LoggingBuilder.MAX_EXERCISES, proposal.entries.size)
        assertEquals(LoggingBuilder.MAX_SETS_PER_EXERCISE, proposal.entries.first().sets.size)
        assertEquals(3, proposal.dropped) // 13 entries, 10 kept
    }

    @Test
    fun unsureMatchNeedsAChoiceAndOffersToCreate() {
        val list = listOf(
            Exercise(id = 1, name = "Bankdrücken Langhantel", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL),
            Exercise(id = 2, name = "Bankdrücken Kurzhantel", type = ExerciseType.STRENGTH, equipment = Equipment.DUMBBELL),
        )

        val proposal = LoggingBuilder.build(ParsedLog(listOf(parsed("Bankdrücken", group()))), list, emptyList(), settings)

        val entry = proposal.entries.single()
        assertNull(entry.selected)
        assertEquals(listOf("Bankdrücken Kurzhantel", "Bankdrücken Langhantel"), entry.options.filterIsInstance<ExerciseChoice.Existing>().map { it.exercise.name })
        assertTrue(entry.options.last() is ExerciseChoice.Create)
        assertTrue(proposal.needsChoice)
        assertFalse(proposal.canSave)

        val chosen = proposal.select(0, 1)
        assertTrue(chosen.canSave)
        assertEquals(1L, (chosen.confirmed.single().choice as ExerciseChoice.Existing).exercise.id)
    }

    @Test
    fun deselectedEntryDoesNotBlockSaving() {
        val list = listOf(
            Exercise(id = 1, name = "Bankdrücken Langhantel", type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL),
            Exercise(id = 2, name = "Bankdrücken Kurzhantel", type = ExerciseType.STRENGTH, equipment = Equipment.DUMBBELL),
            squat,
        )
        val proposal = LoggingBuilder.build(ParsedLog(listOf(parsed("Bankdrücken", group()), parsed("Kniebeuge", group()))), list, emptyList(), settings)

        assertFalse(proposal.canSave)
        val without = proposal.setIncluded(0, false)
        assertTrue(without.canSave)
        assertEquals(listOf("Kniebeuge"), without.confirmed.map { it.spoken })
        assertFalse(without.setIncluded(1, false).canSave)
    }

    @Test
    fun unknownNameBecomesNewExerciseWithHints() {
        val entry = build(parsed("wadenheben sitzend", group(), type = ExerciseType.STRENGTH, equipment = Equipment.MACHINE)).entries.single()

        val create = entry.options.single() as ExerciseChoice.Create
        assertEquals(0, entry.selected)
        assertEquals("Wadenheben sitzend", create.exercise.name)
        assertEquals(ExerciseType.STRENGTH, create.exercise.type)
        assertEquals(Equipment.MACHINE, create.exercise.equipment)
        assertNull(create.exercise.catalogEntry)
    }

    @Test
    fun newExerciseDefaultsWithoutHints() {
        val create = build(parsed("Irgendwas", group())).entries.single().options.single() as ExerciseChoice.Create
        val bodyweight = build(parsed("Irgendwas", group(), type = ExerciseType.BODYWEIGHT)).entries.single().options.single() as ExerciseChoice.Create
        val cardio = build(parsed("Irgendwas", group(), type = ExerciseType.CARDIO)).entries.single().options.single() as ExerciseChoice.Create

        assertEquals(ExerciseType.STRENGTH to Equipment.OTHER, create.exercise.type to create.exercise.equipment)
        assertEquals(ExerciseType.BODYWEIGHT to Equipment.BODYWEIGHT, bodyweight.exercise.type to bodyweight.exercise.equipment)
        assertEquals(ExerciseType.STRENGTH, cardio.exercise.type)
    }

    private val catalogEntry = CatalogEntry(
        id = "Cable_Pullover",
        name = "Cable Pullover",
        type = ExerciseType.STRENGTH,
        equipment = Equipment.CABLE,
        primary = setOf(MuscleGroup.LATS),
        secondary = emptySet(),
        repMin = 10,
        repMax = 15,
        instructions = listOf("Pull."),
    )

    @Test
    fun newExerciseTakesLibraryEntryThatFitsTheName() {
        val create = build(parsed("cable pullover", group()), catalog = listOf(catalogEntry)).entries.single().options.single() as ExerciseChoice.Create

        assertEquals(catalogEntry, create.exercise.catalogEntry)
        val exercise = create.exercise.toExercise(settings)
        assertEquals("Cable pullover", exercise.name)
        assertEquals(Equipment.CABLE, exercise.equipment)
        assertEquals(setOf(MuscleGroup.LATS), exercise.primaryMuscles)
        assertEquals(10..15, exercise.repRangeMin..exercise.repRangeMax)
        assertEquals("fed_Cable_Pullover", exercise.catalogId)
    }

    @Test
    fun newExerciseWithoutLibraryUsesSettingsForIncrementAndRest() {
        val custom = TrainingSettings(restSeconds = 90)
        val exercise = NewExercise("Neu", ExerciseType.STRENGTH, Equipment.DUMBBELL, null).toExercise(custom)

        assertEquals(custom.steps.dumbbellKg, exercise.incrementKg, 0.0)
        assertEquals(90, exercise.restSeconds)
        assertNull(exercise.catalogId)
    }

    @Test
    fun inputListsOnlyUsableExercisesAndTheSettings() {
        val cardio = Exercise(id = 9, name = "Laufen", type = ExerciseType.CARDIO, equipment = Equipment.OTHER)
        val archived = squat.copy(id = 8, name = "Alt", archived = true)

        val input = LoggingInput.from("  Bankdrücken 3x8  ", listOf(squat, cardio, archived, bench), TrainingSettings(rating = SetRating.RPE))

        assertEquals("Bankdrücken 3x8", input.text)
        assertEquals(listOf("Bankdrücken", "Kniebeuge"), input.exercises.map { it.name })
        assertEquals(SetRating.RPE, input.rating)
        assertEquals(WeightUnit.KG, input.unit)
        assertEquals(LoggingInput.MAX_TEXT, LoggingInput.from("x".repeat(5_000), emptyList(), settings).text.length)
    }
}
