package com.example.data.remote.tryon

import com.example.data.media.EncodedTryOnImage

data class VirtualTryOnRequest(
    val personImage: EncodedTryOnImage? = null,
    val clothingImages: List<EncodedTryOnImage> = emptyList(),
    val prompt: String = "",
    val garmentImages: List<EncodedTryOnImage> = clothingImages,
    val garments: List<com.example.domain.model.SelectedTryOnGarment> = emptyList(),
    val fitType: String = "Tailored",
    val pose: String = "Confident Stance",
    val lighting: String = "Warm Daylight",
    val apiKey: String = ""
)
