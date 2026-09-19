package com.jarvis.ai.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun JarvisApp() {
    androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
        AssistantScreen()
    }
}
