package com.dosevia.app

import android.content.Context

/** One-time onboarding flag. Should only reset on uninstall / clear app data from system settings. */
object OnboardingPrefs {
    private const val PREFS = "dosevia_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }

    fun setCompleted(context: Context, completed: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, completed)
            .apply()
    }
}
