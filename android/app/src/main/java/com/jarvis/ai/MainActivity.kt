package com.jarvis.ai
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.jarvis.ai.ui.JarvisApp
import com.jarvis.ai.ui.theme.JarvisTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisTheme { Surface { JarvisApp() } } }
    }
}
