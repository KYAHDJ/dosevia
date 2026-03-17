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
import android.content.Context
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
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.runtime.saveable.rememberSaveable

enum class Screen { HOME, SETTINGS, NOTES, ABOUT_HELP, ACHIEVEMENTS, WIDGET_CUSTOMIZE, WIDGET_THEME_EDITOR, PREMIUM, PRO_OFFER, LIFETIME_OFFER }

class MainActivity : ComponentActivity() {

    // ── Notification runtime permission launcher (Android 13+) ────────────────
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result handled reactively via isBatteryOptimized / isNotificationEnabled
            // which are re-read on every recomposition triggered by lifecycle resume
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiSoundPlayer.init(this)
        enableEdgeToEdge()
        requestExactAlarmPermissionIfNeeded()
        AlarmReliability.syncReliabilityNudges(this)
        setContent { DoseviaApp(activity = this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // ── Permission checks (called on every recompose / resume) ────────────────

    /** True when the OS is still battery-optimising this app (bad — we need exemption). */
    fun isBatteryOptimized(): Boolean = AlarmReliability.isBatteryOptimized(this)

    /** True when notifications are enabled for this app. */
    fun isNotificationEnabled(): Boolean = AlarmReliability.canPostNotifications(this)

    /** True when the OS allows exact alarms (Android 12+). */
    fun isExactAlarmAllowed(): Boolean = AlarmReliability.isExactAlarmAllowed(this)

    // ── Actions ───────────────────────────────────────────────────────────────

    fun openBatterySettings() {
        AlarmReliability.openBestEffortBatterySettings(this)
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

    fun requestExactAlarmPermissionIfNeeded() {
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
            val billingManager = remember { BillingManager(context.applicationContext) }
            val authManager = remember { GoogleAuthManager(context.applicationContext) }
            val cloudSyncManager = remember { CloudSyncManager(context.applicationContext) }
            val accountStateRepository = remember { AccountStateRepository.getInstance(context.applicationContext) }
            val syncStateRepository = remember { SyncStateRepository.getInstance(context.applicationContext) }
            val accountUiState by accountStateRepository.accountState.collectAsState()
            val syncState by syncStateRepository.syncState.collectAsState()
            val initialSyncCompleted by syncStateRepository.initialSyncCompleted.collectAsState()
            val showDrivePermissionDialog by accountStateRepository.showDrivePermissionRequiredDialog.collectAsState()
            val userTier by viewModel.userTier.collectAsState()
            val connectivity = remember { ConnectivityMonitor(context.applicationContext) }
            val isOnline by connectivity.isOnline.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            val inAppUpdateController = remember { InAppUpdateController(context.applicationContext) }
            var updateAvailable by remember { mutableStateOf(false) }
            var showNoBackupDialog by remember { mutableStateOf(false) }
            var showRestoreFromCloudConfirmDialog by remember { mutableStateOf(false) }
            var showCreateBackupFromDeviceDialog by remember { mutableStateOf(false) }

            // ── Google Play Billing bootstrap ───────────────────────────────────
            LaunchedEffect(accountUiState.email) {
                billingManager.start(accountUiState.email)
                billingManager.restorePurchases()
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        billingManager.start(accountUiState.email)
                        billingManager.restorePurchases()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val tierFromBilling by billingManager.tierFromBilling.collectAsState()
            val billingReady by billingManager.billingReady.collectAsState()
            val proDetails by billingManager.proDetails.collectAsState()
            val lifetimeDetails by billingManager.lifetimeDetails.collectAsState()
            val purchaseEvent by billingManager.purchaseEvent.collectAsState()
            val billingResolvedForSignedInAccount = !accountUiState.isSignedIn || (billingReady && tierFromBilling != null)
            val fullscreenAdShowing by AdMobExtras.fullscreenAdShowing.collectAsState()
            val fullscreenAdPending by AdMobExtras.fullscreenAdPending.collectAsState()
            val isAnyFullscreenAdActive = fullscreenAdShowing || fullscreenAdPending
            val canShowAdsForCurrentSession = billingResolvedForSignedInAccount &&
                    AdFeaturePrefs.shouldShowAnyAds(context.applicationContext, userTier)


            LaunchedEffect(purchaseEvent.timestamp) {
                if (purchaseEvent.timestamp > 0L) {
                    UiSoundPlayer.playPurchaseSuccess()
                }
            }

            LaunchedEffect(tierFromBilling) {
                // Billing is the source of truth for premium access.
                // This allows canceled / expired subscriptions to downgrade back to FREE
                // even if older local or synced data still says PRO.
                tierFromBilling?.let { tier ->
                    if (tier != userTier) {
                        viewModel.setUserTier(tier)
                    }
                }
            }

            // ── One-time onboarding gate (fresh install / cleared app data) ─────────
            var onboardingCompleted by remember {
                mutableStateOf(OnboardingPrefs.isCompleted(context.applicationContext))
            }

            fun runSyncNowFlow() {
                if (!syncState.autoUploadEnabled) {
                    cloudSyncManager.checkCloudBackupExists { exists, error ->
                        if (error != null) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Sync failed") }
                            return@checkCloudBackupExists
                        }
                        if (exists) {
                            showRestoreFromCloudConfirmDialog = true
                        } else {
                            showCreateBackupFromDeviceDialog = true
                        }
                    }
                    return
                }

                cloudSyncManager.syncNow { result ->
                    when (result) {
                        SyncNowResult.RESTORED -> {
                            viewModel.reloadFromPrefs()
                            billingManager.restorePurchases()
                            AchievementsManager.recordSync(context.applicationContext)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Data restored successfully") }
                        }
                        SyncNowResult.NO_BACKUP_FOUND -> showNoBackupDialog = true
                        SyncNowResult.ERROR -> coroutineScope.launch { snackbarHostState.showSnackbar("Sync failed") }
                        else -> Unit
                    }
                }
            }

            val signInLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    coroutineScope.launch {
                        val signInResult = authManager.handleSignInResult(result.data)
                        signInResult
                            .onSuccess { signedInAccount ->
                                billingManager.start(signedInAccount.email)
                                billingManager.restorePurchases()
                                viewModel.reloadFromPrefs()
                            }
                            .onFailure {
                                authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
                            }
                    }
                } else {
                    authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
                }
            }

            var currentScreen by remember { mutableStateOf(Screen.HOME) }
            var premiumReturnScreen by remember { mutableStateOf(Screen.HOME) }
            var premiumEntryScreen by remember { mutableStateOf(Screen.HOME) }
            var selectedWidget by remember { mutableStateOf(WidgetKind.SMALL) }

            // ── App-open ad (show only once per actual app launch) ──────────
            var showAppOpenAd by remember { mutableStateOf(false) }
            var appOpenHandledThisLaunch by rememberSaveable { mutableStateOf(false) }
            var startupAdDecisionSettled by remember { mutableStateOf(false) }

            LaunchedEffect(onboardingCompleted, billingReady, userTier, isOnline, billingResolvedForSignedInAccount, canShowAdsForCurrentSession) {
                if (!onboardingCompleted) {
                    showAppOpenAd = false
                    startupAdDecisionSettled = false
                    return@LaunchedEffect
                }

                if (!billingResolvedForSignedInAccount) {
                    showAppOpenAd = false
                    startupAdDecisionSettled = false
                    return@LaunchedEffect
                }

                if (!canShowAdsForCurrentSession || !isOnline) {
                    showAppOpenAd = false
                    startupAdDecisionSettled = true
                    return@LaunchedEffect
                }

                if (!appOpenHandledThisLaunch) {
                    showAppOpenAd = true
                    appOpenHandledThisLaunch = true
                    startupAdDecisionSettled = false
                }
            }

            LaunchedEffect(canShowAdsForCurrentSession, billingResolvedForSignedInAccount) {
                if (!billingResolvedForSignedInAccount || !canShowAdsForCurrentSession) {
                    showAppOpenAd = false
                    startupAdDecisionSettled = billingResolvedForSignedInAccount
                    AdMobExtras.clearAppOpenState()
                }
            }

            // Re-evaluate permissions every time app comes back to foreground
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val lifecycleState by lifecycle.currentStateFlow.collectAsState()

            // Re-read on every lifecycle change so modal disappears immediately after grant
            var batteryOk    by remember { mutableStateOf(!activity.isBatteryOptimized()) }
            var exactOk      by remember { mutableStateOf(activity.isExactAlarmAllowed()) }
            var notifOk      by remember { mutableStateOf(activity.isNotificationEnabled()) }

            // Tutorial (runs once, after permission gates)
            var tutorialDone by remember {
                mutableStateOf(TutorialPrefs.isCompleted(context.applicationContext))
            }

            val isTutorialActive = onboardingCompleted && batteryOk && exactOk && notifOk && !tutorialDone && currentScreen == Screen.HOME


            // Bottom-of-screen message whenever the plan changes (all plans).
            var lastTier by remember { mutableStateOf<UserTier?>(null) }
            LaunchedEffect(userTier) {
                val prev = lastTier
                if (prev != null && prev != userTier) {
                    val msg = if (userTier == UserTier.FREE && !isTutorialActive) {
                        "Plan updated."
                    } else {
                        "Plan updated — thank you for supporting Dosevia."
                    }
                    snackbarHostState.showSnackbar(message = msg)
                }
                lastTier = userTier
            }

            // Recheck whenever lifecycle moves to RESUMED (user returned from Settings)
            LaunchedEffect(lifecycleState, billingReady, onboardingCompleted, userTier, isOnline, isTutorialActive) {
                if (lifecycleState == Lifecycle.State.RESUMED) {
                    batteryOk = !activity.isBatteryOptimized()
                    exactOk   = activity.isExactAlarmAllowed()
                    notifOk   = activity.isNotificationEnabled()
                    AlarmReliability.syncReliabilityNudges(activity.applicationContext)
                    viewModel.refreshWidgets()
                    inAppUpdateController.checkForAvailableUpdate { available ->
                        updateAvailable = available
                    }
                }
            }

            // Alarm taken flag recheck
            val fromAlarm = activity.intent?.getBooleanExtra("from_alarm", false) == true
            LaunchedEffect(fromAlarm) {
                if (fromAlarm) viewModel.recheckAlarmTakenFlag()
            }

            BackHandler(enabled = currentScreen != Screen.HOME) {
                UiSoundPlayer.playBack()
                currentScreen = when (currentScreen) {
                    Screen.PREMIUM -> premiumEntryScreen
                    Screen.PRO_OFFER, Screen.LIFETIME_OFFER -> premiumReturnScreen
                    Screen.ABOUT_HELP -> Screen.SETTINGS
                    Screen.ACHIEVEMENTS -> Screen.SETTINGS
                    Screen.WIDGET_CUSTOMIZE -> Screen.SETTINGS
                    Screen.WIDGET_THEME_EDITOR -> Screen.WIDGET_CUSTOMIZE
                    else -> Screen.HOME
                }
            }

            if (!onboardingCompleted) {
                OnboardingWelcomeScreen(
                    onContinueSkip = {
                        OnboardingPrefs.setCompleted(context.applicationContext, true)
                        onboardingCompleted = true
                    },
                    onContinueSignIn = {
                        OnboardingPrefs.setCompleted(context.applicationContext, true)
                        onboardingCompleted = true
                        signInLauncher.launch(authManager.getSignInIntent())
                    }
                )
            } else {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        val canShowBannerAds = canShowAdsForCurrentSession &&
                                (!accountUiState.isSignedIn || initialSyncCompleted) &&
                                startupAdDecisionSettled &&
                                !showAppOpenAd &&
                                !isAnyFullscreenAdActive

                        if (canShowBannerAds) {
                            SimulatedBannerAd()
                        }
                    }
                ) { scaffoldPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(scaffoldPadding)
                    ) {
                        AnimatedVisibility(visible = updateAvailable) {
                            AppUpdateInlineBanner(
                                onUpdateClick = {
                                    UiSoundPlayer.playPrimaryAction()
                                    inAppUpdateController.startImmediateUpdate(activity)
                                }
                            )
                        }
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
                            modifier = Modifier.fillMaxSize()
                        ) { screen ->
                            when (screen) {
                                Screen.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    accountUiState = accountUiState,
                                    isOnline = isOnline,
                                    isSyncOffWarningVisible = accountUiState.isSignedIn && !syncState.autoUploadEnabled,
                                    onEnableSyncNow = { UiSoundPlayer.playPrimaryAction(); runSyncNowFlow() },
                                    onSignInClick = { UiSoundPlayer.playPrimaryAction(); signInLauncher.launch(authManager.getSignInIntent()) },
                                    onNavigate = {
                                        UiSoundPlayer.playNavigate()
                                        if (it == Screen.PREMIUM) {
                                            premiumEntryScreen = Screen.HOME
                                            premiumReturnScreen = Screen.HOME
                                        }
                                        currentScreen = it
                                    },
                                    showTutorial = onboardingCompleted && batteryOk && exactOk && notifOk && !tutorialDone,
                                    onTutorialFinished = {
                                        TutorialPrefs.setCompleted(context.applicationContext, true)
                                        tutorialDone = true
                                    },
                                    onTutorialSkipped = {
                                        TutorialPrefs.setCompleted(context.applicationContext, true)
                                        tutorialDone = true
                                    }
                                )

                                Screen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    billingManager = billingManager,
                                    accountUiState = accountUiState,
                                    syncState = syncState,
                                    isOnline = isOnline,
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = Screen.HOME },
                                    onOpenWidgetCustomize = { UiSoundPlayer.playNavigate(); currentScreen = Screen.WIDGET_CUSTOMIZE },
                                    onOpenAboutHelp = { UiSoundPlayer.playNavigate(); currentScreen = Screen.ABOUT_HELP },
                                    onOpenAchievements = { UiSoundPlayer.playNavigate(); currentScreen = Screen.ACHIEVEMENTS },
                                    onOpenUpgrade = {
                                        premiumEntryScreen = Screen.SETTINGS
                                        premiumReturnScreen = Screen.SETTINGS
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.PREMIUM
                                    },
                                    onOpenProOffer = {
                                        premiumEntryScreen = Screen.SETTINGS
                                        premiumReturnScreen = Screen.SETTINGS
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.PRO_OFFER
                                    },
                                    onOpenLifetimeOffer = {
                                        premiumEntryScreen = Screen.SETTINGS
                                        premiumReturnScreen = Screen.SETTINGS
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.LIFETIME_OFFER
                                    },
                                    onSignInClick = { UiSoundPlayer.playPrimaryAction(); signInLauncher.launch(authManager.getSignInIntent()) },
                                    onSignOut = {
                                        UiSoundPlayer.playPrimaryAction()
                                        authManager.signOut()
                                        cloudSyncManager.clearSyncMetadataAndStopWork()
                                        AppResetter.wipeAllLocalData(context)
                                        viewModel.reloadFromPrefs()
                                        currentScreen = Screen.HOME
                                        (context as? android.app.Activity)?.let { AppResetter.restartApp(it) }
                                    },
                                    onDeleteAccount = {
                                        UiSoundPlayer.playMissed()
                                        authManager.signOut()
                                        cloudSyncManager.clearSyncMetadataAndStopWork()
                                        AppResetter.wipeAllLocalData(context)
                                        currentScreen = Screen.HOME
                                        (context as? android.app.Activity)?.let { AppResetter.restartApp(it) }
                                    },
                                    onSyncNow = { UiSoundPlayer.playPrimaryAction(); runSyncNowFlow() },
                                    onClearBlister = { UiSoundPlayer.playMissed(); viewModel.clearBlister() }
                                )

