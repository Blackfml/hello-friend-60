package com.jarvis.ai.whatsapp

interface WhatsAppBridge {
    val state: WhatsAppConnectionState
    suspend fun connect(phoneNumber: String): WhatsAppResult
    suspend fun disconnect()
    suspend fun contacts(query: String? = null): List<WhatsAppContact>
    suspend fun send(request: WhatsAppSendRequest): WhatsAppResult
}
