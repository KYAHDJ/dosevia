package com.dosevia.app

// ─────────────────────────────────────────────────────────────────────────────
//  AlarmActivity.kt  — v15
//
//  Full-screen alarm UI shown over lock screen / when screen is OFF.
//
//  Changes in v14:
//   • Colour theme updated to match app's pink ↔ orange gradient palette
//   • Centre icon is now user-selectable (saved in SharedPreferences KEY_NOTIF_ICON)
//   • Settings screen exposes icon picker + sound picker
//
//  Buttons:
//   ✓ TAKEN  → stops alarm, marks pill taken, opens app
//   💤 SNOOZE → stops alarm, reschedules in 5 minutes, closes
//
//  Window flags ensure it shows on lock screen and wakes the display.
// ─────────────────────────────────────────────────────────────────────────────

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Wake device + show over lock screen ───────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED   or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON     or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON     or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val title    = intent.getStringExtra(EXTRA_TITLE)    ?: "Time to take your pill"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: "Don't forget your daily dose"
        val iconPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTIF_ICON, "medication") ?: "medication"

        setContent {
            AlarmScreen(
                title    = title,
                subtitle = subtitle,
                iconKey  = iconPref,
                onTaken  = { handleTaken() },
                onSnooze = { handleSnooze() }
            )
        }
    }

    // ── TAKEN: stop alarm → mark pill taken → open app ────────────────────────
    private fun handleTaken() {
        stopService(Intent(this, AlarmForegroundService::class.java))

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong("alarm_taken_date", normToday())
            .apply()

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("from_alarm", true)
            }
        )
        finish()
    }

    // ── SNOOZE: stop alarm → reschedule in 5 minutes → close ─────────────────
    private fun handleSnooze() {
        stopService(Intent(this, AlarmForegroundService::class.java))

        val snoozeTimeMs = System.currentTimeMillis() + 5 * 60 * 1000L

        val prefs    = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title    = prefs.getString(KEY_TITLE,    "Time to take your pill")    ?: "Time to take your pill"
        val subtitle = prefs.getString(KEY_SUBTITLE, "Don't forget your daily dose") ?: "Don't forget your daily dose"

        scheduleOneShotAlarm(this, snoozeTimeMs, title, subtitle)
        finish()
    }

    private fun normToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* intentionally blocked */ }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Icon key → ImageVector mapping  (shared with SettingsScreen)
// ─────────────────────────────────────────────────────────────────────────────

fun notifIconVector(key: String): ImageVector = when (key) {
    "favorite_heart"    -> Icons.Default.Favorite
    "alarm"             -> Icons.Default.Alarm
    "star"              -> Icons.Default.Star
    "notifications"     -> Icons.Default.Notifications
    "local_pharmacy"    -> Icons.Default.LocalPharmacy
    "health_and_safety" -> Icons.Default.HealthAndSafety
    "healing"           -> Icons.Default.Healing
    "medical_services"  -> Icons.Default.MedicalServices
    "water_drop"        -> Icons.Default.WaterDrop
    else                -> Icons.Default.Medication   // "medication" (default)
}

/** All available alarm-screen icon options exposed to SettingsScreen */
val ICON_OPTIONS: List<Pair<String, String>> = listOf(
    "medication"        to "Medication (pill)",
    "local_pharmacy"    to "Pharmacy bag",
    "medical_services"  to "Medical Services",
    "health_and_safety" to "Health & Safety",
    "healing"           to "Healing",
    "favorite_heart"    to "Heart",
    "water_drop"        to "Water Drop",
    "alarm"             to "Alarm Clock",
    "star"              to "Star",
    "notifications"     to "Bell"
)

// ─────────────────────────────────────────────────────────────────────────────
//  Alarm Screen Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AlarmScreen(
    title: String,
    subtitle: String,
    iconKey: String,
    onTaken: () -> Unit,
    onSnooze: () -> Unit
) {
    // ── App-theme gradient: pink-white → warm orange-tint ─────────────────────
    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFFFFF0FB),   // soft pink-white  (matches settings screen bg)
            Color(0xFFFFDDF6),   // mid pink
            Color(0xFFFFF3E0)    // warm orange-tint bottom
        )
    )

    val accentGradient = Brush.linearGradient(
        listOf(Color(0xFFF609BC), Color(0xFFFAB86D))
    )

    // Pulsing animation
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue  = 0.88f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(850, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Live clock
    var timeStr by remember { mutableStateOf(fmtTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            timeStr = fmtTime()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(64.dp))

            // ── Clock ──────────────────────────────────────────────────────────
            Text(
                text          = timeStr,
                fontSize      = 76.sp,
                fontWeight    = FontWeight.Thin,
                color         = Color(0xFF2D1340),
                letterSpacing = 2.sp
            )
            Text(
                text          = fmtDate(),
                fontSize      = 16.sp,
                color         = Color(0xFF9C27B0).copy(alpha = 0.70f),
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(52.dp))

            // ── Pulsing icon — pink → orange gradient circle ───────────────────
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(accentGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = notifIconVector(iconKey),
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(66.dp)
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Title & subtitle ───────────────────────────────────────────────
            Text(
                text       = title,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF2D1340),
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text      = subtitle,
                fontSize  = 16.sp,
                color     = Color(0xFF6B2D7E).copy(alpha = 0.80f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // ── TAKEN button — gradient ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(accentGradient),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick   = onTaken,
                    modifier  = Modifier.fillMaxSize(),
                    shape     = RoundedCornerShape(34.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text          = "✓   TAKEN",
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color.White,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── SNOOZE button ──────────────────────────────────────────────────
            OutlinedButton(
                onClick   = onSnooze,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF609BC)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    Brush.linearGradient(listOf(Color(0xFFF609BC), Color(0xFFFAB86D)))
                )
            ) {
                Text(
                    text          = "💤  SNOOZE 5 MIN",
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(52.dp))
        }
    }
}

private fun fmtTime(): String =
    SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

private fun fmtDate(): String =
    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
