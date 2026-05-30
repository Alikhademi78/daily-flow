package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechToTextManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateChange: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isListening = false

    init {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            onStateChange(true)
                            isListening = true
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            onStateChange(false)
                        }
                        override fun onError(error: Int) {
                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "خطای صوتی"
                                SpeechRecognizer.ERROR_CLIENT -> "خطای سرویس گیرنده"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "عدم دسترسی به میکروفون"
                                SpeechRecognizer.ERROR_NETWORK -> "خطای ساختار شبکه"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "اتمام مهلت اتصال شبکه"
                                SpeechRecognizer.ERROR_NO_MATCH -> "عبارتی پیدا نشد"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سیستم تشخیص مشغول است"
                                SpeechRecognizer.ERROR_SERVER -> "خطای سرور"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "اتمام زمان صحبت"
                                else -> "خطای نامشخص"
                            }
                            Log.e("SpeechToTextManager", "STT error code: $error")
                            // Suppress ERROR_NO_MATCH noise in general reporting, but feedback is useful
                            onError(errorMessage)
                            onStateChange(false)
                            isListening = false
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val transcript = matches?.firstOrNull() ?: ""
                            if (transcript.isNotEmpty()) {
                                onResult(transcript)
                            } else {
                                onError("عبارتی تشخیص داده نشد")
                            }
                            isListening = false
                        }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    // This is updated dynamically inside startListening
                }
            } else {
                Log.e("SpeechToTextManager", "Speech recognition not available on context")
            }
        } catch (e: Exception) {
            Log.e("SpeechToTextManager", "STT Initialization failure", e)
        }
    }

    fun startListening(languageCode: String = "fa-IR") {
        if (isListening) return
        if (speechRecognizer != null) {
            try {
                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                    // Prolong speech input silences to avoid premature cutoff!
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 6000L) // 6s min
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L) // 3s complete
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L) // 2.5s partial
                }
                speechRecognizer?.startListening(recognizerIntent)
                isListening = true
                onStateChange(true)
            } catch (e: Exception) {
                onError("خطا در راه‌اندازی ضبط: ${e.message}")
            }
        } else {
            onError("سیستم تشخیص گفتار روی این دستگاه وجود ندارد.")
        }
    }

    fun stopListening() {
        if (!isListening) return
        speechRecognizer?.stopListening()
        isListening = false
        onStateChange(false)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentLanguage = Locale("fa", "IR")

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "TTS Initialization error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(currentLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TextToSpeechManager", "Persian voice packages missing or unsupported. Defaulting.")
                tts?.setLanguage(Locale.US)
            }
            isReady = true
        } else {
            Log.e("TextToSpeechManager", "TTS failed onInit status: $status")
        }
    }

    fun setSpeechLanguage(lang: String) {
        currentLanguage = if (lang == "en") Locale.US else Locale("fa", "IR")
        if (isReady && tts != null) {
            try {
                val result = tts?.setLanguage(currentLanguage)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("TextToSpeechManager", "Language $currentLanguage not supported; falling back")
                    tts?.setLanguage(Locale.US)
                }
            } catch (e: Exception) {
                Log.e("TextToSpeechManager", "Error setting language", e)
            }
        }
    }

    fun speak(text: String) {
        if (!isReady || tts == null) {
            Log.e("TextToSpeechManager", "TTS is not ready or has crashed")
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DailyFlowTTS")
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "TTS speak failure", e)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
