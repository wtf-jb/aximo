package io.github.wtfjb.aximo.domain.rest

import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What happens outside the app logic: notification, sound, vibration. Implemented in :app. */
interface RestTimerEffects {
    /** A rest started: show the running notification. */
    fun started()

    /** A rest ran out (not skipped): alert the user. */
    fun finished(next: NextSet?)
}

/**
 * Holds the one running rest of the app (A-03). Lives as long as the app, so the
 * timer keeps running when the workout screen is closed.
 */
class RestTimerController(
    private val time: TimeSource,
    private val scope: CoroutineScope,
    private val effects: RestTimerEffects,
) {
    private val _state = MutableStateFlow<RestTimer?>(null)

    /** The running rest, or null. */
    val state: StateFlow<RestTimer?> = _state.asStateFlow()

    private var expiry: Job? = null

    /** Starts a new rest, replacing a running one. 0 seconds or less means no rest. */
    fun start(seconds: Int, next: NextSet?) {
        if (seconds <= 0) {
            stop()
            return
        }
        val now = time.now()
        run(RestTimer(startedAt = now, endsAt = now + seconds.seconds, next = next))
        effects.started()
    }

    /** "+15 s". Does nothing when no rest is running. */
    fun extend(seconds: Int = RestTimerLogic.EXTEND_SECONDS) {
        val timer = _state.value ?: return
        run(timer.extendedBy(seconds))
    }

    /** Skip the rest, or end it because the workout is over. No alert. */
    fun stop() {
        expiry?.cancel()
        expiry = null
        _state.value = null
    }

    private fun run(timer: RestTimer) {
        expiry?.cancel()
        _state.value = timer
        expiry = scope.launch {
            delay(timer.endsAt - time.now())
            if (_state.value == timer) {
                _state.value = null
                effects.finished(timer.next)
            }
        }
    }
}
