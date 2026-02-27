package com.dosevia.app

import android.app.NotificationManager
import android.content.Context
import java.io.File

object AppResetter {
    private fun normalizeToMidnightMillis(ms: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun wipeAllLocalData(context: Context) {
        val appContext = context.applicationContext

        cancelAlarm(appContext)
        (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancelAll()

        val sharedPrefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
            sharedPrefsDir.listFiles()
                .orEmpty()
                .filter { it.isFile && it.extension == "xml" }
                .forEach { file ->
                    val prefsName = file.nameWithoutExtension
                    appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit()
                }
        }

        appContext.filesDir?.let {
            runCatching { it.deleteRecursively() }
            runCatching { it.mkdirs() }
        }
        appContext.cacheDir?.let {
            runCatching { it.deleteRecursively() }
        }

        // Initialize a fresh blister start date (today) after wipe
        val todayMs = normalizeToMidnightMillis(System.currentTimeMillis())
        appContext.getSharedPreferences("dosevia_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("startDate", todayMs)
            .commit()
    }
}
