package com.jarvis.ai.brain

interface BrainProvider {
    suspend fun generate(request: BrainRequest): BrainResponse
}
data class BrainRequest(
    val messages: List<BrainMessage>,
    val model: String,
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 2048
)
data class BrainMessage(val role: Role, val content: String) {
    enum class Role { USER, MODEL, SYSTEM }
}
sealed interface BrainResponse {
    data class Success(val text: String) : BrainResponse
    data class Failure(val error: BrainError) : BrainResponse
}
enum class BrainError {
    UNAUTHORIZED, FORBIDDEN, RATE_LIMITED, NETWORK, TIMEOUT,
    INVALID_RESPONSE, CONTEXT_LIMIT, UNKNOWN
}
