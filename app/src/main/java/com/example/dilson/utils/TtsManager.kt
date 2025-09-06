package com.example.dilson.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context, private val locale: Locale = Locale.forLanguageTag("ar")) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingRate: Float? = null
    private var pendingPitch: Float? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // fallback to default
                    tts?.language = Locale.getDefault()
                }
            } catch (_: Exception) {
                tts?.language = Locale.getDefault()
            }
            pendingRate?.let { tts?.setSpeechRate(it) }
            pendingPitch?.let { tts?.setPitch(it) }
            ready = true
        }
    }

    fun setSpeechRate(rate: Float) {
        if (ready) tts?.setSpeechRate(rate) else pendingRate = rate
    }

    fun setPitch(pitch: Float) {
        if (ready) tts?.setPitch(pitch) else pendingPitch = pitch
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!ready) return
        tts?.speak(text, queueMode, null, null)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
