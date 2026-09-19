package com.jarvis.ai.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class GoogleNewsRssRepository : NewsRepository {
    override suspend fun search(query: String, language: String): List<NewsItem> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
            val url = URL(
                "https://news.google.com/rss/search?q=$encoded&hl=pt-BR&gl=BR&ceid=BR:pt-419"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/rss+xml, application/xml")
                setRequestProperty("User-Agent", "JARVIS-Android/1.0")
            }
            try {
                if (connection.responseCode !in 200..299) return@withContext emptyList()
                val document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(connection.inputStream)
                val nodes = document.getElementsByTagName("item")
                buildList {
                    for (i in 0 until minOf(nodes.length, 15)) {
                        val item = nodes.item(i)
                        fun text(tag: String): String? =
                            item.childNodes.let { children ->
                                (0 until children.length)
                                    .map { children.item(it) }
                                    .firstOrNull { it.nodeName == tag }
                                    ?.textContent?.trim()
                                    ?.takeIf(String::isNotBlank)
                            }
                        add(
                            NewsItem(
                                title = text("title").orEmpty(),
                                link = text("link").orEmpty(),
                                source = text("source"),
                                publishedAt = text("pubDate")
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
}
