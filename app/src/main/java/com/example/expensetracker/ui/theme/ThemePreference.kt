package com.example.expensetracker.ui.theme

import android.content.Context

/**
 * Device-local dark mode preference stored in SharedPreferences.
 * Never synced to Supabase — this is per-device, per-user-session.
 */
object ThemePreference {
    private const val PREF_NAME = "expense_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    /** Returns true if dark mode was previously enabled. Default: false (light). */
    fun isDarkMode(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)

    /** Saves the dark mode preference synchronously. */
    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }
}
