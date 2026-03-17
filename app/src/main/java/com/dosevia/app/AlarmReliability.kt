package com.dosevia.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Small reliability helpers used by the alarm stack.
 *
 * Main daily reminder uses AlarmClock when possible because OEM firmware tends to
 * treat it more reliably than a plain exact alarm while the app is closed.
 */
object AlarmReliability {

    private const val TAG = "DoseviaAlarmReliability"

    fun isBatteryOptimized(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isExactAlarmAllowed(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else true
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.areNotificationsEnabled()
        }
    }

    fun hasAnyReliabilityBlocker(context: Context): Boolean {
        return isBatteryOptimized(context) || !isExactAlarmAllowed(context) || !canPostNotifications(context)
    }

    fun reliabilitySummary(context: Context): String {
        val missing = buildList {
            if (isBatteryOptimized(context)) add("battery unrestricted access")
            if (!isExactAlarmAllowed(context)) add("exact alarms")
            if (!canPostNotifications(context)) add("notifications")
        }
        return when (missing.size) {
            0 -> "Dosevia reliability settings are ready."
            1 -> "Dosevia still needs ${missing.first()} enabled."
            2 -> "Dosevia still needs ${missing[0]} and ${missing[1]} enabled."
            else -> "Dosevia still needs battery unrestricted access, exact alarms, and notifications enabled."
        }
    }

    fun syncReliabilityNudges(context: Context) {
        if (hasAnyReliabilityBlocker(context)) {
            AlarmReliabilityNudgeScheduler.schedule(context)
        } else {
            AlarmReliabilityNudgeScheduler.cancel(context)
        }
    }

    fun ensureReliabilityChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            RELIABILITY_CHANNEL_ID,
            RELIABILITY_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Dosevia reminders to keep alarm settings reliable"
            enableVibration(true)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun scheduleMainAlarm(
        context: Context,
        triggerAtMillis: Long,
        showIntent: PendingIntent,
        fireIntent: PendingIntent
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                    am.setAlarmClock(info, fireIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isExactAlarmAllowed(context) -> {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isExactAlarmAllowed(context) -> {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
                }
                else -> am.setAndAllowWhileIdleCompat(triggerAtMillis, fireIntent)
            }
            Log.d(TAG, "Scheduled main alarm for $triggerAtMillis")
        } catch (t: Throwable) {
            Log.w(TAG, "Main alarm exact scheduling failed, using inexact fallback", t)
            am.setAndAllowWhileIdleCompat(triggerAtMillis, fireIntent)
        }
    }

    fun scheduleBackgroundAlarm(
        context: Context,
        triggerAtMillis: Long,
        operation: PendingIntent,
        showIntent: PendingIntent? = null,
        preferAlarmClock: Boolean = false
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            when {
                preferAlarmClock && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && showIntent != null -> {
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isExactAlarmAllowed(context) -> {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isExactAlarmAllowed(context) -> {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                }
                else -> am.setAndAllowWhileIdleCompat(triggerAtMillis, operation)
            }
            Log.d(TAG, "Scheduled background alarm for $triggerAtMillis preferAlarmClock=$preferAlarmClock")
        } catch (t: Throwable) {
            Log.w(TAG, "Background alarm exact scheduling failed, using inexact fallback", t)
            am.setAndAllowWhileIdleCompat(triggerAtMillis, operation)
        }
    }

    fun openBestEffortBatterySettings(context: Context) {
        val packageName = context.packageName
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val intents = listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent().apply {
                component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            appDetailsIntent
        )

        intents.firstOrNull { it.resolveActivity(context.packageManager) != null }?.let {
            runCatching { context.startActivity(it) }
        } ?: runCatching { context.startActivity(appDetailsIntent) }
    }
}

class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return

        AlarmReliability.syncReliabilityNudges(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_APP_ACTIVE, true)) return

        scheduleAlarm(
            context = context,
            hour = prefs.getInt(KEY_HOUR, 9),
            minute = prefs.getInt(KEY_MINUTE, 0),
            title = prefs.getString(KEY_TITLE, "Time to take your pill") ?: "Time to take your pill",
            subtitle = prefs.getString(KEY_SUBTITLE, "Don't forget your daily dose") ?: "Don't forget your daily dose",
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            notificationIcon = prefs.getString(KEY_NOTIF_ICON, "medication") ?: "medication",
            notificationSound = prefs.getString(KEY_NOTIF_SOUND, "default") ?: "default"
        )
        schedulePreReminderFromPrefs(context)

        if (shouldFireAlarmNow(context)) {
            scheduleOverdueTick(context, System.currentTimeMillis() + nextOverdueDelayMs(context))
        }
    }
}

private const val RELIABILITY_CHANNEL_ID = "dosevia_reliability_channel"
private const val RELIABILITY_CHANNEL_NAME = "Dosevia reliability"
private const val RELIABILITY_REQUEST_CODE = 3010
private const val RELIABILITY_NOTIFICATION_ID = 3011
private const val RELIABILITY_PREFS = "dosevia_reliability"
private const val KEY_LAST_RELIABILITY_DAY = "last_reliability_day"

class ReliabilityNudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmReliability.syncReliabilityNudges(context)
        if (!AlarmReliability.hasAnyReliabilityBlocker(context)) return
        if (!AlarmReliability.canPostNotifications(context)) return

        val prefs = context.getSharedPreferences(RELIABILITY_PREFS, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        if (prefs.getString(KEY_LAST_RELIABILITY_DAY, null) == today) return

        AlarmReliability.ensureReliabilityChannel(context)

        val openAppIntent = PendingIntent.getActivity(
            context,
            RELIABILITY_REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("open_reliability_help", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RELIABILITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Keep Dosevia reminders reliable")
            .setContentText(AlarmReliability.reliabilitySummary(context))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    AlarmReliability.reliabilitySummary(context) +
                        " Open Dosevia and allow the recommended settings so alarms still ring while the app is closed."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(RELIABILITY_NOTIFICATION_ID, notification)
        prefs.edit().putString(KEY_LAST_RELIABILITY_DAY, today).apply()
    }
}

/** Lightweight daily nudges for users who still have battery / exact alarm / notification blockers. */
object AlarmReliabilityNudgeScheduler {
    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RELIABILITY_REQUEST_CODE,
            Intent(context, ReliabilityNudgeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                else -> am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdleCompat(triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RELIABILITY_REQUEST_CODE,
            Intent(context, ReliabilityNudgeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(RELIABILITY_NOTIFICATION_ID)
    }

    private fun AlarmManager.setAndAllowWhileIdleCompat(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            else -> set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}

private fun AlarmManager.setAndAllowWhileIdleCompat(triggerAtMillis: Long, pendingIntent: PendingIntent) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        else -> set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}
