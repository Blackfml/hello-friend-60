package com.jarvis.ai.tools

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolRouterTest {
    @Test
    fun executesRegisteredTool() = runTest {
        val registry = ToolRegistry()
        registry.register(object : JarvisTool {
            override val definition = ToolDefinition("test_tool", "test")
            override suspend fun execute(arguments: Map<String, Any?>) = ToolResult.Success("ok")
        })
        val result = ToolRouter(registry).execute("test_tool", emptyMap())
        assertEquals(ToolResult.Success("ok"), result)
    }
}
