package com.jarvis.ai.tools

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.jarvis.ai.screencontrol.ControlSessionManager
import com.jarvis.ai.screencontrol.JarvisAccessibilityService
import com.jarvis.ai.screencontrol.ScreenInteractor
import com.jarvis.ai.screencontrol.ScreenSnapshotProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ScreenToolFactory {
    fun registerDefaults(context: android.content.Context, registry: ToolRegistry) {
        val appContext = context.applicationContext
        val control = ControlSessionManager(appContext)
        val interactor = ScreenInteractor()
        val snapshot = ScreenSnapshotProvider()

        registry.register(ScreenTool(
            ToolDefinition(
                name = "ler_tela",
                description = "Lê a estrutura visível da tela atual. Use antes de interagir quando não souber exatamente onde está o elemento.",
                riskLevel = RiskLevel.LOW
            )
        ) {
            if (!control.isControlActive()) {
                return@ScreenTool ToolResult.Failure("O controle do celular não está ativo.", "CONTROL_DISABLED")
            }
            val current = snapshot.capture()
                ?: return@ScreenTool ToolResult.Failure("Não consegui acessar a tela atual.", "SCREEN_UNAVAILABLE")
            ToolResult.Success(
                current.compactText(),
                mapOf("package" to current.packageName, "elementCount" to current.elements.size)
            )
        })

        registry.register(ScreenTool(
            ToolDefinition(
                name = "tocar_a_tela",
                description = "Toca em um elemento da tela pelo texto ou descrição visível. Prefira identificar o elemento pela árvore de acessibilidade em vez de coordenadas.",
                parameters = mapOf("alvo" to "texto ou descrição do elemento"),
                requiredParameters = setOf("alvo"),
                riskLevel = RiskLevel.MEDIUM
            )
        ) { args ->
            if (!control.isControlActive()) return@ScreenTool disabled()
            val target = args["alvo"]?.toString()?.trim().orEmpty()
            val node = findNode(target)
                ?: return@ScreenTool ToolResult.Failure("Não encontrei na tela o elemento "$target".", "ELEMENT_NOT_FOUND")
            val clicked = interactor.tap(node)
            node.recycle()
            if (!clicked) ToolResult.Failure("Encontrei "$target", mas não consegui tocá-lo.", "ACTION_FAILED")
            else ToolResult.Success("Toque executado em "$target".")
        })

        registry.register(ScreenTool(
            ToolDefinition(
                name = "digitar_a_tela",
                description = "Digita texto em um campo editável identificado pelo texto ou descrição. Use apenas quando o controle do celular estiver explicitamente ativo.",
                parameters = mapOf(
                    "alvo" to "texto ou descrição do campo",
                    "texto" to "texto a inserir"
                ),
                requiredParameters = setOf("alvo", "texto"),
                riskLevel = RiskLevel.MEDIUM
            )
        ) { args ->
            if (!control.isControlActive()) return@ScreenTool disabled()
            val target = args["alvo"]?.toString()?.trim().orEmpty()
            val text = args["texto"]?.toString().orEmpty()
            val node = findNode(target, editableOnly = true)
                ?: return@ScreenTool ToolResult.Failure("Não encontrei um campo editável chamado "$target".", "FIELD_NOT_FOUND")
            val typed = interactor.type(node, text)
            node.recycle()
            if (!typed) ToolResult.Failure("Não consegui inserir o texto no campo "$target".", "ACTION_FAILED")
            else ToolResult.Success("Texto inserido no campo "$target".")
        })

        registry.register(ScreenTool(
            ToolDefinition(
                name = "rolar_a_tela",
                description = "Rola a tela para cima ou para baixo usando a árvore de acessibilidade.",
                parameters = mapOf("direcao" to "cima ou baixo"),
                requiredParameters = setOf("direcao"),
                riskLevel = RiskLevel.LOW
            )
        ) { args ->
            if (!control.isControlActive()) return@ScreenTool disabled()
            val direction = args["direcao"]?.toString()?.lowercase()
            val root = JarvisAccessibilityService.instance?.rootNode()
                ?: return@ScreenTool ToolResult.Failure("Não consegui acessar a tela.", "SCREEN_UNAVAILABLE")
            val scrollable = findScrollable(root)
            val success = when (direction) {
                "baixo", "down" -> scrollable?.let(interactor::scrollForward) == true
                "cima", "up" -> scrollable?.let(interactor::scrollBackward) == true
                else -> false
            }
            scrollable?.recycle()
            root.recycle()
            if (!success) ToolResult.Failure("Não consegui rolar a tela nessa direção.", "ACTION_FAILED")
            else ToolResult.Success("Tela rolada para $direction.")
        })

        registry.register(ScreenTool(
            ToolDefinition(
                name = "botao_do_sistema",
                description = "Executa um botão global do Android: voltar, início ou recentes.",
                parameters = mapOf("botao" to "voltar, inicio ou recentes"),
                requiredParameters = setOf("botao"),
                riskLevel = RiskLevel.MEDIUM
            )
        ) { args ->
            if (!control.isControlActive()) return@ScreenTool disabled()
            val service = JarvisAccessibilityService.instance
                ?: return@ScreenTool ToolResult.Failure("Serviço de acessibilidade indisponível.", "SERVICE_UNAVAILABLE")
            val button = args["botao"]?.toString()?.lowercase()
            val action = when (button) {
                "voltar", "back" -> AccessibilityService.GLOBAL_ACTION_BACK
                "inicio", "home" -> AccessibilityService.GLOBAL_ACTION_HOME
                "recentes", "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS
                else -> return@ScreenTool ToolResult.Failure("Botão do sistema desconhecido: $button")
            }
            if (service.performGlobalAction(action)) {
                ToolResult.Success("Botão do sistema "$button" executado.")
            } else {
                ToolResult.Failure("O Android recusou a ação "$button".", "ACTION_FAILED")
            }
        }
    }

    private fun findNode(target: String, editableOnly: Boolean = false): AccessibilityNodeInfo? {
        val root = JarvisAccessibilityService.instance?.rootNode() ?: return null
        val result = findNodeRecursive(root, target, editableOnly)
        root.recycle()
        return result
    }

    private fun findNodeRecursive(
        node: AccessibilityNodeInfo,
        target: String,
        editableOnly: Boolean
    ): AccessibilityNodeInfo? {
        val text = node.text?.toString().orEmpty()
        val description = node.contentDescription?.toString().orEmpty()
        val matches = target.equals(text, ignoreCase = true) ||
            target.equals(description, ignoreCase = true) ||
            target.equals(text, ignoreCase = true) && target.isNotBlank()
        if ((!editableOnly || node.isEditable) && matches && node.isEnabled) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeRecursive(child, target, editableOnly)
            if (found != null) {
                for (j in 0 until node.childCount) {
                    if (j != i) node.getChild(j)?.recycle()
                }
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child)
            if (found != null) {
                for (j in 0 until node.childCount) {
                    if (j != i) node.getChild(j)?.recycle()
                }
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun disabled(): ToolResult =
        ToolResult.Failure("Ative o controle do celular antes de usar essa ferramenta.", "CONTROL_DISABLED")

    private class ScreenTool(
        override val definition: ToolDefinition,
        private val action: suspend (Map<String, Any?>) -> ToolResult
    ) : JarvisTool {
        override suspend fun execute(arguments: Map<String, Any?>): ToolResult =
            withContext(Dispatchers.Main) { action(arguments) }
    }
}
