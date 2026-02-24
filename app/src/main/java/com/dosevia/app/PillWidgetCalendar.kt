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
import java.text.SimpleDateFormat
import java.util.*

class PillWidgetCalendar : AppWidgetProvider() {

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
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val ctx = context.applicationContext
            val statusPrefs = ctx.getSharedPreferences("dosevia_status", Context.MODE_PRIVATE)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val statusByDateKey = mutableMapOf<String, PillStatus>()
            for ((key, value) in statusPrefs.all) {
                if (key.startsWith("status_")) {
                    val statusName = value?.toString() ?: continue
                    val status = try { PillStatus.valueOf(statusName) } catch (_: Exception) { null } ?: continue
                    statusByDateKey[key.removePrefix("status_")] = status
                }
            }

            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val monthStatus = mutableMapOf<Int, PillStatus?>()

            for (day in 1..daysInMonth) {
                val dayCal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val key = keyFmt.format(dayCal.time)
                val status = statusByDateKey[key]
                monthStatus[day] = status
            }

            val bitmap = runCatching {
                WidgetCalendarBitmapRenderer.render(
                    context = ctx,
                    year = year,
                    month = month,
                    dayStatus = monthStatus,
                    widthDp = minW,
                    heightDp = minH
                )
            }.getOrElse {
                WidgetCalendarBitmapRenderer.renderFallback(ctx)
            }

            val views = RemoteViews(ctx.packageName, R.layout.pill_widget_calendar)
            views.setImageViewBitmap(R.id.widgetCalendarImage, bitmap)

            val launchIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(ctx, 2, launchIntent, piFlags)
            views.setOnClickPendingIntent(R.id.widgetCalendarRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestUpdate(context: Context) {
            val ctx = context.applicationContext
            val intent = Intent(ctx, PillWidgetCalendar::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, PillWidgetCalendar::class.java))
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                ctx.sendBroadcast(intent)
            }
        }
    }
}
