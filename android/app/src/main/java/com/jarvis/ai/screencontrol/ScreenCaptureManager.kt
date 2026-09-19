package com.jarvis.ai.screencontrol

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

class ScreenCaptureManager(private val context: Context) {
    fun createPermissionIntent(): Intent {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }

    fun isResultValid(resultCode: Int, data: Intent?): Boolean =
        resultCode == Activity.RESULT_OK && data != null
}
