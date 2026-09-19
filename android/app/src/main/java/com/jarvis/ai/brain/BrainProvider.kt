package com.jarvis.ai.brain

interface BrainProvider {
    suspend fun generate(request: BrainRequest): BrainResponse
}

data class BrainRequest(
    val messages: List<BrainMessage>,
    val model: String,
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 2048,
    val tools: List<BrainToolDefinition> = emptyList()
)

data class BrainMessage(
    val role: Role,
    val content: String,
    val toolCall: BrainToolCall? = null,
    val toolResponse: BrainToolResponse? = null
) {
    enum class Role { USER, MODEL, SYSTEM, TOOL }
}

data class BrainToolCall(
    val name: String,
    val arguments: Map<String, Any?> = emptyMap()
)

data class BrainToolResponse(
    val name: String,
    val result: String
)

data class BrainToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String>,
    val requiredParameters: Set<String>
)

sealed interface BrainResponse {
    data class Success(
        val text: String,
        val toolCalls: List<BrainToolCall> = emptyList()
    ) : BrainResponse
    data class Failure(val error: BrainError) : BrainResponse
}

enum class BrainError {
    UNAUTHORIZED, FORBIDDEN, RATE_LIMITED, NETWORK, TIMEOUT,
    INVALID_RESPONSE, CONTEXT_LIMIT, UNKNOWN
}
