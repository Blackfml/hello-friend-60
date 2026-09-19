package com.jarvis.ai.whatsapp

import com.jarvis.ai.tools.*

class WhatsAppTools(private val bridge: WhatsAppBridge) {
    fun register(registry: ToolRegistry) {
        registry.register(object : JarvisTool {
            override val definition = ToolDefinition(
                "whatsapp_status",
                "Consulta o estado real da conexão local do WhatsApp.",
                riskLevel = RiskLevel.LOW
            )
            override suspend fun execute(arguments: Map<String, Any?>): ToolResult =
                ToolResult.Success("WhatsApp: " + bridge.state.name)
        })

        registry.register(object : JarvisTool {
            override val definition = ToolDefinition(
                "whatsapp_connect",
                "Inicia uma conexão local do WhatsApp quando o usuário pedir explicitamente.",
                parameters = mapOf("phone_number" to "número completo com DDD"),
                requiredParameters = setOf("phone_number"),
                riskLevel = RiskLevel.MEDIUM,
                requiresConfirmation = true
            )
            override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
                val phone = arguments["phone_number"]?.toString().orEmpty()
                return when (val result = bridge.connect(phone)) {
                    is WhatsAppResult.Success -> ToolResult.Success(result.message)
                    is WhatsAppResult.Failure -> ToolResult.Failure(result.message, result.code)
                }
            }
        })

        registry.register(object : JarvisTool {
            override val definition = ToolDefinition(
                "whatsapp_send",
                "Envia uma mensagem pelo WhatsApp através da ponte local conectada.",
                parameters = mapOf(
                    "contact_id" to "identificador do contato",
                    "message" to "texto da mensagem"
                ),
                requiredParameters = setOf("contact_id", "message"),
                riskLevel = RiskLevel.EXTERNAL_COMMUNICATION,
                requiresConfirmation = true
            )
            override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
                if (bridge.state != WhatsAppConnectionState.CONNECTED) {
                    return ToolResult.Failure("WhatsApp não está conectado. Nenhuma mensagem foi enviada.", "NOT_CONNECTED")
                }
                val contact = arguments["contact_id"]?.toString().orEmpty()
                val message = arguments["message"]?.toString().orEmpty()
                if (contact.isBlank() || message.isBlank()) return ToolResult.Failure("Contato e mensagem são obrigatórios.")
                return when (val result = bridge.send(WhatsAppSendRequest(contact, message))) {
                    is WhatsAppResult.Success -> ToolResult.Success(result.message)
                    is WhatsAppResult.Failure -> ToolResult.Failure(result.message, result.code)
                }
            }
        })
    }
}
