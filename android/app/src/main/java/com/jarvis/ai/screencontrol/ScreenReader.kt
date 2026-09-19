package com.jarvis.ai.screencontrol

import android.view.accessibility.AccessibilityNodeInfo

data class ScreenElement(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val clickable: Boolean,
    val enabled: Boolean
)

class ScreenReader {
    fun read(root: AccessibilityNodeInfo?): List<ScreenElement> {
        if (root == null) return emptyList()
        val result = mutableListOf<ScreenElement>()
        walk(root, result)
        return result
    }

    private fun walk(node: AccessibilityNodeInfo, result: MutableList<ScreenElement>) {
        result += ScreenElement(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString(),
            clickable = node.isClickable,
            enabled = node.isEnabled
        )
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                walk(child, result)
                child.recycle()
            }
        }
    }
}
