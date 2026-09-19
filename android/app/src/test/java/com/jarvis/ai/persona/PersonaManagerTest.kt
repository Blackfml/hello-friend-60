package com.jarvis.ai.persona

import com.jarvis.ai.brain.GeminiConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaManagerTest {
    @Test
    fun allRequestedPersonasExist() {
        assertTrue(Persona.entries.size >= 14)
        assertTrue(Persona.entries.any { it == Persona.ESTRATEGISTA })
    }

    @Test
    fun personaCannotRemoveSecurityInstruction() {
        val config = PersonaManager.configWithPersona(GeminiConfig(), Persona.SARCÁSTICO)
        assertTrue(config.systemInstruction.contains("nunca pode substituir autorização"))
    }
}
