package com.jarvis.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceManager(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onState: (VoiceState, String?) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
            SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(this) }
        } else null
    private val tts = TextToSpeech(appContext, this)
    private var ttsReady = false
    private var destroyed = false

    fun startListening() {
        if (destroyed) return
        val engine = recognizer ?: run {
            onState(VoiceState.ERROR, "Reconhecimento de voz indisponível neste aparelho.")
            return
        }
        if (tts.isSpeaking) tts.stop()
        onState(VoiceState.LISTENING, null)
        engine.cancel()
        engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    fun stopListening() {
        recognizer?.stopListening()
        onState(VoiceState.IDLE, null)
    }

    fun cancel() {
        recognizer?.cancel()
        if (tts.isSpeaking) tts.stop()
        onState(VoiceState.IDLE, null)
    }

    fun speak(text: String) {
        if (destroyed || !ttsReady || text.isBlank()) return
        recognizer?.cancel()
        onState(VoiceState.SPEAKING, null)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "jarvis-" + System.currentTimeMillis())
    }

    fun shutdown() {
        destroyed = true
        recognizer?.destroy()
        tts.stop()
        tts.shutdown()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale("pt", "BR")
            tts.setSpeechRate(1.0f)
            tts.setPitch(1.0f)
        } else onState(VoiceState.ERROR, "Síntese de voz indisponível.")
    }

    override fun onReadyForSpeech(params: Bundle?) { onState(VoiceState.LISTENING, null) }
    override fun onBeginningOfSpeech() { onState(VoiceState.LISTENING, null) }
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { onState(VoiceState.PROCESSING, null) }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Não consegui acessar o microfone."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "A conexão de voz falhou."
            SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi o que foi dito."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O reconhecimento de voz está ocupado."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone não concedida."
            else -> "Não foi possível reconhecer sua voz."
        }
        onState(VoiceState.ERROR, message)
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()?.trim().orEmpty()
        if (text.isBlank()) onState(VoiceState.ERROR, "Não consegui entender sua fala.")
        else {
            onState(VoiceState.PROCESSING, null)
            onResult(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
