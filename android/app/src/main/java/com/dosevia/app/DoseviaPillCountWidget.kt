package com.dosevia.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.util.Log

/**
 * Single, clean pill count widget showing "Pills Taken Today: X / Y"
 * 
 * KEY PRINCIPLES:
 * 1. Reads ONLY from SharedPreferences (never calculates or stores its own data)
 * 2. Updates instantly when app changes pill data
 * 3. Opens app when tapped
 * 4. Uses atomic data from single source of truth: "pill_data" SharedPreferences
 */
class DoseviaPillCountWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "DoseviaPillCountWidget"
        
        // SharedPreferences configuration
        private const val PREFS_NAME = "pill_data"
        private const val KEY_PILLS_TAKEN_CURRENT_CYCLE = "pillsTakenCurrentCycle"
        private const val KEY_TOTAL_PILLS_CURRENT_CYCLE = "totalPillsCurrentCycle"
        private const val KEY_LAST_UPDATED = "lastUpdated"
        
        /**
         * Update a single widget instance
         * This is the ONLY place where widget UI is updated
         */
        internal fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Read from SharedPreferences - single source of truth
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pillsTaken = prefs.getInt(KEY_PILLS_TAKEN_CURRENT_CYCLE, 0)
            val totalPills = prefs.getInt(KEY_TOTAL_PILLS_CURRENT_CYCLE, 28)
            val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
            
            Log.d(TAG, "📱 WIDGET UPDATE START")
            Log.d(TAG, "  Widget ID: $appWidgetId")
            Log.d(TAG, "  Reading from SharedPreferences: '$PREFS_NAME'")
            Log.d(TAG, "  Pills Taken: $pillsTaken")
            Log.d(TAG, "  Total Pills: $totalPills")
            Log.d(TAG, "  Last Updated: $lastUpdated")
            Log.d(TAG, "  Display Text: '$pillsTaken / $totalPills'")
            
            // Create RemoteViews for the widget layout
            val views = RemoteViews(context.packageName, R.layout.widget_pill_count)
            
            // Set the pill count text
            views.setTextViewText(
                R.id.widget_pill_count, 
                "$pillsTaken / $totalPills"
            )
            
            // Set up click action to open the app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "✅ WIDGET UPDATE COMPLETE")
        }
        
        /**
         * Trigger update for all instances of this widget
         * Called from app when pill data changes
         */
        fun updateAllInstances(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DoseviaPillCountWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            Log.d(TAG, "Updating ${appWidgetIds.size} widget instance(s)")
            
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
    
    /**
     * Called when widget is added or update interval expires
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widget(s)")
        
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    /**
     * Handle custom broadcast intents for manual updates
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            "com.dosevia.app.UPDATE_WIDGET" -> {
                Log.d(TAG, "Received UPDATE_WIDGET broadcast")
                updateAllInstances(context)
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, DoseviaPillCountWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
    
    /**
     * Called when widget is removed
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "Widget(s) deleted: ${appWidgetIds.size}")
        super.onDeleted(context, appWidgetIds)
    }
    
    /**
     * Called when first widget is added
     */
    override fun onEnabled(context: Context) {
        Log.d(TAG, "First widget enabled")
        super.onEnabled(context)
    }
    
    /**
     * Called when last widget is removed
     */
    override fun onDisabled(context: Context) {
        Log.d(TAG, "Last widget disabled")
        super.onDisabled(context)
    }
}
