package com.jarvis.ai.screencontrol

import android.content.Context
import android.provider.Settings

class ControlSessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("jarvis_control", Context.MODE_PRIVATE)

    fun isAccessibilityEnabled(): Boolean =
        try {
            Settings.Secure.getInt(
                prefsContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1 && JarvisAccessibilityService.instance != null
        } catch (_: Exception) {
            JarvisAccessibilityService.instance != null
        }

    private val prefsContext: Context = context.applicationContext

    fun isControlActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    fun activate(): Boolean {
        if (!isAccessibilityEnabled()) return false
        prefs.edit().putBoolean(KEY_ACTIVE, true).apply()
        return true
    }

    fun deactivate() {
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
    }

    companion object {
        private const val KEY_ACTIVE = "control_active"
    }
}
