package com.dosevia.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

data class PackColorScheme(val brush: Brush, val accent: Color)

val packColorSchemes = listOf(
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFFEF3F9), Color(0xFFFEF9ED), Color(0xFFFEFCE8))),
        PinkPrimary
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFFEF3F9), Color(0xFFFCE7F3), Color(0xFFFEF3F9))),
        Color(0xFFDB2777)
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFFEF9ED), Color(0xFFFED7AA), Color(0xFFFEF9ED))),
        Color(0xFFF97316)
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFFEFCE8), Color(0xFFFEF3C7), Color(0xFFFEFCE8))),
        Color(0xFFEAB308)
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFF0FDF4))),
        Color(0xFF22C55E)
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFEFF6FF))),
        Color(0xFF3B82F6)
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFFAF5FF))),
        Color(0xFFA855F7)
    ),
    PackColorScheme(
        Brush.linearGradient(listOf(Color(0xFFFDF4FF), Color(0xFFFAE8FF), Color(0xFFFDF4FF))),
        Color(0xFFD946EF)
    )
)

@Composable
fun SwipeableBlisterPacks(
    days: List<DayData>,
    onStatusChange: (Int, PillStatus) -> Unit
) {
    val pillsPerPack = 28
    val totalPacks = maxOf(1, (days.size + pillsPerPack - 1) / pillsPerPack)
    val packs = (0 until totalPacks).map { i ->
        days.drop(i * pillsPerPack).take(pillsPerPack)
    }

    var currentPackIndex by remember { mutableStateOf(0) }
    var isTransitioning by remember { mutableStateOf(false) }
    var slideDirection by remember { mutableStateOf(1) } // 1 = left, -1 = right

    // Auto-navigate to today's pack
    LaunchedEffect(days.size) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        val todayIndex = days.indexOfFirst { d ->
            val cal = Calendar.getInstance()
            cal.time = d.date
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.time.time == today.time
        }
        if (todayIndex >= 0) {
            currentPackIndex = todayIndex / pillsPerPack
        }
    }

    val packScheme = packColorSchemes[currentPackIndex % packColorSchemes.size]

    // Modal and animation state
    var selectedDay by remember { mutableStateOf<DayData?>(null) }
    var puncturingDay by remember { mutableStateOf<Int?>(null) }
    var animatingDay by remember { mutableStateOf<DayData?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun handleDayClick(dayData: DayData) {
        selectedDay = dayData
    }

    fun handleStatusChange(day: Int, status: PillStatus) {
        if (status == PillStatus.TAKEN) {
            val dayData = days.find { it.day == day }
            if (dayData != null) {
                puncturingDay = day
                coroutineScope.launch {
                    delay(300)
                    puncturingDay = null
                    animatingDay = dayData
                }
            }
        } else {
            onStatusChange(day, status)
        }
    }

    Column {
        if (totalPacks > 1) {
            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                packs.forEachIndexed { index, _ ->
                    val scheme = packColorSchemes[index % packColorSchemes.size]
                    val isActive = index == currentPackIndex
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(dotWidth)
                            .height(if (isActive) 12.dp else 8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isActive) scheme.accent
                                else Color(0xFFD1D5DB)
                            )
                            .clickable {
                                if (!isTransitioning && index != currentPackIndex) {
                                    slideDirection = if (index > currentPackIndex) 1 else -1
                                    currentPackIndex = index
                                }
                            }
                    )
                }
            }

            // Pack header with arrows
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Previous arrow
                if (currentPackIndex > 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(packScheme.accent)
                            .clickable {
                                if (!isTransitioning && currentPackIndex > 0) {
                                    slideDirection = -1
                                    currentPackIndex--
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                } else {
                    Spacer(Modifier.size(36.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Pack ${currentPackIndex + 1} of $totalPacks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = packScheme.accent
                    )
                    Text(
                        "Pills ${currentPackIndex * pillsPerPack + 1}–${minOf((currentPackIndex + 1) * pillsPerPack, days.size)}",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                // Next arrow
                if (currentPackIndex < totalPacks - 1) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(packScheme.accent)
                            .clickable {
                                if (!isTransitioning && currentPackIndex < totalPacks - 1) {
                                    slideDirection = 1
                                    currentPackIndex++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                } else {
                    Spacer(Modifier.size(36.dp))
                }
            }
        }

        // Pack content with slide animation
        AnimatedContent(
            targetState = currentPackIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                slideInHorizontally(tween(400)) { it * dir } + fadeIn(tween(400)) togetherWith
                    slideOutHorizontally(tween(400)) { -it * dir } + fadeOut(tween(400))
            },
            label = "packSlide"
        ) { packIdx ->
            val currentPack = packs.getOrElse(packIdx) { emptyList() }
            val scheme = packColorSchemes[packIdx % packColorSchemes.size]
            BlisterPack(
                days = currentPack,
                packColor = scheme.brush,
                onDayClick = ::handleDayClick,
                puncturingDay = puncturingDay
            )
        }

        // Navigation hint
        if (totalPacks > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentPackIndex > 0)
                            Text("←", color = packScheme.accent, fontSize = 14.sp)
                        Text("Use arrows to navigate packs", fontSize = 13.sp,
                            fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        if (currentPackIndex < totalPacks - 1)
                            Text("→", color = packScheme.accent, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Day modal
    if (selectedDay != null && animatingDay == null) {
        DayModal(
            dayData = selectedDay,
            onClose = { selectedDay = null },
            onStatusChange = { day, status ->
                handleStatusChange(day, status)
                selectedDay = null
            }
        )
    }

    // Pill taken animation
    if (animatingDay != null) {
        PillTakenAnimation(
            isPlacebo = animatingDay!!.isPlacebo,
            onComplete = {
                onStatusChange(animatingDay!!.day, PillStatus.TAKEN)
                animatingDay = null
            }
        )
    }
}
