package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()

    var showPillTypeModal by remember { mutableStateOf(false) }
    var showCustomConfigModal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val headerGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF609BC), Color(0xFFFAB86D)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFF0FB), Color(0xFFFFF8F0), Color(0xFFFFFDE8))
                )
            )
    ) {
        // ── HEADER ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // App title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dosevia",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Professional Pill Reminder",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Pill Type Selector
                HeaderCard(
                    icon = Icons.Default.Medication,
                    label = "Pill Type (Tap to change)",
                    value = getPillTypeLabel(state.pillType),
                    onClick = { showPillTypeModal = true }
                )

                Spacer(Modifier.height(10.dp))

                // Start Date
                val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(state.startDate)
                HeaderCard(
                    icon = Icons.Default.CalendarMonth,
                    label = "Started (Tap to edit)",
                    value = dateStr,
                    onClick = { showDatePicker = true }
                )
            }
        }

        // ── MAIN CONTENT ────────────────────────────────────────
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp)
        ) {
            // Blister Pack
            SwipeableBlisterPacks(
                days = state.days,
                onStatusChange = { day, status -> viewModel.updateDayStatus(day, status) }
            )

            Spacer(Modifier.height(16.dp))

            // Reminder Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PinkPrimary.copy(alpha = 0.08f),
                                OrangeAccent.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        2.dp,
                        PinkPrimary.copy(alpha = 0.18f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Next Reminder",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PinkDark
                        )
                        Text(
                            if (state.settings.appActive)
                                "Today at ${state.settings.dailyReminderTime}"
                            else "Reminders disabled",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick Actions - 4-column grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Navigate to settings */ }
                )
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "History",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Navigate to history */ }
                )
                QuickActionButton(
                    icon = Icons.Default.BarChart,
                    label = "Stats",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Navigate to stats */ }
                )
                QuickActionButton(
                    icon = Icons.Default.Description,
                    label = "Notes",
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Navigate to notes */ }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── MODALS ──────────────────────────────────────────────────

    if (showPillTypeModal) {
        PillTypeModal(
            currentType = state.pillType,
            onClose = { showPillTypeModal = false },
            onSelect = { type -> viewModel.changePillType(type) },
            onCustomSelect = {
                showPillTypeModal = false
                showCustomConfigModal = true
            }
        )
    }

    if (showCustomConfigModal) {
        CustomPillConfigModal(
            onClose = { showCustomConfigModal = false },
            onSave = { active, placebo, lowDose ->
                viewModel.changeCustomPillConfig(active, placebo, lowDose)
                showCustomConfigModal = false
            }
        )
    }

    if (showDatePicker) {
        StartDatePickerModal(
            currentDate = state.startDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.changeStartDate(date)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun HeaderCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PinkPrimary, OrangeAccent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = PinkDark
                )
                Text(
                    value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = PinkPrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
        }
    }
}
