package com.jarvis.ai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.persona.Persona

@Composable
fun JarvisSettingsDialog(
    state: AssistantUiState,
    controller: AssistantController,
    onDismiss: () -> Unit
) {
    var model by remember(state.model) { mutableStateOf(state.model) }
    var memory by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações do JARVIS") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Inteligência")
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo Gemini") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Temperatura: %.2f".format(state.temperature), Modifier.padding(top = 10.dp))
                Slider(
                    value = state.temperature.toFloat(),
                    onValueChange = { controller.saveTemperature(it.toDouble()) },
                    valueRange = 0f..2f
                )

                Text("Personalidade", Modifier.padding(top = 8.dp))
                Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.persona.title)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Persona.entries.forEach { persona ->
                        DropdownMenuItem(
                            text = { Text(persona.title) },
                            onClick = { controller.setPersona(persona); expanded = false }
                        )
                    }
                }

                Text("Voz", Modifier.padding(top = 12.dp))
                Button(
                    onClick = { controller.setVoiceEnabled(!state.voiceEnabled) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.voiceEnabled) "Voz ativada" else "Voz desativada") }
                Button(
                    onClick = { controller.setAutoSpeakTypedMessages(!state.autoSpeakTypedMessages) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.autoSpeakTypedMessages) "Falar respostas digitadas: sim" else "Falar respostas digitadas: não") }

                Text("Memória", Modifier.padding(top = 12.dp))
                Button(
                    onClick = { controller.setMemoryEnabled(!state.memoryEnabled) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.memoryEnabled) "Memória ativada" else "Memória desativada") }
                OutlinedTextField(
                    value = memory,
                    onValueChange = { memory = it },
                    label = { Text("Adicionar memória autorizada") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { controller.addMemory(memory); memory = "" },
                    enabled = state.memoryEnabled && memory.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar memória") }
                TextButton(
                    onClick = controller::clearMemory,
                    enabled = state.memoryEnabled
                ) { Text("Apagar todas as memórias") }
            }
        },
        confirmButton = {
            Button(onClick = { controller.saveModel(model); onDismiss() }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
