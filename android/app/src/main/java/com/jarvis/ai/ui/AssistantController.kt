package com.jarvis.ai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.brain.*
import com.jarvis.ai.data.SecureApiKeyStore
import com.jarvis.ai.data.JarvisMemoryStore
import com.jarvis.ai.data.JarvisPreferences
import com.jarvis.ai.data.JarvisPreferencesStore
import com.jarvis.ai.data.JarvisSettingsRepository
import com.jarvis.ai.persona.Persona
import com.jarvis.ai.persona.PersonaManager
import com.jarvis.ai.media.Attachment
import com.jarvis.ai.media.AttachmentManager
import com.jarvis.ai.media.AttachmentPolicy
import com.jarvis.ai.media.BitmapMediaEncoder
import com.jarvis.ai.media.GeminiMediaEncoder
import com.jarvis.ai.screencontrol.ScreenshotCaptureManager
import com.jarvis.ai.screencontrol.ScreenTaskExecutor
import com.jarvis.ai.tools.*
import com.jarvis.ai.voice.VoiceManager
import com.jarvis.ai.voice.VoiceSessionManager
import com.jarvis.ai.voice.VoiceState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val attachments: List<Attachment> = emptyList(),
    val error: String? = null,
    val confirmation: ConfirmationRequest? = null,
    val voiceState: VoiceState = VoiceState.IDLE,
    val voiceError: String? = null,
    val model: String = "gemini-3.8-flash",
    val temperature: Double = 0.7,
    val persona: Persona = Persona.EQUILIBRADO,
    val memoryEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val autoSpeakTypedMessages: Boolean = false
)

class AssistantController private constructor(private val appContext: Context) : ViewModel() {
    private val keyStore = SecureApiKeyStore(appContext)
    private val settingsRepository = JarvisSettingsRepository(appContext)
    private val preferencesStore = JarvisPreferencesStore(appContext)
    private val memoryStore = JarvisMemoryStore(appContext)
    @Volatile private var preferences = JarvisPreferences()
    private val toolset = AndroidToolset(appContext)
    private val confirmationManager = ConfirmationManager()
    private val screenExecutor = ScreenTaskExecutor()
    private val attachmentManager = AttachmentManager(appContext.contentResolver)
    private val mediaEncoder = GeminiMediaEncoder(appContext.contentResolver)
    private val screenshotManager = ScreenshotCaptureManager(appContext)
    private var pendingMedia = emptyList<BrainMediaPart>()
    private var confirmationDeferred: CompletableDeferred<Boolean>? = null
    private var agentJob: Job? = null

    private val provider = GeminiProvider {
        val base = runCatching { kotlinx.coroutines.runBlocking { settingsRepository.currentConfig() } }
            .getOrElse { GeminiConfig(apiKey = keyStore.read().orEmpty()) }
        val memoryText = if (preferences.memoryEnabled) {
            runCatching { kotlinx.coroutines.runBlocking { memoryStore.memories.first() } }.getOrDefault(emptyList())
                .takeLast(20)
                .joinToString("\n") { "- " + it }
        } else ""
        val withPersona = PersonaManager.configWithPersona(base, preferences.persona)
        withPersona.copy(
            systemInstruction = withPersona.systemInstruction +
                if (memoryText.isBlank()) "" else "\n\nMEMÓRIAS AUTORIZADAS PELO USUÁRIO:\n" + memoryText
        )
    }

    private val _state = MutableStateFlow(
        AssistantUiState(apiConfigured = !keyStore.read().isNullOrBlank())
    )

