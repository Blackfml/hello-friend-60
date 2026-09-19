package com.jarvis.ai.media

object AttachmentPolicy {
    const val MAX_INLINE_BYTES = 8 * 1024 * 1024
    const val MAX_TOTAL_INLINE_BYTES = 18 * 1024 * 1024

    fun isSupported(mimeType: String): Boolean =
        mimeType.startsWith("image/") ||
            mimeType.startsWith("audio/") ||
            mimeType.startsWith("video/") ||
            mimeType == "application/pdf" ||
            mimeType.startsWith("text/")
}
