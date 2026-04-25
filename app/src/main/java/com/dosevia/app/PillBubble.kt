package com.dosevia.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PillBubble(
    dayData: DayData,
    isCurrentDay: Boolean,
    shouldPuncture: Boolean,
    onClick: () -> Unit,
    pillSizeDp: Dp = 68.dp
) {
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(dayData.date)
    val dateNum  = SimpleDateFormat("d",   Locale.getDefault()).format(dayData.date)
    val isPillPresent = dayData.status != PillStatus.TAKEN

    // All sub-sizes are proportional to pillSizeDp (reference = 68dp original)
    val scaleFactor  = pillSizeDp.value / 68f
    val innerSize    = (62f * scaleFactor).dp
    val coreSize     = (42f * scaleFactor).dp
    val shineSize    = (18f * scaleFactor).dp
    val shineOffset  = (3f  * scaleFactor).dp
    val ringStroke   = (3f  * scaleFactor).dp

    // Font sizes scale with pill but stay readable
    val dayFontSp  = (7f  * scaleFactor).coerceIn(5f, 11f).sp
    val dateFontSp = (12f * scaleFactor).coerceIn(8f, 18f).sp

    val pillColor = when {
        dayData.status == PillStatus.MISSED -> Color(0xFFEF4444)
        dayData.isLowDose                   -> Color(0xFFFBBF24)
        dayData.isPlacebo                   -> Color(0xFFFAB86D)
        else                                -> Color(0xFFF609BC)
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ps"
    )

    val shakeAnim = rememberInfiniteTransition(label = "shake")
    val shakeX by shakeAnim.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse),
        label = "sx"
    )

    val punctureScale by animateFloatAsState(
        targetValue = if (shouldPuncture) 0.88f else 1f,
        animationSpec = tween(150), label = "psc"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(pillSizeDp)
            .drawBehind {
                if (isCurrentDay && isPillPresent) {
                    val ringRadius = (size.minDimension / 2f) * pulseScale
                    drawCircle(
                        color  = pillColor,
                        radius = ringRadius,
                        style  = Stroke(width = ringStroke.toPx())
                    )
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(innerSize)
                .scale(punctureScale)
                .offset(
                    x = if (dayData.status == PillStatus.MISSED && isPillPresent)
                            (shakeX * scaleFactor).dp else 0.dp
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
        ) {
            if (dayData.status == PillStatus.TAKEN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color(0xFF3A3A3A), Color(0xFF1A1A1A))))
                )
                Box(
                    modifier = Modifier
                        .size(coreSize)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF1A1A1A), Color(0xFF000000))))
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(dayName, fontSize = dayFontSp,  fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280), lineHeight = dayFontSp)
                    Text(dateNum, fontSize = dateFontSp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B5563), lineHeight = dateFontSp)
                }

            } else {
                // Silver foil backing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFDDDDDD), Color(0xFF909090)),
                                center = Offset(20f * scaleFactor, 16f * scaleFactor),
                                radius = 80f * scaleFactor
                            )
                        )
                )
                // Plastic dome highlight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(18f * scaleFactor, 12f * scaleFactor),
                                radius = 52f * scaleFactor
                            )
                        )
                )

                if (!shouldPuncture) {
                    val pillGradient: Brush = when {
                        dayData.status == PillStatus.MISSED ->
                            Brush.radialGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFFEF4444)))
                        dayData.isLowDose ->
                            Brush.radialGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFFBBF24)))
                        dayData.isPlacebo ->
                            Brush.radialGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB), Color(0xFF9CA3AF)))
                        else ->
                            Brush.radialGradient(listOf(Color.White, Color(0xFFF3F4F6), Color(0xFFE5E7EB)))
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(coreSize)
                            .clip(CircleShape)
                            .background(pillGradient)
                            .then(
                                if (dayData.status == PillStatus.MISSED)
                                    Modifier.border((2f * scaleFactor).dp, Color(0xFFDC2626), CircleShape)
                                else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(shineSize)
                                .align(Alignment.TopStart)
                                .offset(x = shineOffset, y = shineOffset)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(dayName, fontSize = dayFontSp,  fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827), lineHeight = dayFontSp)
                            Text(dateNum, fontSize = dateFontSp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827), lineHeight = dateFontSp)
                        }
                    }
                }
            }
        }
    }
}
