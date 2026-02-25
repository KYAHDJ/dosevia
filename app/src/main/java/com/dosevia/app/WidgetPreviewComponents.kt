package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class WidgetPreviewType { SMALL, MEDIUM, CALENDAR }

@Composable
fun WidgetPreviewSelector(
    modifier: Modifier = Modifier,
    onSmallClick: () -> Unit = {},
    onMediumClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        WidgetPreviewCard(WidgetPreviewType.SMALL, Modifier.weight(1f), onSmallClick)
        WidgetPreviewCard(WidgetPreviewType.MEDIUM, Modifier.weight(1f), onMediumClick)
        WidgetPreviewCard(WidgetPreviewType.CALENDAR, Modifier.weight(1f), onCalendarClick)
    }
}

@Composable
private fun WidgetPreviewCard(type: WidgetPreviewType, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (type) {
                WidgetPreviewType.SMALL -> SmallWidgetPreview(Modifier.fillMaxWidth().aspectRatio(1f))
                WidgetPreviewType.MEDIUM -> MediumWidgetPreview(Modifier.fillMaxWidth().aspectRatio(1f))
                WidgetPreviewType.CALENDAR -> CalendarWidgetPreview(Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Icon(
                imageVector = when (type) {
                    WidgetPreviewType.SMALL -> Icons.Default.PieChart
                    WidgetPreviewType.MEDIUM -> Icons.Default.Widgets
                    WidgetPreviewType.CALENDAR -> Icons.Default.CalendarMonth
                },
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SmallWidgetPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF111827))
            .border(2.dp, Color(0x3322D3EE), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(CircleShape)
                .border(4.dp, Color(0x336B7280), CircleShape)
        )
        Text(
            text = "12",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun MediumWidgetPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1F2937)))
            )
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                StatMini("Taken", "14", Color(0xFF10B981))
                StatMini("Missed", "1", Color(0xFFEF4444))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF374151))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.72f)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D))))
                )
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(label, color = color, fontSize = 8.sp)
    }
}

@Composable
private fun CalendarWidgetPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE5E7EB))
            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Apr 2026", color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        it == 1 && idx in 2..3 -> Color(0xFFEF4444)
                                        it == 0 && idx in 4..5 -> Color(0xFF334155)
                                        else -> Color(0xFFFFFFFF)
                                    }
                                )
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
