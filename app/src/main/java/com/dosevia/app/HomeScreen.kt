package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(viewModel: AppViewModel, onNavigate: (Screen) -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    var showPillTypeModal     by remember { mutableStateOf(false) }
    var showCustomConfigModal by remember { mutableStateOf(false) }
    var showDatePicker        by remember { mutableStateOf(false) }

    val headerGradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    // Use BoxWithConstraints at root so we can adapt to any screen width
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth

        // Breakpoints: phone < 480dp, tablet >= 480dp
        val isTablet = screenWidth >= 480.dp

        // Adaptive sizes
        val titleFontSp       = if (isTablet) 32.sp else 26.sp
        val subtitleFontSp    = if (isTablet) 16.sp else 13.sp
        val headerPadH: Dp   = if (isTablet) 32.dp  else 16.dp
        val headerPadV: Dp   = if (isTablet) 20.dp  else 16.dp
        val cardIconSize: Dp = if (isTablet) 48.dp  else 40.dp
        val cardIconSizeSm   = if (isTablet) 24.dp  else 20.dp
        val cardLabelSp      = if (isTablet) 13.sp  else 11.sp
        val cardValueSp      = if (isTablet) 17.sp  else 15.sp
        val reminderFontSp   = if (isTablet) 17.sp  else 15.sp
        val quickIconSize    = if (isTablet) 32.dp  else 26.dp
        val quickFontSp      = if (isTablet) 14.sp  else 12.sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFFF0FB), Color(0xFFFFF8F0), Color(0xFFFFFDE8)))
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
                        .statusBarsPadding()
                        .padding(horizontal = headerPadH, vertical = headerPadV)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dosevia",
                            fontSize   = titleFontSp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text("Professional Pill Reminder",
                            fontSize = subtitleFontSp,
                            color    = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    HeaderCard(
                        icon        = Icons.Default.Medication,
                        label       = "Pill Type (Tap to change)",
                        value       = getPillTypeLabel(state.pillType),
                        onClick     = { showPillTypeModal = true },
                        iconSize    = cardIconSize,
                        iconSizeSm  = cardIconSizeSm,
                        labelFontSp = cardLabelSp,
                        valueFontSp = cardValueSp
                    )
                    Spacer(Modifier.height(10.dp))

                    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(state.startDate)
                    HeaderCard(
                        icon        = Icons.Default.CalendarMonth,
                        label       = "Started (Tap to edit)",
                        value       = dateStr,
                        onClick     = { showDatePicker = true },
                        iconSize    = cardIconSize,
                        iconSizeSm  = cardIconSizeSm,
                        labelFontSp = cardLabelSp,
                        valueFontSp = cardValueSp
                    )
                }
            }

            // ── SCROLLABLE CONTENT ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {

                SwipeableBlisterPacks(
                    days           = state.days,
                    onStatusChange = { day, status -> viewModel.updateDayStatus(day, status) }
                )

                Spacer(Modifier.height(16.dp))

                // Reminder card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = headerPadH)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(PinkPrimary.copy(alpha = 0.08f), OrangeAccent.copy(alpha = 0.08f))
                            )
                        )
                        .border(2.dp, PinkPrimary.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(cardIconSize)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent)))
                        ) {
                            Icon(Icons.Default.Notifications, null,
                                tint = Color.White,
                                modifier = Modifier.size(cardIconSizeSm)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Next Reminder",
                                fontSize   = cardLabelSp,
                                fontWeight = FontWeight.Medium,
                                color      = PinkDark
                            )
                            Text(
                                if (state.settings.appActive) "Today at ${state.settings.displayTime}"
                                else "Reminders disabled",
                                fontSize   = reminderFontSp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF111827)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = headerPadH)
                ) {
                    QuickActionButton(Icons.Default.Settings,    "Settings", Modifier.weight(1f),
                        iconSize = quickIconSize, fontSp = quickFontSp) { onNavigate(Screen.SETTINGS) }
                    QuickActionButton(Icons.Default.History,     "History",  Modifier.weight(1f),
                        iconSize = quickIconSize, fontSp = quickFontSp) { onNavigate(Screen.HISTORY) }
                    QuickActionButton(Icons.Default.Description, "Notes",    Modifier.weight(1f),
                        iconSize = quickIconSize, fontSp = quickFontSp) { onNavigate(Screen.NOTES) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── MODALS ──────────────────────────────────────────────────────
    if (showPillTypeModal) {
        PillTypeModal(
            currentType    = state.pillType,
            onClose        = { showPillTypeModal = false },
            onSelect       = { type -> viewModel.changePillType(type); showPillTypeModal = false },
            onCustomSelect = { showPillTypeModal = false; showCustomConfigModal = true }
        )
    }
    if (showCustomConfigModal) {
        CustomPillConfigModal(
            onClose = { showCustomConfigModal = false },
            onSave  = { a, p, l -> viewModel.changeCustomPillConfig(a, p, l); showCustomConfigModal = false }
        )
    }
    if (showDatePicker) {
        StartDatePickerModal(
            currentDate    = state.startDate,
            onDismiss      = { showDatePicker = false },
            onDateSelected = { d -> viewModel.changeStartDate(d); showDatePicker = false }
        )
    }
}

@Composable
private fun HeaderCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    iconSize: Dp,
    iconSizeSm: Dp,
    labelFontSp: androidx.compose.ui.unit.TextUnit,
    valueFontSp: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent)))
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(iconSizeSm))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = labelFontSp, fontWeight = FontWeight.Medium, color = PinkDark)
                Text(value, fontSize = valueFontSp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(iconSizeSm)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    iconSize: Dp,
    fontSp: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier.aspectRatio(1f).clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, label, tint = PinkPrimary, modifier = Modifier.size(iconSize))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = fontSp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TodayNotesCard
//
//  Shown ABOVE the blister pack when today's pill has been taken.
//  Displays the most recent notes for today so the user sees them first.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayNotesCard(
    notes: List<Note>,
    padH: Dp,
    onViewAll: () -> Unit
) {
    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = padH)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Description,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Today's Notes",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF111827)
                )
            }
            Text(
                text     = "View all →",
                fontSize = 12.sp,
                color    = Color(0xFFF609BC),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onViewAll
                )
            )
        }

        // Show up to 3 most recent notes
        notes.take(3).forEach { note ->
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Colored left accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(gradient)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = note.content,
                            fontSize   = 14.sp,
                            color      = Color(0xFF111827),
                            fontWeight = FontWeight.Medium,
                            maxLines   = 3,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text     = note.time,
                            fontSize = 11.sp,
                            color    = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        // If more than 3, show a "and X more" hint
        if (notes.size > 3) {
            Text(
                text     = "+ ${notes.size - 3} more note${if (notes.size - 3 > 1) "s" else ""} — tap View all",
                fontSize = 12.sp,
                color    = Color(0xFF9CA3AF),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
    }
}
