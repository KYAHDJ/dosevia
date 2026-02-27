package com.dosevia.app

// ─────────────────────────────────────────────────────────────────────────────
//  MainActivity.kt  — v13
//
//  Permission gate: both must be ON before the app is usable.
//   1. Battery optimisation exemption  (BatteryOptimisationModal)
//   2. Notification permission         (NotificationPermissionModal)
//
//  Each modal is non-dismissible — no "maybe later". The app content is
//  rendered behind the modal but fully blocked (dialog is not cancellable).
//  Both checks re-run every time the user returns from Settings via onResume,
//  so the modal disappears the moment the permission is granted.
//
//  Order: battery first → then notifications.
//  If battery is still missing, notification modal is not shown yet.
// ─────────────────────────────────────────────────────────────────────────────

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

enum class Screen { HOME, SETTINGS, NOTES, ABOUT_HELP, WIDGET_CUSTOMIZE, WIDGET_THEME_EDITOR }

class MainActivity : ComponentActivity() {

    // ── Notification runtime permission launcher (Android 13+) ────────────────
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result handled reactively via isBatteryOptimized / isNotificationEnabled
            // which are re-read on every recomposition triggered by lifecycle resume
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestExactAlarmPermissionIfNeeded()
        setContent { DoseviaApp(activity = this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // ── Permission checks (called on every recompose / resume) ────────────────

    /** True when the OS is still battery-optimising this app (bad — we need exemption). */
    fun isBatteryOptimized(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }

    /** True when notifications are enabled for this app. */
    fun isNotificationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13 POST_NOTIFICATIONS doesn't exist; check channel enabled
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.areNotificationsEnabled()
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun openBatterySettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            // Fallback: open general app settings
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            } catch (_: Exception) {}
        }
    }

    fun requestOrOpenNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!alreadyGranted) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    // Already denied once — send to app notification settings
                    openNotificationSettings()
                } else {
                    requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
        } catch (_: Exception) {
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            } catch (_: Exception) {}
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
                catch (_: Exception) {}
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DoseviaApp — navigation root + permission gate
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DoseviaApp(activity: MainActivity) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Use applicationContext so the ViewModel is never re-created on recomposition.
            // LocalContext.current returns the Activity which can change; applicationContext is stable.
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(activity.applicationContext)
            )
            val context = LocalContext.current
            val authManager = remember { GoogleAuthManager(context.applicationContext) }
            val cloudSyncManager = remember { CloudSyncManager(context.applicationContext) }
            val accountStateRepository = remember { AccountStateRepository.getInstance(context.applicationContext) }
            val syncStateRepository = remember { SyncStateRepository.getInstance(context.applicationContext) }
            val accountUiState by accountStateRepository.accountState.collectAsState()
            val syncState by syncStateRepository.syncState.collectAsState()
            val initialSyncCompleted by syncStateRepository.initialSyncCompleted.collectAsState()
            val showDrivePermissionDialog by accountStateRepository.showDrivePermissionRequiredDialog.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            var showNoBackupDialog by remember { mutableStateOf(false) }

            val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    coroutineScope.launch {
                        val signInResult = authManager.handleSignInResult(result.data)
                        signInResult.onSuccess { }
                    }
                } else {
                    authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
                }
            }

            var currentScreen by remember { mutableStateOf(Screen.HOME) }
            var selectedWidget by remember { mutableStateOf(WidgetKind.SMALL) }

            // Re-evaluate permissions every time app comes back to foreground
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val lifecycleState by lifecycle.currentStateFlow.collectAsState()

            // Re-read on every lifecycle change so modal disappears immediately after grant
            var batteryOk    by remember { mutableStateOf(!activity.isBatteryOptimized()) }
            var notifOk      by remember { mutableStateOf(activity.isNotificationEnabled()) }

            // Recheck whenever lifecycle moves to RESUMED (user returned from Settings)
            LaunchedEffect(lifecycleState) {
                if (lifecycleState == Lifecycle.State.RESUMED) {
                    batteryOk = !activity.isBatteryOptimized()
                    notifOk   = activity.isNotificationEnabled()
                    viewModel.refreshWidgets()
                }
            }

            // Alarm taken flag recheck
            val fromAlarm = activity.intent?.getBooleanExtra("from_alarm", false) == true
            LaunchedEffect(fromAlarm) {
                if (fromAlarm) viewModel.recheckAlarmTakenFlag()
            }

            BackHandler(enabled = currentScreen != Screen.HOME) {
                currentScreen = Screen.HOME
            }

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { scaffoldPadding ->
            // ── App content (always rendered, gated by modals above it) ────────
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == Screen.HOME) {
                        (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(280)) { it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(200)))
                    }
                },
                label = "screenNav",
                modifier = Modifier.padding(scaffoldPadding)
            ) { screen ->
                when (screen) {
                    Screen.HOME     -> HomeScreen(
                        viewModel = viewModel,
                        accountUiState = accountUiState,
                        onSignInClick = { signInLauncher.launch(authManager.getSignInIntent()) },
                        onSignOutClick = {
                            authManager.signOut()
                            cloudSyncManager.clearSyncMetadataAndStopWork()
                        },
                        onNavigate = { currentScreen = it }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        accountUiState = accountUiState,
                        syncState = syncState,
                        onBack = { currentScreen = Screen.HOME },
                        onOpenWidgetCustomize = { currentScreen = Screen.WIDGET_CUSTOMIZE },
                        onOpenAboutHelp = { currentScreen = Screen.ABOUT_HELP },
                        onDeleteAccount = {
                            authManager.signOut()
                            cloudSyncManager.clearSyncMetadataAndStopWork()
                            context.getSharedPreferences("dosevia_prefs", MODE_PRIVATE).edit().clear().commit()
                            context.getSharedPreferences("dosevia_status", MODE_PRIVATE).edit().clear().commit()
                            context.getSharedPreferences("sync_state_prefs", MODE_PRIVATE).edit().clear().commit()
                            cancelAlarm(context)
                            context.getSharedPreferences("dosevia_prefs", MODE_PRIVATE).edit().remove("alarm_taken_date").commit()
                            (context as? android.app.Activity)?.recreate()
                        },
                        onSyncNow = {
                            cloudSyncManager.syncNow { result ->
                                when (result) {
                                    SyncNowResult.RESTORED -> {
                                        viewModel.reloadFromPrefs()
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Data restored successfully") }
                                    }
                                    SyncNowResult.NO_BACKUP_FOUND -> showNoBackupDialog = true
                                    SyncNowResult.ERROR -> coroutineScope.launch { snackbarHostState.showSnackbar("Sync failed") }
                                    else -> Unit
                                }
                            }
                        }
                    )
                    Screen.NOTES    -> NotesScreen(viewModel, onBack = { currentScreen = Screen.HOME })
                    Screen.ABOUT_HELP -> AboutHelpScreen(
                        onBack = { currentScreen = Screen.SETTINGS }
                    )
                    Screen.WIDGET_CUSTOMIZE -> WidgetCustomizeScreen(
                        onBack = { currentScreen = Screen.SETTINGS },
                        onSelectWidget = { kind ->
                            selectedWidget = kind
                            currentScreen = Screen.WIDGET_THEME_EDITOR
                        }
                    )
                    Screen.WIDGET_THEME_EDITOR -> WidgetThemeEditorScreen(
                        viewModel = viewModel,
                        widgetKind = selectedWidget,
                        onBack = { currentScreen = Screen.WIDGET_CUSTOMIZE }
                    )
                }
            }

            }

            // ── Step 1: Battery gate — shown first, blocks everything ──────────
            if (!batteryOk) {
                PermissionBlockerModal(
                    icon        = Icons.Default.BatteryAlert,
                    iconTint    = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D))),
                    title       = "Allow Background Activity",
                    body        = "Dosevia needs to be excluded from battery optimisation " +
                                  "so your medication alarm fires reliably — even when the " +
                                  "phone is asleep or the app is closed.\n\n" +
                                  "Tap \"Go to Settings\", then select " +
                                  "\"Don't optimise\" or \"Unrestricted\" for Dosevia.",
                    buttonLabel = "Go to Settings",
                    onButton    = { activity.openBatterySettings() }
                )
            }
            // ── Step 2: Notification gate — shown only after battery is OK ─────
            else if (!notifOk) {
                PermissionBlockerModal(
                    icon        = Icons.Default.NotificationsOff,
                    iconTint    = Brush.linearGradient(listOf(Color(0xFF6200A0), Color(0xFFF609BC))),
                    title       = "Enable Notifications",
                    body        = "Dosevia needs notification permission to show your " +
                                  "alarm on the lock screen and in the notification bar " +
                                  "when it's time to take your pill.\n\n" +
                                  "Tap \"Enable Notifications\" and allow notifications " +
                                  "for Dosevia.",
                    buttonLabel = "Enable Notifications",
                    onButton    = { activity.requestOrOpenNotificationPermission() }
                )
            }

            val showInitialSyncModal = accountUiState.isSignedIn && !initialSyncCompleted

            if (showInitialSyncModal) {
                AlertDialog(
                    onDismissRequest = {},
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    ),
                    title = { Text("Sync your data") },
                    text = { Text("To continue, choose whether to sync from cloud backup now.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                cloudSyncManager.syncNow { result ->
                                    when (result) {
                                        SyncNowResult.RESTORED -> {
                                            viewModel.reloadFromPrefs()
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Data restored successfully") }
                                        }
                                        SyncNowResult.NO_BACKUP_FOUND -> showNoBackupDialog = true
                                        SyncNowResult.ERROR -> coroutineScope.launch { snackbarHostState.showSnackbar("Sync failed") }
                                        else -> Unit
                                    }
                                }
                            }
                        ) { Text("Sync Now") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { syncStateRepository.completeInitialChoiceWithoutAutoUpload() }
                        ) { Text("Do Not Sync") }
                    }
                )
            }

            if (showNoBackupDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    ),
                    title = { Text("No backup found.") },
                    text = { Text("No cloud backup exists for this account.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                cloudSyncManager.createBackupNow { result ->
                                    when (result) {
                                        SyncNowResult.BACKUP_CREATED -> {
                                            showNoBackupDialog = false
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Backup created") }
                                        }
                                        SyncNowResult.ERROR -> coroutineScope.launch { snackbarHostState.showSnackbar("Backup failed") }
                                        else -> Unit
                                    }
                                }
                            }
                        ) { Text("Create Backup") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoBackupDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showDrivePermissionDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Google Drive permission required") },
                    text = { Text("Google Drive permission is required to sign in and sync your data.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                accountStateRepository.consumeDrivePermissionRequiredDialog()
                                signInLauncher.launch(authManager.getSignInIntent())
                            }
                        ) {
                            Text("Try again")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { accountStateRepository.consumeDrivePermissionRequiredDialog() }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PermissionBlockerModal
//
//  Non-dismissible full-screen-blocking dialog. No back button, no dismiss on
//  outside tap. The user MUST grant the permission to proceed.
//  Re-used for both battery and notification gates with different copy/icons.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionBlockerModal(
    icon: ImageVector,
    iconTint: Brush,
    title: String,
    body: String,
    buttonLabel: String,
    onButton: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* intentionally blocked — user must grant */ },
        properties = DialogProperties(
            dismissOnBackPress      = false,
            dismissOnClickOutside   = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {

                    // Gradient icon badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(iconTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(38.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text       = title,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF111827),
                        textAlign  = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text       = body,
                        fontSize   = 14.sp,
                        color      = Color(0xFF6B7280),
                        textAlign  = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(28.dp))

                    // Gradient CTA button
                    val gradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(gradient)
                    ) {
                        Button(
                            onClick   = onButton,
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(14.dp),
                            colors    = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(
                                text       = buttonLabel,
                                color      = Color.White,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Required label — makes clear there's no skipping
                    Text(
                        text      = "Required to use Dosevia",
                        fontSize  = 12.sp,
                        color     = Color(0xFFD1D5DB),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
