package com.example.data.remote.tryon

interface VirtualTryOnImageProvider {
    suspend fun generateTryOn(request: VirtualTryOnRequest): android.graphics.Bitmap
}
