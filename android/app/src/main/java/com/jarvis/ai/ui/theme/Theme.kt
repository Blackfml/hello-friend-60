package com.jarvis.ai.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val JarvisColors = darkColorScheme(
    primary = Color(0xFF69E8FF),
    secondary = Color(0xFF7C8CFF),
    tertiary = Color(0xFF65F0C5),
    background = Color(0xFF070A0F),
    surface = Color(0xFF0D121A),
    surfaceVariant = Color(0xFF151D28)
)
@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JarvisColors, content = content)
}
