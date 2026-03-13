package com.dosevia.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Small reliability helpers used by the alarm stack.
 *
 * Main daily reminder uses AlarmClock when possible because OEM firmware tends to
 * treat it more reliably than a plain exact alarm while the app is closed.
 */
object AlarmReliability {

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
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
                }
                else -> am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
            }
        } catch (_: SecurityException) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
                }
                else -> am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent)
            }
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
