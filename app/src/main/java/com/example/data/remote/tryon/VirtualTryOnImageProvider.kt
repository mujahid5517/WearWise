package com.example.data.remote.tryon

import android.graphics.Bitmap

interface VirtualTryOnImageProvider {
    suspend fun generateTryOn(request: VirtualTryOnRequest): Bitmap
    suspend fun generate(apiKey: String, request: VirtualTryOnRequest): ByteArray
    suspend fun generateTryOn(
        userPhotoPath: String,
        garmentPaths: List<String>,
        fitType: String,
        pose: String,
        lighting: String
    ): String
}
