package com.creditchek.approval_android.core.engines

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class ApprovalTtsEngine(
    context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var lastSpokenText: String? = null

    init {
        try {
            // Using context.applicationContext avoids leaking Activity while binding TTS service
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("ApprovalTtsEngine", "Error instantiating TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.i("ApprovalTtsEngine", "TextToSpeech service connected successfully")

            // Configure audio routing for accessibility / guidance speech
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            // Robust language selection with fallbacks
            var langResult = tts?.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = tts?.setLanguage(Locale.getDefault())
            }
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }

            tts?.setSpeechRate(0.95f) // Natural speaking speed
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("ApprovalTtsEngine", "Speech utterance started: $utteranceId")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d("ApprovalTtsEngine", "Speech utterance completed: $utteranceId")
                }
                override fun onError(utteranceId: String?) {
                    Log.w("ApprovalTtsEngine", "Speech utterance error: $utteranceId")
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w("ApprovalTtsEngine", "Speech utterance error: $utteranceId (code: $errorCode)")
                }
            })

            isInitialized = true

            // Speak queued initial text if present
            pendingText?.let { text ->
                Log.i("ApprovalTtsEngine", "Playing pending guidance after init: '$text'")
                speak(text, force = true)
                pendingText = null
            }
        } else {
            Log.e("ApprovalTtsEngine", "TextToSpeech initialization failed (status: $status)")
        }
    }

    /**
     * Speaks guidance instruction. Flushes any previous audio to prevent lag.
     */
    fun speak(text: String, force: Boolean = false) {
        if (text.isBlank() || text == "Starting camera..." || text == "Completing verification...") {
            return
        }
        val cleanText = sanitizeInstruction(text)
        if (cleanText.isBlank()) return

        if (!force && cleanText == lastSpokenText) {
            return
        }

        if (!isInitialized) {
            Log.i("ApprovalTtsEngine", "TTS not initialized yet. Queued: '$cleanText'")
            pendingText = cleanText
            return
        }

        lastSpokenText = cleanText

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val res = tts?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "approval_guidance_${System.currentTimeMillis()}"
        )
        Log.i("ApprovalTtsEngine", "TTS speaking: '$cleanText' (result: $res)")
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
        lastSpokenText = null
        pendingText = null
    }
}
