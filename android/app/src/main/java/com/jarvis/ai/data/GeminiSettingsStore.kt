package com.jarvis.ai.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.ai.brain.GeminiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore by preferencesDataStore("jarvis_settings")

class GeminiSettingsStore(private val context: Context) {
    private object Keys {
        val apiKey = stringPreferencesKey("gemini_api_key")
        val model = stringPreferencesKey("gemini_model")
        val temperature = doublePreferencesKey("gemini_temperature")
        val maxOutputTokens = intPreferencesKey("gemini_max_output_tokens")
        val systemInstruction = stringPreferencesKey("gemini_system_instruction")
    }

    val config: Flow<GeminiConfig> = context.jarvisDataStore.data.map { p ->
        GeminiConfig(
            apiKey = p[Keys.apiKey].orEmpty(),
            model = p[Keys.model] ?: "gemini-3.8-flash",
            temperature = p[Keys.temperature] ?: 0.7,
            maxOutputTokens = p[Keys.maxOutputTokens] ?: 2048,
            systemInstruction = p[Keys.systemInstruction] ?: GeminiConfig.DEFAULT_SYSTEM_INSTRUCTION
        )
    }

    suspend fun save(config: GeminiConfig) {
        context.jarvisDataStore.edit { p ->
            p[Keys.apiKey] = config.apiKey
            p[Keys.model] = config.model
            p[Keys.temperature] = config.temperature
            p[Keys.maxOutputTokens] = config.maxOutputTokens
            p[Keys.systemInstruction] = config.systemInstruction
        }
    }

    suspend fun clearApiKey() {
        context.jarvisDataStore.edit { it.remove(Keys.apiKey) }
    }
}
