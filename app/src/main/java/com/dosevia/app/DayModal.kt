package com.dosevia.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DayModal(
    dayData: DayData?,
    onClose: () -> Unit,
    onStatusChange: (Int, PillStatus) -> Unit
) {
    if (dayData == null) return

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
    val pillCal = Calendar.getInstance().apply {
        time = dayData.date
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
    val isFuture = pillCal.after(today)

    val dayName = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(dayData.date)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            // Modal card
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "modalScale"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .scale(scale)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFEF3F9), Color(0xFFFEF9ED))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "DAY ${dayData.day}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PinkDark,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = dayName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                }
                                IconButton(onClick = onClose) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Pill type badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        if (dayData.isPlacebo)
                                            OrangeAccent.copy(alpha = 0.2f)
                                        else
                                            PinkPrimary.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        dayData.isLowDose -> "Low-Dose Pill"
                                        dayData.isPlacebo -> "Placebo Pill"
                                        else -> "Active Pill"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dayData.isPlacebo) Color(0xFFC2410C) else PinkDark
                                )
                            }
                        }
                    }

                    // Action buttons
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isFuture) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "🚫 Cannot edit future pills",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(
                                        "You can only change today's pill or past pills",
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Mark this day as:",
                                fontSize = 14.sp,
                                color = Color(0xFF4B5563),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Taken button
                        Button(
                            onClick = {
                                if (!isFuture) {
                                    onStatusChange(dayData.day, PillStatus.TAKEN)
                                    onClose()
                                }
                            },
                            enabled = !isFuture,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0xFF9CA3AF)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isFuture)
                                        Brush.linearGradient(listOf(TakenGreen, Color(0xFF059669)))
                                    else
                                        Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF9CA3AF)))
                                )
                        ) {
                            Text(
                                "✓  Taken",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Not Taken button
                        Button(
                            onClick = {
                                if (!isFuture) {
                                    onStatusChange(dayData.day, PillStatus.NOT_TAKEN)
                                    onClose()
                                }
                            },
                            enabled = !isFuture,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0xFF9CA3AF)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB))
                                    )
                                )
                        ) {
                            Text(
                                "Not Taken",
                                color = Color(0xFF374151),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Missed button
                        Button(
                            onClick = {
                                if (!isFuture) {
                                    onStatusChange(dayData.day, PillStatus.MISSED)
                                    onClose()
                                }
                            },
                            enabled = !isFuture,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0xFF9CA3AF)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isFuture)
                                        Brush.linearGradient(listOf(OrangeAccent, Color(0xFFF59E0B)))
                                    else
                                        Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF9CA3AF)))
                                )
                        ) {
                            Text(
                                "⚠  Missed",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
