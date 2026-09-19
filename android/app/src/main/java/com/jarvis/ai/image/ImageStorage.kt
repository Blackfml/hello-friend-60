package com.jarvis.ai.image

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class ImageStorage(private val context: Context) {
    fun save(source: Uri, extension: String = "bin"): Uri? {
        val input = context.contentResolver.openInputStream(source) ?: return null
        val file = File(context.filesDir, "images")
        file.mkdirs()
        val target = File(file, "img_" + System.currentTimeMillis() + "." + extension)
        input.use { sourceStream -> FileOutputStream(target).use { targetStream -> sourceStream.copyTo(targetStream) } }
        return Uri.fromFile(target)
    }
}
