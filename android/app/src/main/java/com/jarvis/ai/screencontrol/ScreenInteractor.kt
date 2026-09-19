package com.jarvis.ai.screencontrol

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class ScreenInteractor {
    fun tap(node: AccessibilityNodeInfo): Boolean =
        node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    fun type(node: AccessibilityNodeInfo, text: String): Boolean {
        if (!node.isEditable) return false
        val args = android.os.Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scrollForward(node: AccessibilityNodeInfo): Boolean =
        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

    fun scrollBackward(node: AccessibilityNodeInfo): Boolean =
        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)

    fun bounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }
}
