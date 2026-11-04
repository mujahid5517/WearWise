package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class TryOnFileStore(private val context: Context) {

    fun storageDirectory(): File {
        val dir = File(context.filesDir, "tryon_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun persistPersonPhoto(uri: Uri): Result<String> = runCatching {
        copyUriToLocal(uri, "person_photo_${System.currentTimeMillis()}.jpg")
    }

    fun persistGarmentPhoto(uri: Uri): Result<String> = runCatching {
        copyUriToLocal(uri, "garment_photo_${System.currentTimeMillis()}.jpg")
    }

    fun persistBitmap(bitmap: Bitmap, prefix: String = "img"): Result<String> = runCatching {
        val file = File(storageDirectory(), "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }

    fun rotateAndCropToPortrait(path: String, rotationDegrees: Int): Result<String> = runCatching {
        rotatePhotoInternal(path, rotationDegrees)
    }

    fun rotatePhoto(path: String, rotationDegrees: Int): Result<String> = runCatching {
        rotatePhotoInternal(path, rotationDegrees)
    }

    private fun rotatePhotoInternal(path: String, degrees: Int): String {
        val src = BitmapFactory.decodeFile(path) ?: error("Failed to decode image file")
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        val file = File(path)
        FileOutputStream(file).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun cropNormalized(
        path: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Result<String> = runCatching {
        val src = BitmapFactory.decodeFile(path) ?: error("Failed to decode image file")
        val x = (left * src.width).toInt().coerceIn(0, src.width - 1)
        val y = (top * src.height).toInt().coerceIn(0, src.height - 1)
        val w = ((right - left) * src.width).toInt().coerceIn(1, src.width - x)
        val h = ((bottom - top) * src.height).toInt().coerceIn(1, src.height - y)
        val cropped = Bitmap.createBitmap(src, x, y, w, h)
        val file = File(storageDirectory(), "crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }

    fun saveToGallery(path: String): Result<Uri> = runCatching {
        val bitmap = BitmapFactory.decodeFile(path) ?: error("Invalid image file")
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "WearWise_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/WearWise")
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: error("Failed to create MediaStore entry")
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        } ?: error("Failed to open output stream")
        uri
    }

    fun shareUriForPath(path: String): Uri {
        val file = File(path)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun deleteIfOwned(path: String): Boolean {
        val file = File(path)
        return if (file.exists() && file.absolutePath.contains("tryon_images")) {
            file.delete()
        } else false
    }

    fun createCameraOutputUri(): Pair<Uri, String> {
        val file = File(storageDirectory(), "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return uri to file.absolutePath
    }

    fun encodeForApi(pathOrUri: String): EncodedTryOnImage? {
        val bitmap = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            val uri = Uri.parse(pathOrUri)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } else {
            BitmapFactory.decodeFile(pathOrUri)
        } ?: return null
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
        return EncodedTryOnImage("image/jpeg", base64)
    }

    fun encodeDrawable(context: Context, resId: Int): EncodedTryOnImage? {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return null
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
        return EncodedTryOnImage("image/jpeg", base64)
    }

    fun persistGeneratedBytes(bytes: ByteArray): Result<String> = runCatching {
        val file = File(storageDirectory(), "generated_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }

    private fun copyUriToLocal(uri: Uri, fileName: String): String {
        val destFile = File(storageDirectory(), fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Failed to open input stream for $uri")
        return destFile.absolutePath
    }
}
