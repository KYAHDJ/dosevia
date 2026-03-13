package com.dosevia.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val PREF_KEY_RECENT_WIDGET_COLORS = "recent_widget_colors"

/**
 * Stored in [PREFS_DOSEVIA] so it automatically participates in the app's existing
 * Google Drive prefs backup/restore flow.
 */
private fun loadRecentWidgetColors(context: Context): List<Int> {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_DOSEVIA, Context.MODE_PRIVATE)
    val raw = prefs.getString(PREF_KEY_RECENT_WIDGET_COLORS, "")?.trim().orEmpty()
    if (raw.isBlank()) return emptyList()
    return raw.split(',')
        .mapNotNull { token ->
            val t = token.trim()
            if (t.isEmpty()) null else t.toIntOrNull()
        }
        .distinct()
        .take(20)
}

private fun pushRecentWidgetColor(context: Context, colorInt: Int) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_DOSEVIA, Context.MODE_PRIVATE)
    val current = loadRecentWidgetColors(context).toMutableList()

    // Move to front (most recent first)
    current.remove(colorInt)
    current.add(0, colorInt)

    val trimmed = current.take(20)
    prefs.edit().putString(PREF_KEY_RECENT_WIDGET_COLORS, trimmed.joinToString(",")).apply()
}

@Composable
private fun CalendarWidgetPreview(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    uiPrefs: CalendarWidgetUiPrefs,
    modifier: Modifier = Modifier
) {
    // IMPORTANT:
    // The calendar bitmap renderer reserves a "footer" area at the bottom INSIDE the same
    // rounded widget background where the action buttons live in the actual widget.
    // If we place buttons below the bitmap, they appear "outside" the widget.
    // So we overlay the buttons within the bitmap bounds.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = "Calendar widget preview",
            modifier = Modifier.fillMaxSize()
        )

        if (uiPrefs.showActionButtons) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Match the real widget proportions (reference image):
                    // slimmer buttons, a bit more side margin, slightly less bottom padding.
                    // Further tuned to match the provided preview PNG:
                    // ~10% smaller buttons + slightly higher placement.
                    // Move the row up a bit so it sits centered in the pink footer area
                    // (not hugging the widget bottom edge).
                    .padding(start = 24.dp, end = 24.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(41.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(uiPrefs.notTakenButtonBackgroundColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not Taken",
                        color = Color(uiPrefs.buttonTextColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(41.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(uiPrefs.takenButtonBackgroundColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Taken",
                        color = Color(uiPrefs.buttonTextColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

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

    // Calendar widget has extra UI prefs stored separately from the theme colors.
    // Keep them as Compose state so the preview + controls update immediately.
    var calendarUiPrefs by remember { mutableStateOf(loadCalendarWidgetUiPrefs(context)) }

    fun updateCalendarUiPrefs(update: (CalendarWidgetUiPrefs) -> CalendarWidgetUiPrefs) {
        if (widgetKind != WidgetKind.CALENDAR) return
        val updated = update(loadCalendarWidgetUiPrefs(context))
        saveCalendarWidgetUiPrefs(context, updated)
        calendarUiPrefs = updated
        PillWidgetCalendar.requestUpdate(context)
    }

    val themedBitmap = remember(state.days, state.pillType, currentTheme, widgetKind, calendarUiPrefs) {
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
                    todayIndicatorColor = calendarUiPrefs.todayIndicatorColor,
                    // The actual widget animates the pulse via periodic updates.
                    // For the in-app preview, show a slightly "pulsed" indicator when enabled.
                    todayPulseScale = if (calendarUiPrefs.todayPulseEnabled) 1.08f else 1f
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(top = 8.dp)
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
                    if (widgetKind == WidgetKind.CALENDAR) {
                        CalendarWidgetPreview(
                            bitmap = themedBitmap.asImageBitmap(),
                            uiPrefs = calendarUiPrefs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            bitmap = themedBitmap.asImageBitmap(),
                            contentDescription = "Widget preview",
                            modifier = Modifier.fillMaxWidth().height(if (widgetKind == WidgetKind.SMALL) 200.dp else 240.dp)
                                .padding(12.dp)
                        )
                    }
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
                    onClick = {
                        // Reset theme colors for the selected widget.
                        viewModel.updateWidgetTheme(widgetKind, defaultWidgetThemeSettings().forKind(widgetKind))

                        // Calendar widget also has extra UI prefs (action buttons, button colors,
                        // today indicator, etc.) stored separately. Reset those too.
                        if (widgetKind == WidgetKind.CALENDAR) {
                            val defaults = CalendarWidgetUiPrefs()
                            saveCalendarWidgetUiPrefs(context, defaults)
                            calendarUiPrefs = defaults
                            PillWidgetCalendar.requestUpdate(context)
                        }
                    },
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
            recentColors = loadRecentWidgetColors(context),
            onDismiss = { editingKey = null },
            onConfirm = { color ->
                when (key) {
                    "takenButton" -> updateCalendarUiPrefs { it.copy(takenButtonBackgroundColor = color) }
                    "notTakenButton" -> updateCalendarUiPrefs { it.copy(notTakenButtonBackgroundColor = color) }
                    "buttonText" -> updateCalendarUiPrefs { it.copy(buttonTextColor = color) }
                    "todayIndicator" -> updateCalendarUiPrefs { it.copy(todayIndicatorColor = color) }
                    else -> updateColor(key, color)
                }

                // Persist recent colors for easy re-use. Stored in SharedPreferences so it
                // automatically syncs via the existing Google Drive prefs backup.
                pushRecentWidgetColor(context, color)

                editingKey = null
            }
        )
    }
}

private fun responsiveThemeLabel(label: String): String {
    val words = label.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.size <= 1 -> label
        else -> words.dropLast(1).joinToString(" ") + "\n" + words.last()
    }
}

@Composable
private fun ColorRow(label: String, colorInt: Int, onChange: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    responsiveThemeLabel(label),
                    color = Color(0xFF111827),
                    lineHeight = 18.sp
                )
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(colorInt)))
            }
            Button(
                onClick = {
                    UiSoundPlayer.playPrimaryAction()
                    onChange()
                },
                modifier = Modifier.widthIn(min = 104.dp, max = 104.dp)
            ) { Text("Change", maxLines = 1) }
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
            Switch(checked = checked, onCheckedChange = { UiSoundPlayer.playToggle(it); onChange(it) })
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Int,
    recentColors: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Presets (grid) — avoids the broken horizontal row.
    val presets = listOf(
        Color(0xFFF609BC), Color(0xFFFAB86D), Color(0xFF22C55E), Color(0xFF3B82F6),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFF0EA5E9),
        Color(0xFF111827), Color(0xFF6B7280), Color(0xFFFFFFFF), Color(0xFFF3F4F6)
    )

    // HSV state
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }   // 0..360
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }   // 0..1
    var value by remember { mutableFloatStateOf(initialHsv[2]) } // 0..1

    fun currentArgb(): Int = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))

    var hexText by remember { mutableStateOf("#" + "%06X".format(0xFFFFFF and currentArgb())) }

    fun syncHexFromState() {
        hexText = "#" + "%06X".format(0xFFFFFF and currentArgb())
    }

    fun applyArgbToState(argb: Int) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
        syncHexFromState()
    }

    val picked = Color(currentArgb())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick color") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(picked)
                )

                // Hex input
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase().trim().replace(" ", "")
                        hexText = if (cleaned.startsWith("#")) cleaned else "#${cleaned}"
                        val candidate = hexText.removePrefix("#")
                        if (candidate.length == 6 && candidate.all { it in '0'..'9' || it in 'A'..'F' }) {
                            try {
                                val rgb = candidate.toInt(16)
                                val argb = 0xFF000000.toInt() or rgb
                                applyArgbToState(argb)
                            } catch (_: Exception) { }
                        }
                    },
                    label = { Text("Hex") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )

                // Presets
                Text("Presets", fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                ) {
                    items(presets) { sw ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(sw)
                                .clickable { applyArgbToState(sw.toArgb()) }
                        )
                    }
                }

                HorizontalDivider()

                // "Color wheel" section (HSV)
                Text("Color wheel", fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                SatValueSquare(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onChange = { s, v ->
                        sat = s
                        value = v
                        syncHexFromState()
                    }
                )

                Text("Hue: ${hue.roundToInt()}°")
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        syncHexFromState()
                    },
                    valueRange = 0f..360f
                )

                // Fine tune RGB (still useful)
                val argb = currentArgb()
                var r by remember(argb) { mutableFloatStateOf(android.graphics.Color.red(argb).toFloat()) }
                var g by remember(argb) { mutableFloatStateOf(android.graphics.Color.green(argb).toFloat()) }
                var b by remember(argb) { mutableFloatStateOf(android.graphics.Color.blue(argb).toFloat()) }
                fun applyRgb() { applyArgbToState(android.graphics.Color.rgb(r.toInt(), g.toInt(), b.toInt())) }

                Text("Fine tune (RGB)", fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                Text("Red: ${r.toInt()}")
                Slider(value = r, onValueChange = { r = it; applyRgb() }, valueRange = 0f..255f)
                Text("Green: ${g.toInt()}")
                Slider(value = g, onValueChange = { g = it; applyRgb() }, valueRange = 0f..255f)
                Text("Blue: ${b.toInt()}")
                Slider(value = b, onValueChange = { b = it; applyRgb() }, valueRange = 0f..255f)

                // Recent colors at the very bottom
                if (recentColors.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Recent", fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        recentColors.take(5).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .clickable { applyArgbToState(c) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(currentArgb()) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SatValueSquare(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Classic HSV S/V picker square.
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val s = (offset.x / w).coerceIn(0f, 1f)
                    val v = (1f - (offset.y / h)).coerceIn(0f, 1f)
                    onChange(s, v)
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val s = (change.position.x / w).coerceIn(0f, 1f)
                    val v = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                    onChange(s, v)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )

        // Thumb
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val x = sat.coerceIn(0f, 1f) * size.width
            val y = (1f - value.coerceIn(0f, 1f)) * size.height
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 12.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}
