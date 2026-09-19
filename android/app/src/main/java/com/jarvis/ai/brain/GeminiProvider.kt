package com.jarvis.ai.brain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GeminiProvider(
    private val configProvider: () -> GeminiConfig
) : BrainProvider {
    override suspend fun generate(request: BrainRequest): BrainResponse = withContext(Dispatchers.IO) {
        val config = configProvider()
        if (!config.isConfigured) return@withContext BrainResponse.Failure(BrainError.UNAUTHORIZED)

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "${config.model}:generateContent?key=${config.apiKey}"

        try {
            val body = JSONObject().apply {
                put("systemInstruction", JSONObject().put("parts",
                    JSONArray().put(JSONObject().put("text", config.systemInstruction))))
                put("contents", JSONArray().apply {
                    request.messages.filter { it.role != BrainMessage.Role.SYSTEM }.forEach { message ->
                        put(JSONObject().apply {
                            put("role", if (message.role == BrainMessage.Role.USER) "user" else "model")
                            put("parts", JSONArray().put(JSONObject().put("text", message.content)))
                        })
                    }
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", request.temperature)
                    put("maxOutputTokens", request.maxOutputTokens)
                })
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 95_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            if (status !in 200..299) return@withContext BrainResponse.Failure(mapHttpError(status))

            val parts = JSONObject(responseText).optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                ?: return@withContext BrainResponse.Failure(BrainError.INVALID_RESPONSE)

            val text = buildString {
                for (i in 0 until parts.length()) {
                    parts.optJSONObject(i)?.optString("text")?.takeIf { it.isNotBlank() }?.let {
                        if (isNotEmpty()) append("\n")
                        append(it)
                    }
                }
            }.trim()

            if (text.isBlank()) BrainResponse.Failure(BrainError.INVALID_RESPONSE)
            else BrainResponse.Success(text)
        } catch (_: java.net.SocketTimeoutException) {
            BrainResponse.Failure(BrainError.TIMEOUT)
        } catch (_: IOException) {
            BrainResponse.Failure(BrainError.NETWORK)
        } catch (_: Exception) {
            BrainResponse.Failure(BrainError.UNKNOWN)
        }
    }

    private fun mapHttpError(status: Int): BrainError = when (status) {
        401, 403 -> BrainError.UNAUTHORIZED
        429 -> BrainError.RATE_LIMITED
        408 -> BrainError.TIMEOUT
        413 -> BrainError.CONTEXT_LIMIT
        in 400..499 -> BrainError.INVALID_RESPONSE
        else -> BrainError.UNKNOWN
    }
}
