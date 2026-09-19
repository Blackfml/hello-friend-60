package com.jarvis.ai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.brain.*
import com.jarvis.ai.data.SecureApiKeyStore
import com.jarvis.ai.screencontrol.ScreenTaskExecutor
import com.jarvis.ai.voice.VoiceManager
import com.jarvis.ai.voice.VoiceSessionManager
import com.jarvis.ai.voice.VoiceState
import com.jarvis.ai.tools.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String
) { enum class Role { USER, ASSISTANT, TOOL } }

data class AssistantUiState(
    val input: String = "",
    val status: String = "STANDBY",
    val apiConfigured: Boolean = false,
    val busy: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val confirmation: ConfirmationRequest? = null,
    val voiceState: VoiceState = VoiceState.IDLE,
    val voiceError: String? = null
)

class AssistantController private constructor(
    private val appContext: Context
) : ViewModel() {
    private val keyStore = SecureApiKeyStore(appContext)
    private val toolset = AndroidToolset(appContext)
    private val confirmationManager = ConfirmationManager()
    private val screenExecutor = ScreenTaskExecutor()
    private var confirmationDeferred: CompletableDeferred<Boolean>? = null
    private var agentJob: Job? = null
    private val voiceManager = VoiceManager(
        context = appContext,
        onResult = { text -> sendVoicePrompt(text) },
        onState = { voiceState, error ->
            _state.value = _state.value.copy(voiceState = voiceState, voiceError = error)
        }
    )
    private val voiceSession = VoiceSessionManager(voiceManager)

    private val provider = GeminiProvider {
        GeminiConfig(apiKey = keyStore.read().orEmpty())
    }

    private val _state = MutableStateFlow(
        AssistantUiState(apiConfigured = !keyStore.read().isNullOrBlank())
    )
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    fun setInput(value: String) { _state.value = _state.value.copy(input = value) }

    fun saveApiKey(value: String) {
        keyStore.save(value.trim())
        _state.value = _state.value.copy(
            apiConfigured = !keyStore.read().isNullOrBlank(),
            error = null
        )
    }

    fun confirmPendingAction() { confirmationDeferred?.complete(true) }
    fun rejectPendingAction() { confirmationDeferred?.complete(false) }

    fun startVoice() {
        if (!_state.value.apiConfigured) {
            _state.value = _state.value.copy(voiceState = VoiceState.ERROR, voiceError = "Configure a API Key da Gemini antes de usar a voz.")
            return
        }
        _state.value = _state.value.copy(voiceError = null)
        voiceSession.start()
    }

    fun stopVoice() {
        voiceSession.stop()
        _state.value = _state.value.copy(voiceState = VoiceState.IDLE, voiceError = null)
    }

    fun cancelVoice() {
        voiceSession.cancel()
        _state.value = _state.value.copy(voiceState = VoiceState.IDLE)
    }

    private fun sendVoicePrompt(text: String) {
        if (text.isBlank()) return
        _state.value = _state.value.copy(input = text, voiceState = VoiceState.PROCESSING)
        send()
    }

    fun clearApiKey() {
        keyStore.clear()
        _state.value = _state.value.copy(apiConfigured = false, error = null)
    }

    fun cancelTask() {
        screenExecutor.cancel()
        agentJob?.cancel()
        confirmationDeferred?.cancel()
        confirmationDeferred = null
        _state.value = _state.value.copy(busy = false, status = "STANDBY", confirmation = null)
    }

    override fun onCleared() {
        screenExecutor.cancel()
        agentJob?.cancel()
        confirmationDeferred?.cancel()
        voiceManager.shutdown()
        super.onCleared()
    }

    fun send() {
        val prompt = _state.value.input.trim()
        if (prompt.isBlank() || _state.value.busy || !_state.value.apiConfigured) return

        screenExecutor.reset()
        _state.value = _state.value.copy(
            input = "",
            busy = true,
            status = "THINKING",
            error = null,
            messages = _state.value.messages + ChatMessage(role = ChatMessage.Role.USER, text = prompt)
        )

        agentJob = viewModelScope.launch {
            val result = runAgent(prompt)
            result.onSuccess { answer ->
                if (!screenExecutor.isCancelled()) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = "STANDBY",
                        messages = _state.value.messages + ChatMessage(role = ChatMessage.Role.ASSISTANT, text = answer)
                    )
                    voiceManager.speak(answer)
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException || screenExecutor.isCancelled()) {
                    _state.value = _state.value.copy(busy = false, status = "STANDBY", confirmation = null)
                } else {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = "ERROR",
                        error = error.message ?: "Não foi possível concluir a tarefa."
                    )
                }
            }
            agentJob = null
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
        if (history.none { it.role == BrainMessage.Role.USER && it.content == prompt }) {
            history += BrainMessage(BrainMessage.Role.USER, prompt)
        }

        val tools = toolset.registry.definitions().map(BrainToolMapper::toBrainDefinition)

        for (step in 1..MAX_STEPS) {
            if (screenExecutor.isCancelled()) {
                return Result.failure(kotlinx.coroutines.CancellationException("Tarefa cancelada pelo usuário."))
            }

            _state.value = _state.value.copy(
                status = if (step == 1) "THINKING" else "THINKING • ETAPA $step/$MAX_STEPS"
            )

            when (val response = provider.generate(
                BrainRequest(
                    messages = history.toList(),
                    model = GeminiConfig().model,
                    maxOutputTokens = 2048,
                    tools = tools
                )
            )) {
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

                    response.toolCalls.forEach { call ->
                        if (screenExecutor.isCancelled()) {
                            return Result.failure(kotlinx.coroutines.CancellationException("Tarefa cancelada pelo usuário."))
                        }

                        val tool = toolset.registry.get(call.name)
                            ?: return Result.failure(Exception("Ferramenta não encontrada: \${call.name}"))

                        if (confirmationManager.requiresConfirmation(tool)) {
                            val request = confirmationManager.createRequest(tool, call.arguments)
                            val deferred = CompletableDeferred<Boolean>()
                            confirmationDeferred = deferred
                            _state.value = _state.value.copy(
                                confirmation = request,
                                status = "WAITING_CONFIRMATION"
                            )
                            val approved = deferred.await()
                            confirmationDeferred = null
                            _state.value = _state.value.copy(confirmation = null, status = "EXECUTING")
                            if (!approved) {
                                history += BrainMessage(
                                    role = BrainMessage.Role.TOOL,
                                    content = "Ação recusada pelo usuário.",
                                    toolResponse = BrainToolResponse(call.name, "Ação recusada pelo usuário.")
                                )
                                return Result.success("Tudo bem. Não executei essa ação.")
                            }
                        }

                        val before = if (isScreenAction(call.name)) {
                            _state.value = _state.value.copy(status = "OBSERVING")
                            screenExecutor.observe()
                        } else null

                        _state.value = _state.value.copy(status = "EXECUTING")
                        val result = toolset.router.execute(call.name, call.arguments)

                        history += BrainMessage(
                            role = BrainMessage.Role.MODEL,
                            content = "",
                            toolCall = call
                        )

                        if (result is ToolResult.Success && isScreenAction(call.name)) {
                            _state.value = _state.value.copy(status = "VERIFYING")
                            screenExecutor.waitForUiChange()
                            val verification = screenExecutor.verifyChange(before)
                            if (!verification.changed && requiresScreenChange(call.name)) {
                                val retryMessage = "A ação foi executada, mas não consegui verificar uma mudança na tela. Não assuma sucesso; observe a tela novamente e tente uma estratégia diferente se necessário."
                                history += BrainMessage(
                                    role = BrainMessage.Role.TOOL,
                                    content = retryMessage,
                                    toolResponse = BrainToolResponse(call.name, retryMessage)
                                )
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + ChatMessage(
                                        role = ChatMessage.Role.TOOL,
                                        text = "Verificação: nenhuma mudança detectada após \${tool.definition.name}."
                                    )
                                )
                                continue
                            }
                        }

                        val resultText = when (result) {
                            is ToolResult.Success -> result.message
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
                                text = "Ferramenta \${tool.definition.name}: $resultText"
                            )
                        )

                        if (result is ToolResult.Failure) {
                            history += BrainMessage(
                                role = BrainMessage.Role.TOOL,
                                content = "A ferramenta falhou. Analise o erro e decida se deve tentar uma estratégia diferente.",
                                toolResponse = BrainToolResponse(call.name, result.message)
                            )
                        }
                    }
                }
            }
        }

        return Result.failure(Exception("A tarefa atingiu o limite de $MAX_STEPS etapas sem uma conclusão segura."))
    }

    private fun isScreenAction(name: String): Boolean =
        name in setOf("ler_tela", "tocar_a_tela", "digitar_a_tela", "rolar_a_tela", "botao_do_sistema")

    private fun requiresScreenChange(name: String): Boolean =
        name in setOf("tocar_a_tela", "digitar_a_tela", "rolar_a_tela", "botao_do_sistema")

    companion object {
        private const val MAX_STEPS = 40

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AssistantController(context.applicationContext) as T
            }
    }
}
