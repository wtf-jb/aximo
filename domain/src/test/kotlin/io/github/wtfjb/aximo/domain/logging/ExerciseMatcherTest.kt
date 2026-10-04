package io.github.wtfjb.aximo.domain.logging

import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseMatcherTest {

    private fun exercise(id: Long, name: String) = Exercise(id = id, name = name, type = ExerciseType.STRENGTH, equipment = Equipment.BARBELL)

    private val bench = exercise(1, "Bankdrücken")
    private val squat = exercise(2, "Kniebeuge")
    private val pullUp = exercise(3, "Klimmzug")
    private val inclineDumbbell = exercise(4, "Schrägbankdrücken Kurzhantel")
    private val all = listOf(bench, squat, pullUp, inclineDumbbell)

    private fun sure(spoken: String, list: List<Exercise> = all): Exercise =
        (ExerciseMatcher.match(spoken, list) as ExerciseMatch.Sure).exercise

    @Test
    fun exactNameIgnoresCaseUmlautsAndPunctuation() {
        assertEquals(bench, sure("Bankdrücken"))
        assertEquals(bench, sure("bankdrucken"))
        assertEquals(bench, sure("  BANKDRÜCKEN. "))
        assertEquals(inclineDumbbell, sure("schraegbankdruecken-kurzhantel".replace("ae", "ä").replace("ue", "ü")))
    }

    @Test
    fun wordOrderDoesNotMatter() {
        assertEquals(inclineDumbbell, sure("Kurzhantel Schrägbankdrücken"))
    }

    @Test
    fun inflectedFormsMatch() {
        assertEquals(squat, sure("Kniebeugen"))
        assertEquals(pullUp, sure("Klimmzüge"))
        assertEquals(pullUp, sure("klimmzuege".replace("ue", "ü")))
    }

    @Test
    fun smallTyposMatch() {
        assertEquals(bench, sure("Bankdrukcen"))
        assertEquals(squat, sure("Knibeuge"))
    }

    @Test
    fun exactBeatsLongerNameWithSameWords() {
        val list = listOf(exercise(1, "Bankdrücken Langhantel"), bench)
        assertEquals(bench, sure("Bankdrücken", list))
    }

    @Test
    fun singleLongerNameIsEnough() {
        val long = exercise(1, "Bankdrücken Langhantel")
        assertEquals(long, sure("Bankdrücken", listOf(long, squat)))
    }

    @Test
    fun equalHitsAreUnsureAndListedByName() {
        val list = listOf(exercise(1, "Bankdrücken Langhantel"), exercise(2, "Bankdrücken Kurzhantel"), squat)

        val match = ExerciseMatcher.match("Bankdrücken", list) as ExerciseMatch.Unsure

        assertEquals(listOf("Bankdrücken Kurzhantel", "Bankdrücken Langhantel"), match.candidates.map { it.name })
    }

    @Test
    fun partialWordGivesCandidatesNotAutomaticMatch() {
        val match = ExerciseMatcher.match("Bank", all) as ExerciseMatch.Unsure

        assertEquals(listOf(bench), match.candidates)
    }

    @Test
    fun unrelatedNameHasNoMatch() {
        assertEquals(ExerciseMatch.None, ExerciseMatcher.match("Wadenheben", all))
        assertEquals(ExerciseMatch.None, ExerciseMatcher.match("Bench Press", all))
        assertEquals(ExerciseMatch.None, ExerciseMatcher.match("", all))
        assertEquals(ExerciseMatch.None, ExerciseMatcher.match("!!", all))
        assertEquals(ExerciseMatch.None, ExerciseMatcher.match("Bankdrücken", emptyList()))
    }

    @Test
    fun twoLetterWordsNeedToBeEqual() {
        assertEquals(0.0, ExerciseMatcher.score("ab", "ac"), 0.0)
        assertEquals(1.0, ExerciseMatcher.score("ab", "ab"), 0.0)
    }

    @Test
    fun sureMatchOffersWeakerHitsAsAlternatives() {
        val list = listOf(bench, inclineDumbbell, exercise(5, "Bankdrücken eng"))

        val match = ExerciseMatcher.match("Bankdrücken", list) as ExerciseMatch.Sure

        assertEquals(bench, match.exercise)
        assertTrue(match.alternatives.isNotEmpty())
        assertTrue(match.alternatives.size <= ExerciseMatcher.MAX_CANDIDATES - 1)
    }

    @Test
    fun duplicateNamesAreNotSure() {
        val list = listOf(exercise(1, "Bankdrücken"), exercise(2, "Bankdrücken"))

        assertTrue(ExerciseMatcher.match("Bankdrücken", list) is ExerciseMatch.Unsure)
    }

    @Test
    fun accentsAreFolded() {
        assertEquals(exercise(1, "Crème"), sure("creme", listOf(exercise(1, "Crème"))))
    }
}
