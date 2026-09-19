package com.jarvis.ai.whatsapp

enum class WhatsAppConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class WhatsAppContact(
    val id: String,
    val displayName: String,
    val phone: String? = null
)

data class WhatsAppMessage(
    val id: String,
    val chatId: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val outgoing: Boolean
)

data class WhatsAppSendRequest(
    val contactId: String,
    val message: String
)

sealed interface WhatsAppResult {
    data class Success(val message: String) : WhatsAppResult
    data class Failure(val message: String, val code: String? = null) : WhatsAppResult
}
