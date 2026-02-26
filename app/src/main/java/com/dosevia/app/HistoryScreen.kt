package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val days = state.days

    val takenCount   = days.count { it.status == PillStatus.TAKEN }
    val missedCount  = days.count { it.status == PillStatus.MISSED }
    val pendingCount = days.count { it.status == PillStatus.NOT_TAKEN }
    val adherence    = if (days.isNotEmpty()) (takenCount.toFloat() / days.size * 100) else 0f

    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        val isTablet   = maxWidth >= 480.dp
        val padH       = if (isTablet) 32.dp else 16.dp
        val titleSp    = if (isTablet) 22.sp  else 18.sp
        val cardPad    = if (isTablet) 20.dp  else 14.dp
        val statNumSp  = if (isTablet) 36.sp  else 28.sp
        val statLblSp  = if (isTablet) 13.sp  else 11.sp
        val rowNameSp  = if (isTablet) 15.sp  else 13.sp
        val rowDateSp  = if (isTablet) 13.sp  else 11.sp

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .statusBarsPadding()
                    .padding(horizontal = padH, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White,
                            modifier = Modifier.size(if (isTablet) 28.dp else 22.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("History", fontSize = titleSp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = padH, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Summary cards ────────────────────────────────────
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            label     = "Taken",
                            value     = takenCount.toString(),
                            iconColor = Color(0xFF10B981),
                            bgColor   = Color(0xFFD1FAE5),
                            modifier  = Modifier.weight(1f),
                            numSp     = statNumSp,
                            lblSp     = statLblSp,
                            cardPad   = cardPad
                        )
                        StatCard(
                            label     = "Missed",
                            value     = missedCount.toString(),
                            iconColor = Color(0xFFF59E0B),
                            bgColor   = Color(0xFFFEF3C7),
                            modifier  = Modifier.weight(1f),
                            numSp     = statNumSp,
                            lblSp     = statLblSp,
                            cardPad   = cardPad
                        )
                    }
                }

                // Adherence card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(cardPad)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Adherence Rate", fontSize = statLblSp,
                                    fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${adherence.toInt()}%",
                                    fontSize   = (statNumSp.value * 1.3f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = PinkPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                // Progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFE5E7EB))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(adherence / 100f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(gradient)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 56.dp else 44.dp)
                                    .clip(CircleShape)
                                    .background(PinkPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarMonth, null,
                                    tint     = PinkPrimary,
                                    modifier = Modifier.size(if (isTablet) 28.dp else 22.dp))
                            }
                        }
                    }
                }

                // Pending
                item {
                    StatCard(
                        label     = "Remaining",
                        value     = pendingCount.toString(),
                        iconColor = Color(0xFF6B7280),
                        bgColor   = Color(0xFFF3F4F6),
                        modifier  = Modifier.fillMaxWidth(),
                        numSp     = statNumSp,
                        lblSp     = statLblSp,
                        cardPad   = cardPad
                    )
                }

                // ── Section header ───────────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Daily Log",
                        fontSize   = (statLblSp.value * 0.9f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF9CA3AF),
                        letterSpacing = 1.sp
                    )
                }

                // ── Day rows ─────────────────────────────────────────
                items(days) { day ->
                    HistoryRow(day, rowNameSp, rowDateSp, padH = 14.dp)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier,
    numSp: androidx.compose.ui.unit.TextUnit,
    lblSp: androidx.compose.ui.unit.TextUnit,
    cardPad: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(cardPad)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (label) {
                        "Taken"     -> Icons.Default.CheckCircle
                        "Missed"    -> Icons.Default.Warning
                        else        -> Icons.Default.Schedule
                    },
                    null, tint = iconColor, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = lblSp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
            Text(value, fontSize = numSp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        }
    }
}

@Composable
private fun HistoryRow(
    day: DayData,
    nameSp: androidx.compose.ui.unit.TextUnit,
    dateSp: androidx.compose.ui.unit.TextUnit,
    padH: androidx.compose.ui.unit.Dp
) {
    val fmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    val (iconVec, iconColor, badgeText, badgeBg, badgeText2) = when (day.status) {
        PillStatus.TAKEN     -> Tuple5(Icons.Default.CheckCircle, Color(0xFF10B981), "Taken",   Color(0xFFD1FAE5), Color(0xFF065F46))
        PillStatus.MISSED    -> Tuple5(Icons.Default.Warning,     Color(0xFFF59E0B), "Missed",  Color(0xFFFEF3C7), Color(0xFF92400E))
        PillStatus.NOT_TAKEN -> Tuple5(Icons.Default.Schedule,    Color(0xFF9CA3AF), "Pending", Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = padH, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconVec, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Day ${day.day}", fontSize = nameSp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                    if (day.isPlacebo) {
                        Spacer(Modifier.width(6.dp))
                        Text("Placebo", fontSize = (dateSp.value * 0.85f).sp, color = Color(0xFF9CA3AF))
                    }
                    if (day.isLowDose) {
                        Spacer(Modifier.width(6.dp))
                        Text("Low-dose", fontSize = (dateSp.value * 0.85f).sp, color = Color(0xFF9CA3AF))
                    }
                }
                Text(fmt.format(day.date), fontSize = dateSp, color = Color(0xFF6B7280))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(badgeText, fontSize = (dateSp.value * 0.9f).sp, fontWeight = FontWeight.Medium, color = badgeText2)
            }
        }
    }
}

// Tiny data holder to avoid destructuring issues
private data class Tuple5(
    val a: androidx.compose.ui.graphics.vector.ImageVector,
    val b: Color,
    val c: String,
    val d: Color,
    val e: Color
)
