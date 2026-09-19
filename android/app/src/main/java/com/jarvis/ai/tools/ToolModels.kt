package com.jarvis.ai.tools

enum class RiskLevel { LOW, MEDIUM, HIGH, DESTRUCTIVE, EXTERNAL_COMMUNICATION }

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
    val requiredParameters: Set<String> = emptySet(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresConfirmation: Boolean = false
)
sealed interface ToolResult {
    data class Success(val message: String, val data: Map<String, Any?> = emptyMap()) : ToolResult
    data class Failure(val message: String, val code: String? = null) : ToolResult
}
interface JarvisTool {
    val definition: ToolDefinition
    suspend fun execute(arguments: Map<String, Any?>): ToolResult
}
