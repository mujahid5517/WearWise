package com.example.data.remote.tryon

import com.example.data.media.EncodedTryOnImage

data class VirtualTryOnRequest(
    val personImage: EncodedTryOnImage,
    val clothingImages: List<EncodedTryOnImage>,
    val prompt: String
)
