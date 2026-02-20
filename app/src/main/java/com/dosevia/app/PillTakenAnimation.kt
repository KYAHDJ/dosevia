package com.dosevia.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosevia.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class AnimStage { PUNCTURE, EMERGE, SPIN, CHECK, CONFIRM }

@Composable
fun PillTakenAnimation(
    isPlacebo: Boolean,
    onComplete: () -> Unit
) {
    var stage by remember { mutableStateOf(AnimStage.PUNCTURE) }

    LaunchedEffect(Unit) {
        delay(300); stage = AnimStage.EMERGE
        delay(500); stage = AnimStage.SPIN
        delay(800); stage = AnimStage.CHECK
        delay(600); stage = AnimStage.CONFIRM
    }

    // Pill scale anim
    val pillScale by animateFloatAsState(
        targetValue = when (stage) {
            AnimStage.PUNCTURE -> 0.6f
            AnimStage.EMERGE -> 1.2f
            AnimStage.SPIN -> 1.2f
            AnimStage.CHECK -> 1.2f
            AnimStage.CONFIRM -> 1.2f
        },
        animationSpec = when (stage) {
            AnimStage.EMERGE -> spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
            else -> tween(300)
        },
        label = "pillScale"
    )

    // Spin animation
    val spinRotation = remember { Animatable(0f) }
    LaunchedEffect(stage) {
        if (stage == AnimStage.SPIN) {
            spinRotation.animateTo(360f, tween(800, easing = FastOutSlowInEasing))
        }
    }

    // Check mark animation
    val checkScale by animateFloatAsState(
        targetValue = if (stage == AnimStage.CHECK || stage == AnimStage.CONFIRM) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )
    val checkRotation by animateFloatAsState(
        targetValue = if (stage == AnimStage.CHECK || stage == AnimStage.CONFIRM) 0f else -180f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "checkRotation"
    )

    // Confirm button
    val confirmVisible = stage == AnimStage.CONFIRM

    // Glow pulse
    val glowScale = rememberInfiniteTransition(label = "glow")
    val glowPulse by glowScale.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "glowPulse"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xB3000000), Color(0xE6000000))
                    )
                )
        ) {
            // Glow behind pill
            if (stage == AnimStage.EMERGE || stage == AnimStage.SPIN) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(glowPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PinkPrimary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Particles on puncture
            if (stage == AnimStage.PUNCTURE) {
                repeat(8) { i ->
                    val angle = (i / 8.0) * Math.PI * 2
                    PuncturParticle(
                        targetX = (cos(angle) * 60).toFloat(),
                        targetY = (sin(angle) * 60).toFloat()
                    )
                }
            }

            // Main pill
            if (stage != AnimStage.PUNCTURE) {
                val pillGradient = if (isPlacebo)
                    Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB), Color(0xFF9CA3AF)))
                else
                    Brush.linearGradient(listOf(Color.White, Color(0xFFF3F4F6), Color(0xFFE5E7EB)))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pillScale)
                        .rotate(if (stage == AnimStage.SPIN) spinRotation.value else 0f)
                        .clip(CircleShape)
                        .background(pillGradient)
                ) {
                    // Pill shine
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.TopStart)
                            .offset(x = 8.dp, y = 8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent)
                                )
                            )
                    )

                    // Checkmark
                    if (stage == AnimStage.CHECK || stage == AnimStage.CONFIRM) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(checkScale)
                                .rotate(checkRotation)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Taken",
                                tint = TakenGreen,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            } else {
                // Puncture flash effect
                PunctureEffect()
            }

            // Confirm button
            AnimatedVisibility(
                visible = confirmVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp),
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(PinkPrimary, PinkDark)),
                            RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "✓ Confirm Taken",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PunctureEffect() {
    val scale by animateFloatAsState(
        targetValue = 2f,
        animationSpec = tween(300),
        label = "flash"
    )
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(300),
        label = "flashAlpha"
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
fun PuncturParticle(targetX: Float, targetY: Float) {
    val xAnim by animateFloatAsState(
        targetValue = targetX,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "px"
    )
    val yAnim by animateFloatAsState(
        targetValue = targetY,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "py"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(500),
        label = "pa"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(x = xAnim.dp, y = yAnim.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 1f - alphaAnim))
    )
}
