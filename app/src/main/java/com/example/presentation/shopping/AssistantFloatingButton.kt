package com.example.presentation.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import com.example.data.local.AssistantButtonPreferences
import com.example.data.local.calculateSnappedAssistantButtonPosition
import com.example.ui.theme.CardBorder
import com.example.ui.theme.RoyalElectricViolet
import kotlin.math.roundToInt

@Composable
fun AssistantFloatingButton(
    preferences: AssistantButtonPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val buttonSize = 56.dp
        val buttonSizePx = with(density) { buttonSize.toPx() }
        val marginPx = with(density) { 12.dp.toPx() }
        val minY = marginPx.coerceAtMost((heightPx - buttonSizePx).coerceAtLeast(0f))
        val maxY = (heightPx - buttonSizePx - marginPx).coerceAtLeast(minY)
        val maxX = (widthPx - buttonSizePx).coerceAtLeast(0f)
        val leftX = marginPx.coerceAtMost(maxX)
        val rightX = (maxX - marginPx).coerceAtLeast(leftX)
        val initialPosition = remember(preferences) { preferences.getPosition() }

        var isOnLeft by remember { mutableStateOf(initialPosition.isOnLeft) }
        var verticalFraction by remember { mutableFloatStateOf(initialPosition.verticalFraction) }
        var positionX by remember { mutableFloatStateOf(0f) }
        var positionY by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(widthPx, heightPx) {
            positionX = if (isOnLeft) leftX else rightX
            positionY = minY + (maxY - minY) * verticalFraction
        }

        fun saveSnappedPosition() {
            val savedPosition = calculateSnappedAssistantButtonPosition(
                x = positionX,
                y = positionY,
                screenWidth = widthPx,
                screenHeight = heightPx,
                buttonSize = buttonSizePx,
                margin = marginPx
            )
            isOnLeft = savedPosition.isOnLeft
            positionX = if (isOnLeft) leftX else rightX
            verticalFraction = savedPosition.verticalFraction
            positionY = minY + (maxY - minY) * verticalFraction
            preferences.savePosition(savedPosition)
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(positionX.roundToInt(), positionY.roundToInt()) }
                .size(buttonSize)
                .shadow(12.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(RoyalElectricViolet)
                .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(20.dp))
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures(
                        onDragEnd = ::saveSnappedPosition,
                        onDragCancel = ::saveSnappedPosition,
                        onDrag = { change, dragAmount ->
                            change.consume()
                            positionX = (positionX + dragAmount.x).coerceIn(0f, maxX)
                            positionY = (positionY + dragAmount.y).coerceIn(minY, maxY)
                        }
                    )
                }
                .clickable(
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics { contentDescription = "Open AI Fashion Assistant" }
                .testTag("assistant_floating_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
