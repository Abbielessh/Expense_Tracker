package com.example.expensetracker.utils

/**
 * Maps a category name or icon_key to an emoji icon.
 * Falls back gracefully so the app never crashes on unknown categories.
 */
object CategoryIconUtils {

    private val iconKeyToEmoji = mapOf(
        "food"          to "🍔",
        "travel"        to "🚗",
        "shopping"      to "🛍️",
        "rent"          to "🏠",
        "bills"         to "⚡",
        "health"        to "🏥",
        "education"     to "🎓",
        "entertainment" to "🎬",
        "fuel"          to "⛽",
        "income"        to "💰",
        "other"         to "📌"
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
        "gym"           to "health",
        "fuel"          to "fuel",
        "petrol"        to "fuel",
        "diesel"        to "fuel",
        "gas"           to "fuel",
        "income"        to "income",
        "salary"        to "income",
        "freelance"     to "income"
    )

    val allIconKeys: List<String> = iconKeyToEmoji.keys.toList()

    fun emojiForKey(iconKey: String?): String {
        if (iconKey.isNullOrBlank()) return "📌"
        return iconKeyToEmoji[iconKey.lowercase().trim()] ?: "📌"
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
     * iconKey takes priority; falls back to name-based guess.
     */
    fun iconForCategory(categoryName: String, iconKey: String?): String {
        val key = when {
            !iconKey.isNullOrBlank() && iconKey != "other" -> iconKey
            else -> guessIconKeyFromName(categoryName)
        }
        return emojiForKey(key)
    }
}
