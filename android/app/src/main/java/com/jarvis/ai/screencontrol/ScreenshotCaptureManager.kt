package com.jarvis.ai.screencontrol
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
class ScreenshotCaptureManager(private val context: Context) {
    fun createPermissionIntent(): Intent {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.createScreenCaptureIntent()
    }
    suspend fun capture(resultCode: Int, data: Intent): Bitmap? = withContext(Dispatchers.Main) {
        if (resultCode != Activity.RESULT_OK) return@withContext null
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = manager.getMediaProjection(resultCode, data) ?: return@withContext null
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var display: VirtualDisplay? = null
        try {
            display = projection.createVirtualDisplay("JARVIS-Screenshot", width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, null)
            var image: android.media.Image? = null
            repeat(12) {
                delay(80)
                image = reader.acquireLatestImage()
                if (image != null) return@repeat
            }
            val captured = image ?: return@withContext null
            captured.use {
                val plane = it.planes[0]
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val raw = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                raw.copyPixelsFromBuffer(plane.buffer)
                return@withContext Bitmap.createBitmap(raw, 0, 0, width, height)
            }
        } finally {
            display?.release()
            reader.close()
            projection.stop()
        }
    }
}