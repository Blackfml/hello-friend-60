package com.jarvis.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.ai.screencontrol.ControlSessionManager
import com.jarvis.ai.screencontrol.ScreenshotCaptureManager

@Composable
fun AssistantScreen(
    controller: AssistantController = viewModel(factory = AssistantController.factory(LocalContext.current))
) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var controlActive by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val controlManager = remember { ControlSessionManager(context) }
    val screenshotManager = remember { ScreenshotCaptureManager(context) }

    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.startVoice()
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(controller::attachUri)
    }
    val screenshotLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            controller.captureScreen(result.resultCode, result.data)
        }
    }

    fun refreshControlState() {
        accessibilityEnabled = controlManager.isAccessibilityEnabled()
        controlActive = controlManager.isControlActive()
    }

    LaunchedEffect(Unit) { refreshControlState() }

    if (showSettings) {
        JarvisSettingsDialog(state = state, controller = controller, onDismiss = { showSettings = false })
    }

    state.confirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = controller::rejectPendingAction,
            title = { Text(confirmation.title) },
            text = { Text(confirmation.description + "\n\nFerramenta: " + confirmation.toolName) },
            confirmButton = { Button(onClick = controller::confirmPendingAction) { Text("Confirmar") } },
            dismissButton = { Button(onClick = controller::rejectPendingAction) { Text("Cancelar") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("JARVIS • " + state.status); Text("Modelo: " + state.model + " • " + state.persona.title) }
            TextButton(onClick = { showSettings = true }) { Text("⚙ Configurações") }
        }
        Text(
            when (state.voiceState) {
                com.jarvis.ai.voice.VoiceState.IDLE -> "Voz: pronta"
                com.jarvis.ai.voice.VoiceState.LISTENING -> "Voz: ouvindo..."
                com.jarvis.ai.voice.VoiceState.PROCESSING -> "Voz: processando..."
                com.jarvis.ai.voice.VoiceState.SPEAKING -> "Voz: falando..."
                com.jarvis.ai.voice.VoiceState.ERROR -> "Voz: erro"
            },
            Modifier.padding(top = 4.dp)
        )

        Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (controlActive) "CONTROLE DO CELULAR ATIVO"
                else if (accessibilityEnabled) "Acessibilidade disponível — controle ainda não liberado"
                else "Controle do celular desativado"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                    Text("Acessibilidade")
                }
                Button(onClick = {
                    if (controlActive) controlManager.deactivate() else controlManager.activate()
                    refreshControlState()
                }) {
                    Text(if (controlActive) "Parar controle" else "Ativar controle")
                }
            }
        }

        if (!state.apiConfigured) {
            Text("Configure a API Key da Gemini para começar.", Modifier.padding(top = 12.dp, bottom = 8.dp))
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Gemini API Key") }
            )
            Button(onClick = { controller.saveApiKey(apiKey); apiKey = "" }, Modifier.padding(top = 8.dp)) {
                Text("Salvar chave")
            }
        }

        if (state.attachments.isNotEmpty()) {
            Text("Anexos selecionados", Modifier.padding(top = 8.dp))
            state.attachments.forEach { attachment ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text((attachment.name ?: attachment.kind.name) + " • " + attachment.mimeType, Modifier.weight(1f))
                    TextButton(onClick = { controller.removeAttachment(attachment.id) }) { Text("Remover") }
                }
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
            items(state.messages, key = { it.id }) { message ->
                Text(
                    (if (message.role == ChatMessage.Role.USER) "Você" else if (message.role == ChatMessage.Role.TOOL) "Sistema" else "JARVIS") +
                        ": " + message.text,
                    Modifier.padding(vertical = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = state.input, onValueChange = controller::setInput,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy && state.apiConfigured,
            placeholder = { Text(if (state.attachments.isEmpty()) "Fale com o JARVIS" else "Diga o que fazer com o anexo") }
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                enabled = !state.busy && state.apiConfigured
            ) { Text("📎 Anexo") }

            Button(
                onClick = { screenshotLauncher.launch(screenshotManager.createPermissionIntent()) },
                enabled = !state.busy && state.apiConfigured
            ) { Text("📱 Tela") }

            Button(
                onClick = {
                    if (state.voiceState == com.jarvis.ai.voice.VoiceState.LISTENING) {
                        controller.stopVoice()
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        controller.startVoice()
                    } else microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                enabled = state.apiConfigured && !state.busy
            ) { Text(if (state.voiceState == com.jarvis.ai.voice.VoiceState.LISTENING) "Parar" else "🎙") }

            Button(onClick = controller::send, enabled = !state.busy && state.apiConfigured) {
                Text(if (state.busy) "..." else "Enviar")
            }
        }

        state.error?.let { Text(it, Modifier.padding(top = 8.dp)) }
        state.voiceError?.let { Text(it, Modifier.padding(top = 8.dp)) }

        if (state.apiConfigured) {
            TextButton(onClick = controller::clearApiKey) { Text("Remover chave") }
        }
    }
}
