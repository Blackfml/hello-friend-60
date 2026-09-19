package com.jarvis.ai.news

data class NewsItem(
    val title: String,
    val link: String,
    val source: String?,
    val publishedAt: String?
)

interface NewsRepository {
    suspend fun search(query: String, language: String = "pt-BR"): List<NewsItem>
}
