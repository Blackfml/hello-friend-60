package com.jarvis.ai.data

import android.content.Context
import com.jarvis.ai.brain.GeminiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class JarvisSettingsRepository(context: Context) {
    private val store = GeminiSettingsStore(context)
    private val secure = SecureSecretStore(context)

    val config: Flow<GeminiConfig> = store.config.map { config ->
        config.copy(apiKey = secure.get(API_KEY).orEmpty())
    }

    suspend fun currentConfig(): GeminiConfig = config.first()

    suspend fun save(config: GeminiConfig) {
        secure.put(API_KEY, config.apiKey)
        store.save(config.copy(apiKey = ""))
    }

    fun clearApiKey() {
        secure.remove(API_KEY)
    }

    companion object {
        private const val API_KEY = "gemini_api_key"
    }
}
