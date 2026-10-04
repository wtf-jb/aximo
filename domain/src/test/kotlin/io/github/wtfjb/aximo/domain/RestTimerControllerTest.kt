package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.rest.NextSet
import io.github.wtfjb.aximo.domain.rest.RestTimerController
import io.github.wtfjb.aximo.domain.rest.RestTimerEffects
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerControllerTest {

    private class RecordingEffects : RestTimerEffects {
        var started = 0
        val finished = mutableListOf<NextSet?>()
        override fun started() {
            started++
        }
        override fun finished(next: NextSet?) {
            finished += next
        }
    }

    private val effects = RecordingEffects()
    private val next = NextSet("Bankdrücken", 3)

    /** Clock that follows the virtual time of the test. */
    private fun TestScope.controller() = RestTimerController(
        time = TimeSource { Instant.fromEpochMilliseconds(1_790_000_000_000 + currentTime) },
        scope = backgroundScope,
        effects = effects,
    )

    @Test
    fun runsOutAndAlerts() = runTest {
        val rest = controller()
        rest.start(90, next)
        assertEquals(1, effects.started)
        assertEquals(90, rest.state.value!!.totalSeconds)

        advanceTimeBy(89.seconds)
        runCurrent()
        assertNotNull(rest.state.value)
        assertEquals(emptyList<NextSet?>(), effects.finished)

        advanceTimeBy(2.seconds)
        runCurrent()
        assertNull(rest.state.value)
        assertEquals(listOf<NextSet?>(next), effects.finished)
    }

    @Test
    fun extendingPostponesTheEnd() = runTest {
        val rest = controller()
        rest.start(60, next)
        advanceTimeBy(50.seconds)
        rest.extend()

        advanceTimeBy(20.seconds)
        runCurrent()
        assertNotNull(rest.state.value)

        advanceTimeBy(10.seconds)
        runCurrent()
        assertNull(rest.state.value)
        assertEquals(1, effects.finished.size)
    }

    @Test
    fun skippingEndsWithoutAlert() = runTest {
        val rest = controller()
        rest.start(60, next)
        rest.stop()
        assertNull(rest.state.value)

        advanceTimeBy(120.seconds)
        runCurrent()
        assertEquals(0, effects.finished.size)
    }

    @Test
    fun startingAgainReplacesTheRunningRest() = runTest {
        val rest = controller()
        rest.start(60, next)
        advanceTimeBy(30.seconds)
        rest.start(60, null)

        advanceTimeBy(40.seconds)
        runCurrent()
        assertNotNull(rest.state.value)
        assertEquals(0, effects.finished.size)

        advanceTimeBy(30.seconds)
        runCurrent()
        assertEquals(listOf<NextSet?>(null), effects.finished)
    }

    @Test
    fun zeroSecondsStartsNothing() = runTest {
        val rest = controller()
        rest.start(0, next)
        assertNull(rest.state.value)
        assertEquals(0, effects.started)
    }

    @Test
    fun extendWithoutRestDoesNothing() = runTest {
        val rest = controller()
        rest.extend()
        assertNull(rest.state.value)
    }
}
