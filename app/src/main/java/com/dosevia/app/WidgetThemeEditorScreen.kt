package com.dosevia.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WidgetThemeEditorScreen(
    viewModel: AppViewModel,
    widgetKind: WidgetKind,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    val currentTheme = state.widgetThemes.forKind(widgetKind)
    var editingKey by remember { mutableStateOf<String?>(null) }
    val calendarUiPrefs = loadCalendarWidgetUiPrefs(context)

    fun updateCalendarUiPrefs(update: (CalendarWidgetUiPrefs) -> CalendarWidgetUiPrefs) {
        if (widgetKind != WidgetKind.CALENDAR) return
        saveCalendarWidgetUiPrefs(context, update(loadCalendarWidgetUiPrefs(context)))
        PillWidgetCalendar.requestUpdate(context)
    }

    val themedBitmap = remember(state.days, state.pillType, currentTheme, widgetKind) {
        when (widgetKind) {
            WidgetKind.SMALL -> WidgetBitmapRenderer.render(
                context = context,
                pillLabel = state.pillType.displayName,
                takenPills = state.days.count { it.status == PillStatus.TAKEN },
                totalPills = state.days.size,
                theme = currentTheme
            )
            WidgetKind.MEDIUM -> WidgetMediumBitmapRenderer.render(
                context = context,
                totalPills = state.days.size,
                takenPills = state.days.count { it.status == PillStatus.TAKEN },
                missedPills = state.days.count { it.status == PillStatus.MISSED },
                widthDp = 240,
                heightDp = 150,
                theme = currentTheme
            )
            WidgetKind.CALENDAR -> {
                val uiPrefs = loadCalendarWidgetUiPrefs(context)
                WidgetCalendarBitmapRenderer.render(
                    context = context,
                    year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                    dayStatus = state.days.associate { d ->
                        java.util.Calendar.getInstance().apply { time = d.date }.get(java.util.Calendar.DAY_OF_MONTH) to d.status
                    },
                    missedCount = state.days.count { it.status == PillStatus.MISSED },
                    widthDp = 320,
                    heightDp = 230,
                    theme = currentTheme,
                    todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH),
                    todayIndicatorColor = uiPrefs.todayIndicatorColor,
                    todayPulseScale = 1f
                )
            }
        }
    }

    val title = when (widgetKind) {
        WidgetKind.SMALL -> "Small Widget Theme"
        WidgetKind.MEDIUM -> "Medium Widget Theme"
        WidgetKind.CALENDAR -> "Calendar Widget Theme"
    }

    fun updateColor(key: String, color: Int) {
        val updated = when (key) {
            "background" -> currentTheme.copy(background = color)
            "accent1" -> currentTheme.copy(accent1 = color)
            "accent2" -> currentTheme.copy(accent2 = color)
            "textPrimary" -> currentTheme.copy(textPrimary = color)
            else -> currentTheme.copy(textSecondary = color)
        }
        viewModel.updateWidgetTheme(widgetKind, updated)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        val isTablet = maxWidth >= 480.dp
        val padH = if (isTablet) 32.dp else 16.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().background(gradient).statusBarsPadding()
                    .padding(horizontal = padH, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isTablet) 22.sp else 18.sp)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = padH, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    androidx.compose.foundation.Image(
                        bitmap = themedBitmap.asImageBitmap(),
                        contentDescription = "Widget preview",
                        modifier = Modifier.fillMaxWidth().height(if (widgetKind == WidgetKind.SMALL) 200.dp else 240.dp)
                            .padding(12.dp)
                    )
                }

                ColorRow("Background color", currentTheme.background) { editingKey = "background" }
                ColorRow("Accent / bar color", currentTheme.accent1) { editingKey = "accent1" }
                ColorRow("Accent 2", currentTheme.accent2) { editingKey = "accent2" }
                ColorRow("Primary text color", currentTheme.textPrimary) { editingKey = "textPrimary" }
                ColorRow("Secondary text color", currentTheme.textSecondary) { editingKey = "textSecondary" }

                if (widgetKind == WidgetKind.CALENDAR) {
                    ToggleRow(
                        label = "Show quick buttons",
                        checked = calendarUiPrefs.showActionButtons,
                        onChange = { checked ->
                            updateCalendarUiPrefs { it.copy(showActionButtons = checked) }
                        }
                    )
                    ToggleRow(
                        label = "Pulse today indicator",
                        checked = calendarUiPrefs.todayPulseEnabled,
                        onChange = { checked ->
                            updateCalendarUiPrefs { it.copy(todayPulseEnabled = checked) }
                        }
                    )
                    ColorRow("Taken button color", calendarUiPrefs.takenButtonBackgroundColor) { editingKey = "takenButton" }
                    ColorRow("Not taken button color", calendarUiPrefs.notTakenButtonBackgroundColor) { editingKey = "notTakenButton" }
                    ColorRow("Button text color", calendarUiPrefs.buttonTextColor) { editingKey = "buttonText" }
                    ColorRow("Today indicator color", calendarUiPrefs.todayIndicatorColor) { editingKey = "todayIndicator" }
                }

                OutlinedButton(
                    onClick = { viewModel.updateWidgetTheme(widgetKind, defaultWidgetThemeSettings().forKind(widgetKind)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset to Default") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (editingKey != null) {
        val key = editingKey!!
        val current = when (key) {
            "background" -> currentTheme.background
            "accent1" -> currentTheme.accent1
            "accent2" -> currentTheme.accent2
            "textPrimary" -> currentTheme.textPrimary
            "takenButton" -> calendarUiPrefs.takenButtonBackgroundColor
            "notTakenButton" -> calendarUiPrefs.notTakenButtonBackgroundColor
            "buttonText" -> calendarUiPrefs.buttonTextColor
            "todayIndicator" -> calendarUiPrefs.todayIndicatorColor
            else -> currentTheme.textSecondary
        }
        ColorPickerDialog(
            initialColor = current,
            onDismiss = { editingKey = null },
            onConfirm = { color ->
                when (key) {
                    "takenButton" -> updateCalendarUiPrefs { it.copy(takenButtonBackgroundColor = color) }
                    "notTakenButton" -> updateCalendarUiPrefs { it.copy(notTakenButtonBackgroundColor = color) }
                    "buttonText" -> updateCalendarUiPrefs { it.copy(buttonTextColor = color) }
                    "todayIndicator" -> updateCalendarUiPrefs { it.copy(todayIndicatorColor = color) }
                    else -> updateColor(key, color)
                }
                editingKey = null
            }
        )
    }
}

@Composable
private fun ColorRow(label: String, colorInt: Int, onChange: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF111827))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(colorInt)))
                Spacer(Modifier.size(10.dp))
                Button(onClick = onChange) { Text("Change") }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF111827))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun ColorPickerDialog(initialColor: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var r by remember { mutableFloatStateOf(android.graphics.Color.red(initialColor).toFloat()) }
    var g by remember { mutableFloatStateOf(android.graphics.Color.green(initialColor).toFloat()) }
    var b by remember { mutableFloatStateOf(android.graphics.Color.blue(initialColor).toFloat()) }

    val swatches = listOf(
        Color(0xFFF609BC), Color(0xFFFAB86D), Color(0xFF22C55E), Color(0xFF3B82F6),
        Color(0xFFEF4444), Color(0xFF111827), Color(0xFFFFFFFF), Color(0xFF8B5CF6),
        Color(0xFFF59E0B), Color(0xFF0EA5E9)
    )

    val picked = Color(android.graphics.Color.rgb(r.toInt(), g.toInt(), b.toInt()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    swatches.forEach { sw ->
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(sw)
                                .clickable {
                                    r = sw.red * 255f
                                    g = sw.green * 255f
                                    b = sw.blue * 255f
                                }
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp)).background(picked))
                Text("Red: ${r.toInt()}")
                Slider(value = r, onValueChange = { r = it }, valueRange = 0f..255f)
                Text("Green: ${g.toInt()}")
                Slider(value = g, onValueChange = { g = it }, valueRange = 0f..255f)
                Text("Blue: ${b.toInt()}")
                Slider(value = b, onValueChange = { b = it }, valueRange = 0f..255f)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(android.graphics.Color.rgb(r.toInt(), g.toInt(), b.toInt())) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
