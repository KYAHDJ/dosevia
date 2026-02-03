package com.dosevia.app

import android.content.Context
import android.content.Intent
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import android.util.Log

@CapacitorPlugin(name = "WidgetSync")
class WidgetSyncPlugin : Plugin() {

    private val TAG = "WidgetSyncPlugin"

    /**
     * Internal method that does the actual widget refresh.
     * Does NOT touch PluginCall — safe to call from anywhere.
     */
    private fun doUpdateAllWidgets() {
        val context = context ?: return

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)

        // --- Small widgets ---
        val smallIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, DoseviaSmallWidget::class.java)
        )
        for (id in smallIds) {
            DoseviaSmallWidget.updateAppWidget(context, appWidgetManager, id)
        }
        Log.d(TAG, "Updated ${smallIds.size} small widget(s)")

        // --- Medium widgets ---
        val mediumIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, DoseviaMediumWidget::class.java)
        )
        for (id in mediumIds) {
            DoseviaMediumWidget.updateAppWidget(context, appWidgetManager, id)
        }
        Log.d(TAG, "Updated ${mediumIds.size} medium widget(s)")

        // --- Large widgets ---
        val largeIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, DoseviaLargeWidget::class.java)
        )
        for (id in largeIds) {
            DoseviaLargeWidget.updateAppWidget(context, appWidgetManager, id)
        }
        Log.d(TAG, "Updated ${largeIds.size} large widget(s)")

        // Broadcast as belt-and-suspenders backup
        context.sendBroadcast(Intent("com.dosevia.app.UPDATE_WIDGETS"))
    }

    // ---- Capacitor-facing methods ----

    @PluginMethod
    fun updateWidgets(call: PluginCall) {
        try {
            doUpdateAllWidgets()
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateWidgets", e)
            call.reject("Failed to update widgets: ${e.message}")
        }
    }

    @PluginMethod
    fun savePillData(call: PluginCall) {
        try {
            val days        = call.getString("days")
            val startDate   = call.getString("startDate")
            val pillType    = call.getString("pillType")
            val takenCount  = call.getInt("takenCount")  ?: 0
            val missedCount = call.getInt("missedCount") ?: 0
            val totalCount  = call.getInt("totalCount")  ?: 0

            Log.d(TAG, "savePillData: total=$totalCount taken=$takenCount missed=$missedCount start=$startDate type=$pillType daysLen=${days?.length}")

            val prefs  = context.getSharedPreferences("dosevia_data", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            days?.let        { editor.putString("days",        it) }
            startDate?.let   { editor.putString("startDate",   it) }
            pillType?.let    { editor.putString("pillType",    it) }
            editor.putInt("takenCount",  takenCount)
            editor.putInt("missedCount", missedCount)
            editor.putInt("totalCount",  totalCount)
            editor.putLong("lastUpdate", System.currentTimeMillis())
            editor.apply()

            Log.d(TAG, "Pill data saved to SharedPreferences")

            // Now refresh widgets — uses the internal helper, NOT the PluginMethod
            doUpdateAllWidgets()

            // Resolve the call exactly once
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "Error in savePillData", e)
            call.reject("Failed to save pill data: ${e.message}")
        }
    }

    @PluginMethod
    fun requestPinWidget(call: PluginCall) {
        // placeholder — just resolve
        call.resolve()
    }
}
