package io.github.wtfjb.aximo.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import io.github.wtfjb.aximo.domain.logging.SpeechError
import io.github.wtfjb.aximo.domain.logging.SpeechEvent
import io.github.wtfjb.aximo.domain.logging.SpeechInput
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Android's [SpeechRecognizer], on-device only (B-04). Before Android 12 there is no
 * way to ask for an on-device recognizer (the system may silently use a cloud one),
 * so [isAvailable] is false there. No Google Play Services involved.
 */
class AndroidSpeechInput(private val context: Context) : SpeechInput {

    override fun isAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun listen(languageTag: String): Flow<SpeechEvent> = callbackFlow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isAvailable()) {
            trySend(SpeechEvent.Failed(SpeechError.FAILED))
            close()
            return@callbackFlow
        }
        val recognizer = onDeviceRecognizer()
        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(SpeechEvent.Listening)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    firstText(partialResults)?.let { trySend(SpeechEvent.Partial(it)) }
                }

                override fun onResults(results: Bundle?) {
                    val text = firstText(results)
                    trySend(if (text != null) SpeechEvent.Result(text) else SpeechEvent.Failed(SpeechError.NOTHING_HEARD))
                    close()
                }

                override fun onError(error: Int) {
                    trySend(SpeechEvent.Failed(errorFor(error)))
                    close()
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        recognizer.startListening(intent)
        awaitClose {
            recognizer.cancel()
            recognizer.destroy()
        }
        // The recognizer must be used on the main thread.
    }.flowOn(Dispatchers.Main.immediate)

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onDeviceRecognizer(): SpeechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

    private fun firstText(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun errorFor(code: Int): SpeechError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NOTHING_HEARD
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION
        // ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE (API 31)
        ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE -> SpeechError.LANGUAGE_UNAVAILABLE
        else -> SpeechError.FAILED
    }

    private companion object {
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }
}
