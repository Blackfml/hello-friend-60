package com.jarvis.ai.news
import com.jarvis.ai.tools.*
class NewsTool(private val repository: NewsRepository) : JarvisTool {
    override val definition = ToolDefinition(
        name = "buscar_noticias",
        description = "Busca notícias atuais sobre um assunto. Use quando o usuário pedir notícias atuais, recentes ou de hoje.",
        parameters = mapOf("query" to "string"),
        requiredParameters = setOf("query"),
        riskLevel = RiskLevel.LOW
    )
    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"]?.toString().orEmpty()
        if (query.isBlank()) return ToolResult.Failure("Informe o assunto da notícia.")
        val items = repository.search(query)
        if (items.isEmpty()) return ToolResult.Failure("Não encontrei notícias atuais para esse assunto.")
        val text = items.take(10).joinToString("\n") { item ->
            buildString {
                append("Título: ").append(item.title)
                item.source?.let { append(" | Fonte: ").append(it) }
                item.publishedAt?.let { append(" | Data: ").append(it) }
                append(" | Link: ").append(item.link)
            }
        }
        return ToolResult.Success("Resultados atuais encontrados:" + "\n" + text)
    }
}