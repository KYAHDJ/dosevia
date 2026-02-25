package com.dosevia.app

// ─────────────────────────────────────────────────────────────────────────────
//  SettingsScreen.kt  — v15
//
//  Changes vs v14:
//   • "Notification Sound" row now opens a full custom sound picker.
//   • Users can browse their device audio files (any common audio format).
//   • Picked files are copied into the app's private alarm_sounds/ folder.
//   • The picker shows saved sounds (select, preview, delete), a "Default
//     Alarm" option, and a "Silent" option.
//   • Runtime storage permission is requested automatically for Android 13+.
// ─────────────────────────────────────────────────────────────────────────────

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dosevia.app.ui.theme.*
import java.io.File

// ══════════════════════════════════════════════════════════════════════════════
//  Main Settings Screen
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit = {}
) {
    val state    by viewModel.state.collectAsState()
    val settings = state.settings
    val context  = LocalContext.current

    var showTimePicker   by remember { mutableStateOf(false) }
    var showTitleDialog  by remember { mutableStateOf(false) }
    var showSubDialog    by remember { mutableStateOf(false) }
    var showIconPicker   by remember { mutableStateOf(false) }
    var showSoundPicker  by remember { mutableStateOf(false) }
    var tempText         by remember { mutableStateOf("") }

    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
    val masterOn = settings.appActive

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF0FB))) {
        val isTablet = maxWidth >= 480.dp
        val padH     = if (isTablet) 32.dp else 16.dp
        val titleSp  = if (isTablet) 22.sp  else 18.sp
        val secLblSp = if (isTablet) 12.sp  else 10.sp
        val rowLblSp = if (isTablet) 16.sp  else 14.sp
        val rowSubSp = if (isTablet) 13.sp  else 12.sp

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
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
                    Text("Settings", fontSize = titleSp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = padH, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── MASTER SWITCH ─────────────────────────────────────────────
                SettingsSection("MASTER CONTROL", secLblSp) {
                    MasterSwitchRow(
                        enabled  = masterOn,
                        onChange = { viewModel.updateSettings(settings.copy(appActive = it)) },
                        lblSp    = rowLblSp,
                        subSp    = rowSubSp
                    )
                }

                AnimatedVisibility(visible = !masterOn, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsOff, null,
                                tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("All alarms and notifications are disabled.",
                                fontSize = rowSubSp, color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── REMINDER ──────────────────────────────────────────────────
                Box(modifier = Modifier.alpha(if (masterOn) 1f else 0.38f)) {
                    SettingsSection("REMINDER", secLblSp) {

                        SettingsChevronRow(
                            icon    = Icons.Default.Schedule,
                            iconBg  = PinkDark,
                            title   = "Daily Reminder Time",
                            sub     = settings.displayTime,
                            enabled = masterOn,
                            onClick = { if (masterOn) showTimePicker = true },
                            lblSp   = rowLblSp, subSp = rowSubSp
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        SettingsToggleRow(
                            icon     = Icons.Default.NotificationsActive,
                            iconBg   = OrangeAccent,
                            title    = "Placebo Reminder",
                            sub      = "Remind me during placebo / low-dose days",
                            checked  = settings.placeboReminder,
                            enabled  = masterOn,
                            onChange = { if (masterOn) viewModel.updateSettings(settings.copy(placeboReminder = it)) },
                            lblSp    = rowLblSp, subSp = rowSubSp
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        SettingsToggleRow(
                            icon     = Icons.Default.Vibration,
                            iconBg   = PinkPrimary,
                            title    = "Vibration",
                            sub      = "Vibrate when the reminder fires",
                            checked  = settings.vibrationEnabled,
                            enabled  = masterOn,
                            onChange = { if (masterOn) viewModel.updateSettings(settings.copy(vibrationEnabled = it)) },
                            lblSp    = rowLblSp, subSp = rowSubSp
                        )
                    }
                }

                // ── NOTIFICATION TEXT ─────────────────────────────────────────
                Box(modifier = Modifier.alpha(if (masterOn) 1f else 0.38f)) {
                    SettingsSection("NOTIFICATION TEXT", secLblSp) {
                        SettingsChevronRow(
                            icon    = Icons.Default.Title,
                            iconBg  = PinkPrimary,
                            title   = "Notification Title",
                            sub     = settings.notificationTitle,
                            enabled = masterOn,
                            onClick = { if (masterOn) { tempText = settings.notificationTitle; showTitleDialog = true } },
                            lblSp   = rowLblSp, subSp = rowSubSp
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        SettingsChevronRow(
                            icon    = Icons.Default.Message,
                            iconBg  = OrangeAccent,
                            title   = "Notification Subtitle",
                            sub     = settings.notificationSubtitle,
                            enabled = masterOn,
                            onClick = { if (masterOn) { tempText = settings.notificationSubtitle; showSubDialog = true } },
                            lblSp   = rowLblSp, subSp = rowSubSp
                        )
                    }
                }

                // ── ALARM APPEARANCE ──────────────────────────────────────────
                Box(modifier = Modifier.alpha(if (masterOn) 1f else 0.38f)) {
                    SettingsSection("ALARM APPEARANCE", secLblSp) {

                        // Icon picker
                        val currentIconLabel = ICON_OPTIONS
                            .firstOrNull { it.first == settings.notificationIcon }?.second
                            ?: "Medication (pill)"
                        SettingsChevronRow(
                            icon    = notifIconVector(settings.notificationIcon),
                            iconBg  = PinkPrimary,
                            title   = "Alarm Screen Icon",
                            sub     = currentIconLabel,
                            enabled = masterOn,
                            onClick = { if (masterOn) showIconPicker = true },
                            lblSp   = rowLblSp, subSp = rowSubSp
                        )

                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Sound picker — shows current selection name
                        val soundSub = when {
                            settings.notificationSound == "default" -> "Default Alarm"
                            settings.notificationSound == "silent"  -> "Silent (no sound)"
                            else -> soundDisplayName(settings.notificationSound)
                        }
                        SettingsChevronRow(
                            icon    = Icons.Default.MusicNote,
                            iconBg  = OrangeAccent,
                            title   = "Notification Sound",
                            sub     = soundSub,
                            enabled = masterOn,
                            onClick = { if (masterOn) showSoundPicker = true },
                            lblSp   = rowLblSp, subSp = rowSubSp
                        )
                    }
                }

                // ── OTHER ─────────────────────────────────────────────────────
                SettingsSection("OTHER", secLblSp) {
                    SettingsChevronRow(
                        icon    = Icons.Default.Widgets,
                        iconBg  = Color(0xFF8B5CF6),
                        title   = "Customize Widgets",
                        sub     = "Preview small, medium, and calendar widget styles",
                        enabled = true,
                        onClick = { onNavigate(Screen.CUSTOMIZE_WIDGETS) },
                        lblSp   = rowLblSp, subSp = rowSubSp
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    SettingsChevronRow(
                        icon    = Icons.Default.Info,
                        iconBg  = Color(0xFF6B7280),
                        title   = "About & Help",
                        sub     = "App info and support",
                        enabled = true,
                        onClick = { },
                        lblSp   = rowLblSp, subSp = rowSubSp
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    SettingsChevronRow(
                        icon    = Icons.Default.Star,
                        iconBg  = Color(0xFFFBBF24),
                        title   = "Rate App",
                        sub     = "Share your feedback",
                        enabled = true,
                        onClick = { },
                        lblSp   = rowLblSp, subSp = rowSubSp
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showTimePicker) {
        TimePickerDialog(
            initialHour   = settings.dailyReminderHour,
            initialMinute = settings.dailyReminderMinute,
            onDismiss     = { showTimePicker = false },
            onConfirm     = { h, m ->
                viewModel.updateSettings(settings.copy(dailyReminderHour = h, dailyReminderMinute = m))
                showTimePicker = false
            }
        )
    }

    if (showTitleDialog) {
        TextEditDialog(
            title     = "Notification Title",
            value     = tempText,
            onChange  = { tempText = it },
            onDismiss = { showTitleDialog = false },
            onConfirm = { viewModel.updateSettings(settings.copy(notificationTitle = tempText)); showTitleDialog = false }
        )
    }

    if (showSubDialog) {
        TextEditDialog(
            title     = "Notification Subtitle",
            value     = tempText,
            onChange  = { tempText = it },
            onDismiss = { showSubDialog = false },
            onConfirm = { viewModel.updateSettings(settings.copy(notificationSubtitle = tempText)); showSubDialog = false }
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            currentKey = settings.notificationIcon,
            onDismiss  = { showIconPicker = false },
            onSelect   = { key ->
                viewModel.updateSettings(settings.copy(notificationIcon = key))
                showIconPicker = false
            }
        )
    }

    if (showSoundPicker) {
        SoundPickerDialog(
            currentSoundPref = settings.notificationSound,
            onDismiss        = { showSoundPicker = false },
            onSelect         = { soundPref ->
                viewModel.updateSettings(settings.copy(notificationSound = soundPref))
                showSoundPicker = false
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Sound Picker Dialog  — custom file picker + saved sounds manager
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SoundPickerDialog(
    currentSoundPref: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current

    // ── State ─────────────────────────────────────────────────────────────────
    var savedSounds   by remember { mutableStateOf(listSavedSounds(context)) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewPath   by remember { mutableStateOf<String?>(null) }  // null = nothing playing

    // Stop preview when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.apply { if (isPlaying) stop(); release() }
            previewPlayer = null
        }
    }

    fun stopPreview() {
        previewPlayer?.apply { if (isPlaying) stop(); release() }
        previewPlayer = null
        previewPath   = null
    }

    fun startPreview(filePath: String) {
        stopPreview()
        try {
            val file = File(filePath)
            if (!file.exists()) return
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mp = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener { stopPreview() }
            }
            previewPlayer = mp
            previewPath   = filePath
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Permission + File picker launcher ─────────────────────────────────────
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        errorMsg = null
        val result = copyAudioFileToApp(context, uri)
        result.fold(
            onSuccess = { file ->
                savedSounds = listSavedSounds(context)
                onSelect(file.absolutePath)
            },
            onFailure = { e ->
                errorMsg = e.message ?: "Could not import the audio file."
            }
        )
    }

    // Permission launcher (Android 13+ needs READ_MEDIA_AUDIO)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) filePickerLauncher.launch("audio/*")
        else errorMsg = "Storage permission is required to pick audio files."
    }

    fun launchFilePicker() {
        errorMsg = null
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        val granted = ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED

        if (granted) filePickerLauncher.launch("audio/*")
        else permissionLauncher.launch(permission)
    }

    // ── Dialog ────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = { stopPreview(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White,
                                modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Notification Sound", fontSize = 18.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Pick any audio file from your device",
                                fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
                        }
                    }
                }

                // ── Body (scrollable) ─────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // ── Browse button ─────────────────────────────────────────
                    Button(
                        onClick  = { launchFilePicker() },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFF0FB)
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, null,
                            tint = PinkPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Device for Audio File",
                            color = PinkPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // Supported formats note
                    Text(
                        "Supported: MP3, M4A, WAV, OGG, FLAC, AAC, OPUS, WMA, AMR, 3GP",
                        fontSize = 11.sp, color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Error message
                    if (errorMsg != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null,
                                    tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(errorMsg!!, fontSize = 12.sp, color = Color(0xFFB91C1C))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Built-in options ──────────────────────────────────────
                    Text("BUILT-IN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9CA3AF), letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp))

                    SoundOptionRow(
                        icon       = Icons.Default.VolumeUp,
                        label      = "Default Alarm",
                        subLabel   = "System default alarm sound",
                        isSelected = currentSoundPref == "default",
                        isPlaying  = false,
                        onSelect   = { stopPreview(); onSelect("default") },
                        onPreview  = null,   // can't preview default from here easily
                        onDelete   = null
                    )

                    SoundOptionRow(
                        icon       = Icons.Default.VolumeOff,
                        label      = "Silent",
                        subLabel   = "No sound — vibration only",
                        isSelected = currentSoundPref == "silent",
                        isPlaying  = false,
                        onSelect   = { stopPreview(); onSelect("silent") },
                        onPreview  = null,
                        onDelete   = null
                    )

                    // ── Saved custom sounds ───────────────────────────────────
                    if (savedSounds.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("MY SOUNDS  (${savedSounds.size})",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9CA3AF), letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp))

                        savedSounds.forEach { sound ->
                            val isSelected = currentSoundPref == sound.file.absolutePath
                            val isPlaying  = previewPath == sound.file.absolutePath

                            SoundOptionRow(
                                icon       = Icons.Default.AudioFile,
                                label      = sound.displayName,
                                subLabel   = formatFileSize(sound.file.length()),
                                isSelected = isSelected,
                                isPlaying  = isPlaying,
                                onSelect   = {
                                    stopPreview()
                                    onSelect(sound.file.absolutePath)
                                },
                                onPreview  = {
                                    if (isPlaying) stopPreview()
                                    else startPreview(sound.file.absolutePath)
                                },
                                onDelete   = {
                                    stopPreview()
                                    deleteSound(sound.file)
                                    savedSounds = listSavedSounds(context)
                                    // If deleted sound was selected, revert to default
                                    if (currentSoundPref == sound.file.absolutePath) {
                                        onSelect("default")
                                    }
                                }
                            )
                        }
                    } else {
                        // Empty state hint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF9FAFB))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LibraryMusic, null,
                                    tint = Color(0xFFD1D5DB), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No custom sounds yet",
                                    fontSize = 13.sp, color = Color(0xFF9CA3AF))
                                Text("Tap \"Browse\" above to import a sound",
                                    fontSize = 11.sp, color = Color(0xFFD1D5DB))
                            }
                        }
                    }
                }

                // ── Footer close button ───────────────────────────────────────
                HorizontalDivider(color = Color(0xFFF3F4F6))
                TextButton(
                    onClick  = { stopPreview(); onDismiss() },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Close", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Individual sound option row ───────────────────────────────────────────────

@Composable
private fun SoundOptionRow(
    icon: ImageVector,
    label: String,
    subLabel: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPreview: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFFFFF0FB) else Color(0xFFF9FAFB)
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Icon bubble
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected)
                        Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
                    else
                        Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint     = if (isSelected) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(10.dp))

        // Labels
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PinkPrimary else Color(0xFF111827),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subLabel, fontSize = 11.sp, color = Color(0xFF9CA3AF))
        }

        // Preview button (only for custom files)
        if (onPreview != null) {
            IconButton(
                onClick  = onPreview,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.StopCircle
                                  else Icons.Default.PlayCircle,
                    contentDescription = if (isPlaying) "Stop preview" else "Preview",
                    tint = if (isPlaying) PinkPrimary else Color(0xFF9CA3AF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Selected checkmark
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null,
                tint = PinkPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
        }

        // Delete button (only for custom files)
        if (onDelete != null) {
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, null,
                    tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Format file size helper ───────────────────────────────────────────────────

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024       -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else                -> "%.1f MB".format(bytes / 1024f / 1024f)
}

// ══════════════════════════════════════════════════════════════════════════════
//  Icon Picker Dialog
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun IconPickerDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Medication, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Alarm Screen Icon", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = PinkPrimary)
                        Text("Choose the icon shown on the alarm screen",
                            fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }

                val rows = ICON_OPTIONS.chunked(2)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (key, label) ->
                            val isSelected = key == currentKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected)
                                            Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
                                        else
                                            Brush.linearGradient(listOf(Color(0xFFF3F4F6), Color(0xFFF3F4F6)))
                                    )
                                    .clickable { onSelect(key) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector        = notifIconVector(key),
                                        contentDescription = label,
                                        tint               = if (isSelected) Color.White else Color(0xFF6B7280),
                                        modifier           = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(text = label, fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF374151),
                                        maxLines = 2)
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Reusable UI components
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSection(
    title: String,
    titleSp: androidx.compose.ui.unit.TextUnit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(title, fontSize = titleSp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF9CA3AF), letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)) {
            Column { content() }
        }
    }
}

