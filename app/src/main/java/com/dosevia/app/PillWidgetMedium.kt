package com.dosevia.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import java.text.SimpleDateFormat
import java.util.*

class PillWidgetMedium : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Re-render at new size when user resizes the widget
        updateWidget(context, appWidgetManager, appWidgetId)
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
            val savedStartMs = configPrefs.getLong("startDate", 0L)
            val startDate = if (savedStartMs != 0L) Date(savedStartMs) else Date()

            var takenPills  = 0
            var missedPills = 0
            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance().apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            repeat(totalPills) {
                when (statusPrefs.getString("status_${keyFmt.format(cal.time)}", null)) {
                    PillStatus.TAKEN.name  -> takenPills++
                    PillStatus.MISSED.name -> missedPills++
                }
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Get current widget size in dp so bitmap scales with resize
            val options  = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minH     = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

            val theme = loadWidgetThemesFromPrefs(ctx).forKind(WidgetKind.MEDIUM)

            val locked = PremiumAccess.readTier(ctx) == UserTier.FREE

            val bitmap = WidgetMediumBitmapRenderer.render(
                context     = ctx,
                totalPills  = totalPills,
                takenPills  = takenPills,
                missedPills = missedPills,
                widthDp     = minW,
                heightDp    = minH,
                theme       = theme,
                locked      = locked
            )

            val views = RemoteViews(ctx.packageName, R.layout.pill_widget_medium)
            views.setImageViewBitmap(R.id.widgetMediumImage, bitmap)

            val launchIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(ctx, 1, launchIntent, piFlags)
            views.setOnClickPendingIntent(R.id.widgetMediumRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestUpdate(context: Context) {
            val ctx    = context.applicationContext
            val intent = Intent(ctx, PillWidgetMedium::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, PillWidgetMedium::class.java))
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                ctx.sendBroadcast(intent)
            }
        }
    }
}
