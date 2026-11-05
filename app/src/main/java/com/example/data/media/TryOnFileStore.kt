package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class TryOnFileStore(private val context: Context) {
    private val dir = File(context.filesDir, "tryon_media").apply { mkdirs() }

    fun storageDirectory(): File = dir

    fun saveImage(bytes: ByteArray): String {
        val file = File(dir, "img_${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun saveUri(uri: Uri): String = uri.toString()

    fun createCameraOutputUri(): Uri {
        val file = File(dir, "cam_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun deleteIfOwned(path: String) {
        runCatching {
            val file = File(path)
            if (file.exists() && file.parentFile?.absolutePath == dir.absolutePath) {
                file.delete()
            }
        }
    }

    fun persistPersonPhoto(uri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read stream")
        saveImage(bytes)
    }

    fun persistGarmentPhoto(uri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read stream")
        saveImage(bytes)
    }

    fun persistBitmap(bitmap: Bitmap, tag: String): Result<String> = runCatching {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        saveImage(stream.toByteArray())
    }

    fun rotateAndCropToPortrait(path: String, rotationDegrees: Int): Result<String> = runCatching {
        val bitmap = BitmapFactory.decodeFile(path) ?: error("Cannot decode image")
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val stream = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        saveImage(stream.toByteArray())
    }

    fun rotatePhoto(path: String, rotationDegrees: Int): Result<String> = rotateAndCropToPortrait(path, rotationDegrees)

    fun cropNormalized(path: String, left: Float, top: Float, right: Float, bottom: Float): Result<String> = runCatching {
        val bitmap = BitmapFactory.decodeFile(path) ?: error("Cannot decode image")
        val x = (left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((right - left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val h = ((bottom - top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
        val stream = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        saveImage(stream.toByteArray())
    }

    fun saveToGallery(path: String): Result<Uri> = runCatching {
        val file = File(path)
        val galleryFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: dir, file.name)
        file.copyTo(galleryFile, overwrite = true)
        Uri.fromFile(galleryFile)
    }

    fun shareUriForPath(path: String): Uri {
        val file = File(path)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun persistGeneratedBytes(bytes: ByteArray): String = saveImage(bytes)

    fun encodeForApi(uriStr: String): EncodedTryOnImage {
        val uri = Uri.parse(uriStr)
        val bytes = if (uri.scheme == "content" || uri.scheme == "file") {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: File(uriStr).readBytes()
        } else {
            File(uriStr).readBytes()
        }
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return EncodedTryOnImage("image/jpeg", base64)
    }

    fun encodeDrawable(resId: Int): EncodedTryOnImage? {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId) ?: return null
        val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                val canvas = android.graphics.Canvas(it)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        return EncodedTryOnImage("image/jpeg", base64)
    }
}
