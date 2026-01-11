package com.hskmaster.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages premium purchase state.
 *
 * Currently uses SharedPreferences for state storage.
 * When integrating Google Play Billing, this class will be updated to:
 * 1. Query BillingClient for purchase state
 * 2. Verify purchases with your backend
 * 3. Cache results in SharedPreferences
 */
class PurchaseManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "purchases"
        private const val KEY_IS_PREMIUM = "is_premium"

        // Product ID for Google Play Console (use this when setting up billing)
        const val PRODUCT_ID_PREMIUM = "premium_unlock"
    }

    /**
     * Check if user has premium access
     */
    fun isPremium(): Boolean {
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    /**
     * Set premium status (call this after successful purchase verification)
     */
    fun setPremium(value: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, value).apply()
    }

    /**
     * For testing - toggle premium status
     */
    fun togglePremium(): Boolean {
        val newValue = !isPremium()
        setPremium(newValue)
        return newValue
    }
}
