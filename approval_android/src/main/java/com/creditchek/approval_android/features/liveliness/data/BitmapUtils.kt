package com.creditchek.approval_android.features.liveliness.data

import android.graphics.Bitmap
import android.util.Base64
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
}