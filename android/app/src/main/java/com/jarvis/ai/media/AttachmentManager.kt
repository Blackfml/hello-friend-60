package com.jarvis.ai.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.util.UUID

class AttachmentManager(private val resolver: ContentResolver) {
    fun inspect(uri: Uri): Attachment? {
        val mime = resolver.getType(uri) ?: return null
        var name: String? = null
        var size = 0L
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (ni >= 0) name = cursor.getString(ni)
                if (si >= 0 && !cursor.isNull(si)) size = cursor.getLong(si)
            }
        }
        val kind = when {
            mime.startsWith("image/") -> AttachmentKind.IMAGE
            mime.startsWith("video/") -> AttachmentKind.VIDEO
            mime.startsWith("audio/") -> AttachmentKind.AUDIO
            mime == "application/pdf" || mime.startsWith("text/") || mime.contains("document") ->
                AttachmentKind.DOCUMENT
            else -> AttachmentKind.OTHER
        }
        return Attachment(UUID.randomUUID().toString(), uri.toString(), name, mime, size, kind)
    }
}
