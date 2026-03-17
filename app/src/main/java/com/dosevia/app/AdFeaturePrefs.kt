package com.dosevia.app

import android.content.Context
import java.util.concurrent.TimeUnit

object AdFeaturePrefs {
    private const val PREFS = "ad_feature_prefs"
    private const val KEY_AD_FREE_UNTIL = "ad_free_until"
    private const val KEY_LAST_REWARDED_AT = "last_rewarded_at"
    private const val KEY_LAST_REWARD_DIALOG_AT = "last_reward_dialog_at"
    private const val KEY_HOME_OPEN_COUNT = "home_open_count"
    private const val KEY_TAKEN_COUNT = "taken_count"
    private const val KEY_NOTES_COUNT = "notes_count"
    private const val KEY_LAST_INTERSTITIAL_AT = "last_interstitial_at"

    private const val REWARDED_24H_MS = 24L * 60L * 60L * 1000L
    private const val REWARD_DIALOG_COOLDOWN_MS = 8L * 60L * 60L * 1000L
    private const val INTERSTITIAL_COOLDOWN_MS = 5L * 60L * 1000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isTemporarilyAdFree(context: Context): Boolean {
        return System.currentTimeMillis() < prefs(context).getLong(KEY_AD_FREE_UNTIL, 0L)
    }

    fun shouldShowAnyAds(context: Context, userTier: UserTier): Boolean {
        return userTier == UserTier.FREE && !isTemporarilyAdFree(context)
    }

    fun activateRewardedAdFree24Hours(context: Context) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putLong(KEY_AD_FREE_UNTIL, now + REWARDED_24H_MS)
            .putLong(KEY_LAST_REWARDED_AT, now)
            .apply()
    }

    fun remainingAdFreeMillis(context: Context): Long {
        val remaining = prefs(context).getLong(KEY_AD_FREE_UNTIL, 0L) - System.currentTimeMillis()
        return remaining.coerceAtLeast(0L)
    }

    fun formattedRemaining(context: Context): String {
        val millis = remainingAdFreeMillis(context)
        if (millis <= 0L) return ""
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun shouldOfferRewardDialog(context: Context, userTier: UserTier): Boolean {
        if (!shouldShowAnyAds(context, userTier)) return false

        val now = System.currentTimeMillis()
        val p = prefs(context)
        val homeOpenCount = p.getInt(KEY_HOME_OPEN_COUNT, 0) + 1
        p.edit().putInt(KEY_HOME_OPEN_COUNT, homeOpenCount).apply()

        val lastShown = p.getLong(KEY_LAST_REWARD_DIALOG_AT, 0L)
        val enoughTimePassed = now - lastShown >= REWARD_DIALOG_COOLDOWN_MS

        return homeOpenCount >= 3 && enoughTimePassed
    }

    fun markRewardDialogShown(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_REWARD_DIALOG_AT, System.currentTimeMillis())
            .putInt(KEY_HOME_OPEN_COUNT, 0)
            .apply()
    }

    private fun canShowInterstitial(context: Context, userTier: UserTier): Boolean {
        if (!shouldShowAnyAds(context, userTier)) return false

        val lastShown = prefs(context).getLong(KEY_LAST_INTERSTITIAL_AT, 0L)
        return System.currentTimeMillis() - lastShown >= INTERSTITIAL_COOLDOWN_MS
    }

    fun shouldShowTakenInterstitial(context: Context, userTier: UserTier): Boolean {
        val p = prefs(context)
        val count = p.getInt(KEY_TAKEN_COUNT, 0) + 1
        p.edit().putInt(KEY_TAKEN_COUNT, count).apply()

        if (count % 3 != 0) return false
        if (!canShowInterstitial(context, userTier)) return false

        p.edit().putLong(KEY_LAST_INTERSTITIAL_AT, System.currentTimeMillis()).apply()
        return true
    }

    fun shouldShowNotesInterstitial(context: Context, userTier: UserTier): Boolean {
        val p = prefs(context)
        val count = p.getInt(KEY_NOTES_COUNT, 0) + 1
        p.edit().putInt(KEY_NOTES_COUNT, count).apply()

        if (count % 2 != 0) return false
        if (!canShowInterstitial(context, userTier)) return false

        p.edit().putLong(KEY_LAST_INTERSTITIAL_AT, System.currentTimeMillis()).apply()
        return true
    }

    // New helper used by the new reward popup system
    fun isAdFreeActive(context: Context): Boolean {
        return System.currentTimeMillis() < prefs(context).getLong(KEY_AD_FREE_UNTIL, 0L)
    }

    // New helper used by the timer badge
    fun getAdFreeUntil(context: Context): Long {
        return prefs(context).getLong(KEY_AD_FREE_UNTIL, 0L)
    }

    // New helper used by RewardOfferDialog reward flow
    fun enableAdFree24Hours(context: Context) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putLong(KEY_AD_FREE_UNTIL, now + REWARDED_24H_MS)
            .putLong(KEY_LAST_REWARDED_AT, now)
            .apply()
    }
}