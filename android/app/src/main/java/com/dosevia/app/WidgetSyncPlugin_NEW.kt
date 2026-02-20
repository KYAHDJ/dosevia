package com.dosevia.app

import android.content.Context
import android.util.Log
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Capacitor plugin for syncing pill data with home screen widget
 * 
 * CRITICAL RULES:
 * 1. Updates SharedPreferences FIRST
 * 2. Then triggers widget refresh
 * 3. Never reverse this order
 * 4. Uses atomic writes with .apply()
 */
@CapacitorPlugin(name = "WidgetSync")
class WidgetSyncPlugin : Plugin() {

    companion object {
        private const val TAG = "WidgetSyncPlugin"
        
        // MUST match widget's SharedPreferences name
        private const val PREFS_NAME = "pill_data"
        
        // Storage keys for cycle-based tracking
        private const val KEY_PILLS_TAKEN_CURRENT_CYCLE = "pillsTakenCurrentCycle"
        private const val KEY_TOTAL_PILLS_CURRENT_CYCLE = "totalPillsCurrentCycle"
        private const val KEY_LAST_UPDATED = "lastUpdated"
    }
    
    /**
     * Save pill count data for current cycle and trigger widget update
     * 
     * This is called whenever:
     * - User marks a pill as taken
     * - User untakes a pill
     * - User starts a new cycle
     * - User changes pill type (which resets the cycle)
     * 
     * FLOW:
     * 1. Validate data
     * 2. Write to SharedPreferences synchronously (commit())
     * 3. Update widget immediately
     * 4. Resolve call
     */
    @PluginMethod
    fun savePillCount(call: PluginCall) {
        try {
            // Get data from JavaScript
            val pillsTaken = call.getInt("pillsTakenCurrentCycle") ?: 0
            val totalPills = call.getInt("totalPillsCurrentCycle") ?: 28
            val pillType = call.getString("pillType") // Optional pill type
            
            Log.d(TAG, "💊 Saving pill count: $pillsTaken / $totalPills (type: $pillType)")
            
            // STEP 1: Update SharedPreferences SYNCHRONOUSLY with commit()
            // This is CRITICAL - apply() is async and causes race conditions!
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt(KEY_PILLS_TAKEN_CURRENT_CYCLE, pillsTaken)
            editor.putInt(KEY_TOTAL_PILLS_CURRENT_CYCLE, totalPills)
            editor.putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            
            // Store pill type if provided
            pillType?.let { editor.putString("pillType", it) }
            
            // CRITICAL: Use commit() not apply() to avoid race condition
            val success = editor.commit()
            
            if (!success) {
                Log.e(TAG, "⚠️ WARNING: SharedPreferences commit returned false!")
            } else {
                Log.d(TAG, "✅ SharedPreferences committed successfully: $pillsTaken / $totalPills")
            }
            
            // STEP 2: Now widget update is safe - data is guaranteed to be persisted
            try {
                // Method 1: Direct update
                DoseviaPillCountWidget.updateAllInstances(context)
                Log.d(TAG, "✅ Direct widget update triggered")
                
                // Method 2: Broadcast update (backup)
                val intent = android.content.Intent(context, DoseviaPillCountWidget::class.java)
                intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = android.appwidget.AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(android.content.ComponentName(context, DoseviaPillCountWidget::class.java))
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
                Log.d(TAG, "✅ Broadcast sent to ${ids.size} widget(s)")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Widget update failed: ${e.message}")
            }
            
            // STEP 3: Notify JavaScript that save completed
            call.resolve()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving pill count", e)
            call.reject("Failed to save pill count: ${e.message}")
        }
    }
    
    /**
     * Force refresh widget without saving new data
     * Useful for manual refresh or debugging
     */
    @PluginMethod
    fun refreshWidget(call: PluginCall) {
        try {
            Log.d(TAG, "Manual widget refresh requested")
            
            DoseviaPillCountWidget.updateAllInstances(context)
            
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing widget", e)
            call.reject("Failed to refresh widget: ${e.message}")
        }
    }
    
    /**
     * Get current pill count from SharedPreferences
     * Useful for verifying sync status
     */
    @PluginMethod
    fun getPillCount(call: PluginCall) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pillsTaken = prefs.getInt(KEY_PILLS_TAKEN_CURRENT_CYCLE, 0)
            val totalPills = prefs.getInt(KEY_TOTAL_PILLS_CURRENT_CYCLE, 28)
            val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
            
            val result = mapOf(
                "pillsTakenCurrentCycle" to pillsTaken,
                "totalPillsCurrentCycle" to totalPills,
                "lastUpdated" to lastUpdated
            )
            
            call.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pill count", e)
            call.reject("Failed to get pill count: ${e.message}")
        }
    }

    /**
     * BACKWARD COMPATIBILITY: Old method that syncs all widget data
     * This is called by the existing syncWidgetData function
     */
    @PluginMethod
    fun savePillData(call: PluginCall) {
        try {
            val takenCount = call.getInt("takenCount") ?: 0
            val totalCount = call.getInt("totalCount") ?: 28
            val pillType = call.getString("pillType") // Optional
            
            Log.d(TAG, "📊 savePillData: $takenCount / $totalCount (type: $pillType)")
            
            // Save to SharedPreferences SYNCHRONOUSLY with commit()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt(KEY_PILLS_TAKEN_CURRENT_CYCLE, takenCount)
            editor.putInt(KEY_TOTAL_PILLS_CURRENT_CYCLE, totalCount)
            editor.putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            
            // Store pill type if provided
            pillType?.let { editor.putString("pillType", it) }
            
            // CRITICAL: commit() not apply()
            val success = editor.commit()
            
            if (!success) {
                Log.e(TAG, "⚠️ WARNING: SharedPreferences commit returned false!")
            } else {
                Log.d(TAG, "✅ SharedPreferences committed: $takenCount / $totalCount")
            }
            
            // Update widget safely now
            try {
                DoseviaPillCountWidget.updateAllInstances(context)
                Log.d(TAG, "✅ Direct widget update triggered")
                
                // Also send broadcast
                val intent = android.content.Intent(context, DoseviaPillCountWidget::class.java)
                intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = android.appwidget.AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(android.content.ComponentName(context, DoseviaPillCountWidget::class.java))
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
                Log.d(TAG, "✅ Broadcast sent to ${ids.size} widget(s)")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Widget update failed: ${e.message}")
            }
            
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in savePillData", e)
            call.reject("Failed to save pill data: ${e.message}")
        }
    }
}
