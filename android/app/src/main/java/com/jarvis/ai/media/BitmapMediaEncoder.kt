package com.jarvis.ai.media

import android.graphics.Bitmap
import android.util.Base64
import com.jarvis.ai.brain.BrainMediaPart
import java.io.ByteArrayOutputStream

object BitmapMediaEncoder {
    fun encode(bitmap: Bitmap, quality: Int = 82): BrainMediaPart {
        val scaled = if (bitmap.width > 1600) {
            val ratio = 1600f / bitmap.width.toFloat()
            Bitmap.createScaledBitmap(bitmap, 1600, (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        if (scaled !== bitmap) scaled.recycle()
        return BrainMediaPart(
            mimeType = "image/jpeg",
            base64Data = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        )
    }
}
