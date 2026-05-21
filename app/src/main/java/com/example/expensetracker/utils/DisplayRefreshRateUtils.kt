package com.example.expensetracker.utils

import android.app.Activity
import android.content.Context
import android.os.Build

object DisplayRefreshRateUtils {

    const val PREF_FILE = "display_prefs"
    const val KEY_MODE  = "display_refresh_rate_mode"
    const val KEY_VALUE = "display_refresh_rate_value"

    const val MODE_AUTO    = "AUTO"
    const val MODE_HIGHEST = "HIGHEST"
    const val MODE_CUSTOM  = "CUSTOM"

    /**
     * Returns distinct refresh rates (Hz) supported by the primary display,
     * sorted ascending.  Falls back to [60f] on older APIs or on error.
     */
    fun getAvailableRefreshRates(context: Context): List<Float> {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return listOf(60f)
            val activity = context as? Activity ?: return listOf(60f)
            @Suppress("DEPRECATION")
            val modes = activity.windowManager.defaultDisplay.supportedModes
            modes.map { it.refreshRate }
                .map { rate -> (rate * 10).toInt() / 10f }   // round to 1 decimal
                .distinct()
                .sorted()
                .ifEmpty { listOf(60f) }
        } catch (e: Exception) {
            listOf(60f)
        }
    }

    /**
     * Returns the display's current active refresh rate.
     */
    fun getCurrentRefreshRate(context: Context): Float {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val activity = context as? Activity ?: return 60f
                @Suppress("DEPRECATION")
                return activity.windowManager.defaultDisplay.refreshRate
            }
            val activity = context as? Activity ?: return 60f
            @Suppress("DEPRECATION")
            val rate = activity.windowManager.defaultDisplay.mode.refreshRate
            (rate * 10).toInt() / 10f
        } catch (e: Exception) {
            60f
        }
    }

    /**
     * Applies [mode]/[hz] to the Activity window's preferredRefreshRate.
     * Silently falls back to Auto (0f) on any failure or if the chosen Hz
     * is not in the supported list.
     */
    fun applyRefreshRate(context: Context, mode: String, hz: Float = 0f) {
        try {
            val activity = context as? Activity ?: return
            val available = getAvailableRefreshRates(context)
            val target: Float = when (mode) {
                MODE_AUTO    -> 0f
                MODE_HIGHEST -> available.maxOrNull() ?: 0f
                MODE_CUSTOM  -> {
                    // Accept if within ±1 Hz of a supported mode
                    if (available.any { it >= hz - 1f && it <= hz + 1f }) hz
                    else {
                        android.util.Log.w("DisplayRefreshRate",
                            "${hz}Hz not in supported modes $available — falling back to Auto")
                        0f
                    }
                }
                else -> 0f
            }
            val params = activity.window.attributes
            params.preferredRefreshRate = target
            activity.window.attributes = params
        } catch (e: Exception) {
            android.util.Log.e("DisplayRefreshRate", "applyRefreshRate failed", e)
        }
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    fun saveMode(context: Context, mode: String, hz: Float = 0f) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode)
            .putFloat(KEY_VALUE, hz)
            .apply()
    }

    fun loadSavedMode(context: Context): String =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_MODE, MODE_AUTO) ?: MODE_AUTO

    fun loadSavedHz(context: Context): Float =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getFloat(KEY_VALUE, 0f)

    // ── Display helpers ────────────────────────────────────────────────────────

    /** Human-readable label for a saved mode/hz pair shown in the UI. */
    fun modeLabel(mode: String, hz: Float, available: List<Float>): String = when (mode) {
        MODE_AUTO    -> "Auto (system default)"
        MODE_HIGHEST -> "Highest Available (${(available.maxOrNull() ?: 60f).toInt()}Hz)"
        MODE_CUSTOM  -> "${hz.toInt()}Hz"
        else         -> "Auto"
    }
}
