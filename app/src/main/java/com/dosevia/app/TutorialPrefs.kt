package com.dosevia.app

import android.content.Context

object TutorialPrefs {
    private const val PREF = "dosevia_tutorial"
    private const val KEY_DONE = "done_v1"

    fun isCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)
    }

    fun setCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DONE, completed)
            .apply()
    }
}
