package com.jarvis.ai.tools

data class ConfirmationRequest(
    val toolName: String,
    val title: String,
    val description: String,
    val arguments: Map<String, Any?> = emptyMap()
)

class ConfirmationManager {
    fun requiresConfirmation(tool: JarvisTool): Boolean =
        tool.definition.requiresConfirmation ||
            tool.definition.riskLevel == RiskLevel.HIGH ||
            tool.definition.riskLevel == RiskLevel.DESTRUCTIVE ||
            tool.definition.riskLevel == RiskLevel.EXTERNAL_COMMUNICATION

    fun createRequest(tool: JarvisTool, arguments: Map<String, Any?>): ConfirmationRequest =
        ConfirmationRequest(
            toolName = tool.definition.name,
            title = "Confirmação necessária",
            description = "O JARVIS precisa executar uma ação que pode ter efeitos externos. " +
                "Revise os detalhes antes de continuar.",
            arguments = arguments
        )
}
