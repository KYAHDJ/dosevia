package com.dosevia.app

/**
 * Local entitlement tier used throughout the app.
 *
 * Today: controlled by a developer toggle (so you can test locking/unlocking).
 * Later: you will set this based on Google Play Billing purchase state.
 */
enum class UserTier {
    FREE,
    PRO,
    LIFETIME;

    fun hasPro(): Boolean = this == PRO || this == LIFETIME
    fun hasLifetime(): Boolean = this == LIFETIME
}
