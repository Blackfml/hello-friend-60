package com.jarvis.ai.whatsapp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppAgentTest {
    @Test
    fun rejectedSendNeverCallsBridge() = runTest {
        var called = false
        val bridge = object : WhatsAppBridge {
            override val state = WhatsAppConnectionState.CONNECTED
            override suspend fun connect(phoneNumber: String) = WhatsAppResult.Success("connected")
            override suspend fun disconnect() = Unit
            override suspend fun contacts(query: String?) = emptyList<WhatsAppContact>()
            override suspend fun send(request: WhatsAppSendRequest): WhatsAppResult {
                called = true
                return WhatsAppResult.Success("sent")
            }
        }
        val agent = WhatsAppAgent(bridge, WhatsAppWatcher())
        val result = agent.sendAfterConfirmation(WhatsAppSendRequest("1", "teste"), false)
        assertEquals(false, called)
        assertEquals(WhatsAppResult.Failure("Envio recusado pelo usuário.", "USER_REJECTED"), result)
    }
}
