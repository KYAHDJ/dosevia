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

data class PackScheme(val brush: Brush, val accent: Color)

val PACK_SCHEMES = listOf(
    PackScheme(Brush.linearGradient(listOf(Color(0xFFFEF3F9), Color(0xFFFEF9ED), Color(0xFFFEFCE8))), PinkPrimary),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFFEF3F9), Color(0xFFFCE7F3), Color(0xFFFEF3F9))), Color(0xFFDB2777)),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFFEF9ED), Color(0xFFFED7AA), Color(0xFFFEF9ED))), Color(0xFFF97316)),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFFEFCE8), Color(0xFFFEF3C7), Color(0xFFFEFCE8))), Color(0xFFEAB308)),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFF0FDF4))), Color(0xFF22C55E)),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFEFF6FF))), Color(0xFF3B82F6)),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFFAF5FF))), Color(0xFFA855F7)),
    PackScheme(Brush.linearGradient(listOf(Color(0xFFFDF4FF), Color(0xFFFAE8FF), Color(0xFFFDF4FF))), Color(0xFFD946EF))
)

@Composable
fun SwipeableBlisterPacks(
    days: List<DayData>,
    onStatusChange: (Int, PillStatus) -> Unit,
    onPillTaken: (DayData) -> Unit = {}
) {
    val pillsPerPack = 28
    val totalPacks   = maxOf(1, (days.size + pillsPerPack - 1) / pillsPerPack)
    val packs        = (0 until totalPacks).map { i -> days.drop(i * pillsPerPack).take(pillsPerPack) }

    var currentPackIndex by remember { mutableStateOf(0) }
    val coroutineScope   = rememberCoroutineScope()

    // ── Auto-jump to today's pack whenever the days list or start date changes
    // Use the startDate of day[0] as a key so any date/type change triggers re-navigation
    val packKey = if (days.isNotEmpty()) days[0].date.time else 0L
    LaunchedEffect(packKey, days.size) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val idx = days.indexOfFirst { d ->
            val c = Calendar.getInstance(); c.time = d.date
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis == today
        }
        currentPackIndex = if (idx >= 0) idx / pillsPerPack else 0
    }

    val scheme = PACK_SCHEMES[currentPackIndex % PACK_SCHEMES.size]

    // Modal state
    var selectedDay   by remember { mutableStateOf<DayData?>(null) }
    var puncturingDay by remember { mutableStateOf<Int?>(null) }
    var animatingDay  by remember { mutableStateOf<DayData?>(null) }

    fun handleStatusChange(day: Int, status: PillStatus) {
        if (status == PillStatus.TAKEN) {
            val d = days.find { it.day == day } ?: return
            puncturingDay = day
            coroutineScope.launch {
                delay(300)
                puncturingDay = null
                animatingDay  = d
            }
        } else {
            onStatusChange(day, status)
        }
    }

    Column {
        // ── Dot indicators ────────────────────────────────────────
        if (totalPacks > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                packs.forEachIndexed { idx, _ ->
                    val s      = PACK_SCHEMES[idx % PACK_SCHEMES.size]
                    val active = idx == currentPackIndex
                    val dotW by animateDpAsState(
                        if (active) 26.dp else 8.dp,
                        spring(Spring.DampingRatioMediumBouncy), label = "dw$idx"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(dotW)
                            .height(if (active) 10.dp else 8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (active) s.accent else Color(0xFFD1D5DB))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { currentPackIndex = idx }
                    )
                }
            }

            // Pack header with arrows
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (currentPackIndex > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable { currentPackIndex-- }
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                } else Spacer(Modifier.size(36.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Pack ${currentPackIndex + 1} of $totalPacks",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.accent
                    )
                    Text(
                        "Days ${currentPackIndex * pillsPerPack + 1}–${minOf((currentPackIndex + 1) * pillsPerPack, days.size)}",
                        fontSize = 12.sp, color = Color(0xFF6B7280)
                    )
                }

                if (currentPackIndex < totalPacks - 1) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable { currentPackIndex++ }
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                } else Spacer(Modifier.size(36.dp))
            }
        }

        // ── Animated blister pack ─────────────────────────────────
        AnimatedContent(
            targetState = currentPackIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(360)) { it * dir } + fadeIn(tween(260))) togetherWith
                (slideOutHorizontally(tween(360)) { -it * dir } + fadeOut(tween(260)))
            },
            label = "packSlide"
        ) { packIdx ->
            val s    = PACK_SCHEMES[packIdx % PACK_SCHEMES.size]
            val pack = packs.getOrElse(packIdx) { emptyList() }
            BlisterPack(
                days          = pack,
                packBg        = s.brush,
                accentColor   = s.accent,
                onDayClick    = { selectedDay = it },
                puncturingDay = if (packIdx == currentPackIndex) puncturingDay else null
            )
        }

        // Nav hint
        if (totalPacks > 1) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    if (currentPackIndex > 0)
                        Text("←", color = scheme.accent, fontSize = 13.sp)
                    Text("Use arrows to navigate packs",
                        fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                    if (currentPackIndex < totalPacks - 1)
                        Text("→", color = scheme.accent, fontSize = 13.sp)
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

    // Full-screen animation
    if (animatingDay != null) {
        PillTakenAnimation(
            isPlacebo = animatingDay!!.isPlacebo,
            onComplete = {
                val taken = animatingDay!!
                onStatusChange(taken.day, PillStatus.TAKEN)
                onPillTaken(taken)
                animatingDay = null
            }
        )
    }
}
