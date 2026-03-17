package com.dosevia.app

import android.app.Activity
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosevia.app.ui.theme.OrangeAccent
import com.dosevia.app.ui.theme.PinkDark
import com.dosevia.app.ui.theme.PinkPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    accountUiState: AccountUiState = AccountUiState(),
    isOnline: Boolean = true,
    isSyncOffWarningVisible: Boolean = false,
    onEnableSyncNow: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onNavigate: (Screen) -> Unit = {},
    showTutorial: Boolean = false,
    onTutorialFinished: () -> Unit = {},
    onTutorialSkipped: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val userTier by viewModel.userTier.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val interstitialAdUnitId = androidx.compose.ui.res.stringResource(R.string.admob_interstitial_unit_id)
    val rewardedAdUnitId = androidx.compose.ui.res.stringResource(R.string.admob_rewarded_unit_id)

    var showPillTypeModal by remember { mutableStateOf(false) }
    var showCustomConfigModal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showQuickNoteCard by remember { mutableStateOf(false) }
    var selectedQuickNote by remember { mutableStateOf<String?>(null) }
    var customQuickNote by remember { mutableStateOf("") }
    var showRewardOfferDialog by remember { mutableStateOf(false) }

    val headerGradient = Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))

    val tutorialSteps = remember {
        listOf(
            TutorialStep("pillType", "Choose pill type", "Select your blister type so Dosevia tracks correctly."),
            TutorialStep("startDate", "Set your start date", "This determines which day you’re on in your blister."),
            TutorialStep("blister", "Mark pills taken", "Tap a day to mark Taken / Missed / None."),
            TutorialStep("reminder", "Daily reminders", "Dosevia will keep alarming until today is marked as taken."),
            TutorialStep("settings", "Settings", "Change reminders, sounds, widgets, sync, and more."),
            TutorialStep("notes", "Notes", "Save notes after taking your pill."),
        )
    }

    LaunchedEffect(Unit) {
        RewardOfferManager.onAppOpened(context.applicationContext)
    }

    LaunchedEffect(userTier, accountUiState.isSignedIn) {
        if (RewardOfferManager.shouldShow(
                context = context.applicationContext,
                isFreeUser = userTier == UserTier.FREE,
                adFreeRewardActive = AdFeaturePrefs.isAdFreeActive(context.applicationContext),
                isSignedInPremium = userTier != UserTier.FREE
            )
        ) {
            showRewardOfferDialog = true
            RewardOfferManager.markShown(context.applicationContext)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val isTablet = screenWidth >= 480.dp

        val headerPadH: Dp = if (isTablet) 32.dp else 16.dp
        val headerPadV: Dp = if (isTablet) 20.dp else 16.dp
        val cardIconSize: Dp = if (isTablet) 48.dp else 40.dp
        val cardIconSizeSm = if (isTablet) 24.dp else 20.dp
        val cardLabelSp = if (isTablet) 13.sp else 11.sp
        val cardValueSp = if (isTablet) 17.sp else 15.sp
        val reminderFontSp = if (isTablet) 17.sp else 15.sp
        val quickIconSize = if (isTablet) 32.dp else 26.dp
        val quickFontSp = if (isTablet) 14.sp else 12.sp

        val scrollState = rememberScrollState()
        var tutorialIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(showTutorial) {
            if (showTutorial) tutorialIndex = 0
        }

        val tutorialStep = tutorialSteps.getOrNull(tutorialIndex)
        val tutorialStepKey = tutorialStep?.key

        LaunchedEffect(showTutorial, tutorialStepKey, scrollState.maxValue) {
            if (!showTutorial) return@LaunchedEffect
            val key = tutorialStepKey ?: return@LaunchedEffect
            val destination = when (key) {
                "pillType", "startDate" -> 0
                "blister" -> (scrollState.maxValue * 0.35f).toInt()
                "reminder", "settings", "notes" -> scrollState.maxValue
                else -> 0
            }
            scrollState.animateScrollTo(destination.coerceIn(0, scrollState.maxValue))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFFF0FB),
                                Color(0xFFFFF8F0),
                                Color(0xFFFFFDE8)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerGradient)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .padding(horizontal = headerPadH, vertical = headerPadV)
                    ) {
                        val showPremiumIcon = remember { kotlin.random.Random.nextInt(5) != 0 }

                        HeaderAccountRow(
                            accountUiState = accountUiState,
                            onSignInClick = onSignInClick,
                            profileName = state.profileName,
                            profilePhotoB64 = state.profilePhotoB64,
                            showPremiumIcon = showPremiumIcon && userTier == UserTier.FREE,
                            onPremiumClick = { onNavigate(Screen.PREMIUM) },
                            premiumModifier = Modifier,
                            onRewardClick = {
                                if (userTier == UserTier.FREE &&
                                    !AdFeaturePrefs.isAdFreeActive(context.applicationContext)
                                ) {
                                    showRewardOfferDialog = true
                                }
                            }
                        )

                        val adFreeUntil = AdFeaturePrefs.getAdFreeUntil(context.applicationContext)
                        if (AdFeaturePrefs.isAdFreeActive(context.applicationContext)) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "Ad-Free ${formatAdFreeRemaining(adFreeUntil)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (!isOnline) {
                            Spacer(Modifier.height(10.dp))
                            WarningPill(
                                text = "No internet connection — syncing is disabled until you're back online.",
                                bg = Color(0xFF7C2D12)
                            )
                        }
                        Spacer(Modifier.height(14.dp))

                        HeaderCard(
                            icon = Icons.Default.Medication,
                            label = "Pill Type (Tap to change)",
                            value = getPillTypeLabel(state.pillType),
                            onClick = { showPillTypeModal = true },
                            modifier = Modifier
                                .tutorialPulse(showTutorial && tutorialStepKey == "pillType"),
                            iconSize = cardIconSize,
                            iconSizeSm = cardIconSizeSm,
                            labelFontSp = cardLabelSp,
                            valueFontSp = cardValueSp
                        )
                        Spacer(Modifier.height(10.dp))

                        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(state.startDate)
                        HeaderCard(
                            icon = Icons.Default.CalendarMonth,
                            label = "Started (Tap to edit)",
                            value = dateStr,
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .tutorialPulse(showTutorial && tutorialStepKey == "startDate"),
                            iconSize = cardIconSize,
                            iconSizeSm = cardIconSizeSm,
                            labelFontSp = cardLabelSp,
                            valueFontSp = cardValueSp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp)
                ) {
                    if (isSyncOffWarningVisible) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = headerPadH)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFB45309))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sync is off. Changes won’t appear on other devices.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(onClick = onEnableSyncNow) {
                                    Text("Enable Sync")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (!accountUiState.isSignedIn) {
                        Text(
                            text = "Sync is off. Changes won’t appear on other devices.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = headerPadH)
                                .alpha(0.9f)
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    QuickNotePromptCard(
                        visible = showQuickNoteCard,
                        selectedNote = selectedQuickNote,
                        customNote = customQuickNote,
                        generatedNotes = QUICK_NOTE_SUGGESTIONS,
                        onSelectNote = {
                            selectedQuickNote = it
                            customQuickNote = ""
                        },
                        onCustomNoteChange = {
                            customQuickNote = it
                            selectedQuickNote = null
                        },
                        onDismiss = {
                            showQuickNoteCard = false
                            selectedQuickNote = null
                            customQuickNote = ""
                        },
                        onSave = {
                            val noteContent = (selectedQuickNote ?: customQuickNote).trim()
                            if (noteContent.isNotEmpty()) {
                                val now = Date()
                                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
                                viewModel.addNote(noteContent, now, time)
                                showQuickNoteCard = false
                                selectedQuickNote = null
                                customQuickNote = ""
                            }
                        },
                        modifier = Modifier.padding(horizontal = headerPadH)
                    )

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .tutorialPulse(showTutorial && tutorialStepKey == "blister", cornerRadius = 26.dp)
                    ) {
                        SwipeableBlisterPacks(
                            days = state.days,
                            onStatusChange = { day, status -> viewModel.updateDayStatus(day, status) },
                            onPillTaken = {
                                showQuickNoteCard = true
                                selectedQuickNote = QUICK_NOTE_SUGGESTIONS.random()
                                customQuickNote = ""

                                RewardOfferManager.onTakenCompleted(context.applicationContext)

                                val shouldShowRewardOffer = RewardOfferManager.shouldShow(
                                    context = context.applicationContext,
                                    isFreeUser = userTier == UserTier.FREE,
                                    adFreeRewardActive = AdFeaturePrefs.isAdFreeActive(context.applicationContext),
                                    isSignedInPremium = userTier != UserTier.FREE
                                )

                                if (shouldShowRewardOffer) {
                                    showRewardOfferDialog = true
                                    RewardOfferManager.markShown(context.applicationContext)
                                } else if (activity != null && AdFeaturePrefs.shouldShowTakenInterstitial(context.applicationContext, userTier)) {
                                    AdMobExtras.showInterstitial(
                                        activity = activity,
                                        adUnitId = interstitialAdUnitId
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = headerPadH)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        PinkPrimary.copy(alpha = 0.08f),
                                        OrangeAccent.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(2.dp, PinkPrimary.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                            .tutorialPulse(showTutorial && tutorialStepKey == "reminder")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(cardIconSize)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent)))
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(cardIconSizeSm)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Next Reminder",
                                    fontSize = cardLabelSp,
                                    fontWeight = FontWeight.Medium,
                                    color = PinkDark
                                )
                                Text(
                                    if (state.settings.appActive) "Today at ${state.settings.displayTime}"
                                    else "Reminders disabled",
                                    fontSize = reminderFontSp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111827)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = headerPadH)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            modifier = Modifier
                                .weight(1f)
                                .tutorialPulse(showTutorial && tutorialStepKey == "settings"),
                            iconSize = quickIconSize,
                            fontSp = quickFontSp
                        ) {
                            onNavigate(Screen.SETTINGS)
                        }

                        QuickActionButton(
                            icon = Icons.Default.Description,
                            label = "Notes",
                            modifier = Modifier
                                .weight(1f)
                                .tutorialPulse(showTutorial && tutorialStepKey == "notes"),
                            iconSize = quickIconSize,
                            fontSp = quickFontSp
                        ) {
                            if (activity != null && AdFeaturePrefs.shouldShowNotesInterstitial(context.applicationContext, userTier)) {
                                AdMobExtras.showInterstitial(
                                    activity = activity,
                                    adUnitId = interstitialAdUnitId,
                                    onFinished = { onNavigate(Screen.NOTES) }
                                )
                            } else {
                                onNavigate(Screen.NOTES)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }

            if (showTutorial) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(tutorialIndex) {
                            detectTapGestures {
                                val next = tutorialIndex + 1
                                if (next >= tutorialSteps.size) onTutorialFinished() else tutorialIndex = next
                            }
                        }
                )
            }

            if (showTutorial && tutorialStep != null) {
                val placement = if (tutorialStepKey == "pillType" || tutorialStepKey == "startDate") {
                    TutorialCoachPlacement.BOTTOM
                } else {
                    TutorialCoachPlacement.TOP
                }
                TutorialCoachBar(
                    title = tutorialStep.title,
                    body = tutorialStep.body,
                    placement = placement,
                    onSkip = onTutorialSkipped
                ) {
                    val next = tutorialIndex + 1
                    if (next >= tutorialSteps.size) onTutorialFinished() else tutorialIndex = next
                }
            }
        }
    }

    if (showRewardOfferDialog) {
        RewardOfferDialog(
            onDismiss = { showRewardOfferDialog = false },
            onWatchAd = {
                showRewardOfferDialog = false
                if (activity != null) {
                    AdMobExtras.showRewarded(
                        activity = activity,
                        adUnitId = rewardedAdUnitId,
                        onRewardEarned = {
                            AdFeaturePrefs.enableAdFree24Hours(context.applicationContext)
                        }
                    )
                }
            }
        )
    }

    if (showPillTypeModal) {
        PillTypeModal(
            currentType = state.pillType,
            onClose = { showPillTypeModal = false },
            onSelect = { type ->
                viewModel.changePillType(type)
                showPillTypeModal = false
            },
            onCustomSelect = {
                showPillTypeModal = false
                showCustomConfigModal = true
            }
        )
    }

    if (showCustomConfigModal) {
        CustomPillConfigModal(
            onClose = { showCustomConfigModal = false },
            onSave = { a, p, l ->
                viewModel.changeCustomPillConfig(a, p, l)
                showCustomConfigModal = false
            }
        )
    }

    if (showDatePicker) {
        StartDatePickerModal(
            currentDate = state.startDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { d ->
                viewModel.changeStartDate(d)
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickNotePromptCard(
    visible: Boolean,
    selectedNote: String?,
    customNote: String,
    generatedNotes: List<String>,
    onSelectNote: (String) -> Unit,
    onCustomNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val canSave = (selectedNote ?: customNote).orEmpty().trim().isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, PinkPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PinkPrimary, OrangeAccent)))
                    ) {
                        Icon(Icons.Default.Mood, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Quick note",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PinkDark
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                }
            }

            Text(
                "Want to log how you feel after taking your pill?",
                fontSize = 12.sp,
                color = Color(0xFF4B5563)
            )

            val displayedSuggestions = remember(generatedNotes, selectedNote) {
                if (generatedNotes.isEmpty()) {
                    emptyList()
                } else {
                    val pool = generatedNotes.shuffled().take(5).toMutableList()
                    if (selectedNote != null && selectedNote !in pool) {
                        if (pool.size >= 5) pool[pool.lastIndex] = selectedNote else pool.add(selectedNote)
                    }
                    pool
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedSuggestions.forEach { suggestion ->
                    val isSelected = suggestion == selectedNote
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) PinkPrimary.copy(alpha = 0.15f) else Color(0xFFF9FAFB))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PinkPrimary.copy(alpha = 0.5f) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .clickable { onSelectNote(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            suggestion,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            color = if (isSelected) PinkDark else Color(0xFF4B5563)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customNote,
                onValueChange = onCustomNoteChange,
                placeholder = { Text("Or write your own quick note…", fontSize = 12.sp) },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkPrimary,
                    focusedContainerColor = Color(0xFFFFF9FC),
                    unfocusedContainerColor = Color(0xFFFFF9FC)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private val QUICK_NOTE_SUGGESTIONS = listOf(
    "Feeling good and energized today 😄",
    "A little sleepy, but I took it ✅",
    "Mild headache today 🤕",
    "Feeling calm and steady 🌿",
    "Bit nauseous this morning 🤢",
    "Happy mood all day 😊",
    "Slight cramps, manageable 💛",
    "Feeling anxious today 😟",
    "No side effects so far 🙌",
    "Very focused and productive 💪",
    "Felt dizzy for a short while 😵",
    "Hydrated and feeling better 💧",
    "Mood swings today 🎭",
    "Feeling hopeful and positive ✨",
    "Low energy this afternoon 💤",
    "Everything feels normal today 👍",
    "Stomach felt weird after lunch 🍽️",
    "Feeling confident and in control 🌸",
    "A bit emotional today 🥺",
    "Great day overall, feeling strong 🌞",
    "I feel sick with a cold today 🤒",
    "Mild bloating noted 🎈",
    "Feeling relieved I stayed on track 🗓️",
    "Slight breast tenderness today 💗",
    "Feeling social and cheerful 🎉",
    "A little stressed from work 😣",
    "Noticed spotting today 🩸",
    "Feeling peaceful tonight 🌙",
    "Body feels heavy today 🪨",
    "Feeling playful and upbeat 😋",
    "Had cravings but doing okay 🍫",
    "Proud of my consistency 🏅",
    "Back pain was noticeable today 🧍",
    "Feeling sensitive and teary 😢",
    "Clear mind and stable mood 🧠",
    "Felt faint for a moment, now okay ⚠️",
    "Feeling grateful for my progress 🙏",
    "A little irritable this evening 😤",
    "No pain today, feeling free 🕊️",
    "Feeling under the weather 🤧",
    "Strong and motivated today 🚀",
    "Had acne flare-up today 😬",
    "Feeling balanced and refreshed 🌈",
    "Tired but still committed 💊",
    "Slight nausea but manageable 🌼",
    "Mood improved after resting 🛌",
    "Feeling extra hungry today 🍜",
    "Feeling optimistic about this cycle 📈",
    "Had trouble sleeping last night 🌃",
    "Feeling okay, one day at a time 🤍"
)

@Composable
private fun HeaderCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp,
    iconSizeSm: Dp,
    labelFontSp: androidx.compose.ui.unit.TextUnit,
    valueFontSp: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = modifier
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
                Text(
                    value,
                    fontSize = valueFontSp,
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
        modifier = modifier.aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Today's Notes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
            Text(
                text = "View all →",
                fontSize = 12.sp,
                color = Color(0xFFF609BC),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewAll
                )
            )
        }

        notes.take(3).forEach { note ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
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
                            text = note.content,
                            fontSize = 14.sp,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Medium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = note.time,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        if (notes.size > 3) {
            Text(
                text = "+ ${notes.size - 3} more note${if (notes.size - 3 > 1) "s" else ""} — tap View all",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun HeaderAccountRow(
    accountUiState: AccountUiState,
    onSignInClick: () -> Unit,
    profileName: String,
    profilePhotoB64: String,
    showPremiumIcon: Boolean,
    onPremiumClick: () -> Unit,
    premiumModifier: Modifier = Modifier,
    onRewardClick: () -> Unit
) {
    val displayName = profileName.ifBlank { accountUiState.displayName ?: "User" }
    val profileBitmap = remember(profilePhotoB64) { decodeBase64Bitmap(profilePhotoB64) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                Text(
                    text = if (accountUiState.isSignedIn) "Signed in" else "Guest",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showPremiumIcon) {
                IconButton(
                    onClick = onRewardClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "No ads for 24 hours",
                        tint = Color.White
                    )
                }

                Spacer(Modifier.width(8.dp))
            }

            if (showPremiumIcon) {
                val infinite = rememberInfiniteTransition(label = "premiumPulse")
                val premiumScale by infinite.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "premiumScale"
                )
                IconButton(
                    onClick = onPremiumClick,
                    modifier = premiumModifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .scale(premiumScale)
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = Color.White
                    )
                }

                Spacer(Modifier.width(8.dp))
            }

            if (!accountUiState.isSignedIn) {
                TextButton(onClick = onSignInClick) {
                    Text("Sign in", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun WarningPill(text: String, bg: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun Modifier.tutorialPulse(
    enabled: Boolean,
    cornerRadius: Dp = 16.dp,
    strokeWidth: Dp = 3.dp
): Modifier = composed {
    if (!enabled) return@composed this

    val edgeBrush = Brush.linearGradient(
        listOf(Color(0xFFF609BC), Color(0xFFFAB86D))
    )

    val transition = rememberInfiniteTransition(label = "tutorialPulse")

    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tutorialScale"
    )

    return@composed this
        .scale(scale)
        .border(
            width = strokeWidth,
            brush = edgeBrush,
            shape = RoundedCornerShape(cornerRadius)
        )
}