package com.dosevia.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import java.text.SimpleDateFormat
import java.util.*

class PillWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val ctx         = context.applicationContext
            val configPrefs = ctx.getSharedPreferences("dosevia_prefs",  Context.MODE_PRIVATE)
            val statusPrefs = ctx.getSharedPreferences("dosevia_status", Context.MODE_PRIVATE)

            val pillTypeName = configPrefs.getString("pillType", PillType.TYPE_21_7.name) ?: PillType.TYPE_21_7.name
            val pillType     = try { PillType.valueOf(pillTypeName) } catch (_: Exception) { PillType.TYPE_21_7 }

            val gson = GsonBuilder()
                .registerTypeAdapter(Date::class.java, JsonDeserializer { json, _, _ ->
                    try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(json.asString) }
                    catch (_: Exception) { try { Date(json.asLong) } catch (_: Exception) { Date() } }
                })
                .registerTypeAdapter(Date::class.java, JsonSerializer<Date> { src, _, _ ->
                    com.google.gson.JsonPrimitive(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(src))
                })
                .create()

            val customConfig = configPrefs.getString("customPillConfig", null)?.let {
                try { gson.fromJson(it, CustomPillConfig::class.java) } catch (_: Exception) { null }
            } ?: CustomPillConfig()

            val config     = getPillConfiguration(pillType, customConfig)
            val totalPills = config.total

            // Count TAKEN by scanning statusPrefs keys directly — no JSON parsing needed
            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            var takenPills = 0
            val allStatus  = statusPrefs.all
            for ((key, value) in allStatus) {
                if (key.startsWith("status_") && value == PillStatus.TAKEN.name) {
                    takenPills++
                }
            }

            val bitmap = WidgetBitmapRenderer.render(
                context    = ctx,
                pillLabel  = pillType.displayName,
                takenPills = takenPills,
                totalPills = totalPills
            )

            val views = RemoteViews(ctx.packageName, R.layout.pill_widget)
            views.setImageViewBitmap(R.id.widgetImage, bitmap)

            val launchIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(ctx, 0, launchIntent, piFlags)
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestUpdate(context: Context) {
            val ctx    = context.applicationContext
            val intent = Intent(ctx, PillWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, PillWidget::class.java))
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                ctx.sendBroadcast(intent)
            }
        }
    }
}
