package com.dosevia.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosevia.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

private enum class Stage { PUNCTURE, EMERGE, SPIN, CHECK, CONFIRM }

@Composable
fun PillTakenAnimation(isPlacebo: Boolean, onComplete: () -> Unit) {
    var stage by remember { mutableStateOf(Stage.PUNCTURE) }

    LaunchedEffect(Unit) {
        delay(300); stage = Stage.EMERGE
        delay(500); stage = Stage.SPIN
        delay(800); stage = Stage.CHECK
        delay(600); stage = Stage.CONFIRM
    }

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
                        listOf(Color(0xB3000000), Color(0xE6000000))
                    )
                )
        ) {
            // Stage 1: Puncture flash + particles
            if (stage == Stage.PUNCTURE) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                    // 8 particles
                    repeat(8) { i ->
                        val angle = (i / 8.0) * PI * 2
                        val targetX = (cos(angle) * 70).toFloat()
                        val targetY = (sin(angle) * 70).toFloat()
                        val px by animateFloatAsState(targetX, tween(500, easing = FastOutSlowInEasing), label = "px$i")
                        val py by animateFloatAsState(targetY, tween(500, easing = FastOutSlowInEasing), label = "py$i")
                        val pa by animateFloatAsState(1f, tween(500), label = "pa$i")
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(px.dp, py.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = pa))
                        )
                    }
                    // Flash expanding circle
                    val flashS by animateFloatAsState(2f, tween(300), label = "fs")
                    val flashA by animateFloatAsState(0f, tween(300), label = "fa")
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(flashS)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = 0.8f * flashA), Color.Transparent)
                                )
                            )
                    )
                }
            }

            // Stages 2-5: The pill
            if (stage != Stage.PUNCTURE) {
                // Pill scale – spring in
                val pillScale by animateFloatAsState(
                    targetValue = 1.25f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                    label = "pScale"
                )

                // 3-D spin during SPIN stage
                val rotY = remember { Animatable(0f) }
                val rotX = remember { Animatable(0f) }
                LaunchedEffect(stage) {
                    if (stage == Stage.SPIN) {
                        launch { rotY.animateTo(360f, tween(800, easing = FastOutSlowInEasing)) }
                        launch { rotX.animateTo(360f, tween(800, easing = FastOutSlowInEasing)) }
                    } else if (stage == Stage.CHECK || stage == Stage.CONFIRM) {
                        rotY.snapTo(0f); rotX.snapTo(0f)
                    }
                }

                // Glow blob
                val glowAnim = rememberInfiniteTransition(label = "glow")
                val glowScale by glowAnim.animateFloat(1f, 1.4f,
                    infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "gs")

                // Check mark spring-in
                val checkScale by animateFloatAsState(
                    if (stage == Stage.CHECK || stage == Stage.CONFIRM) 1f else 0f,
                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), label = "cs"
                )
                val checkRot by animateFloatAsState(
                    if (stage == Stage.CHECK || stage == Stage.CONFIRM) 0f else -180f,
                    spring(Spring.DampingRatioMediumBouncy), label = "cr"
                )
                val checkPulse = rememberInfiniteTransition(label = "cp")
                val checkPulseS by checkPulse.animateFloat(1f, 1.1f,
                    infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "cps")

                Box(contentAlignment = Alignment.Center) {
                    // Pink glow behind pill (emerge + spin only)
                    if (stage == Stage.EMERGE || stage == Stage.SPIN) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(glowScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(PinkPrimary.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Pill disc
                    val pillGrad = if (isPlacebo)
                        Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB), Color(0xFF9CA3AF)))
                    else
                        Brush.linearGradient(listOf(Color.White, Color(0xFFF3F4F6), Color(0xFFE5E7EB)))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pillScale)
                            .graphicsLayer {
                                rotationY = rotY.value
                                rotationX = rotX.value
                                cameraDistance = 12f * density
                            }
                            .clip(CircleShape)
                            .background(pillGrad)
                    ) {
                        // Top-left shine
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.TopStart)
                                .offset(8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = 0.9f), Color.Transparent)
                                    )
                                )
                        )

                        // Checkmark (check + confirm stages)
                        if (stage == Stage.CHECK || stage == Stage.CONFIRM) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(checkScale)
                                    .graphicsLayer { rotationZ = checkRot }
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF10B981).copy(alpha = 0.18f), Color.Transparent)
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(checkPulseS)
                                )
                            }
                        }
                    }
                }
            }

            // Confirm button
            AnimatedVisibility(
                visible = stage == Stage.CONFIRM,
                enter = fadeIn() + slideInVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) {
                val scope = rememberCoroutineScope()
                val btnScale = remember { Animatable(1f) }

                Box(
                    modifier = Modifier
                        .scale(btnScale.value)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(PinkPrimary, PinkDark)),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                btnScale.animateTo(0.95f, tween(80))
                                btnScale.animateTo(1f, tween(80))
                                onComplete()
                            }
                        }
                        .padding(horizontal = 40.dp, vertical = 16.dp)
                ) {
                    Text(
                        "✓  Confirm Taken",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
