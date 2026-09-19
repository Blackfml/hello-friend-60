package com.jarvis.ai.core
enum class JarvisStatus { STANDBY, LISTENING, THINKING, SPEAKING, CONTROLLING, ERROR }
data class JarvisState(
    val status: JarvisStatus = JarvisStatus.STANDBY,
    val isOnline: Boolean = false,
    val activeTaskId: String? = null,
    val controlEnabled: Boolean = false,
    val lastError: String? = null
)
