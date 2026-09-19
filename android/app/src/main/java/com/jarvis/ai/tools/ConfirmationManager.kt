package com.jarvis.ai.tools

class ConfirmationManager {
    fun requiresConfirmation(tool: JarvisTool): Boolean =
        tool.definition.requiresConfirmation ||
            tool.definition.riskLevel == RiskLevel.HIGH ||
            tool.definition.riskLevel == RiskLevel.DESTRUCTIVE ||
            tool.definition.riskLevel == RiskLevel.EXTERNAL_COMMUNICATION
}
