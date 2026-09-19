package com.jarvis.ai.whatsapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WhatsAppWatcher {
    private val _messages = MutableStateFlow<List<WhatsAppMessage>>(emptyList())
    val messages: StateFlow<List<WhatsAppMessage>> = _messages.asStateFlow()

    fun publish(message: WhatsAppMessage) {
        _messages.value = (_messages.value + message).takeLast(100)
    }

    fun clear() {
        _messages.value = emptyList()
    }
}
