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
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

// ── Constants ─────────────────────────────────────────────────────────────────

const val ALARM_CHANNEL_ID   = "dosevia_alarm_channel"
const val ALARM_CHANNEL_NAME = "Pill Alarm"
const val ALARM_NOTIF_ID     = 2001
private const val ALARM_REQUEST_CODE = 3001

const val REMINDER_CHANNEL_ID   = "dosevia_reminder_channel"
const val REMINDER_CHANNEL_NAME = "Pill Reminders"

private const val PRE_REMINDER_REQUEST_CODE    = 3002
private const val OVERDUE_TICK_REQUEST_CODE    = 3003

// Default: heads-up reminder 15 minutes before alarm time.
private const val DEFAULT_PRE_REMINDER_MINUTES = 15

private const val STATUS_PREFS_NAME = "dosevia_status"


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

        if (!shouldFireAlarmNow(context)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ALARM_NOTIF_ID)
            context.stopService(Intent(context, AlarmForegroundService::class.java))
            cancelOverdueTick(context)
            cancelSnoozeAlarm(context)
            return
        }

        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_TITLE,    title)
            putExtra(EXTRA_SUBTITLE, subtitle)
        }

        // ContextCompat.startForegroundService works from API 16+
        // On Android 8+ it calls startForegroundService; below 8 it calls startService
        ContextCompat.startForegroundService(context, serviceIntent)

        // 🔥 Critical reliability fix:
        // AlarmManager alarms are one-shot. If we don't re-schedule when it fires,
        // the alarm can "work once" and then never fire again until the user opens
        // the app and applyAlarm() runs.
        //
        // Re-schedule the next daily alarm + pre-reminder using persisted prefs.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appActive = prefs.getBoolean(KEY_APP_ACTIVE, true)
        if (appActive) {
            scheduleAlarm(
                context           = context,
                hour              = prefs.getInt(KEY_HOUR, 9),
                minute            = prefs.getInt(KEY_MINUTE, 0),
                title             = prefs.getString(KEY_TITLE, title) ?: title,
                subtitle          = prefs.getString(KEY_SUBTITLE, subtitle) ?: subtitle,
                vibrationEnabled  = prefs.getBoolean(KEY_VIBRATION, true),
                notificationIcon  = prefs.getString(KEY_NOTIF_ICON, "medication") ?: "medication",
                notificationSound = prefs.getString(KEY_NOTIF_SOUND, "default") ?: "default"
            )
            schedulePreReminderFromPrefs(context)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PreReminderReceiver — heads-up notification before scheduled pill time
// ─────────────────────────────────────────────────────────────────────────────

class PreReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureReminderChannel(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_APP_ACTIVE, true)) return

        val hour = prefs.getInt(KEY_HOUR, 9)
        val min  = prefs.getInt(KEY_MINUTE, 0)
        val timeStr = try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
            }
            DateFormat.getTimeFormat(context).format(cal.time)
        } catch (_: Exception) {
            String.format("%02d:%02d", hour, min)
        }

        val openPi = PendingIntent.getActivity(
            context,
            4100,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Pill time soon")
            .setContentText("Your pill is scheduled at $timeStr")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()

        nm.notify(PRE_REMINDER_REQUEST_CODE, n)

        // Re-schedule tomorrow's pre-reminder (one-shot alarms don't repeat).
        schedulePreReminderFromPrefs(context)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  OverdueTickReceiver — strict minute-by-minute alarm after scheduled time
// ─────────────────────────────────────────────────────────────────────────────

class OverdueTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_APP_ACTIVE, true)) { cancelOverdueTick(context); return }

        val todayMs = normMidnightMs(Calendar.getInstance())
        val remindDate = prefs.getLong(KEY_REMIND_DATE_MS, 0L)
        val shouldRemindToday = prefs.getBoolean(KEY_SHOULD_REMIND_TODAY, true)
        if (remindDate != todayMs || !shouldRemindToday || !shouldFireAlarmNow(context)) {
            cancelOverdueTick(context)
            cancelSnoozeAlarm(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ALARM_NOTIF_ID)
            context.stopService(Intent(context, AlarmForegroundService::class.java))
            return
        }

        val hour = prefs.getInt(KEY_HOUR, 9)
        val min  = prefs.getInt(KEY_MINUTE, 0)
        val dueAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (System.currentTimeMillis() < dueAt) {
            scheduleOverdueTick(context, dueAt)
            return
        }

        val title    = prefs.getString(KEY_TITLE, "Time to take your pill") ?: "Time to take your pill"
        val subtitle = prefs.getString(KEY_SUBTITLE, "Don't forget your daily dose") ?: "Don't forget your daily dose"
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_SUBTITLE, subtitle)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // STRICT: keep ringing every minute until the user marks today's pill as TAKEN.
        scheduleOverdueTick(context, System.currentTimeMillis() + 60_000L)
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
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_DATE_CHANGED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED" &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

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

        // Restore supporting reminders as well.
        val todayMs = normMidnightMs(Calendar.getInstance())
        prefs.edit()
            .putBoolean(KEY_SHOULD_REMIND_TODAY, true)
            .putLong(KEY_REMIND_DATE_MS, todayMs)
            .apply()
        schedulePreReminderFromPrefs(context)

        val dueAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.getInt(KEY_HOUR, 9))
            set(Calendar.MINUTE, prefs.getInt(KEY_MINUTE, 0))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        scheduleOverdueTick(context, dueAt)
    }
}

