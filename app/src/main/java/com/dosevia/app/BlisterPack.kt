package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
fun BlisterPack(
    days: List<DayData>,
    packColor: Brush,
    onDayClick: (DayData) -> Unit,
    puncturingDay: Int?
) {
    // Get month range
    val monthRange = if (days.isEmpty()) "" else {
        val fmt = SimpleDateFormat("MMMM", Locale.getDefault())
        val first = fmt.format(days.first().date)
        val last = fmt.format(days.last().date)
        if (first == last) first else "$first – $last"
    }

    // Split into 4 columns of 7
    val columns = Array(4) { col ->
        days.drop(col * 7).take(7)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(packColor)
    ) {
        // Foil texture overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x0AF609BC),
                            Color.Transparent,
                            Color(0x0AFAB86D)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 4-column grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                columns.forEach { column ->
                    if (column.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            column.forEachIndexed { rowIndex, dayData ->
                                val today = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.time
                                val pillCal = Calendar.getInstance().apply {
                                    time = dayData.date
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.time
                                val isCurrentDay = pillCal.time == today.time

                                PillBubble(
                                    dayData = dayData,
                                    isCurrentDay = isCurrentDay,
                                    shouldPuncture = puncturingDay == dayData.day,
                                    onClick = {
                                        val pillDate = Calendar.getInstance().apply {
                                            time = dayData.date
                                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                        }.time
                                        if (!pillDate.after(today)) {
                                            onDayClick(dayData)
                                        }
                                    }
                                )

                                // Arrow connector
                                if (rowIndex < column.size - 1) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = when {
                                            dayData.isLowDose -> LowDoseAmber.copy(alpha = 0.6f)
                                            dayData.isPlacebo -> OrangeAccent.copy(alpha = 0.6f)
                                            else -> PinkPrimary.copy(alpha = 0.6f)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Month label
            Spacer(Modifier.height(12.dp))
            Text(
                text = monthRange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PinkDark,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
