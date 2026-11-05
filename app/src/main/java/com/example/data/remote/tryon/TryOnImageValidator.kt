package com.example.data.remote.tryon

object TryOnImageValidator {
    fun validate(mimeType: String, sizeBytes: Int, width: Int, height: Int): Result<Unit> {
        return Result.success(Unit)
    }
}
