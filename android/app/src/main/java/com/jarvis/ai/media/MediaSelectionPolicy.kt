package com.jarvis.ai.media

object MediaSelectionPolicy {
    fun requiresExplicitIntent(): Boolean = true

    fun promptForAttachment(attachment: Attachment): String =
        "Recebi o anexo " + (attachment.name ?: "sem nome") +
        ". O que você quer que eu faça com ele? Posso analisar ou preparar uma ação, " +
        "mas não vou enviar, excluir ou alterar nada sem sua autorização."
}
