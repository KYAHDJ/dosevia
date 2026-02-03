package com.dosevia.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.dosevia.app.R
import org.json.JSONArray
import android.util.Log

class DoseviaLargeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == "com.dosevia.app.UPDATE_WIDGETS") {
            context?.let {
                val mgr = AppWidgetManager.getInstance(it)
                val ids = mgr.getAppWidgetIds(
                    android.content.ComponentName(it, DoseviaLargeWidget::class.java)
                )
                onUpdate(it, mgr, ids)
            }
        }
    }

    companion object {
        // Exactly 28 pill IDs matching the layout (pills 1-28 only)
        private val PILL_IDS = intArrayOf(
            R.id.pill_1,  R.id.pill_2,  R.id.pill_3,  R.id.pill_4,
            R.id.pill_5,  R.id.pill_6,  R.id.pill_7,
            R.id.pill_8,  R.id.pill_9,  R.id.pill_10, R.id.pill_11,
            R.id.pill_12, R.id.pill_13, R.id.pill_14,
            R.id.pill_15, R.id.pill_16, R.id.pill_17, R.id.pill_18,
            R.id.pill_19, R.id.pill_20, R.id.pill_21,
            R.id.pill_22, R.id.pill_23, R.id.pill_24, R.id.pill_25,
            R.id.pill_26, R.id.pill_27, R.id.pill_28
        )

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_large)

            val prefs      = context.getSharedPreferences("dosevia_data", Context.MODE_PRIVATE)
            val daysJson   = prefs.getString("days", null)
            val pillType   = prefs.getString("pillType", "21+7")

            // Header
            views.setTextViewText(R.id.widget_calendar_title, "PILL PACK")
            views.setTextViewText(R.id.widget_calendar_subtitle, subtitleFor(pillType))

            // --- Reset all 28 pills to default "not taken" silver appearance ---
            for (id in PILL_IDS) {
                views.setInt(id, "setVisibility", android.view.View.VISIBLE)
                views.setInt(id, "setBackgroundResource", R.drawable.pill_not_taken)
                views.setTextColor(id, Color.parseColor("#374151"))
            }

            // --- Overlay real data if available ---
            if (daysJson != null) {
                try {
                    val arr = JSONArray(daysJson)
                    val now = java.util.Calendar.getInstance()

                    for (i in 0 until minOf(arr.length(), PILL_IDS.size)) {
                        val day    = arr.getJSONObject(i)
                        val status = day.getString("status")
                        val dayNum = day.getInt("day")
                        val dateStr = day.optString("date", "")

                        val pillId = PILL_IDS[i]

                        // Always show the day number
                        views.setTextViewText(pillId, dayNum.toString())

                        // Determine today
                        var isToday  = false
                        var isFuture = false
                        if (dateStr.isNotEmpty()) {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val date = sdf.parse(dateStr)
                                if (date != null) {
                                    val pilCal = java.util.Calendar.getInstance()
                                    pilCal.time = date
                                    isToday = pilCal.get(java.util.Calendar.YEAR)     == now.get(java.util.Calendar.YEAR) &&
                                              pilCal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
                                    isFuture = date.after(now.time)
                                }
                            } catch (_: Exception) {}
                        }

                        when (status) {
                            "taken" -> {
                                views.setInt(pillId, "setBackgroundResource", R.drawable.pill_taken)
                                views.setTextColor(pillId, Color.WHITE)
                            }
                            "missed" -> {
                                views.setInt(pillId, "setBackgroundResource", R.drawable.pill_missed)
                                views.setTextColor(pillId, Color.WHITE)
                            }
                            else -> {
                                // not_taken
                                if (isToday) {
                                    views.setInt(pillId, "setBackgroundResource", R.drawable.pill_today)
                                    views.setTextColor(pillId, Color.parseColor("#374151"))
                                } else if (isFuture) {
                                    views.setInt(pillId, "setBackgroundResource", R.drawable.pill_future)
                                    views.setTextColor(pillId, Color.parseColor("#6B7280"))
                                } else {
                                    views.setInt(pillId, "setBackgroundResource", R.drawable.pill_not_taken)
                                    views.setTextColor(pillId, Color.parseColor("#374151"))
                                }
                            }
                        }
                    }
                    Log.d("DoseviaLargeWidget", "Calendar updated with ${arr.length()} pills")
                } catch (e: Exception) {
                    Log.e("DoseviaLargeWidget", "Error parsing days JSON", e)
                }
            } else {
                Log.w("DoseviaLargeWidget", "No days data in SharedPreferences")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun subtitleFor(pillType: String?): String = when (pillType) {
            "21+7"        -> "28 Day Calendar"
            "24+4"        -> "28 Day Calendar"
            "26+2"        -> "28 Day Calendar"
            "28-day"      -> "28 Day Calendar"
            "84+7"        -> "91 Day Pack"
            "84+7-low"    -> "91 Day Pack"
            "365-day"     -> "365 Day Pack"
            "28-pop"      -> "28 Day POP"
            else          -> "28 Day Calendar"
        }
    }
}
