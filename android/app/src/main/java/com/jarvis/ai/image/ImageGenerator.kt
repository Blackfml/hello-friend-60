package com.jarvis.ai.image
interface ImageGenerator { suspend fun generate(prompt: String): Result<GeneratedImage> }
data class GeneratedImage(val uri: String, val mimeType: String = "image/png")