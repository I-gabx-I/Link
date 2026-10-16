package com.example.link.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class SpeechRecognitionError { PermissionDenied, Unavailable, NoSpeech, Network, Other }

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data class Listening(val partialText: String = "") : SpeechRecognitionState
    data class Processing(val partialText: String = "") : SpeechRecognitionState
    data class Result(val text: String) : SpeechRecognitionState
    data class Error(val reason: SpeechRecognitionError, val message: String) : SpeechRecognitionState
}

class SpeechRecognizerManager(private val context: Context) : RecognitionListener {
    var state: SpeechRecognitionState by mutableStateOf(SpeechRecognitionState.Idle)
        private set
    private var recognizer: SpeechRecognizer? = null
    private var partialText = ""
    private var cancelled = false
    private var destroyed = false

    fun startListening() {
        if (destroyed) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            state = SpeechRecognitionState.Error(SpeechRecognitionError.Unavailable, "Este teléfono no tiene un servicio de reconocimiento de voz disponible.")
            return
        }
        cancelled = false
        partialText = ""
        try {
            val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(this)
                recognizer = it
            }
            state = SpeechRecognitionState.Listening()
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-GT")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
        } catch (_: SecurityException) {
            showPermissionDenied()
        } catch (_: RuntimeException) {
            state = SpeechRecognitionState.Error(SpeechRecognitionError.Other, "No pude iniciar el micrófono. Intenta de nuevo.")
        }
    }

    fun showPermissionDenied() {
        state = SpeechRecognitionState.Error(SpeechRecognitionError.PermissionDenied, "Permite el acceso al micrófono para dictar tu solicitud.")
    }

    fun cancelListening() {
        cancelled = true
        recognizer?.cancel()
        state = SpeechRecognitionState.Idle
    }

    fun destroy() {
        destroyed = true
        cancelled = true
        recognizer?.destroy()
        recognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) { if (!cancelled) state = SpeechRecognitionState.Listening() }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onEndOfSpeech() { if (!cancelled) state = SpeechRecognitionState.Processing(partialText) }
    override fun onPartialResults(results: Bundle?) {
        if (cancelled || destroyed) return
        partialText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        state = SpeechRecognitionState.Listening(partialText)
    }
    override fun onResults(results: Bundle?) {
        if (cancelled || destroyed) return
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull { it.isNotBlank() }
        state = if (text != null) SpeechRecognitionState.Result(text.trim())
        else SpeechRecognitionState.Error(SpeechRecognitionError.NoSpeech, "No alcancé a escuchar una frase. Intenta nuevamente.")
    }
    override fun onError(error: Int) {
        if (cancelled || destroyed) return
        state = when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionState.Error(SpeechRecognitionError.PermissionDenied, "Permite el acceso al micrófono.")
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionState.Error(SpeechRecognitionError.NoSpeech, "No alcancé a escuchar una frase. Intenta nuevamente.")
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognitionState.Error(SpeechRecognitionError.Network, "Revisa tu conexión a Internet y vuelve a intentarlo.")
            else -> SpeechRecognitionState.Error(SpeechRecognitionError.Other, "El reconocimiento de voz no pudo terminar. Intenta nuevamente.")
        }
    }
}
