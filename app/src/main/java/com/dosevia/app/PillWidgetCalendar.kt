package com.dosevia.app

import android.app.AlarmManager
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PillWidgetCalendar : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MARK_TAKEN -> {
                setStatusForToday(context, PillStatus.TAKEN)
                requestUpdate(context)
                PillWidget.requestUpdate(context)
                PillWidgetMedium.requestUpdate(context)
            }
            ACTION_MARK_NOT_TAKEN -> {
                setStatusForToday(context, PillStatus.NOT_TAKEN)
                requestUpdate(context)
                PillWidget.requestUpdate(context)
                PillWidgetMedium.requestUpdate(context)
            }
            ACTION_PULSE, ACTION_MIDNIGHT_REFRESH -> {
                requestUpdate(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePulse(context)
        scheduleMidnightRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelAlarm(context, ACTION_PULSE, RC_PULSE)
        cancelAlarm(context, ACTION_MIDNIGHT_REFRESH, RC_MIDNIGHT)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        schedulePulse(context)
        scheduleMidnightRefresh(context)
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
        private const val ACTION_MARK_TAKEN = "com.dosevia.app.widget.calendar.MARK_TAKEN"
        private const val ACTION_MARK_NOT_TAKEN = "com.dosevia.app.widget.calendar.MARK_NOT_TAKEN"
        private const val ACTION_PULSE = "com.dosevia.app.widget.calendar.PULSE"
        private const val ACTION_MIDNIGHT_REFRESH = "com.dosevia.app.widget.calendar.MIDNIGHT_REFRESH"
        private const val RC_MARK_TAKEN = 3101
        private const val RC_MARK_NOT_TAKEN = 3102
        private const val RC_PULSE = 3103
        private const val RC_MIDNIGHT = 3104

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val ctx = context.applicationContext
            val configPrefs = ctx.getSharedPreferences("dosevia_prefs", Context.MODE_PRIVATE)
            val statusPrefs = ctx.getSharedPreferences("dosevia_status", Context.MODE_PRIVATE)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val uiPrefs = loadCalendarWidgetUiPrefs(ctx)

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
            val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val pillTypeName = configPrefs.getString("pillType", PillType.TYPE_21_7.name) ?: PillType.TYPE_21_7.name
            val pillType = try { PillType.valueOf(pillTypeName) } catch (_: Exception) { PillType.TYPE_21_7 }
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
            val config = getPillConfiguration(pillType, customConfig)
            val savedStartMs = configPrefs.getLong("startDate", 0L)
            val startDate = if (savedStartMs != 0L) Date(savedStartMs) else Date()
            val startCal = Calendar.getInstance().apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startCal.timeInMillis
                add(Calendar.DAY_OF_MONTH, config.total - 1)
            }

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
            var missedCount = 0

            for (day in 1..daysInMonth) {
                val dayCal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val inRegimen = dayCal.timeInMillis in startCal.timeInMillis..endCal.timeInMillis
                if (!inRegimen) {
                    monthStatus[day] = null
                    continue
                }
                val key = keyFmt.format(dayCal.time)
                val status = statusByDateKey[key]
                monthStatus[day] = status
                if (status == PillStatus.MISSED) missedCount++
            }

            val theme = loadWidgetThemesFromPrefs(ctx).forKind(WidgetKind.CALENDAR)

            val pulseScale = if (uiPrefs.todayPulseEnabled) {
                val phase = (System.currentTimeMillis() / 900.0) * Math.PI
                val raw = 0.5f + (0.5f * kotlin.math.sin(phase).toFloat())
                0.92f + (raw * 0.13f)
            } else {
                1f
            }

            val bitmap = WidgetCalendarBitmapRenderer.render(
                context = ctx,
                year = year,
                month = month,
                dayStatus = monthStatus,
                missedCount = missedCount,
                widthDp = minW,
                heightDp = minH,
                theme = theme,
                todayDay = todayDayOfMonth,
                todayIndicatorColor = uiPrefs.todayIndicatorColor,
                todayPulseScale = pulseScale
            )

            val views = RemoteViews(ctx.packageName, R.layout.pill_widget_calendar)
            views.setImageViewBitmap(R.id.widgetCalendarImage, bitmap)

            val launchIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val piFlags = immutableUpdateCurrentFlags()
            views.setOnClickPendingIntent(
                R.id.widgetCalendarRoot,
                PendingIntent.getActivity(ctx, 2, launchIntent, piFlags)
            )

            val takenIntent = Intent(ctx, PillWidgetCalendar::class.java).apply { action = ACTION_MARK_TAKEN }
            val notTakenIntent = Intent(ctx, PillWidgetCalendar::class.java).apply { action = ACTION_MARK_NOT_TAKEN }
            views.setOnClickPendingIntent(
                R.id.calendarButtonTaken,
                PendingIntent.getBroadcast(ctx, RC_MARK_TAKEN, takenIntent, piFlags)
            )
            views.setOnClickPendingIntent(
                R.id.calendarButtonNotTaken,
                PendingIntent.getBroadcast(ctx, RC_MARK_NOT_TAKEN, notTakenIntent, piFlags)
            )

            val visibility = if (uiPrefs.showActionButtons) android.view.View.VISIBLE else android.view.View.GONE
            views.setViewVisibility(R.id.calendarButtonsRow, visibility)
            views.setInt(R.id.calendarButtonTaken, "setBackgroundColor", uiPrefs.takenButtonBackgroundColor)
            views.setInt(R.id.calendarButtonNotTaken, "setBackgroundColor", uiPrefs.notTakenButtonBackgroundColor)
            views.setTextColor(R.id.calendarButtonTaken, uiPrefs.buttonTextColor)
            views.setTextColor(R.id.calendarButtonNotTaken, uiPrefs.buttonTextColor)

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

        private fun setStatusForToday(context: Context, status: PillStatus) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayKey = fmt.format(Date())
            context.applicationContext
                .getSharedPreferences("dosevia_status", Context.MODE_PRIVATE)
                .edit()
                .putString("status_$todayKey", status.name)
                .apply()
        }

        private fun immutableUpdateCurrentFlags(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }

        private fun schedulePulse(context: Context) {
            val ctx = context.applicationContext
            val ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, PillWidgetCalendar::class.java))
            if (ids.isEmpty()) return

            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pulseIntent = Intent(ctx, PillWidgetCalendar::class.java).apply { action = ACTION_PULSE }
            val pending = PendingIntent.getBroadcast(ctx, RC_PULSE, pulseIntent, immutableUpdateCurrentFlags())
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                System.currentTimeMillis() + 900L,
                pending
            )
        }

        private fun scheduleMidnightRefresh(context: Context) {
            val ctx = context.applicationContext
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val midnightIntent = Intent(ctx, PillWidgetCalendar::class.java).apply { action = ACTION_MIDNIGHT_REFRESH }
            val pending = PendingIntent.getBroadcast(ctx, RC_MIDNIGHT, midnightIntent, immutableUpdateCurrentFlags())

            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMidnight.timeInMillis, pending)
        }

        private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
            val ctx = context.applicationContext
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ctx, PillWidgetCalendar::class.java).apply { this.action = action }
            val pending = PendingIntent.getBroadcast(ctx, requestCode, intent, immutableUpdateCurrentFlags())
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }
}
