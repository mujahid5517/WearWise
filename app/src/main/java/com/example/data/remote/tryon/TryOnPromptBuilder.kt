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
        return "Edit the first image into a realistic virtual try-on using the actual garment reference images that follow it. " +
            "Dress the same person in these selected WearWise garments: $list. " +
            "Preserve the person's identity, face, body shape, skin tone, and original framing; do not replace the person. " +
            "Match each garment's colors, pattern, cut, and visible details as closely as possible; do not invent substitute garments. " +
            "Fit: $fitType. Pose: $pose. Lighting: $lighting. Return the edited image."
    }
}
