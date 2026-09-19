package com.jarvis.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.ai.persona.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisPreferences by preferencesDataStore("jarvis_preferences")

data class JarvisPreferences(
    val persona: Persona = Persona.EQUILIBRADO,
    val voiceEnabled: Boolean = true,
    val autoSpeakTypedMessages: Boolean = false,
    val memoryEnabled: Boolean = true
)

class JarvisPreferencesStore(private val context: Context) {
    private object Keys {
        val persona = stringPreferencesKey("persona")
        val voiceEnabled = booleanPreferencesKey("voice_enabled")
        val autoSpeakTyped = booleanPreferencesKey("auto_speak_typed")
        val memoryEnabled = booleanPreferencesKey("memory_enabled")
    }

    val preferences: Flow<JarvisPreferences> = context.jarvisPreferences.data.map { p ->
        JarvisPreferences(
            persona = runCatching { Persona.valueOf(p[Keys.persona] ?: Persona.EQUILIBRADO.name) }.getOrDefault(Persona.EQUILIBRADO),
            voiceEnabled = p[Keys.voiceEnabled] ?: true,
            autoSpeakTypedMessages = p[Keys.autoSpeakTyped] ?: false,
            memoryEnabled = p[Keys.memoryEnabled] ?: true
        )
    }

    suspend fun update(transform: (JarvisPreferences) -> JarvisPreferences) {
        context.jarvisPreferences.edit { p ->
            val current = JarvisPreferences(
                persona = runCatching { Persona.valueOf(p[Keys.persona] ?: Persona.EQUILIBRADO.name) }.getOrDefault(Persona.EQUILIBRADO),
                voiceEnabled = p[Keys.voiceEnabled] ?: true,
                autoSpeakTypedMessages = p[Keys.autoSpeakTyped] ?: false,
                memoryEnabled = p[Keys.memoryEnabled] ?: true
            )
            val next = transform(current)
            p[Keys.persona] = next.persona.name
            p[Keys.voiceEnabled] = next.voiceEnabled
            p[Keys.autoSpeakTyped] = next.autoSpeakTypedMessages
            p[Keys.memoryEnabled] = next.memoryEnabled
        }
    }
}