@Composable
private fun RowIcon(icon: ImageVector, bg: Color, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.75f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun MasterSwitchRow(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    lblSp: androidx.compose.ui.unit.TextUnit,
    subSp: androidx.compose.ui.unit.TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null)
            { onChange(!enabled) }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
                    else         Brush.linearGradient(listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("App Active", fontSize = lblSp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
            Text(
                if (enabled) "All reminders are ON" else "All reminders are OFF",
                fontSize = subSp,
                color = if (enabled) Color(0xFF10B981) else Color(0xFFEF4444),
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = enabled, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White, checkedTrackColor   = PinkPrimary,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFD1D5DB)
            )
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector, iconBg: Color,
    title: String, sub: String,
    checked: Boolean, enabled: Boolean,
    onChange: (Boolean) -> Unit,
    lblSp: androidx.compose.ui.unit.TextUnit,
    subSp: androidx.compose.ui.unit.TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        RowIcon(icon, iconBg)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = lblSp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
            Text(sub,   fontSize = subSp, color = Color(0xFF6B7280))
        }
        Switch(
            checked = checked, onCheckedChange = { if (enabled) onChange(it) }, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = PinkPrimary,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFD1D5DB),
                disabledCheckedThumbColor = Color.White, disabledCheckedTrackColor = Color(0xFFD1D5DB),
                disabledUncheckedThumbColor = Color.White, disabledUncheckedTrackColor = Color(0xFFE5E7EB)
            )
        )
    }
}

