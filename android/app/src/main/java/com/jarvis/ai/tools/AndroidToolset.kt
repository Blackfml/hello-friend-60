package com.jarvis.ai.tools

import android.content.Context

class AndroidToolset(context: Context) {
    val registry = ToolRegistry().also {
        AndroidToolFactory.registerDefaults(context.applicationContext, it)
    }
    val router = ToolRouter(registry)
}
