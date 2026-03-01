package com.dosevia.app

import android.content.Context

/**
 * Central place for premium entitlement persistence.
 *
 * This is intentionally simple now (SharedPreferences string), so later you can
 * swap the *writer* with Billing while keeping the rest of the app untouched.
 */
object PremiumAccess {
    const val KEY_USER_TIER = "userTier"

    /** Product IDs you will later create in Play Console (placeholders). */
    object Products {
        const val PRO_SUB_MONTHLY  = "pro_monthly"  // SUBS
        const val PRO_SUB_YEARLY   = "pro_yearly"   // SUBS
        const val LIFETIME_INAPP   = "lifetime"     // INAPP
        const val REMOVE_ADS_INAPP = "remove_ads"   // optional if you want separate
    }

    fun readTier(context: Context): UserTier {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_DOSEVIA, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_USER_TIER, UserTier.FREE.name) ?: UserTier.FREE.name
        return try { UserTier.valueOf(raw) } catch (_: Exception) { UserTier.FREE }
    }

    fun writeTier(context: Context, tier: UserTier) {
        context.applicationContext
            .getSharedPreferences(PREFS_DOSEVIA, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_TIER, tier.name)
            .commit()
    }
}
