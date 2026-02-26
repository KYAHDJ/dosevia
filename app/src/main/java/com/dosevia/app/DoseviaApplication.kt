package com.dosevia.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class DoseviaApplication : Application() {

    private lateinit var cloudSyncManager: CloudSyncManager
    private val listeners = mutableListOf<Pair<SharedPreferences, SharedPreferences.OnSharedPreferenceChangeListener>>()

    override fun onCreate() {
        super.onCreate()
        cloudSyncManager = CloudSyncManager(this)
        registerPrefsChangeListeners()
    }

    private fun registerPrefsChangeListeners() {
        SharedPrefsBackupSerializer.backupPrefFiles.forEach { prefName ->
            val prefs = getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                cloudSyncManager.markLocalChanged()
                cloudSyncManager.requestSyncDebounced()
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            listeners += prefs to listener
        }
    }
}
