package io.github.wtfjb.aximo.domain.time

import kotlin.time.Clock
import kotlin.time.Instant

/** Current time. An interface so tests can use a fixed time. */
fun interface TimeSource {
    fun now(): Instant

    companion object {
        val System = TimeSource { Clock.System.now() }
    }
}
