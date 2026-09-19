package com.jarvis.ai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.brain.BrainMessage
import com.jarvis.ai.brain.BrainRequest
import com.jarvis.ai.brain.BrainResponse
import com.jarvis.ai.brain.GeminiConfig
import com.jarvis.ai.brain.GeminiProvider
import com.jarvis.ai.brain.BrainToolResponse
import com.jarvis.ai.data.SecureApiKeyStore
import com.jarvis.ai.tools.AndroidToolset
import com.jarvis.ai.tools.BrainToolMapper
import com.jarvis.ai.tools.toUserMessage
import com.jarvis.ai.tools.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String
) {
    enum class Role { USER, ASSISTANT, TOOL }
}

data class AssistantUiState(
    val input: String = "",
    val status: String = "STANDBY",
    val apiConfigured: Boolean = false,
    val busy: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null
)

class AssistantController private constructor(
    private val appContext: Context
) : ViewModel() {

    private val keyStore = SecureApiKeyStore(appContext)
    private val toolset = AndroidToolset(appContext)
    private val provider = GeminiProvider {
        GeminiConfig(apiKey = keyStore.read().orEmpty())
    }

    private val _state = MutableStateFlow(
        AssistantUiState(apiConfigured = !keyStore.read().isNullOrBlank())
    )
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    fun setInput(value: String) {
        _state.value = _state.value.copy(input = value)
    }

    fun saveApiKey(value: String) {
        keyStore.save(value.trim())
        _state.value = _state.value.copy(
            apiConfigured = !keyStore.read().isNullOrBlank(),
            error = null
        )
    }

    fun clearApiKey() {
        keyStore.clear()
        _state.value = _state.value.copy(apiConfigured = false, error = null)
    }

    fun send() {
        val prompt = _state.value.input.trim()
        if (prompt.isBlank() || _state.value.busy || !_state.value.apiConfigured) return

        _state.value = _state.value.copy(
            input = "",
            busy = true,
            status = "THINKING",
            error = null,
            messages = _state.value.messages + ChatMessage(
                role = ChatMessage.Role.USER,
                text = prompt
            )
        )

        viewModelScope.launch {
            val result = runAgent(prompt)
            result.onSuccess { answer ->
                _state.value = _state.value.copy(
                    busy = false,
                    status = "STANDBY",
                    messages = _state.value.messages + ChatMessage(
                        role = ChatMessage.Role.ASSISTANT,
                        text = answer
                    )
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    busy = false,
                    status = "ERROR",
                    error = error.message ?: "Não foi possível concluir a tarefa."
                )
            }
        }
    }

    private suspend fun runAgent(prompt: String): Result<String> {
        val history = mutableListOf<BrainMessage>()
        _state.value.messages.forEach { message ->
            if (message.text.isBlank()) return@forEach
            history += BrainMessage(
                role = when (message.role) {
                    ChatMessage.Role.USER -> BrainMessage.Role.USER
                    ChatMessage.Role.ASSISTANT -> BrainMessage.Role.MODEL
                    ChatMessage.Role.TOOL -> BrainMessage.Role.TOOL
                },
                content = message.text
            )
        }
        history += BrainMessage(BrainMessage.Role.USER, prompt)

        val tools = toolset.registry.definitions().map(BrainToolMapper::toBrainDefinition)

        repeat(MAX_STEPS) {
            val response = provider.generate(
                BrainRequest(
                    messages = history.toList(),
                    model = GeminiConfig().model,
                    maxOutputTokens = 2048,
                    tools = tools
                )
            )

            when (response) {
                is BrainResponse.Failure ->
                    return Result.failure(Exception(response.error.toUserMessage()))

                is BrainResponse.Success -> {
                    if (response.toolCalls.isEmpty()) {
                        return Result.success(
                            response.text.ifBlank {
                                "Concluí a tarefa, mas não recebi uma resposta textual do Gemini."
                            }
                        )
                    }

                    _state.value = _state.value.copy(status = "EXECUTING")

                    response.toolCalls.forEach { call ->
                        val tool = toolset.registry.get(call.name)
                            ?: return Result.failure(
                                Exception("Ferramenta não encontrada: " + call.name)
                            )

                        val result = toolset.router.execute(call.name, call.arguments)

                        history += BrainMessage(
                            role = BrainMessage.Role.MODEL,
                            content = "",
                            toolCall = call
                        )

                        val resultText = when (result) {
                            is ToolResult.Success -> {
                                _state.value = _state.value.copy(status = "VERIFYING")
                                result.message
                            }
                            is ToolResult.Failure -> result.message
                        }

                        history += BrainMessage(
                            role = BrainMessage.Role.TOOL,
                            content = resultText,
                            toolResponse = BrainToolResponse(call.name, resultText)
                        )

                        _state.value = _state.value.copy(
                            messages = _state.value.messages + ChatMessage(
                                role = ChatMessage.Role.TOOL,
                                text = "Ferramenta " + tool.definition.name + ": " + resultText
                            )
                        )

                        if (result is ToolResult.Failure) {
                            return Result.failure(Exception(result.message))
                        }
                    }
                }
            }
        }

        return Result.failure(
            Exception("A tarefa atingiu o limite de etapas sem uma conclusão segura.")
        )
    }

    companion object {
        private const val MAX_STEPS = 10

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AssistantController(context.applicationContext) as T
                }
            }
    }
}
