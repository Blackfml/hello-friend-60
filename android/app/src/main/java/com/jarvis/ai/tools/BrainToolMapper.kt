package com.jarvis.ai.tools

import com.jarvis.ai.brain.BrainError
import com.jarvis.ai.brain.BrainToolDefinition

object BrainToolMapper {
    fun toBrainDefinition(tool: ToolDefinition): BrainToolDefinition =
        BrainToolDefinition(
            name = tool.name,
            description = tool.description,
            parameters = tool.parameters,
            requiredParameters = tool.requiredParameters
        )

    fun BrainError.toUserMessage(): String = when (this) {
        BrainError.UNAUTHORIZED -> "A chave da API Gemini não está configurada ou não é válida."
        BrainError.FORBIDDEN -> "A API Gemini recusou o acesso. Verifique a chave e o projeto."
        BrainError.RATE_LIMITED -> "O limite da API Gemini foi atingido. Tente novamente em instantes."
        BrainError.NETWORK -> "Não consegui conectar ao Gemini. Verifique sua internet."
        BrainError.TIMEOUT -> "O Gemini demorou demais para responder."
        BrainError.CONTEXT_LIMIT -> "A conversa ficou grande demais para o limite atual."
        BrainError.INVALID_RESPONSE -> "O Gemini retornou uma resposta inválida."
        BrainError.UNKNOWN -> "Ocorreu um erro inesperado ao falar com o Gemini."
    }
}
