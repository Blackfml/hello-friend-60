package com.jarvis.ai.whatsapp

class WhatsAppAgent(
    private val bridge: WhatsAppBridge,
    private val watcher: WhatsAppWatcher
) {
    suspend fun sendAfterConfirmation(request: WhatsAppSendRequest, confirmed: Boolean): WhatsAppResult {
        if (!confirmed) return WhatsAppResult.Failure("Envio recusado pelo usuário.", "USER_REJECTED")
        return bridge.send(request)
    }

    fun onIncoming(message: WhatsAppMessage) {
        watcher.publish(message)
    }
}
