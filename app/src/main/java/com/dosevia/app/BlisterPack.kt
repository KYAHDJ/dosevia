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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BlisterPack(
    days: List<DayData>,
    packBg: Brush,
    accentColor: Color,
    onDayClick: (DayData) -> Unit,
    puncturingDay: Int?
) {
    if (days.isEmpty()) return

    val monthRange = run {
        val fmt   = SimpleDateFormat("MMMM", Locale.getDefault())
        val first = fmt.format(days.first().date)
        val last  = fmt.format(days.last().date)
        if (first == last) first else "$first – $last"
    }

    // 4 columns x 7 rows
    val columns = Array(4) { col -> days.drop(col * 7).take(7) }

    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.time
    }

    // Measure available width and derive pill size dynamically
    // Layout: 8dp side padding each side + 12dp inner padding each side + 4 pills + 3 gaps (4dp each)
    // pillSize = (availableWidth - 16dp - 24dp - 12dp) / 4
    // We clamp between 48dp (tiny phone) and 80dp (large tablet)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(packBg)
    ) {
        val availableWidth: Dp = maxWidth
        // 16dp outer padding + 24dp inner padding + 3 * 4dp column gaps = 52dp overhead
        val overhead = 16.dp + 24.dp + (3 * 4).dp
        val rawPillSize = (availableWidth - overhead) / 4
        val pillSizeDp  = rawPillSize.coerceIn(48.dp, 80.dp)

        // Arrow icon scales with pill too
        val arrowSize = (pillSizeDp.value * 0.21f).dp.coerceIn(10.dp, 18.dp)
        // Month label font
        val monthFontSp = (pillSizeDp.value * 0.175f).coerceIn(10f, 14f).sp

        // Foil crosshatch texture
        Box(modifier = Modifier
            .matchParentSize()
            .drawBehind {
                val spacing = 4.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(Color(0x14F609BC), Offset(0f, y), Offset(size.width, y), 1f)
                    y += spacing
                }
                var x = 0f
                while (x < size.width) {
                    drawLine(Color(0x0FFAB86D), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += spacing
                }
            }
        )

        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                columns.forEach { column ->
                    if (column.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            column.forEachIndexed { rowIdx, dayData ->
                                val pillDate = Calendar.getInstance().apply {
                                    time = dayData.date
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
                                }.time
                                val isToday = pillDate.time == today.time

                                PillBubble(
                                    dayData        = dayData,
                                    isCurrentDay   = isToday,
                                    shouldPuncture = puncturingDay == dayData.day,
                                    onClick        = {
                                        if (!pillDate.after(today)) onDayClick(dayData)
                                    },
                                    pillSizeDp     = pillSizeDp
                                )

                                if (rowIdx < column.size - 1) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = when {
                                            dayData.isLowDose -> Color(0xFFFBBF24).copy(alpha = 0.6f)
                                            dayData.isPlacebo -> Color(0xFFFAB86D).copy(alpha = 0.6f)
                                            else              -> Color(0xFFF609BC).copy(alpha = 0.6f)
                                        },
                                        modifier = Modifier.size(arrowSize)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text       = monthRange,
                fontSize   = monthFontSp,
                fontWeight = FontWeight.SemiBold,
                color      = PinkDark,
                modifier   = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Embossed border
        Box(modifier = Modifier
            .matchParentSize()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color(0x14F609BC))
                    ),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(2.dp.toPx())
                )
            }
        )
    }
}
