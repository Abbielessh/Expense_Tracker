package com.example.expensetracker.utils

import androidx.annotation.DrawableRes
import com.example.expensetracker.R

/**
 * Maps a category icon_key to a drawable resource ID.
 * Falls back gracefully so the app never crashes on unknown categories.
 */
object CategoryIconUtils {

    private val iconKeyToDrawable: Map<String, Int> = mapOf(
        "food"          to R.drawable.ic_cat_food,
        "travel"        to R.drawable.ic_cat_travel,
        "shopping"      to R.drawable.ic_cat_shopping,
        "rent"          to R.drawable.ic_cat_rent,
        "bills"         to R.drawable.ic_cat_bills,
        "health"        to R.drawable.ic_cat_health,
        "education"     to R.drawable.ic_cat_education,
        "entertainment" to R.drawable.ic_cat_entertainment,
        "fuel"          to R.drawable.ic_cat_fuel,
        "income"        to R.drawable.ic_cat_income,
        "other"         to R.drawable.ic_cat_other,
        "zepto"         to R.drawable.ic_cat_zepto,
        "personal"      to R.drawable.ic_cat_personal
    )

    /** Best-effort guess of icon_key from a category display name */
    private val nameToIconKey = mapOf(
        "food"          to "food",
        "lunch"         to "food",
        "dinner"        to "food",
        "breakfast"     to "food",
        "restaurant"    to "food",
        "grocery"       to "food",
        "groceries"     to "food",
        "travel"        to "travel",
        "trip"          to "travel",
        "transport"     to "travel",
        "uber"          to "travel",
        "cab"           to "travel",
        "shopping"      to "shopping",
        "clothes"       to "shopping",
        "clothing"      to "shopping",
        "online"        to "shopping",
        "amazon"        to "shopping",
        "rent"          to "rent",
        "house"         to "rent",
        "home"          to "rent",
        "bills"         to "bills",
        "bill"          to "bills",
        "electricity"   to "bills",
        "water"         to "bills",
        "internet"      to "bills",
        "wifi"          to "bills",
        "utility"       to "bills",
        "utilities"     to "bills",
        "health"        to "health",
        "medical"       to "health",
        "doctor"        to "health",
        "medicine"      to "health",
        "hospital"      to "health",
        "pharmacy"      to "health",
        "gym"           to "health",
        "education"     to "education",
        "school"        to "education",
        "college"       to "education",
        "course"        to "education",
        "book"          to "education",
        "books"         to "education",
        "tuition"       to "education",
        "entertainment" to "entertainment",
        "movies"        to "entertainment",
        "movie"         to "entertainment",
        "netflix"       to "entertainment",
        "spotify"       to "entertainment",
        "games"         to "entertainment",
        "game"          to "entertainment",
        "fuel"          to "fuel",
        "petrol"        to "fuel",
        "diesel"        to "fuel",
        "gas"           to "fuel",
        "income"        to "income",
        "salary"        to "income",
        "freelance"     to "income",
        "zepto"         to "zepto",
        "personal"      to "personal",
        "grooming"      to "personal",
        "haircut"       to "personal",
        "salon"         to "personal",
        "spa"           to "personal"
    )

    val allIconKeys: List<String> = iconKeyToDrawable.keys.toList()

    /** Returns the drawable resource ID for the given icon key. Falls back to ic_cat_other. */
    @DrawableRes
    fun drawableForKey(iconKey: String?): Int {
        if (iconKey.isNullOrBlank()) return R.drawable.ic_cat_other
        return iconKeyToDrawable[iconKey.lowercase().trim()] ?: R.drawable.ic_cat_other
    }

    fun guessIconKeyFromName(categoryName: String): String {
        val lower = categoryName.lowercase().trim()
        // Exact match
        nameToIconKey[lower]?.let { return it }
        // Partial match
        nameToIconKey.entries.firstOrNull { lower.contains(it.key) }?.let { return it.value }
        return "other"
    }

    /**
     * Primary entry point used in UI.
     * Returns the drawable resource ID for the given category.
     * iconKey takes priority; falls back to name-based guess.
     */
    @DrawableRes
    fun iconResForCategory(categoryName: String, iconKey: String?): Int {
        val key = when {
            !iconKey.isNullOrBlank() && iconKey != "other" -> iconKey
            else -> guessIconKeyFromName(categoryName)
        }
        return drawableForKey(key)
    }

    // ── Legacy emoji helpers kept for any old callers (returns empty string now) ──
    // These are no longer used in the UI but kept to avoid breaking any call sites
    // that may not have been updated yet.
    @Deprecated("Use drawableForKey() or iconResForCategory() instead")
    fun emojiForKey(iconKey: String?): String = ""

    @Deprecated("Use iconResForCategory() instead")
    fun iconForCategory(categoryName: String, iconKey: String?): String = ""
}
