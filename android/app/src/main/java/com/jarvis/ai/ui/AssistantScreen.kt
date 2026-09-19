package com.jarvis.ai.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.ai.screencontrol.ControlSessionManager

@Composable
fun AssistantScreen(
    controller: AssistantController = viewModel(
        factory = AssistantController.factory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var controlActive by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    val controlManager = remember { ControlSessionManager(context) }

    fun refreshControlState() {
        accessibilityEnabled = controlManager.isAccessibilityEnabled()
        controlActive = controlManager.isControlActive()
    }

    LaunchedEffect(Unit) {
        refreshControlState()
    }

    if (state.confirmation != null) {
        AlertDialog(
            onDismissRequest = { controller.rejectPendingAction() },
            title = { Text(state.confirmation.title) },
            text = {
                Text(
                    state.confirmation.description +
                        "\n\nFerramenta: " + state.confirmation.toolName
                )
            },
            confirmButton = {
                Button(onClick = controller::confirmPendingAction) { Text("Confirmar") }
            },
            dismissButton = {
                Button(onClick = controller::rejectPendingAction) { Text("Cancelar") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("JARVIS • " + state.status)

        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (controlActive) {
                    "CONTROLE DO CELULAR ATIVO"
                } else if (accessibilityEnabled) {
                    "Acessibilidade disponível — controle ainda não liberado"
                } else {
                    "Controle do celular desativado"
                }
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    }
                ) {
                    Text("Configurar acessibilidade")
                }

                Button(
                    onClick = {
                        if (controlActive) {
                            controlManager.deactivate()
                        } else {
                            controlManager.activate()
                        }
                        refreshControlState()
                    }
                ) {
                    Text(if (controlActive) "Parar controle" else "Ativar controle")
                }
            }
        }

        if (!state.apiConfigured) {
            Text(
                "Configure a API Key da Gemini para começar.",
                Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Gemini API Key") }
            )
            Button(
                onClick = {
                    controller.saveApiKey(apiKey)
                    apiKey = ""
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Salvar chave")
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                Text(
                    (if (message.role == ChatMessage.Role.USER) "Você" else "JARVIS") +
                        ": " + message.text,
                    Modifier.padding(vertical = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = controller::setInput,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy && state.apiConfigured,
            placeholder = { Text("Fale com o JARVIS") }
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { controller.send() },
                enabled = !state.busy && state.apiConfigured
            ) {
                Text(if (state.busy) "Analisando..." else "Enviar")
            }
        }

        state.error?.let { Text(it, Modifier.padding(top = 8.dp)) }

        if (state.apiConfigured) {
            Button(
                onClick = controller::clearApiKey,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Remover chave")
            }
        }
    }
}
