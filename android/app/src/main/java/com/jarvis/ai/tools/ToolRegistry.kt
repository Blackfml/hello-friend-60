package com.jarvis.ai.tools

class ToolRegistry {
    private val tools = linkedMapOf<String, JarvisTool>()
    fun register(tool: JarvisTool) {
        require(tool.definition.name.isNotBlank())
        check(tools.put(tool.definition.name, tool) == null) {
            "Tool already registered: ${tool.definition.name}"
        }
    }
    fun get(name: String): JarvisTool? = tools[name]
    fun definitions(): List<ToolDefinition> = tools.values.map { it.definition }
    fun contains(name: String): Boolean = tools.containsKey(name)
}
