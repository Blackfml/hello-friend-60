package com.jarvis.ai.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisMemoryDataStore by preferencesDataStore("jarvis_memory")

class JarvisMemoryStore(private val context: Context) {
    private object Keys { val memories = stringPreferencesKey("memories") }

    val memories: Flow<List<String>> = context.jarvisMemoryDataStore.data.map { prefs ->
        prefs[Keys.memories].orEmpty().split("\n").map(String::trim).filter(String::isNotBlank).takeLast(100)
    }

    suspend fun add(memory: String) {
        val value = memory.trim()
        if (value.isBlank()) return
        context.jarvisMemoryDataStore.edit { prefs ->
            val current = prefs[Keys.memories].orEmpty().split("\n").filter(String::isNotBlank)
            prefs[Keys.memories] = (current + value).takeLast(100).joinToString("\n")
        }
    }

    suspend fun remove(memory: String) {
        context.jarvisMemoryDataStore.edit { prefs ->
            prefs[Keys.memories] = prefs[Keys.memories].orEmpty().split("\n").filter { it.isNotBlank() && it != memory }.joinToString("\n")
        }
    }

    suspend fun clear() { context.jarvisMemoryDataStore.edit { it.remove(Keys.memories) } }
}