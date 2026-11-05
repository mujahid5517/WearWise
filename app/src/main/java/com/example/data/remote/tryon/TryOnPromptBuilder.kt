package com.example.data.remote.tryon

object TryOnPromptBuilder {
    fun build(garmentsText: String, fitType: String, pose: String, lighting: String, count: Int): String {
        return "Realistic virtual try-on featuring $garmentsText. Fit: $fitType, Pose: $pose, Lighting: $lighting."
    }
}
