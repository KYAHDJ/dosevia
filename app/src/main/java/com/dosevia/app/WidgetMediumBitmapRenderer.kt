package com.dosevia.app

import android.content.Context
import android.graphics.*

object WidgetMediumBitmapRenderer {

    private const val MIN_DP = 110
    private const val MAX_DP = 250

    fun render(
        context: Context,
        totalPills: Int,
        takenPills: Int,
        missedPills: Int,
        widthDp: Int  = MIN_DP,
        heightDp: Int = MIN_DP,
        theme: WidgetThemeColors,
        locked: Boolean = false
    ): Bitmap {
        val dm = context.resources.displayMetrics

        // Clamp to safe min/max so widget never looks broken
        val clampedW = widthDp.coerceIn(MIN_DP, MAX_DP)
        val clampedH = heightDp.coerceIn(MIN_DP, MAX_DP)

        val w = (clampedW * dm.density).toInt()
        val h = (clampedH * dm.density).toInt()

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pad    = w * 0.06f
        val corner = w * 0.10f

        // Card background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.background
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), corner, corner, bgPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textPrimary
            textSize = w * 0.10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val titleY = pad + titlePaint.textSize
        canvas.drawText("Pill Statistics", pad, titleY, titlePaint)

        if (locked) {
            // Locked overlay (FREE tier)
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.textSecondary
                textSize = w * 0.065f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }
            val y1 = titleY + pad * 1.1f
            canvas.drawText("Upgrade to PRO to unlock", pad, y1, subPaint)
            val y2 = y1 + subPaint.textSize * 1.4f
            canvas.drawText("stats + customization", pad, y2, subPaint)

            // Badge
            val badgeH = h * 0.16f
            val badgeW = w * 0.28f
            val badgeRect = RectF(w - pad - badgeW, pad, w - pad, pad + badgeH)
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.accent1 }
            canvas.drawRoundRect(badgeRect, badgeH * 0.45f, badgeH * 0.45f, badgePaint)
            val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = badgeH * 0.50f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val by = badgeRect.centerY() + badgeText.textSize * 0.35f
            canvas.drawText("PRO", badgeRect.centerX(), by, badgeText)
            return bitmap
        }

        // Three rows filling remaining height
        val rowTop = titleY + pad * 0.8f
        val rowH   = (h - rowTop - pad) / 3f
        val rowGap = rowH * 0.08f

        drawRow(canvas, w, rowTop + rowH * 0 + rowGap, rowH - rowGap * 2, pad, corner * 0.5f,
            Color.parseColor("#FFFFFF"), Color.parseColor("#9E9EAE"),
            "Total Pills", "$totalPills", theme.textPrimary, theme.textSecondary)

        drawRow(canvas, w, rowTop + rowH * 1 + rowGap, rowH - rowGap * 2, pad, corner * 0.5f,
            Color.parseColor("#EDFFF4"), theme.accent1,
            "Taken", "$takenPills", theme.accent1, theme.textSecondary)

        drawRow(canvas, w, rowTop + rowH * 2 + rowGap, rowH - rowGap * 2, pad, corner * 0.5f,
            Color.parseColor("#FFF0F0"), theme.accent2,
            "Missed", "$missedPills", theme.accent2, theme.textSecondary)

        return bitmap
    }

    private fun drawRow(
        canvas: Canvas, w: Int,
        top: Float, rowH: Float, hPad: Float, corner: Float,
        bgColor: Int, dotColor: Int,
        label: String, value: String, valueColor: Int, labelColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(hPad, top, w - hPad, top + rowH), corner, corner, bgPaint)

        val cx   = hPad * 2.2f
        val cy   = top + rowH / 2f
        val dotR = rowH * 0.26f

        val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor; style = Paint.Style.FILL; alpha = 45 }
        canvas.drawCircle(cx, cy, dotR, fadePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor; style = Paint.Style.FILL }
        canvas.drawCircle(cx, cy, dotR * 0.45f, dotPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = rowH * 0.36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val baseline = cy + labelPaint.textSize * 0.35f
        canvas.drawText(label, cx + dotR + hPad * 0.6f, baseline, labelPaint)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueColor
            textSize = rowH * 0.44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(value, w - hPad * 1.5f, baseline, valuePaint)
    }
}
