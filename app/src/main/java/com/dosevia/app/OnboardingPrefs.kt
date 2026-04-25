package com.dosevia.app

import android.content.Context

/** One-time onboarding flag. Should only reset on uninstall / clear app data from system settings. */
object OnboardingPrefs {
    /**
     * Store this in the main prefs file so:
     * - it persists across normal app launches
     * - it survives sign-in/out (unless the user clears app data)
     * - it is included in your existing Drive prefs sync/restore.
     */
    private const val KEY_COMPLETED = "onboarding_completed"

    // Legacy (older builds stored onboarding in a separate prefs file).
    private const val LEGACY_PREFS = "dosevia_onboarding"
    private const val LEGACY_KEY_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean {
        val app = context.applicationContext

        // New source of truth (synced): dosevia_prefs
        val main = app.getSharedPreferences(PREFS_DOSEVIA, Context.MODE_PRIVATE)
        val completed = main.getBoolean(KEY_COMPLETED, false)
        if (completed) return true

        // Migration: if legacy flag exists, copy it into the main prefs.
        val legacy = app.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val legacyCompleted = legacy.getBoolean(LEGACY_KEY_COMPLETED, false)
        if (legacyCompleted) {
            main.edit().putBoolean(KEY_COMPLETED, true).apply()
            return true
        }
        return false
    }

    fun setCompleted(context: Context, completed: Boolean) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS_DOSEVIA, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, completed)
            .apply()
    }
}
