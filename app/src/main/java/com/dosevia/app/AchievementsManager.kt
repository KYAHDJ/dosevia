package com.dosevia.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val accent: Int,
)

data class AchievementStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalTaken: Int,
    val totalNotes: Int,
    val totalSyncs: Int,
)

object AchievementsManager {
    private const val PREFS = "dosevia_achievements"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_BEST_STREAK = "best_streak"
    private const val KEY_TOTAL_TAKEN = "total_taken"
    private const val KEY_TOTAL_NOTES = "total_notes"
    private const val KEY_TOTAL_SYNCS = "total_syncs"
    private const val KEY_LAST_TAKEN_DAY = "last_taken_day"
    private const val KEY_LAST_MESSAGE_INDEX = "last_message_index"
    private const val KEY_LAST_NOTIFIED_STREAK = "last_notified_streak"
    private const val CHANNEL_ID = "dosevia_achievements"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Achievements",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Streaks and achievement milestones"
        }
        manager.createNotificationChannel(channel)
    }

    fun getStats(context: Context): AchievementStats {
        val p = prefs(context)
        return AchievementStats(
            currentStreak = p.getInt(KEY_CURRENT_STREAK, 0),
            bestStreak = p.getInt(KEY_BEST_STREAK, 0),
            totalTaken = p.getInt(KEY_TOTAL_TAKEN, 0),
            totalNotes = p.getInt(KEY_TOTAL_NOTES, 0),
            totalSyncs = p.getInt(KEY_TOTAL_SYNCS, 0),
        )
    }

    private fun getCompatLong(context: Context, key: String, defaultValue: Long): Long {
        val p = prefs(context)
        val raw = p.all[key] ?: return defaultValue

        return when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is Float -> raw.toLong()
            is String -> raw.toLongOrNull() ?: defaultValue
            is Number -> raw.toLong()
            else -> defaultValue
        }
    }

    fun recordPillTaken(context: Context) {
        val p = prefs(context)
        val todayDay = System.currentTimeMillis() / 86_400_000L
        val lastTakenDay = getCompatLong(context, KEY_LAST_TAKEN_DAY, Long.MIN_VALUE)

        if (lastTakenDay == todayDay) return

        val previousStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        val newStreak = when (todayDay - lastTakenDay) {
            1L -> previousStreak + 1
            else -> 1
        }
        val best = maxOf(newStreak, p.getInt(KEY_BEST_STREAK, 0))
        val totalTaken = p.getInt(KEY_TOTAL_TAKEN, 0) + 1

        p.edit()
            .putLong(KEY_LAST_TAKEN_DAY, todayDay)
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putInt(KEY_BEST_STREAK, best)
            .putInt(KEY_TOTAL_TAKEN, totalTaken)
            .apply()

        maybeNotifyMilestone(context, newStreak)
    }

    fun recordNoteAdded(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_TOTAL_NOTES, p.getInt(KEY_TOTAL_NOTES, 0) + 1).apply()
    }

    fun recordSync(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_TOTAL_SYNCS, p.getInt(KEY_TOTAL_SYNCS, 0) + 1).apply()
    }

    private fun maybeNotifyMilestone(context: Context, streak: Int) {
        val milestones = setOf(3, 7, 10, 14, 20, 30, 45, 60, 90, 120, 180, 365)
        if (streak !in milestones) return

        val p = prefs(context)
        if (p.getInt(KEY_LAST_NOTIFIED_STREAK, 0) == streak) return

        val templates = listOf(
            "You're on a %d day streak — keep it up!",
            "%d days strong. You're doing amazing!",
            "A %d day streak is no joke — stay consistent!",
            "%d days in a row. Dosevia is proud of you!",
            "Look at you — %d straight days!"
        )

        val lastIndex = p.getInt(KEY_LAST_MESSAGE_INDEX, -1)
        val availableIndices = templates.indices.filter { it != lastIndex }
        val selectedIndex = if (availableIndices.isNotEmpty()) {
            availableIndices.random()
        } else {
            0
        }

        val body = templates[selectedIndex].format(streak)

        p.edit()
            .putInt(KEY_LAST_MESSAGE_INDEX, selectedIndex)
            .putInt(KEY_LAST_NOTIFIED_STREAK, streak)
            .apply()

        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Achievement unlocked")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_purple))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(4000 + streak, notification)
        } catch (_: SecurityException) {
        }
    }

    fun getAchievements(context: Context): List<AchievementItem> {
        val stats = getStats(context)

        fun ach(
            id: String,
            title: String,
            description: String,
            unlocked: Boolean,
            accent: Int
        ) = AchievementItem(id, title, description, unlocked, accent)

        return listOf(
            ach("a01", "First Step", "Take your first pill.", stats.totalTaken >= 1, 0xFFF59E0B.toInt()),
            ach("a02", "Tiny Streak", "Reach a 3 day streak.", stats.bestStreak >= 3, 0xFFFB7185.toInt()),
            ach("a03", "Strong Start", "Reach a 5 day streak.", stats.bestStreak >= 5, 0xFFEC4899.toInt()),
            ach("a04", "Weekly Glow", "Reach a 7 day streak.", stats.bestStreak >= 7, 0xFF8B5CF6.toInt()),
            ach("a05", "Tenacious", "Reach a 10 day streak.", stats.bestStreak >= 10, 0xFF6366F1.toInt()),
            ach("a06", "Steady Heart", "Reach a 14 day streak.", stats.bestStreak >= 14, 0xFF3B82F6.toInt()),
            ach("a07", "Consistency Queen", "Reach a 20 day streak.", stats.bestStreak >= 20, 0xFF06B6D4.toInt()),
            ach("a08", "Monthly Magic", "Reach a 30 day streak.", stats.bestStreak >= 30, 0xFF10B981.toInt()),
            ach("a09", "Radiant Routine", "Reach a 45 day streak.", stats.bestStreak >= 45, 0xFF22C55E.toInt()),
            ach("a10", "Iron Bloom", "Reach a 60 day streak.", stats.bestStreak >= 60, 0xFF84CC16.toInt()),
            ach("a11", "Ninety Nice", "Reach a 90 day streak.", stats.bestStreak >= 90, 0xFFEAB308.toInt()),
            ach("a12", "Golden Habit", "Reach a 120 day streak.", stats.bestStreak >= 120, 0xFFF97316.toInt()),
            ach("a13", "Half-Year Hero", "Reach a 180 day streak.", stats.bestStreak >= 180, 0xFFEF4444.toInt()),
            ach("a14", "Year of Care", "Reach a 365 day streak.", stats.bestStreak >= 365, 0xFF7C3AED.toInt()),
            ach("a15", "Ten Taken", "Take 10 pills total.", stats.totalTaken >= 10, 0xFFFB7185.toInt()),
            ach("a16", "Silver Stack", "Take 25 pills total.", stats.totalTaken >= 25, 0xFFEC4899.toInt()),
            ach("a17", "Golden Stack", "Take 50 pills total.", stats.totalTaken >= 50, 0xFF8B5CF6.toInt()),
            ach("a18", "Century Club", "Take 100 pills total.", stats.totalTaken >= 100, 0xFF3B82F6.toInt()),
            ach("a19", "Daily Devotion", "Take 180 pills total.", stats.totalTaken >= 180, 0xFF14B8A6.toInt()),
            ach("a20", "Dosevia Legend", "Take 365 pills total.", stats.totalTaken >= 365, 0xFF22C55E.toInt()),
            ach("a21", "First Note", "Save your first note.", stats.totalNotes >= 1, 0xFF06B6D4.toInt()),
            ach("a22", "Journal Spark", "Save 5 notes.", stats.totalNotes >= 5, 0xFF0EA5E9.toInt()),
            ach("a23", "Memory Keeper", "Save 10 notes.", stats.totalNotes >= 10, 0xFF6366F1.toInt()),
            ach("a24", "Insight Collector", "Save 25 notes.", stats.totalNotes >= 25, 0xFF8B5CF6.toInt()),
            ach("a25", "First Sync", "Sync your data once.", stats.totalSyncs >= 1, 0xFFF59E0B.toInt()),
            ach("a26", "Cloud Friend", "Sync your data 5 times.", stats.totalSyncs >= 5, 0xFFF97316.toInt()),
            ach("a27", "Backup Beauty", "Sync your data 15 times.", stats.totalSyncs >= 15, 0xFFEF4444.toInt()),
            ach("a28", "Calm and Consistent", "Have a 7 day streak and 10 total taken.", stats.bestStreak >= 7 && stats.totalTaken >= 10, 0xFF10B981.toInt()),
            ach("a29", "Premium Rhythm", "Have a 30 day streak and 25 total taken.", stats.bestStreak >= 30 && stats.totalTaken >= 25, 0xFF14B8A6.toInt()),
            ach("a30", "Unstoppable", "Have a 60 day streak and 100 total taken.", stats.bestStreak >= 60 && stats.totalTaken >= 100, 0xFF7C3AED.toInt()),
        )
    }
}