    init {
        viewModelScope.launch {
            preferencesStore.preferences.collect { p ->
                preferences = p
                _state.value = _state.value.copy(
                    persona = p.persona,
                    memoryEnabled = p.memoryEnabled,
                    voiceEnabled = p.voiceEnabled,
                    autoSpeakTypedMessages = p.autoSpeakTypedMessages
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.config.collect { config ->
                _state.value = _state.value.copy(model = config.model, temperature = config.temperature)
            }
        }
    }
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    private val voiceManager = VoiceManager(
        context = appContext,
        onResult = { text -> sendVoicePrompt(text) },
        onState = { voiceState, error ->
            _state.value = _state.value.copy(voiceState = voiceState, voiceError = error)
        }
    )
    private val voiceSession = VoiceSessionManager(voiceManager)

    fun setInput(value: String) { _state.value = _state.value.copy(input = value) }

    fun saveModel(model: String) {
        viewModelScope.launch {
            val current = settingsRepository.currentConfig()
            settingsRepository.save(current.copy(model = model.trim().ifBlank { current.model }))
        }
    }

    fun saveTemperature(value: Double) {
        viewModelScope.launch {
            val current = settingsRepository.currentConfig()
            settingsRepository.save(current.copy(temperature = value.coerceIn(0.0, 2.0)))
        }
    }

    fun setPersona(persona: Persona) {
        viewModelScope.launch { preferencesStore.update { it.copy(persona = persona) } }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.update { it.copy(memoryEnabled = enabled) } }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.update { it.copy(voiceEnabled = enabled) } }
    }

