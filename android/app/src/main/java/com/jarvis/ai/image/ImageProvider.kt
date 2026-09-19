package com.jarvis.ai.image

interface ImageProvider {
    suspend fun generate(prompt: String): Result<GeneratedImage>
    suspend fun edit(sourceUri: String, instruction: String): Result<GeneratedImage>
}
