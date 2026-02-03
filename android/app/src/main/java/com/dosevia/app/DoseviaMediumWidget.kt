package com.dosevia.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dosevia.app.R

class DoseviaMediumWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == "com.dosevia.app.UPDATE_WIDGETS") {
            context?.let {
                val appWidgetManager = AppWidgetManager.getInstance(it)
                val thisWidget = android.content.ComponentName(it, DoseviaMediumWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                onUpdate(it, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_medium)
            
            val prefs = context.getSharedPreferences("dosevia_data", Context.MODE_PRIVATE)
            val takenCount = prefs.getInt("takenCount", 0)
            val missedCount = prefs.getInt("missedCount", 0)
            val totalCount = prefs.getInt("totalCount", 28)
            
            // Update statistics
            views.setTextViewText(R.id.widget_total_count, totalCount.toString())
            views.setTextViewText(R.id.widget_taken_count, takenCount.toString())
            views.setTextViewText(R.id.widget_missed_count, missedCount.toString())
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
