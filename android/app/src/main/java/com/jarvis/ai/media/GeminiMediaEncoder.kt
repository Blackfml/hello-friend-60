package com.jarvis.ai.media

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import com.jarvis.ai.brain.BrainMediaPart
import java.io.ByteArrayOutputStream

class GeminiMediaEncoder(private val resolver: ContentResolver) {
    fun encode(uri: Uri, mimeType: String, maxBytes: Int = 8 * 1024 * 1024): BrainMediaPart? {
        val input = resolver.openInputStream(uri) ?: return null
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                output.write(buffer, 0, read)
            }
            return BrainMediaPart(
                mimeType = mimeType,
                base64Data = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            )
        }
    }
}
