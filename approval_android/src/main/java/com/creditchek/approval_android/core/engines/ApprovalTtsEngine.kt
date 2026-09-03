package com.creditchek.approval_android.core.engines

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class ApprovalTtsEngine(
    context: Context
): TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenText: String? = null


    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                tts?.language = Locale.getDefault()
            }
                tts?.setSpeechRate(0.95f) //Natural speaking speed
                isInitialized = true

            //Speed queued initial text if present
            lastSpokenText?.let { text ->
                speak(text, force = true)
            }
        } else {
            Log.w("ApprovalTtsEngine", "TTS initialization failed (status: $status)")
        }
    }
    /**
     * Speaks guidance instruction. Flushes any previous audio to prevent lag.
     */
    fun speak(text: String, force: Boolean = false) {
//        if (!isEnabled || text.isBlank()) return
        val cleanText = sanitizeInstruction(text)
        if (!force && cleanText == lastSpokenText) return
        lastSpokenText = cleanText
        if (isInitialized) {
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "approval_guidance_utterance")
        }
    }
    /**
     * Cleans up prefixes like "Step 1 of 8: Hold still" -> "Hold still"
     * for a natural, clear voice experience.
     */
    private fun sanitizeInstruction(raw: String): String {
        return if (raw.contains(": ")) {
            raw.substringAfter(": ").trim()
        } else {
            raw.trim()
        }
    }
    fun stop() {
        tts?.stop()
    }
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
