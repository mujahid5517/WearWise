package com.example.data.remote.tryon

import android.graphics.Bitmap

object TryOnImageValidator {
    fun validate(mimeType: String, sizeBytes: Int, width: Int, height: Int): Result<Unit> {
        return Result.success(Unit)
    }

    fun validate(bitmap: Bitmap): Boolean = bitmap.width > 0 && bitmap.height > 0
}
