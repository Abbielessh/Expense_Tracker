package com.example.expensetracker.utils

import com.example.expensetracker.model.Category
import com.example.expensetracker.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Result of parsing a quick-add natural-language input string.
 * [dateStr] is in yyyy-MM-dd format for Supabase storage.
 */
data class ParsedTransactionDraft(
    val type: TransactionType,
    val title: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val dateStr: String,   // yyyy-MM-dd
    val note: String
)

/**
 * Parses a natural-language expense/income note into a [ParsedTransactionDraft].
 *
 * @param input              Raw text, e.g. "food 60 morning" or "salary 20000 income".
 * @param defaultCurrency    Currency code to embed in the draft.
 * @param todayDateStr       Today in yyyy-MM-dd format (injected for testability).
 * @param availableCategories User/profile categories checked before default mappings.
 * @throws IllegalArgumentException with a user-facing message when no valid amount is found.
 */
fun parseQuickAdd(
    input: String,
    defaultCurrency: String,
    todayDateStr: String,
    availableCategories: List<Category> = emptyList()
): ParsedTransactionDraft {

    if (input.isBlank()) {
        throw IllegalArgumentException("Please include amount. Example: food 60")
    }

    val trimmed = input.trim()
    val lower   = trimmed.lowercase(Locale.getDefault())

    // ── Normalise currency symbols / words before tokenising ─────────────────
    val normalised = lower
        .replace("₹", " ")
        .replace(Regex("\\b(rs\\.?|rupees?|inr)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    val tokens = normalised.split("\\s+".toRegex())

    // ── Amount: first valid positive number (commas allowed) ─────────────────
    val amount = tokens.firstNotNullOfOrNull {
        val d = it.replace(",", "").toDoubleOrNull()
        if (d != null && d > 0) d else null
    } ?: throw IllegalArgumentException("Please include amount. Example: food 60")

    // ── Transaction type ──────────────────────────────────────────────────────
    val type = when {
        tokens.any { it in setOf("salary", "income", "credited", "refund", "bonus", "dividend", "reimbursement") } ->
            TransactionType.INCOME
        tokens.containsAll(listOf("payment", "received")) ->
            TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    // ── Date ──────────────────────────────────────────────────────────────────
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dateStr: String = when {
        tokens.any { it == "yesterday" } -> {
            val cal = Calendar.getInstance()
            try { cal.time = sdf.parse(todayDateStr) ?: cal.time } catch (_: Exception) {}
            cal.add(Calendar.DAY_OF_YEAR, -1)
            sdf.format(cal.time)
        }
        // today / tomorrow / current / time-of-day words → today
        else -> todayDateStr
    }

    // ── Noise sets (must not become the title) ────────────────────────────────
    val dateWords = setOf("today", "yesterday", "tomorrow", "current")
    val timeWords = setOf("morning", "evening", "night", "afternoon", "noon")
    val noiseWords = dateWords + timeWords + setOf("now", "just")

    // ── Default category keyword mapping (order = priority) ──────────────────
    data class KwMapping(val keywords: Set<String>, val category: String)

    val defaultMappings = listOf(
        KwMapping(
            setOf(
                "food", "lunch", "breakfast", "dinner", "tea", "coffee",
                "snacks", "snack", "milk", "egg", "eggs", "paneer", "chicken",
                "dosa", "chapati", "chapathi", "groceries", "grocery",
                "biryani", "pizza", "burger", "idli", "vada", "juice",
                "restaurant", "cafe", "canteen", "sweets", "sweet",
                "fruit", "fruits", "veggie", "veggies", "vegetable"
            ), "Food"
        ),
        KwMapping(setOf("zepto"), "Zepto"),
        KwMapping(
            setOf(
                "haircut", "grooming", "salon", "personal", "spa",
                "barber", "gym", "fitness"
            ), "Personal"
        ),
        KwMapping(setOf("petrol", "diesel", "fuel", "gas"), "Fuel"),
        KwMapping(
            setOf(
                "bus", "train", "auto", "taxi", "uber", "ola",
                "metro", "rickshaw", "cab", "rapido", "flight"
            ), "Travel"
        ),
        KwMapping(setOf("rent", "flat", "apartment"), "Rent"),
        KwMapping(
            setOf(
                "bill", "bills", "electricity", "recharge", "wifi",
                "internet", "mobile", "postpaid", "broadband", "dth", "cable"
            ), "Bills"
        ),
        KwMapping(
            setOf(
                "medicine", "hospital", "doctor", "pharmacy",
                "medical", "clinic", "health"
            ), "Health"
        ),
        KwMapping(
            setOf(
                "movie", "cinema", "game", "games", "netflix",
                "spotify", "hotstar", "ott", "entertainment"
            ), "Entertainment"
        ),
        KwMapping(
            setOf(
                "shopping", "amazon", "flipkart", "myntra",
                "meesho", "clothes", "clothing", "shoes", "accessories"
            ), "Shopping"
        ),
        KwMapping(
            setOf("salary", "income", "received", "credited", "refund", "bonus"),
            "Income"
        )
    )

    // ── Step 1: match user/profile categories first ───────────────────────────
    var detectedCategory = ""
    var detectedKeyword: String? = null

    if (availableCategories.isNotEmpty()) {
        outerUser@ for (token in tokens) {
            if (token.length < 3) continue            // skip very short tokens
            if (token.toDoubleOrNull() != null) continue  // skip numbers
            if (token in noiseWords) continue
            for (cat in availableCategories) {
                val catLower = cat.name.lowercase(Locale.getDefault())
                if (token == catLower ||
                    (token.length >= 4 && catLower.contains(token)) ||
                    (catLower.length >= 4 && token.contains(catLower))
                ) {
                    detectedCategory = cat.name
                    detectedKeyword  = token
                    break@outerUser
                }
            }
        }
    }

    // ── Step 2: fall back to default keyword mappings ─────────────────────────
    if (detectedCategory.isBlank()) {
        outerDefault@ for (mapping in defaultMappings) {
            for (token in tokens) {
                if (token in mapping.keywords) {
                    detectedCategory = mapping.category
                    detectedKeyword  = token
                    break@outerDefault
                }
            }
        }
    }

    // ── Step 3: final fallback ────────────────────────────────────────────────
    if (detectedCategory.isBlank()) {
        detectedCategory = if (type == TransactionType.INCOME) "Income" else "Other"
    }

    // ── Title ─────────────────────────────────────────────────────────────────
    val title: String = when {
        type == TransactionType.INCOME && tokens.any { it == "salary" } -> "Salary"
        type == TransactionType.INCOME && tokens.any { it == "refund" }  -> "Refund"
        type == TransactionType.INCOME && tokens.any { it == "bonus" }   -> "Bonus"
        type == TransactionType.INCOME                                    -> "Income"
        detectedKeyword != null -> detectedKeyword!!.replaceFirstChar { it.uppercase() }
        else -> {
            tokens.firstOrNull { token ->
                token.toDoubleOrNull() == null &&
                token !in noiseWords &&
                token.length >= 2
            }?.replaceFirstChar { it.uppercase() }
                ?: "Expense"
        }
    }

    // ── Note: preserve original input (includes time-of-day context) ─────────
    val note = trimmed

    return ParsedTransactionDraft(
        type     = type,
        title    = title,
        category = detectedCategory,
        amount   = amount,
        currency = defaultCurrency,
        dateStr  = dateStr,
        note     = note
    )
}
