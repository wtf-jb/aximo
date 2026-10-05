package io.github.wtfjb.aximo.ui.coach

import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.review.AiSuggestion
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionReason
import org.junit.Assert.assertEquals
import org.junit.Test

class CoachCategoryTest {

    private fun category(change: SuggestionChange, reason: SuggestionReason?) =
        AiSuggestion(change = change, rationale = "r", reason = reason).category()

    private val setCount = SuggestionChange.SetCount(1, 2, 3, 4)
    private val repRange = SuggestionChange.RepRange(1, 2, 6, 8, 8, 10)

    @Test
    fun reasonDecidesTheChip() {
        assertEquals(R.string.coach_category_volume, category(repRange, SuggestionReason.VOLUME_LOW))
        assertEquals(R.string.coach_category_volume, category(repRange, SuggestionReason.VOLUME_HIGH))
        listOf(SuggestionReason.PROGRESS, SuggestionReason.STAGNATION, SuggestionReason.REGRESSION, SuggestionReason.RETURNING, SuggestionReason.REP_CEILING)
            .forEach { assertEquals(it.name, R.string.coach_category_performance, category(setCount, it)) }
        assertEquals(R.string.coach_category_effort, category(setCount, SuggestionReason.EFFORT_HIGH))
        assertEquals(R.string.coach_category_effort, category(setCount, SuggestionReason.EFFORT_LOW))
    }

    @Test
    fun withoutReasonTheTypeDecides() {
        assertEquals(R.string.coach_category_volume, category(setCount, null))
        assertEquals(R.string.coach_category_exercise, category(repRange, null))
        assertEquals(R.string.coach_category_volume, category(setCount, SuggestionReason.OTHER))
        assertEquals(R.string.coach_category_exercise, category(repRange, SuggestionReason.OTHER))
        assertEquals(R.string.coach_category_routine, category(SuggestionChange.RenameRoutine(1, "A", "B"), null))
    }
}
