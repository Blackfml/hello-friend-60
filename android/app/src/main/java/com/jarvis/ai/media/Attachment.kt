package com.jarvis.ai.media

data class Attachment(
    val id: String,
    val uri: String,
    val name: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val kind: AttachmentKind
)

enum class AttachmentKind { IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER }
