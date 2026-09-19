package com.jarvis.ai.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JarvisApp() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF070A0F), Color(0xFF0A1018), Color(0xFF070A0F)))
            ).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("JARVIS", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("ONLINE • STANDBY", color = Color(0xFF65F0C5), fontSize = 12.sp)
                }
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Configurações") }
            }
            Spacer(Modifier.weight(1f))
            Text("ASSISTENTE PESSOAL", fontSize = 11.sp, letterSpacing = 2.sp, color = Color(0xFF69E8FF))
            Spacer(Modifier.height(12.dp))
            Text("Como posso ajudar?", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                Suggestion("Que horas são?")
                Suggestion("Status da bateria")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                Suggestion("Abra o YouTube")
                Suggestion("Defina um alarme")
            }
            Spacer(Modifier.height(34.dp))
            IconButton(onClick = {}, Modifier.clip(CircleShape).background(Color(0xFF142A36))) {
                Icon(Icons.Default.Mic, "Ouvir", tint = Color(0xFF69E8FF))
            }
            Spacer(Modifier.weight(1f))
            Text("JARVIS • FASE 1", fontSize = 11.sp, color = Color(0xFF667180))
        }
    }
}
@Composable
private fun Suggestion(label: String) {
    Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(label, fontSize = 11.sp) }
}
