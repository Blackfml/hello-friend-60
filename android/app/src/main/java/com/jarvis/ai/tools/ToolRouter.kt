package com.jarvis.ai.tools

class ToolRouter(private val registry: ToolRegistry) {
    suspend fun execute(name: String, arguments: Map<String, Any?>): ToolResult {
        val tool = registry.get(name) ?: return ToolResult.Failure("Ferramenta não encontrada: $name")
        val missing = tool.definition.requiredParameters.filter { key ->
            val value = arguments[key]
            value == null || (value is String && value.isBlank())
        }
        if (missing.isNotEmpty()) {
            return ToolResult.Failure("Parâmetros ausentes: ${missing.joinToString()}", "MISSING_PARAMETERS")
        }
        return runCatching { tool.execute(arguments) }
            .getOrElse { ToolResult.Failure("Falha ao executar ${tool.definition.name}: ${it.message}", "EXECUTION_ERROR") }
    }
}
