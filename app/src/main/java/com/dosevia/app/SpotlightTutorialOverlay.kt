package com.dosevia.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple spotlight tutorial overlay:
 *  - darkens the screen
 *  - clears a rounded-rect hole over the target
 *  - tap anywhere advances (only when the highlight is available)
 *  - Skip button top-left
 */

data class TutorialStep(val key: String, val title: String, val body: String)

@Composable
fun SpotlightTutorialOverlay(
    steps: List<TutorialStep>,
    targetRects: Map<String, Rect>,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    onStepChanged: ((TutorialStep) -> Unit)? = null
) {
    if (steps.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    val step = steps.getOrNull(index) ?: return

    // Notify host (HomeScreen) so it can scroll the target into view.
    LaunchedEffect(step.key) {
        onStepChanged?.invoke(step)
    }

    val rect = targetRects[step.key]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // Only allow advancing when we have a valid highlight rectangle.
            .clickable(enabled = rect != null) {
                if (index >= steps.lastIndex) onFinish() else index++
            }
    ) {
        // Spotlight hole
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            // Draw the dim overlay
            drawRect(Color.Black.copy(alpha = 0.55f))

            rect?.let {
                val pad = 10.dp.toPx()
                val hole = Rect(
                    left = it.left - pad,
                    top = it.top - pad,
                    right = it.right + pad,
                    bottom = it.bottom + pad
                )

                // Clear rounded rect
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(hole.left, hole.top),
                    size = androidx.compose.ui.geometry.Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    blendMode = BlendMode.Clear,
                    style = Fill
                )
            }
        }

        // Skip button
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Button(onClick = onSkip) {
                Text("Skip")
            }
        }

        // Description card
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(step.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Text(step.body, fontSize = 13.sp, color = Color(0xFF374151), lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (rect != null) "Tap anywhere to continue" else "Hold on… aligning highlight",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
