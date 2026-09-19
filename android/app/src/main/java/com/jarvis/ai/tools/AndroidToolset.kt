package com.jarvis.ai.tools

import android.content.Context
import com.jarvis.ai.news.GoogleNewsRssRepository
import com.jarvis.ai.news.NewsTool

class AndroidToolset(context: Context) {
    val registry = ToolRegistry().also {
        AndroidToolFactory.registerDefaults(context.applicationContext, it)
        it.register(NewsTool(GoogleNewsRssRepository()))
    }
    val router = ToolRouter(registry)
}