private fun todayStatus(context: Context): PillStatus? {
    val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(System.currentTimeMillis())
    val raw = context.applicationContext
        .getSharedPreferences(STATUS_PREFS_NAME, Context.MODE_PRIVATE)
        .getString("status_$todayKey", null)
        ?: return null
    return try { PillStatus.valueOf(raw) } catch (_: Exception) { null }
}

fun shouldFireAlarmNow(context: Context): Boolean {
    val appContext = context.applicationContext
    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(KEY_APP_ACTIVE, true)) return false

    val todayMs = normMidnightMs(Calendar.getInstance())
    if (prefs.getLong(KEY_REMIND_DATE_MS, todayMs) != todayMs) return false
    if (!prefs.getBoolean(KEY_SHOULD_REMIND_TODAY, true)) return false

    return todayStatus(appContext) != PillStatus.TAKEN
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

/** Channel for non-alarm reminder notifications (heads-up pre-reminders, etc.). */
fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description          = "Dosevia reminders"
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
    val showPi = buildShowAlarmPendingIntent(context)

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

    AlarmReliability.scheduleMainAlarm(
        context = context,
        triggerAtMillis = cal.timeInMillis,
        showIntent = showPi,
        fireIntent = pi
    )
}

/** Schedule a heads-up reminder notification before the pill time. */
fun schedulePreReminderFromPrefs(context: Context, leadMinutes: Int = DEFAULT_PRE_REMINDER_MINUTES) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(KEY_APP_ACTIVE, true)) return
    val hour = prefs.getInt(KEY_HOUR, 9)
    val min  = prefs.getInt(KEY_MINUTE, 0)

    val dueCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, min)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
    }

    val trigger = dueCal.timeInMillis - leadMinutes * 60_000L
    if (trigger <= System.currentTimeMillis()) return

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = PendingIntent.getBroadcast(
        context,
        PRE_REMINDER_REQUEST_CODE,
        Intent(context, PreReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            else ->
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    } catch (_: SecurityException) {
        am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
    }
}

fun cancelPreReminder(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = PendingIntent.getBroadcast(
        context,
        PRE_REMINDER_REQUEST_CODE,
        Intent(context, PreReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
}

/** One-shot tick used to enforce strict overdue alarms. */
fun scheduleOverdueTick(context: Context, triggerAtMs: Long) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = PendingIntent.getBroadcast(
        context,
        OVERDUE_TICK_REQUEST_CODE,
        Intent(context, OverdueTickReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            else ->
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    } catch (_: SecurityException) {
        am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
    }
}

fun cancelOverdueTick(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = PendingIntent.getBroadcast(
        context,
        OVERDUE_TICK_REQUEST_CODE,
        Intent(context, OverdueTickReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
}

private fun normMidnightMs(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/**
 * Cancel the daily alarm and mark it as inactive in prefs.
 */
fun cancelAlarm(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_APP_ACTIVE, false)
        .putBoolean(KEY_SHOULD_REMIND_TODAY, false)
        .putLong(KEY_REMIND_DATE_MS, normMidnightMs(Calendar.getInstance()))
        .apply()

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(buildAlarmPendingIntent(context, "", ""))
    cancelPreReminder(context)
    cancelOverdueTick(context)
    cancelSnoozeAlarm(context)

    // Also stop the service if it is currently running (e.g. user turned off alarm mid-ring)
    context.stopService(Intent(context, AlarmForegroundService::class.java))

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(ALARM_NOTIF_ID)
    nm.cancel(PRE_REMINDER_REQUEST_CODE)
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


private fun buildShowAlarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("open_alarm_settings", true)
    }
    return PendingIntent.getActivity(
        context,
        3009,
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

// Written by AppViewModel.applyAlarm() so background receivers can make correct decisions.
const val KEY_SHOULD_REMIND_TODAY = "s_should_remind_today"
const val KEY_REMIND_DATE_MS      = "s_remind_date_ms"

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

fun cancelSnoozeAlarm(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = PendingIntent.getBroadcast(
        context,
        SNOOZE_REQUEST_CODE,
        Intent(context, PillAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
}

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
