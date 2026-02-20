package com.dosevia.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PillBubble(
    dayData: DayData,
    isCurrentDay: Boolean,
    shouldPuncture: Boolean,
    onClick: () -> Unit
) {
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(dayData.date)
    val dateNum = SimpleDateFormat("d", Locale.getDefault()).format(dayData.date)
    val isPillPresent = dayData.status != PillStatus.TAKEN

    val pillColor = when {
        dayData.status == PillStatus.MISSED -> MissedRed
        dayData.isLowDose -> LowDoseAmber
        dayData.isPlacebo -> OrangeAccent
        else -> PinkPrimary
    }

    // Pulse animation for current day
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Shake animation for missed pills
    val shake = rememberInfiniteTransition(label = "shake")
    val shakeX by shake.animateFloat(
        initialValue = 0f,
        targetValue = if (dayData.status == PillStatus.MISSED) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(125, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeX"
    )

    // Puncture scale animation
    var punctureAnim by remember { mutableStateOf(false) }
    LaunchedEffect(shouldPuncture) {
        if (shouldPuncture) punctureAnim = true
    }
    val punctureScale by animateFloatAsState(
        targetValue = if (shouldPuncture) 0.9f else 1f,
        animationSpec = tween(150),
        label = "puncture"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(64.dp)
    ) {
        // Pulsing ring for current day
        if (isCurrentDay && isPillPresent) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(3.dp, pillColor, CircleShape)
            )
        }

        // Main pill button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .scale(if (shouldPuncture) punctureScale else 1f)
                .offset(x = if (dayData.status == PillStatus.MISSED) shakeX.dp else 0.dp)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            if (dayData.status == PillStatus.TAKEN) {
                // Dark broken foil look
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF3A3A3A), Color(0xFF1A1A1A))
                            )
                        )
                ) {
                    // Inner empty hole
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF0A0A0A))
                                )
                            )
                    )
                }
                // Taken day/date text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = dayName,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = dateNum,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                // Foil backing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFD4D4D4), Color(0xFF8A8A8A)),
                                radius = 80f
                            )
                        )
                )

                // Pill inside
                val pillGradient = when {
                    dayData.status == PillStatus.MISSED ->
                        Brush.linearGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFCA5A5), MissedRed))
                    dayData.isLowDose ->
                        Brush.linearGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), LowDoseAmber))
                    dayData.isPlacebo ->
                        Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB), Color(0xFF9CA3AF)))
                    else ->
                        Brush.linearGradient(listOf(Color.White, Color(0xFFF3F4F6), Color(0xFFE5E7EB)))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(brush = pillGradient)
                        .then(
                            if (dayData.status == PillStatus.MISSED)
                                Modifier.border(2.dp, Color(0xFFDC2626), CircleShape)
                            else Modifier
                        )
                ) {
                    // Shine highlight
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopStart)
                            .offset(x = 4.dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                    )
                    // Day + date text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = dateNum,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                }

                // Plastic dome highlight overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                center = Offset(22f, 16f),
                                radius = 50f
                            )
                        )
                )
            }
        }
    }
}
