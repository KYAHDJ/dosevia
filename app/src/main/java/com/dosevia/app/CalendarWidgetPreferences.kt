package com.dosevia.app

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson

data class CalendarWidgetUiPrefs(
    val showActionButtons: Boolean = true,
    val takenButtonBackgroundColor: Int = Color.parseColor("#166534"),
    val notTakenButtonBackgroundColor: Int = Color.parseColor("#B91C1C"),
    val buttonTextColor: Int = Color.WHITE,
    val todayIndicatorColor: Int = Color.parseColor("#22C55E"),
    val todayPulseEnabled: Boolean = true
)

private const val CALENDAR_WIDGET_UI_PREFS_KEY = "calendarWidgetUiPrefs"

fun loadCalendarWidgetUiPrefs(context: Context, gson: Gson = Gson()): CalendarWidgetUiPrefs {
    val prefs = context.applicationContext.getSharedPreferences("dosevia_prefs", Context.MODE_PRIVATE)
    val raw = prefs.getString(CALENDAR_WIDGET_UI_PREFS_KEY, null) ?: return CalendarWidgetUiPrefs()
    return try {
        gson.fromJson(raw, CalendarWidgetUiPrefs::class.java) ?: CalendarWidgetUiPrefs()
    } catch (_: Exception) {
        CalendarWidgetUiPrefs()
    }
}

fun saveCalendarWidgetUiPrefs(context: Context, uiPrefs: CalendarWidgetUiPrefs, gson: Gson = Gson()) {
    context.applicationContext
        .getSharedPreferences("dosevia_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString(CALENDAR_WIDGET_UI_PREFS_KEY, gson.toJson(uiPrefs))
        .apply()
}
