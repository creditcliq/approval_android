package com.creditchek.approval_android.core.engines

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

object BitmapUtils {

    /**
     * Normalizes a Bitmap to an exact 600x600 square JPEG and returns the Data URL.
     * Matches Flutter's _ensureNormalizedJpeg 1:1.
     */
    fun toBase64JpegDataUrl(bitmap: Bitmap, targetSize: Int = 600, quality: Int = 90): String {
        val squareBitmap = cropCenterSquare(bitmap)
        val resizedBitmap = if (squareBitmap.width != targetSize || squareBitmap.height != targetSize) {
            squareBitmap.scale(targetSize, targetSize)
        } else {
            squareBitmap
        }

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val jpegBytes = outputStream.toByteArray()

        val base64String = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64String"
    }

    /**
     * Crops a rectangular bitmap into a 1:1 centered square.
     */
    private fun cropCenterSquare(srcBmp: Bitmap): Bitmap {
        val width = srcBmp.width
        val height = srcBmp.height

        val dimension = minOf(width, height)
        val xOffset = (width - dimension) / 2
        val yOffset = (height - dimension) / 2

        return Bitmap.createBitmap(srcBmp, xOffset, yOffset, dimension, dimension)
    }


    /**
     * Decode base 64 string to Bitmap image
     */
    fun decodeBase64ToBitmap(base64String: String?): ImageBitmap? {
        if (base64String.isNullOrBlank()) return null
        return try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0,decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception){
            null
        }
    }
}