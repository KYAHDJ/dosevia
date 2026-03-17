package com.dosevia.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

suspend fun encodeProfilePhotoToBase64(context: Context, uri: Uri, targetSize: Int = 128): String =
    withContext(Dispatchers.Default) {
        val sourceBitmap = decodeBitmapFromUri(context, uri) ?: return@withContext ""
        val cropped = centerCropSquare(sourceBitmap)
        if (cropped != sourceBitmap) sourceBitmap.recycle()
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (scaled != cropped) cropped.recycle()

        val firstPass = compressBitmap(scaled, 60)
        val finalBytes = if (firstPass.size > 50_000) compressBitmap(scaled, 40) else firstPass
        scaled.recycle()

        Base64.encodeToString(finalBytes, Base64.NO_WRAP)
    }

fun decodeBase64Bitmap(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(input, null, opts)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun centerCropSquare(bitmap: Bitmap): Bitmap {
    val size = minOf(bitmap.width, bitmap.height)
    val x = (bitmap.width - size) / 2
    val y = (bitmap.height - size) / 2
    return Bitmap.createBitmap(bitmap, x, y, size, size)
}

private fun compressBitmap(bitmap: Bitmap, quality: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.JPEG
    }
    bitmap.compress(format, quality, out)
    return out.toByteArray()
}
