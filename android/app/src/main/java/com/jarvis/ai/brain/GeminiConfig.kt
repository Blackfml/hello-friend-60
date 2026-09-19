package com.jarvis.ai.brain

data class GeminiConfig(
    val apiKey: String = "",
    val model: String = "gemini-3.8-flash",
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 2048,
    val systemInstruction: String = DEFAULT_SYSTEM_INSTRUCTION
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
    companion object {
        const val DEFAULT_SYSTEM_INSTRUCTION =
            "Você é JARVIS, um assistente pessoal Android em português do Brasil. " +
            "Seja natural, útil e explicativo. Nunca invente ações ou resultados. " +
            "Quando uma ação do dispositivo for necessária, use as ferramentas disponíveis."
    }
}