@Composable
private fun SettingsChevronRow(
    icon: ImageVector, iconBg: Color,
    title: String, sub: String,
    enabled: Boolean,
    onClick: () -> Unit,
    lblSp: androidx.compose.ui.unit.TextUnit,
    subSp: androidx.compose.ui.unit.TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        RowIcon(icon, iconBg)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = lblSp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
            Text(sub,   fontSize = subSp, color = Color(0xFF6B7280), maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Time Picker Dialog
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int, initialMinute: Int,
    onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(24.dp))
            .background(Color.White).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Daily Reminder Time", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                        Text("Select when to remind you", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
                TimePicker(state = state, colors = TimePickerDefaults.colors(
                    clockDialColor = Color(0xFFFFF0FB),
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = Color(0xFF374151),
                    selectorColor = PinkPrimary, containerColor = Color.White,
                    periodSelectorSelectedContainerColor = PinkPrimary,
                    periodSelectorUnselectedContainerColor = Color(0xFFF3F4F6),
                    periodSelectorSelectedContentColor = Color.White,
                    periodSelectorUnselectedContentColor = Color(0xFF374151),
                    timeSelectorSelectedContainerColor = PinkPrimary,
                    timeSelectorUnselectedContainerColor = Color(0xFFF3F4F6),
                    timeSelectorSelectedContentColor = Color.White,
                    timeSelectorUnselectedContentColor = Color(0xFF374151)
                ))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280))
                    ) { Text("Cancel") }
                    Button(onClick = { onConfirm(state.hour, state.minute) }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Text Edit Dialog
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextEditDialog(
    title: String, value: String,
    onChange: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 18.sp) },
        text  = {
            OutlinedTextField(
                value = value, onValueChange = onChange, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = PinkPrimary,
                    unfocusedBorderColor = Color(0xFFD1D5DB)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                shape  = RoundedCornerShape(10.dp)
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF6B7280)) }
        },
        containerColor = Color.White, shape = RoundedCornerShape(20.dp)
    )
}
