package com.example.data.local

import android.content.Context

data class AssistantButtonPosition(
    val isOnLeft: Boolean = false,
    val verticalFraction: Float = 0.5f
)

class AssistantButtonPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getPosition(): AssistantButtonPosition {
        val savedFraction = preferences.getFloat(KEY_VERTICAL_FRACTION, DEFAULT_VERTICAL_FRACTION)
        return AssistantButtonPosition(
            isOnLeft = preferences.getBoolean(KEY_IS_ON_LEFT, false),
            verticalFraction = savedFraction.takeIf { it in 0f..1f } ?: DEFAULT_VERTICAL_FRACTION
        )
    }

    fun savePosition(position: AssistantButtonPosition) {
        val verticalFraction = position.verticalFraction
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, 1f)
            ?: DEFAULT_VERTICAL_FRACTION
        preferences.edit()
            .putBoolean(KEY_IS_ON_LEFT, position.isOnLeft)
            .putFloat(KEY_VERTICAL_FRACTION, verticalFraction)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "wearwise_assistant_button_prefs"
        const val KEY_IS_ON_LEFT = "is_on_left"
        const val KEY_VERTICAL_FRACTION = "vertical_fraction"
        const val DEFAULT_VERTICAL_FRACTION = 0.5f
    }
}

internal fun calculateSnappedAssistantButtonPosition(
    x: Float,
    y: Float,
    screenWidth: Float,
    screenHeight: Float,
    buttonSize: Float,
    margin: Float
): AssistantButtonPosition {
    val maxY = (screenHeight - buttonSize).coerceAtLeast(0f)
    val minY = margin.coerceAtMost(maxY)
    val bottomY = (screenHeight - buttonSize - margin).coerceAtLeast(minY)
    val boundedY = y.coerceIn(minY, bottomY)

    return AssistantButtonPosition(
        isOnLeft = x + buttonSize / 2f < screenWidth / 2f,
        verticalFraction = if (bottomY > minY) {
            (boundedY - minY) / (bottomY - minY)
        } else {
            0.5f
        }
    )
}
