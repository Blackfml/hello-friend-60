package com.jarvis.ai.tools

import android.content.Context
import com.jarvis.ai.news.GoogleNewsRssRepository
import com.jarvis.ai.news.NewsTool
import com.jarvis.ai.whatsapp.WhatsAppModule

class AndroidToolset(context: Context) {
    private val whatsappModule = WhatsAppModule(context.applicationContext)

    val registry = ToolRegistry().also {
        AndroidToolFactory.registerDefaults(context.applicationContext, it)
        it.register(NewsTool(GoogleNewsRssRepository()))
        whatsappModule.register(it)
    }
    val router = ToolRouter(registry)
}
