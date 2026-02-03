package com.dosevia.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dosevia.app.R

class DoseviaSmallWidget : AppWidgetProvider() {

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
                    android.content.ComponentName(it, DoseviaSmallWidget::class.java)
                )
                onUpdate(it, mgr, ids)
            }
        }
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_small)

            val prefs      = context.getSharedPreferences("dosevia_data", Context.MODE_PRIVATE)
            val takenCount = prefs.getInt("takenCount", 0)
            val totalCount = prefs.getInt("totalCount", 28)

            views.setTextViewText(R.id.widget_count, "$takenCount")
            views.setTextViewText(R.id.widget_total, "/ $totalCount")

            // Checkmark circle is the default icon (matches reference).
            // Swap to pill icon only when ALL pills are done.
            if (takenCount >= totalCount && totalCount > 0) {
                views.setInt(R.id.widget_checkmark,  "setVisibility", android.view.View.GONE)
                views.setInt(R.id.widget_pill_icon,  "setVisibility", android.view.View.VISIBLE)
            } else {
                views.setInt(R.id.widget_checkmark,  "setVisibility", android.view.View.VISIBLE)
                views.setInt(R.id.widget_pill_icon,  "setVisibility", android.view.View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
