package com.dosevia.app

import android.content.Context
import android.graphics.*
import java.text.DateFormatSymbols
import java.util.*

object WidgetCalendarBitmapRenderer {

    private const val MIN_W_DP = 220
    private const val MIN_H_DP = 170
    private const val MAX_W_DP = 320
    private const val MAX_H_DP = 280

    // Keep bitmap payload safely below binder transaction limits for RemoteViews
    private const val MAX_BITMAP_PIXELS = 220_000

    fun render(
        context: Context,
        year: Int,
        month: Int,
        dayStatus: Map<Int, PillStatus?>,
        missedCount: Int,
        widthDp: Int,
        heightDp: Int
    ): Bitmap {
        val dm = context.resources.displayMetrics
        val requestedW = (widthDp.coerceIn(MIN_W_DP, MAX_W_DP) * dm.density).toInt()
        val requestedH = (heightDp.coerceIn(MIN_H_DP, MAX_H_DP) * dm.density).toInt()

        val scale = kotlin.math.min(
            1f,
            kotlin.math.sqrt(MAX_BITMAP_PIXELS.toFloat() / (requestedW * requestedH).toFloat())
        )
        val w = (requestedW * scale).toInt().coerceAtLeast(280)
        val h = (requestedH * scale).toInt().coerceAtLeast(200)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        // Pink background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F609BC") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Subtle diagonal stripes (closer to reference)
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
            strokeWidth = (w * 0.015f)
        }
        var x = -h.toFloat()
        while (x < w + h) {
            canvas.drawLine(x, 0f, x + h, h.toFloat(), stripePaint)
            x += w * 0.09f
        }

        val sidePad = w * 0.05f

        // Header capsule
        val headerRect = RectF(
            sidePad,
            h * 0.06f,
            w - sidePad,
            h * 0.30f
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                headerRect.top,
                0f,
                headerRect.bottom,
                intArrayOf(Color.parseColor("#DADDE3"), Color.parseColor("#BCC2CB")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, w * 0.045f, w * 0.045f, headerPaint)

        val headerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80909AA7")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.006f
        }
        canvas.drawRoundRect(headerRect, w * 0.045f, w * 0.045f, headerStroke)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.085f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DOSEVIA CALENDAR", w / 2f, headerRect.top + headerRect.height() * 0.45f, titlePaint)

        val monthLabel = "${DateFormatSymbols.getInstance().months[month]} $year"
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = w * 0.047f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(monthLabel, w / 2f, headerRect.top + headerRect.height() * 0.78f, monthPaint)

        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.043f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val gridLeft = sidePad + w * 0.03f
        val gridRight = w - sidePad - w * 0.03f
        val weekY = h * 0.40f
        val colWidth = (gridRight - gridLeft) / 7f

        for (i in weekdays.indices) {
            val cx = gridLeft + i * colWidth + colWidth / 2f
            canvas.drawText(weekdays[i], cx, weekY, weekPaint)
        }

        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridTop = weekY + h * 0.04f
        val rowHeight = (h - gridTop - h * 0.06f) / 5f
        val pillRadius = minOf(colWidth, rowHeight) * 0.41f

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = pillRadius * 0.86f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        for (day in 1..totalDays) {
            val slot = firstDayOffset + (day - 1)
            val row = slot / 7
            val col = slot % 7

            val cx = gridLeft + col * colWidth + colWidth / 2f
            val cy = gridTop + row * rowHeight + rowHeight / 2f

            val status = dayStatus[day]
            val (outer, inner, text) = when (status) {
                PillStatus.MISSED -> Triple(Color.parseColor("#EF4444"), Color.parseColor("#F87171"), Color.WHITE)
                PillStatus.TAKEN -> Triple(Color.parseColor("#1F2937"), Color.parseColor("#374151"), Color.parseColor("#D1D5DB"))
                PillStatus.NOT_TAKEN -> Triple(Color.parseColor("#A8ADB5"), Color.parseColor("#DCE1E8"), Color.parseColor("#111827"))
                null -> Triple(Color.parseColor("#A8ADB5"), Color.parseColor("#DCE1E8"), Color.parseColor("#111827"))
            }

            outerPaint.color = outer
            innerPaint.color = inner
            dayPaint.color = text

            canvas.drawCircle(cx, cy, pillRadius, outerPaint)
            canvas.drawCircle(cx, cy, pillRadius * 0.82f, innerPaint)
            val textY = cy - (dayPaint.ascent() + dayPaint.descent()) / 2f
            canvas.drawText(day.toString(), cx, textY, dayPaint)
        }

        return bitmap
    }

    fun renderFallback(context: Context): Bitmap {
        val dm = context.resources.displayMetrics
        val w = (MIN_W_DP * dm.density).toInt().coerceAtLeast(280)
        val h = (MIN_H_DP * dm.density).toInt().coerceAtLeast(200)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F609BC") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = w * 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DOSEVIA CALENDAR", w / 2f, h / 2f, textPaint)
        return bitmap
    }
}
