package com.jarvis.ai.screencontrol

data class ScreenSnapshot(
    val packageName: String?,
    val elements: List<ScreenElement>
) {
    fun compactText(maxElements: Int = 120): String = buildString {
        append("APP=").append(packageName ?: "desconhecido").append('\n')
        elements.asSequence()
            .filter { !it.text.isNullOrBlank() || !it.contentDescription.isNullOrBlank() }
            .take(maxElements)
            .forEachIndexed { index, element ->
                append(index + 1).append(". ")
                append(element.text ?: element.contentDescription ?: "")
                if (element.clickable) append(" [clicável]")
                if (!element.enabled) append(" [desativado]")
                append('\n')
            }
    }
}

class ScreenSnapshotProvider(
    private val reader: ScreenReader = ScreenReader()
) {
    fun capture(): ScreenSnapshot? {
        val service = JarvisAccessibilityService.instance ?: return null
        val root = service.rootNode() ?: return null
        val packageName = root.packageName?.toString()
        return ScreenSnapshot(packageName, reader.read(root))
    }
}
