package com.dosevia.app

import android.app.NotificationManager
import android.content.Context
import java.io.File

object AppResetter {
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
    }
}
