package com.jarvis.ai.image
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
class ImageManager(private val context: Context) {
    fun saveJpeg(bitmap: Bitmap, quality: Int = 90): File {
        val file = File(context.cacheDir, "jarvis_image_" + System.currentTimeMillis() + ".jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), out) }
        return file
    }
}