    fun setAutoSpeakTypedMessages(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.update { it.copy(autoSpeakTypedMessages = enabled) } }
    }

    fun addMemory(text: String) {
        viewModelScope.launch { if (preferences.memoryEnabled) memoryStore.add(text) }
    }

    fun clearMemory() { viewModelScope.launch { memoryStore.clear() } }


    fun saveApiKey(value: String) {
        keyStore.save(value.trim())
        _state.value = _state.value.copy(apiConfigured = !keyStore.read().isNullOrBlank(), error = null)
    }

    fun confirmPendingAction() { confirmationDeferred?.complete(true) }
    fun rejectPendingAction() { confirmationDeferred?.complete(false) }

    fun attachUri(uri: Uri) {
        val attachment = attachmentManager.inspect(uri)
        if (attachment == null) {
            _state.value = _state.value.copy(error = "Não consegui ler esse anexo.")
            return
        }
        if (!AttachmentPolicy.isSupported(attachment.mimeType)) {
            _state.value = _state.value.copy(error = "Esse tipo de arquivo ainda não é suportado pelo JARVIS.")
            return
        }
        if (attachment.sizeBytes > AttachmentPolicy.MAX_INLINE_BYTES) {
            _state.value = _state.value.copy(error = "Esse arquivo ultrapassa o limite seguro para envio direto ao Gemini.")
            return
        }
        val media = mediaEncoder.encode(uri, attachment.mimeType)
        if (media == null) {
            _state.value = _state.value.copy(error = "Esse arquivo é grande demais para ser preparado com segurança.")
            return
        }
        pendingMedia = pendingMedia + media
        _state.value = _state.value.copy(
            attachments = _state.value.attachments + attachment,
            error = null,
            input = if (_state.value.input.isBlank()) {
                "O que você quer que eu faça com este anexo?"
            } else _state.value.input
        )
    }

    fun removeAttachment(id: String) {
        val index = _state.value.attachments.indexOfFirst { it.id == id }
        if (index < 0) return
        pendingMedia = pendingMedia.toMutableList().also { it.removeAt(index) }
        _state.value = _state.value.copy(attachments = _state.value.attachments.filterNot { it.id == id })
    }

    fun captureScreen(resultCode: Int, data: Intent?) {
        if (data == null) {
            _state.value = _state.value.copy(error = "A captura da tela foi cancelada.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(status = "CAPTURING_SCREEN", error = null)
            val bitmap = screenshotManager.capture(resultCode, data)
            if (bitmap == null) {
                _state.value = _state.value.copy(status = "STANDBY", error = "Não foi possível capturar a tela.")
                return@launch
            }
            pendingMedia = pendingMedia + BitmapMediaEncoder.encode(bitmap)
            bitmap.recycle()
            _state.value = _state.value.copy(
                status = "STANDBY",
                input = if (_state.value.input.isBlank()) {
                    "O que você quer que eu analise nesta tela?"
                } else _state.value.input
            )
        }
    }

    fun startVoice() {
        if (!_state.value.apiConfigured) {
            _state.value = _state.value.copy(voiceState = VoiceState.ERROR, voiceError = "Configure a API Key da Gemini antes de usar a voz.")
            return
        }
        _state.value = _state.value.copy(voiceError = null)
        voiceSession.start()
    }

    fun stopVoice() { voiceSession.stop(); _state.value = _state.value.copy(voiceState = VoiceState.IDLE, voiceError = null) }
    fun cancelVoice() { voiceSession.cancel(); _state.value = _state.value.copy(voiceState = VoiceState.IDLE) }
    private fun sendVoicePrompt(text: String) { if (text.isBlank()) return; _state.value = _state.value.copy(input = text, voiceState = VoiceState.PROCESSING); send() }

    fun clearApiKey() { keyStore.clear(); _state.value = _state.value.copy(apiConfigured = false, error = null) }

    fun cancelTask() {
        screenExecutor.cancel(); agentJob?.cancel(); confirmationDeferred?.cancel(); confirmationDeferred = null
        _state.value = _state.value.copy(busy = false, status = "STANDBY", confirmation = null)
    }

    override fun onCleared() {
        screenExecutor.cancel(); agentJob?.cancel(); confirmationDeferred?.cancel(); voiceManager.shutdown()
        super.onCleared()
    }

    fun send() {
        val prompt = _state.value.input.trim()
        if (prompt.isBlank() || _state.value.busy || !_state.value.apiConfigured) return
        val mediaForRequest = pendingMedia
        pendingMedia = emptyList()
        screenExecutor.reset()
        _state.value = _state.value.copy(
            input = "", busy = true, status = "THINKING", error = null,
            attachments = emptyList(),
            messages = _state.value.messages + ChatMessage(role = ChatMessage.Role.USER, text = prompt)
        )
        agentJob = viewModelScope.launch {
            val result = runAgent(prompt, mediaForRequest)
            result.onSuccess { answer ->
                if (!screenExecutor.isCancelled()) {
                    _state.value = _state.value.copy(
                        busy = false, status = "STANDBY",
                        messages = _state.value.messages + ChatMessage(role = ChatMessage.Role.ASSISTANT, text = answer)
                    )
                    if (preferences.voiceEnabled && preferences.autoSpeakTypedMessages) voiceManager.speak(answer)
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException || screenExecutor.isCancelled()) {
                    _state.value = _state.value.copy(busy = false, status = "STANDBY", confirmation = null)
                } else {
                    _state.value = _state.value.copy(busy = false, status = "ERROR", error = error.message ?: "Não foi possível concluir a tarefa.")
                }
            }
            agentJob = null
        }
    }

    private suspend fun runAgent(prompt: String, media: List<BrainMediaPart>): Result<String> {
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
            history += BrainMessage(BrainMessage.Role.USER, prompt, media = media)
        }

        val tools = toolset.registry.definitions().map(BrainToolMapper::toBrainDefinition)
        var noActionSteps = 0
        for (step in 1..MAX_STEPS) {
            if (screenExecutor.isCancelled()) return Result.failure(kotlinx.coroutines.CancellationException("Tarefa cancelada pelo usuário."))
            _state.value = _state.value.copy(status = if (step == 1) "THINKING" else "THINKING • ETAPA " + step + "/" + MAX_STEPS)
            val config = settingsRepository.currentConfig()
            val effectiveConfig = PersonaManager.configWithPersona(config, preferences.persona)
            val response = try {
                withTimeout(STEP_TIMEOUT_MS) {
                    provider.generate(BrainRequest(
                        messages = history.toList(),
                        model = effectiveConfig.model,
                        temperature = effectiveConfig.temperature,
                        maxOutputTokens = effectiveConfig.maxOutputTokens,
                        tools = tools
                    ))
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                return Result.failure(Exception("A etapa demorou mais de ${STEP_TIMEOUT_MS / 1000}s e foi interrompida com segurança."))
            }
            when (response) {
                is BrainResponse.Failure -> return Result.failure(Exception(response.error.toUserMessage()))
                is BrainResponse.Success -> {
                    if (response.toolCalls.isEmpty()) {
                        noActionSteps++
                        return Result.success(response.text.ifBlank { "Recebi o pedido, mas o Gemini não retornou uma resposta textual." })
                    }
                    noActionSteps = 0
                    response.toolCalls.forEach { call ->
                        if (screenExecutor.isCancelled()) return Result.failure(kotlinx.coroutines.CancellationException("Tarefa cancelada pelo usuário."))
                        val tool = toolset.registry.get(call.name) ?: return Result.failure(Exception("Ferramenta não encontrada: " + call.name))
                        if (confirmationManager.requiresConfirmation(tool)) {
                            val request = confirmationManager.createRequest(tool, call.arguments)
                            val deferred = CompletableDeferred<Boolean>()
                            confirmationDeferred = deferred
                            _state.value = _state.value.copy(confirmation = request, status = "WAITING_CONFIRMATION")
                            val approved = deferred.await()
                            confirmationDeferred = null
                            _state.value = _state.value.copy(confirmation = null, status = "EXECUTING")
                            if (!approved) {
                                history += BrainMessage(BrainMessage.Role.TOOL, "Ação recusada pelo usuário.", toolResponse = BrainToolResponse(call.name, "Ação recusada pelo usuário."))
                                return Result.success("Tudo bem. Não executei essa ação.")
                            }
                        }
                        val before = if (isScreenAction(call.name)) { _state.value = _state.value.copy(status = "OBSERVING"); screenExecutor.observe() } else null
                        _state.value = _state.value.copy(status = "EXECUTING")
                        val result = toolset.router.execute(call.name, call.arguments)
                        history += BrainMessage(BrainMessage.Role.MODEL, "", toolCall = call)
                        if (result is ToolResult.Success && isScreenAction(call.name)) {
                            _state.value = _state.value.copy(status = "VERIFYING")
                            screenExecutor.waitForUiChange()
                            val verification = screenExecutor.verifyChange(before)
                            if (!verification.changed && requiresScreenChange(call.name)) {
                                val retry = "A ação foi executada, mas não consegui verificar uma mudança na tela. Não assuma sucesso; observe novamente e tente outra estratégia."
                                history += BrainMessage(BrainMessage.Role.TOOL, retry, toolResponse = BrainToolResponse(call.name, retry))
                                continue
                            }
                        }
                        val resultText = when (result) {
                            is ToolResult.Success -> result.message
                            is ToolResult.Failure -> result.message
                        }
                        history += BrainMessage(BrainMessage.Role.TOOL, resultText, toolResponse = BrainToolResponse(call.name, resultText))
                        _state.value = _state.value.copy(messages = _state.value.messages + ChatMessage(ChatMessage.Role.TOOL, text = "Ferramenta " + tool.definition.name + ": " + resultText))
                    }
                }
            }
        }
        return Result.failure(Exception("A tarefa atingiu o limite de " + MAX_STEPS + " etapas sem uma conclusão segura."))
    }

    private fun isScreenAction(name: String) = name in setOf("ler_tela", "tocar_a_tela", "digitar_a_tela", "rolar_a_tela", "botao_do_sistema")
    private fun requiresScreenChange(name: String) = name in setOf("tocar_a_tela", "digitar_a_tela", "rolar_a_tela", "botao_do_sistema")

    companion object {
        private const val MAX_STEPS = 40
        private const val MAX_NO_ACTION = 3
        private const val MAX_RETRIES = 3
        private const val STEP_TIMEOUT_MS = 95_000L
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AssistantController(context.applicationContext) as T
        }
    }
}
