package com.dosevia.app

import android.content.Context
import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renders the widget as a Bitmap so we can draw anything we want
 * (arcs, gradients, custom fonts) inside a RemoteViews ImageView.
 */
object WidgetBitmapRenderer {

    fun render(
        context: Context,
        pillLabel: String,
        takenPills: Int,
        totalPills: Int,
        theme: WidgetThemeColors
    ): Bitmap {
        val dm = context.resources.displayMetrics
        val size = (160 * dm.density).toInt().coerceAtLeast(160)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        // ── Background: dark semi-transparent circle ──────────────────────────
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.background // very dark navy, ~80% opaque
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, cx * 0.92f, bgPaint)

        // ── Outer open-arc track (incomplete circle — gap at top) ─────────────
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = cx * 0.065f
            strokeCap = Paint.Cap.ROUND
        }
        val arcInset = cx * 0.10f
        val arcRect = RectF(arcInset, arcInset, size - arcInset, size - arcInset)

        // Arc starts at 130° and spans 280° — leaving a 80° gap at the top
        val startAngle = 130f
        val sweepTotal = 280f
        canvas.drawArc(arcRect, startAngle, sweepTotal, false, trackPaint)

        // ── Progress arc (pink→orange gradient) ──────────────────────────────
        val progress = if (totalPills > 0) takenPills.toFloat() / totalPills else 0f
        val sweepProgress = sweepTotal * progress

        if (sweepProgress > 0f) {
            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = cx * 0.065f
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(
                    cx, cy,
                    intArrayOf(
                        theme.accent1,
                        theme.accent2,
                        theme.accent1
                    ),
                    floatArrayOf(0f, 0.5f, 1f)
                ).also {
                    // Rotate gradient to align with arc start
                    val matrix = Matrix()
                    matrix.preRotate(startAngle, cx, cy)
                    it.setLocalMatrix(matrix)
                }
            }
            canvas.drawArc(arcRect, startAngle, sweepProgress, false, gradientPaint)
        }

        // ── Arc end glow dot ─────────────────────────────────────────────────
        if (sweepProgress > 2f) {
            val endAngleRad = Math.toRadians((startAngle + sweepProgress).toDouble())
            val r = cx - arcInset
            val dotX = cx + r * Math.cos(endAngleRad).toFloat()
            val dotY = cy + r * Math.sin(endAngleRad).toFloat()
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accent2
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dotX, dotY, cx * 0.045f, dotPaint)
        }

        // ── Date (top, small) ─────────────────────────────────────────────────
        val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textSecondary
            textSize = cx * 0.155f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(dateStr, cx, cy * 0.58f, datePaint)

        // ── Taken count (big, center) ─────────────────────────────────────────
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textPrimary
            textSize = cx * 0.52f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        // Vertically center the number
        val countBounds = Rect()
        countPaint.getTextBounds("$takenPills", 0, "$takenPills".length, countBounds)
        canvas.drawText("$takenPills", cx, cy + countBounds.height() / 2f, countPaint)

        // ── "/ total" beneath count ──────────────────────────────────────────
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textSecondary
            textSize = cx * 0.155f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("of $totalPills pills", cx, cy * 1.42f, totalPaint)

        // ── Pill type label (bottom) ──────────────────────────────────────────
        val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accent1
            textSize = cx * 0.165f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(pillLabel, cx, cy * 1.62f, typePaint)

        return bitmap
    }
}
