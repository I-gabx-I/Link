package com.example.link.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

class SystemSpeaker(context: Context) {
    private var ready = false
    private var pending: String? = null
    private var engine: TextToSpeech? = null
    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            Handler(Looper.getMainLooper()).post {
                if (status == TextToSpeech.SUCCESS) {
                    val speech = engine ?: return@post
                    val language = Locale.forLanguageTag("es-GT")
                    speech.language = if (speech.isLanguageAvailable(language) >= TextToSpeech.LANG_AVAILABLE) language else Locale.forLanguageTag("es")
                    ready = true
                    pending?.let { speak(it) }
                    pending = null
                }
            }
        }
    }
    fun speak(text: String) {
        if (text.isBlank()) return
        if (ready) engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "link-reply") else pending = text
    }
    fun stop() { pending = null; engine?.stop() }
    fun destroy() { pending = null; ready = false; engine?.shutdown(); engine = null }
}
