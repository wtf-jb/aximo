package io.github.wtfjb.aximo.domain.logging

import kotlinx.coroutines.flow.Flow

/** Why speech recognition produced nothing. */
enum class SpeechError {
    /** No microphone permission. */
    PERMISSION,

    /** Nothing (understandable) was said. */
    NOTHING_HEARD,

    /** The on-device recognizer lacks the language pack. */
    LANGUAGE_UNAVAILABLE,

    /** No on-device recognizer, or it failed. */
    FAILED,
}

/** What happens while listening. The flow ends after [Result] or [Failed]. */
sealed interface SpeechEvent {
    /** The recognizer is ready, the user can speak. */
    data object Listening : SpeechEvent

    /** Text so far; replaced by the next one. */
    data class Partial(val text: String) : SpeechEvent

    data class Result(val text: String) : SpeechEvent

    data class Failed(val error: SpeechError) : SpeechEvent
}

/**
 * Speech to text (B-04). Implemented in `:app` with Android's recognizer, on-device
 * only: audio never goes to a cloud recognizer.
 */
interface SpeechInput {
    /** An on-device recognizer exists. Without one the microphone is not offered. */
    fun isAvailable(): Boolean

    /** Starts listening in [languageTag] ("de-DE"); cancelling the collector stops it. */
    fun listen(languageTag: String): Flow<SpeechEvent>
}
