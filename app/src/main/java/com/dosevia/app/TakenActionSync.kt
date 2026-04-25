package com.dosevia.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val STATUS_PREFS_NAME = "dosevia_status"

fun markTodayTakenFromBackground(context: Context, openAppForFree: Boolean) {
    val appContext = context.applicationContext
    val now = System.currentTimeMillis()
    val todayMs = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

    // Persist the actual day status immediately so background receivers/widgets
    // do not wait for MainActivity/AppViewModel to reconcile it later.
    appContext.getSharedPreferences(STATUS_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("status_$todayKey", PillStatus.TAKEN.name)
        .putLong("takenAt_$todayKey", now)
        .commit()

    // Keep compatibility with the existing app-start reconciliation path.
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong("alarm_taken_date", todayMs)
        .putBoolean(KEY_SHOULD_REMIND_TODAY, false)
        .putLong(KEY_REMIND_DATE_MS, todayMs)
        .commit()

    // Stop anything current/today-related without disabling future daily alarms.
    cancelOverdueTick(appContext)
    cancelSnoozeAlarm(appContext)
    appContext.stopService(Intent(appContext, AlarmForegroundService::class.java))

    cancelPreReminder(appContext)
    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(ALARM_NOTIF_ID)

    runCatching { AchievementsManager.recordPillTaken(appContext) }

    PillWidget.requestUpdate(appContext)
    PillWidgetMedium.requestUpdate(appContext)
    PillWidgetCalendar.requestUpdate(appContext)

    runCatching { CloudSyncManager(appContext).requestSyncDebounced(0L) }

    AlarmReliability.syncReliabilityNudges(appContext)

    val tier = PremiumAccess.readTier(appContext)
    val shouldOpenApp = openAppForFree && tier == UserTier.FREE
    if (shouldOpenApp) {
        appContext.startActivity(Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("from_alarm", true)
        })
    }
}


fun markTodayNotTakenFromBackground(context: Context) {
    val appContext = context.applicationContext
    val now = System.currentTimeMillis()
    val todayMs = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

    appContext.getSharedPreferences(STATUS_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("status_$todayKey", PillStatus.NOT_TAKEN.name)
        .remove("takenAt_$todayKey")
        .commit()

    val schedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    schedPrefs.edit()
        .putLong("alarm_taken_date", 0L)
        .putBoolean(KEY_SHOULD_REMIND_TODAY, true)
        .putLong(KEY_REMIND_DATE_MS, todayMs)
        .commit()

    val title = schedPrefs.getString(KEY_TITLE, "Time to take your pill") ?: "Time to take your pill"
    val subtitle = schedPrefs.getString(KEY_SUBTITLE, "Don't forget your daily dose") ?: "Don't forget your daily dose"
    val hour = schedPrefs.getInt(KEY_HOUR, 9)
    val minute = schedPrefs.getInt(KEY_MINUTE, 0)
    val vibrationEnabled = schedPrefs.getBoolean(KEY_VIBRATION, true)
    val notificationIcon = schedPrefs.getString(KEY_NOTIF_ICON, "medication") ?: "medication"
    val notificationSound = schedPrefs.getString(KEY_NOTIF_SOUND, "default") ?: "default"
    val appActive = schedPrefs.getBoolean(KEY_APP_ACTIVE, true)

    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(ALARM_NOTIF_ID)
    appContext.stopService(Intent(appContext, AlarmForegroundService::class.java))
    cancelSnoozeAlarm(appContext)
    cancelOverdueTick(appContext)

    AlarmReliability.syncReliabilityNudges(appContext)

    if (appActive) {
        scheduleAlarm(
            context = appContext,
            hour = hour,
            minute = minute,
            title = title,
            subtitle = subtitle,
            vibrationEnabled = vibrationEnabled,
            notificationIcon = notificationIcon,
            notificationSound = notificationSound
        )
        schedulePreReminderFromPrefs(appContext)

        val dueAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (System.currentTimeMillis() >= dueAt && shouldFireAlarmNow(appContext)) {
            val serviceIntent = Intent(appContext, AlarmForegroundService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBTITLE, subtitle)
            }
            ContextCompat.startForegroundService(appContext, serviceIntent)
            scheduleOverdueTick(appContext, System.currentTimeMillis() + 60_000L)
        } else {
            scheduleOverdueTick(appContext, dueAt)
        }
    }

    PillWidget.requestUpdate(appContext)
    PillWidgetMedium.requestUpdate(appContext)
    PillWidgetCalendar.requestUpdate(appContext)

    runCatching { CloudSyncManager(appContext).requestSyncDebounced(0L) }
}
