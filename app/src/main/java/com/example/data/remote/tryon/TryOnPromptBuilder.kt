package com.example.data.remote.tryon

object TryOnPromptBuilder {
    fun build(garmentsText: String, fitType: String, pose: String, lighting: String, count: Int): String {
        return "Realistic virtual try-on featuring $garmentsText. Fit: $fitType, Pose: $pose, Lighting: $lighting."
    }

    fun buildPrompt(
        garments: List<com.example.domain.model.SelectedTryOnGarment>,
        fitType: String,
        pose: String,
        lighting: String
    ): String {
        val list = garments.joinToString(", ") { "${it.name} (${it.category})" }
        return "High fashion virtual try-on featuring $list with $fitType fit, $pose posture, under $lighting lighting."
    }
}
