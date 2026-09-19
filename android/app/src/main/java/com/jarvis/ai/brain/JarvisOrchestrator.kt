package com.jarvis.ai.brain

import com.jarvis.ai.tools.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class JarvisOrchestrator(
    private val brainProvider: BrainProvider,
    private val toolRegistry: ToolRegistry,
    private val configProvider: () -> GeminiConfig
) {
    private val _state = MutableStateFlow(OrchestratorState())
    val state: StateFlow<OrchestratorState> = _state.asStateFlow()
    private val history = mutableListOf<BrainMessage>()

    suspend fun ask(text: String): BrainResponse {
        val taskId = UUID.randomUUID().toString()
        _state.value = OrchestratorState(taskId, true, "Analisando...")
        history += BrainMessage(BrainMessage.Role.USER, text)

        val config = configProvider()
        val response = brainProvider.generate(
            BrainRequest(
                messages = history.takeLast(30),
                model = config.model,
                temperature = config.temperature,
                maxOutputTokens = config.maxOutputTokens
            )
        )

        if (response is BrainResponse.Success) {
            history += BrainMessage(BrainMessage.Role.MODEL, response.text)
            _state.value = OrchestratorState(taskId, false, "Concluído")
        } else {
            _state.value = OrchestratorState(taskId, false, "Erro")
        }
        return response
    }

    fun clearHistory() = history.clear()
    fun availableTools() = toolRegistry.definitions()
}

data class OrchestratorState(
    val taskId: String? = null,
    val processing: Boolean = false,
    val status: String = "Pronto"
)
