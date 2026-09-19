package com.jarvis.ai.image
interface ImageEditor { suspend fun edit(sourceUri: String, instruction: String): Result<GeneratedImage> }