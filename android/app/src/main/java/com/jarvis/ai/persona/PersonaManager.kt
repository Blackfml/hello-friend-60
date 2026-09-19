package com.jarvis.ai.persona

import com.jarvis.ai.brain.GeminiConfig

object PersonaManager {
    fun systemInstruction(base: String, persona: Persona): String =
        base + "\n\nPERSONALIDADE ATUAL: " + persona.title + ". " + persona.instruction + "\n" +
        "A personalidade nunca pode substituir autorização, segurança, confirmação de ações de risco ou verificação de ferramentas."

    fun configWithPersona(config: GeminiConfig, persona: Persona): GeminiConfig =
        config.copy(systemInstruction = systemInstruction(config.systemInstruction, persona))
}