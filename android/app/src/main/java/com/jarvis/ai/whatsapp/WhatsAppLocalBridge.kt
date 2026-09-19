package com.jarvis.ai.whatsapp

import android.content.Context

class WhatsAppLocalBridge(private val context: Context) : WhatsAppBridge {
    @Volatile private var connectionState = WhatsAppConnectionState.DISCONNECTED

    override val state: WhatsAppConnectionState
        get() = connectionState

    override suspend fun connect(phoneNumber: String): WhatsAppResult {
        val normalized = phoneNumber.filter(Char::isDigit)
        if (normalized.length < 10) return WhatsAppResult.Failure("Informe um número completo com DDD.", "INVALID_PHONE")
        connectionState = WhatsAppConnectionState.ERROR
        return WhatsAppResult.Failure(
            "A ponte local do WhatsApp ainda não está instalada/configurada neste APK. " +
                "Não vou fingir que a conexão foi realizada.",
            "BRIDGE_NOT_CONFIGURED"
        )
    }

    override suspend fun disconnect() {
        connectionState = WhatsAppConnectionState.DISCONNECTED
    }

    override suspend fun contacts(query: String?): List<WhatsAppContact> = emptyList()

    override suspend fun send(request: WhatsAppSendRequest): WhatsAppResult =
        WhatsAppResult.Failure(
            "O envio do WhatsApp exige uma ponte local conectada. Nenhuma mensagem foi enviada.",
            "BRIDGE_NOT_CONFIGURED"
        )
}