                                Screen.ACHIEVEMENTS -> AchievementsScreen(
                                    viewModel = viewModel,
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = Screen.SETTINGS }
                                )

                                Screen.PREMIUM -> PremiumHubScreen(
                                    currentTier = userTier,
                                    proDetails = proDetails,
                                    lifetimeDetails = lifetimeDetails,
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = premiumEntryScreen },
                                    onOpenPro = {
                                        premiumReturnScreen = Screen.PREMIUM
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.PRO_OFFER
                                    },
                                    onOpenLifetime = {
                                        premiumReturnScreen = Screen.PREMIUM
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.LIFETIME_OFFER
                                    }
                                )

                                Screen.PRO_OFFER -> ProOfferScreen(
                                    currentTier = userTier,
                                    productDetails = proDetails,
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = premiumReturnScreen },
                                    onSelectPro = {
                                        if (!accountUiState.isSignedIn) {
                                            UiSoundPlayer.playPrimaryAction()
                                            signInLauncher.launch(authManager.getSignInIntent())
                                        } else {
                                            UiSoundPlayer.playPrimaryAction()
                                            billingManager.launchPurchase(activity, BillingProducts.PRO_PRODUCT_ID, accountUiState.email)
                                        }
                                    }
                                )

                                Screen.LIFETIME_OFFER -> LifetimeOfferScreen(
                                    currentTier = userTier,
                                    productDetails = lifetimeDetails,
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = premiumReturnScreen },
                                    onSelectLifetime = {
                                        if (!accountUiState.isSignedIn) {
                                            UiSoundPlayer.playPrimaryAction()
                                            signInLauncher.launch(authManager.getSignInIntent())
                                        } else {
                                            UiSoundPlayer.playPrimaryAction()
                                            billingManager.launchPurchase(activity, BillingProducts.LIFETIME_PRODUCT_ID, accountUiState.email)
                                        }
                                    }
                                )

                                Screen.NOTES -> NotesScreen(viewModel, onBack = { UiSoundPlayer.playBack(); currentScreen = Screen.HOME })
                                Screen.ABOUT_HELP -> AboutHelpScreen(onBack = { UiSoundPlayer.playBack(); currentScreen = Screen.SETTINGS })
                                Screen.WIDGET_CUSTOMIZE -> WidgetCustomizeScreen(
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = Screen.SETTINGS },
                                    onSelectWidget = { kind ->
                                        selectedWidget = kind
                                        UiSoundPlayer.playNavigate()
                                        currentScreen = Screen.WIDGET_THEME_EDITOR
                                    },
                                    onOpenProOffer = {
                                        premiumEntryScreen = Screen.WIDGET_CUSTOMIZE
                                        premiumReturnScreen = Screen.WIDGET_CUSTOMIZE
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.PRO_OFFER
                                    },
                                    onOpenLifetimeOffer = {
                                        premiumEntryScreen = Screen.WIDGET_CUSTOMIZE
                                        premiumReturnScreen = Screen.WIDGET_CUSTOMIZE
                                        UiSoundPlayer.playPrimaryAction()
                                        currentScreen = Screen.LIFETIME_OFFER
                                    },
                                    userTier = userTier
                                )

                                Screen.WIDGET_THEME_EDITOR -> WidgetThemeEditorScreen(
                                    viewModel = viewModel,
                                    widgetKind = selectedWidget,
                                    onBack = { UiSoundPlayer.playBack(); currentScreen = Screen.WIDGET_CUSTOMIZE }
                                )
                            }
                        }
                    }
                }
            }


            LaunchedEffect(onboardingCompleted, showAppOpenAd, canShowAdsForCurrentSession, billingResolvedForSignedInAccount) {
                if (onboardingCompleted && !showAppOpenAd && !canShowAdsForCurrentSession) {
                    startupAdDecisionSettled = billingResolvedForSignedInAccount
                }
            }

            // ── Simulated App-Open Ad overlay (FREE + online) ────────────────
            if (onboardingCompleted && showAppOpenAd && canShowAdsForCurrentSession && isOnline && !isTutorialActive) {
                SimulatedAppOpenAd(
                    minSeconds = 5,
                    onClose = {
                        showAppOpenAd = false
                        startupAdDecisionSettled = true
                    }
                )
            }

            // ── Step 1: Battery gate — shown first, blocks everything ──────────
            if (onboardingCompleted && !batteryOk) {
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
            // ── Step 2: Exact-alarm gate — shown only after battery is OK ─────
            else if (onboardingCompleted && !exactOk) {
                PermissionBlockerModal(
                    icon        = Icons.Default.BatteryAlert,
                    iconTint    = Brush.linearGradient(listOf(Color(0xFF433BFF), Color(0xFF2F27CE))),
                    title       = "Allow Exact Alarms",
                    body        = "Dosevia uses exact alarms so your pill reminder is reliable " +
                            "(even in Doze mode).\n\n" +
                            "Tap \"Allow Exact Alarms\" then enable it for Dosevia.",
                    buttonLabel = "Allow Exact Alarms",
                    onButton    = { activity.requestExactAlarmPermissionIfNeeded() }
                )
            }
            // ── Step 3: Notification gate — shown only after battery + exact alarm are OK ─────
            else if (onboardingCompleted && !notifOk) {
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
                                            billingManager.restorePurchases()
                                            AchievementsManager.recordSync(context.applicationContext)
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Data restored successfully") }

                                            // restart app after sync finishes
                                            val restartIntent = activity.intent
                                            activity.finish()
                                            activity.startActivity(restartIntent)

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
                                            AchievementsManager.recordSync(context.applicationContext)
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

            if (showRestoreFromCloudConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreFromCloudConfirmDialog = false },
                    title = { Text("Restore from cloud?") },
                    text = { Text("This will replace the data on this device with the backup from your account. Any unsynced changes on this device may be lost.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRestoreFromCloudConfirmDialog = false
                                cloudSyncManager.restoreFromCloudNow { result ->
                                    when (result) {
                                        SyncNowResult.RESTORED -> {
                                            viewModel.reloadFromPrefs()
                                            billingManager.restorePurchases()
                                            AchievementsManager.recordSync(context.applicationContext)
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Data restored successfully") }
                                        }
                                        SyncNowResult.ERROR -> coroutineScope.launch { snackbarHostState.showSnackbar("Sync failed") }
                                        else -> Unit
                                    }
                                }
                            }
                        ) { Text("Restore Cloud") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreFromCloudConfirmDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showCreateBackupFromDeviceDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateBackupFromDeviceDialog = false },
                    title = { Text("No cloud backup found") },
                    text = { Text("No cloud backup found. Create one from this device?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCreateBackupFromDeviceDialog = false
                                cloudSyncManager.createBackupNow { result ->
                                    when (result) {
                                        SyncNowResult.BACKUP_CREATED -> {
                                            AchievementsManager.recordSync(context.applicationContext)
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
                        TextButton(onClick = { showCreateBackupFromDeviceDialog = false }) { Text("Cancel") }
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

@Composable
private fun AppUpdateInlineBanner(
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update available",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "A newer Dosevia version is ready. Update now to keep the app current.",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            Button(
                onClick = onUpdateClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF609BC))
            ) {
                Text("Update", color = Color.White)
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
