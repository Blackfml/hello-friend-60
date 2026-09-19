package com.jarvis.ai.screencontrol

object ControlCommandParser {
    private val activate = setOf(
        "ATIVAR CELULAR",
        "ASSUMIR CELULAR",
        "LIBERAR CONTROLE"
    )
    private val stop = setOf(
        "PARAR",
        "SOLTAR CELULAR",
        "DESATIVAR CONTROLE"
    )

    enum class Command { ACTIVATE, STOP, NONE }

    fun parse(text: String): Command {
        val normalized = text
            .trim()
            .uppercase()
            .replace(Regex("\\s+"), " ")
        return when {
            activate.any { normalized == it } -> Command.ACTIVATE
            stop.any { normalized == it } -> Command.STOP
            else -> Command.NONE
        }
    }
}
