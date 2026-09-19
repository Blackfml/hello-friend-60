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
            "${config.model}:generateContent"

        try {
            val body = JSONObject().apply {
                put("systemInstruction", JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", config.systemInstruction))
                ))

                put("contents", JSONArray().apply {
                    request.messages.forEach { message ->
                        when {
                            message.toolResponse != null -> {
                                put(JSONObject().apply {
                                    put("role", "user")
                                    put("parts", JSONArray().put(
                                        JSONObject().put(
                                            "functionResponse",
                                            JSONObject()
                                                .put("name", message.toolResponse.name)
                                                .put(
                                                    "response",
                                                    JSONObject().put("result", message.toolResponse.result)
                                                )
                                        )
                                    ))
                                })
                            }
                            message.toolCall != null -> {
                                put(JSONObject().apply {
                                    put("role", "model")
                                    put("parts", JSONArray().put(
                                        JSONObject().put(
                                            "functionCall",
                                            JSONObject()
                                                .put("name", message.toolCall.name)
                                                .put("args", JSONObject(message.toolCall.arguments))
                                        )
                                    ))
                                })
                            }
                            message.role != BrainMessage.Role.SYSTEM &&
                                message.content.isNotBlank() -> {
                                put(JSONObject().apply {
                                    put(
                                        "role",
                                        if (message.role == BrainMessage.Role.MODEL) "model" else "user"
                                    )
                                    put("parts", JSONArray().put(
                                        JSONObject().put("text", message.content)
                                    ))
                                })
                            }
                        }
                    }
                })

                if (request.tools.isNotEmpty()) {
                    val declarations = JSONArray()
                    request.tools.forEach { tool ->
                        val properties = JSONObject()
                        tool.parameters.forEach { (name, type) ->
                            properties.put(
                                name,
                                JSONObject()
                                    .put("type", geminiType(type))
                                    .put("description", "Parâmetro $name")
                            )
                        }

                        declarations.put(
                            JSONObject()
                                .put("name", tool.name)
                                .put("description", tool.description)
                                .put(
                                    "parameters",
                                    JSONObject()
                                        .put("type", "OBJECT")
                                        .put("properties", properties)
                                        .put(
                                            "required",
                                            JSONArray(tool.requiredParameters.toList())
                                        )
                                )
                        )
                    }

                    put("tools", JSONArray().put(
                        JSONObject().put("functionDeclarations", declarations)
                    ))
                }

                put("generationConfig", JSONObject().apply {
                    if (!request.model.startsWith("gemini-3.6") &&
                        !request.model.startsWith("gemini-3.8")
                    ) {
                        put("temperature", request.temperature)
                    }
                    put("maxOutputTokens", request.maxOutputTokens)
                })
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 95_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", config.apiKey)
                setRequestProperty("Accept", "application/json")
            }

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            if (status !in 200..299) {
                return@withContext BrainResponse.Failure(mapHttpError(status))
            }

            val parts = JSONObject(responseText)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: return@withContext BrainResponse.Failure(BrainError.INVALID_RESPONSE)

            val textParts = mutableListOf<String>()
            val calls = mutableListOf<BrainToolCall>()

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                part.optString("text")
                    .takeIf { it.isNotBlank() }
                    ?.let(textParts::add)

                part.optJSONObject("functionCall")?.let { call ->
                    val args = mutableMapOf<String, Any?>()
                    val rawArgs = call.optJSONObject("args")
                    if (rawArgs != null) {
                        rawArgs.keys().forEach { key -> args[key] = rawArgs.opt(key) }
                    }
                    val name = call.optString("name")
                    if (name.isNotBlank()) calls += BrainToolCall(name, args)
                }
            }

            val text = textParts.joinToString("\n").trim()
            if (text.isBlank() && calls.isEmpty()) {
                BrainResponse.Failure(BrainError.INVALID_RESPONSE)
            } else {
                BrainResponse.Success(text, calls)
            }
        } catch (_: java.net.SocketTimeoutException) {
            BrainResponse.Failure(BrainError.TIMEOUT)
        } catch (_: IOException) {
            BrainResponse.Failure(BrainError.NETWORK)
        } catch (_: Exception) {
            BrainResponse.Failure(BrainError.UNKNOWN)
        }
    }

    private fun geminiType(type: String): String = when (type.lowercase()) {
        "integer", "int", "long" -> "INTEGER"
        "number", "double", "float" -> "NUMBER"
        "boolean", "bool" -> "BOOLEAN"
        "array" -> "ARRAY"
        else -> "STRING"
    }

    private fun mapHttpError(status: Int): BrainError = when (status) {
        401 -> BrainError.UNAUTHORIZED
        403 -> BrainError.FORBIDDEN
        429 -> BrainError.RATE_LIMITED
        408 -> BrainError.TIMEOUT
        413 -> BrainError.CONTEXT_LIMIT
        in 400..499 -> BrainError.INVALID_RESPONSE
        else -> BrainError.UNKNOWN
    }
}
