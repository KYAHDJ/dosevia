package com.dosevia.app

// ─────────────────────────────────────────────────────────────────────────────
//  NotificationScheduler.kt
//
//  Responsibilities:
//   • Schedule / cancel the exact daily alarm via AlarmManager
//   • Persist alarm settings to SharedPreferences (for BootReceiver restore)
//   • Expose ensureNotificationChannel() used by AlarmForegroundService
//
//  Architecture (per PDF guide):
//    AlarmManager  →  PillAlarmReceiver  →  AlarmForegroundService
//                                              ├─ startForeground (notification)
//                                              ├─ fullScreenIntent → AlarmActivity
//                                              └─ MediaPlayer (looping alarm sound)
// ─────────────────────────────────────────────────────────────────────────────

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Calendar

// ── Constants ─────────────────────────────────────────────────────────────────

const val ALARM_CHANNEL_ID   = "dosevia_alarm_channel"
const val ALARM_CHANNEL_NAME = "Pill Alarm"
const val ALARM_NOTIF_ID     = 2001
private const val ALARM_REQUEST_CODE = 3001

// ─────────────────────────────────────────────────────────────────────────────
//  PillAlarmReceiver
//
//  Fires when AlarmManager triggers.  Per the PDF guide we NEVER launch
//  an Activity directly from a BroadcastReceiver (restricted on Android 8+).
//  Instead we start AlarmForegroundService via ContextCompat.startForegroundService.
// ─────────────────────────────────────────────────────────────────────────────

class PillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title    = intent.getStringExtra(EXTRA_TITLE)    ?: "Time to take your pill"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: "Don't forget your daily dose"

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_TITLE,    title)
            putExtra(EXTRA_SUBTITLE, subtitle)
        }

        // ContextCompat.startForegroundService works from API 16+
        // On Android 8+ it calls startForegroundService; below 8 it calls startService
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BootReceiver
//
//  Called after device reboot. Re-reads persisted alarm settings and
//  re-schedules the alarm so it survives power cycles.
// ─────────────────────────────────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        val prefs     = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appActive = prefs.getBoolean(KEY_APP_ACTIVE, true)
        if (!appActive) return

        scheduleAlarm(
            context           = context,
            hour              = prefs.getInt(KEY_HOUR,    9),
            minute            = prefs.getInt(KEY_MINUTE,  0),
            title             = prefs.getString(KEY_TITLE,    "Time to take your pill") ?: "Time to take your pill",
            subtitle          = prefs.getString(KEY_SUBTITLE, "Don't forget your daily dose") ?: "Don't forget your daily dose",
            vibrationEnabled  = prefs.getBoolean(KEY_VIBRATION, true),
            notificationIcon  = prefs.getString(KEY_NOTIF_ICON,  "medication") ?: "medication",
            notificationSound = prefs.getString(KEY_NOTIF_SOUND, "default")   ?: "default"
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Public API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Create (or ensure) the high-importance alarm notification channel.
 * Must be called before posting any notification on Android 8+.
 */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description          = "Daily pill alarm notification"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
            }
            nm.createNotificationChannel(channel)
        }
    }
}

/**
 * Schedule the exact daily alarm.
 *
 * Uses setExactAndAllowWhileIdle (API 23+) which fires even in Doze mode.
 * Falls back to setExact (API 19–22) or set (API < 19 / security exception).
 *
 * Persists all settings so BootReceiver can restore them after reboot.
 */
fun scheduleAlarm(
    context: Context,
    hour: Int,
    minute: Int,
    title: String,
    subtitle: String,
    vibrationEnabled: Boolean = true,
    notificationIcon: String = "medication",
    notificationSound: String = "alarm"
) {
    // Persist
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_APP_ACTIVE,  true)
        .putInt(KEY_HOUR,            hour)
        .putInt(KEY_MINUTE,          minute)
        .putString(KEY_TITLE,        title)
        .putString(KEY_SUBTITLE,     subtitle)
        .putBoolean(KEY_VIBRATION,   vibrationEnabled)
        .putString(KEY_NOTIF_ICON,   notificationIcon)
        .putString(KEY_NOTIF_SOUND,  notificationSound)
        .apply()

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = buildAlarmPendingIntent(context, title, subtitle)

    // Calculate next trigger: if time already passed today → schedule tomorrow
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE,      minute)
        set(Calendar.SECOND,      0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    try {
        when {
            // API 23+ — fires even in Doze mode (required for reliable alarms)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
            // API 19–22
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
            // API < 19 (unlikely given minSdk 26, but safe)
            else -> {
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    } catch (e: SecurityException) {
        // Fallback if exact alarm permission was revoked at runtime
        am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }
}

/**
 * Cancel the daily alarm and mark it as inactive in prefs.
 */
fun cancelAlarm(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_APP_ACTIVE, false)
        .apply()

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(buildAlarmPendingIntent(context, "", ""))

    // Also stop the service if it is currently running (e.g. user turned off alarm mid-ring)
    context.stopService(Intent(context, AlarmForegroundService::class.java))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Private helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun buildAlarmPendingIntent(
    context: Context,
    title: String,
    subtitle: String
): PendingIntent {
    val intent = Intent(context, PillAlarmReceiver::class.java).apply {
        putExtra(EXTRA_TITLE,    title)
        putExtra(EXTRA_SUBTITLE, subtitle)
    }
    return PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SharedPreferences keys & Intent extras
// ─────────────────────────────────────────────────────────────────────────────

const val PREFS_NAME         = "dosevia_prefs"
const val KEY_APP_ACTIVE     = "s_appActive"
const val KEY_HOUR           = "s_hour"
const val KEY_MINUTE         = "s_minute"
const val KEY_TITLE          = "s_title"
const val KEY_SUBTITLE       = "s_sub"
const val KEY_VIBRATION      = "s_vibration"     // persisted so AlarmForegroundService can read it
const val KEY_NOTIF_ICON     = "s_notif_icon"    // alarm screen centre icon key
const val KEY_NOTIF_SOUND    = "s_notif_sound"   // notification sound key

const val EXTRA_TITLE    = "alarm_title"
const val EXTRA_SUBTITLE = "alarm_subtitle"

// ─────────────────────────────────────────────────────────────────────────────
//  scheduleOneShotAlarm
//
//  Used by snooze: fires once at the exact given timestamp.
//  Uses a different request code (ALARM_REQUEST + 10) so it does not
//  cancel the regular daily alarm pending intent.
// ─────────────────────────────────────────────────────────────────────────────

private const val SNOOZE_REQUEST_CODE = 3011

fun scheduleOneShotAlarm(
    context: Context,
    triggerAtMs: Long,
    title: String,
    subtitle: String
) {
    val intent = Intent(context, PillAlarmReceiver::class.java).apply {
        putExtra(EXTRA_TITLE,    title)
        putExtra(EXTRA_SUBTITLE, subtitle)
    }
    val pi = PendingIntent.getBroadcast(
        context,
        SNOOZE_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            else ->
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    } catch (e: SecurityException) {
        am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
    }
}
