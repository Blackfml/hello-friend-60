package com.jarvis.ai.whatsapp

import android.content.Context
import com.jarvis.ai.tools.ToolRegistry

class WhatsAppModule(context: Context) {
    private val bridge: WhatsAppBridge = WhatsAppLocalBridge(context.applicationContext)
    val watcher = WhatsAppWatcher()
    val agent = WhatsAppAgent(bridge, watcher)
    val scheduler = WhatsAppScheduler(context.applicationContext)
    val tools = WhatsAppTools(bridge)

    fun register(registry: ToolRegistry) {
        tools.register(registry)
    }
}
