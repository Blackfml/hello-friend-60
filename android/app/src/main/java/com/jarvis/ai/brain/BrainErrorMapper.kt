package com.jarvis.ai.brain

object BrainErrorMapper {
    fun userMessage(error: BrainError): String = when (error) {
        BrainError.UNAUTHORIZED -> "A chave da API do Gemini não foi configurada ou não foi aceita."
        BrainError.FORBIDDEN -> "O Gemini recusou o acesso solicitado."
        BrainError.RATE_LIMITED -> "O limite de requisições do Gemini foi atingido. Tente novamente mais tarde."
        BrainError.NETWORK -> "Não consegui conectar ao Gemini. Verifique sua internet."
        BrainError.TIMEOUT -> "O Gemini demorou demais para responder."
        BrainError.INVALID_RESPONSE -> "O Gemini retornou uma resposta que o Jarvis não conseguiu interpretar."
        BrainError.CONTEXT_LIMIT -> "A conversa ficou grande demais para este modelo."
        BrainError.UNKNOWN -> "Ocorreu um erro inesperado ao falar com o Gemini."
    }
}
