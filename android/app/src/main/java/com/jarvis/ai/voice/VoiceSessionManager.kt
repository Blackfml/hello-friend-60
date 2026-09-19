package com.jarvis.ai.voice

class VoiceSessionManager(private val manager: VoiceManager) {
    fun start() = manager.startListening()
    fun stop() = manager.stopListening()
    fun cancel() = manager.cancel()
}
