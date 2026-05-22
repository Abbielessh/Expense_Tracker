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

// ─── Month name map ───────────────────────────────────────────────────────────

private val MONTH_MAP = mapOf(
    "jan" to 1, "january" to 1,
    "feb" to 2, "february" to 2,
    "mar" to 3, "march" to 3,
    "apr" to 4, "april" to 4,
    "may" to 5,
    "jun" to 6, "june" to 6,
    "jul" to 7, "july" to 7,
    "aug" to 8, "august" to 8,
    "sep" to 9, "sept" to 9, "september" to 9,
    "oct" to 10, "october" to 10,
    "nov" to 11, "november" to 11,
    "dec" to 12, "december" to 12
)

// ─── Noise sets ───────────────────────────────────────────────────────────────

private val CURRENCY_WORDS = setOf(
    "rs", "rs.", "inr", "rupees", "rupee",
    "usd", "dollars", "dollar", "eur", "euros", "euro",
    "gbp", "pounds", "pound"
)

private val DATE_NOISE = setOf(
    "today", "yesterday", "current", "now", "on", "at"
) + MONTH_MAP.keys

private val TIME_WORDS = setOf("morning", "evening", "night", "afternoon", "noon", "midnight")

private val INCOME_WORDS = setOf(
    "salary", "income", "received", "credited", "refund", "freelance", "bonus", "dividend"
)

// ─── Default category keyword mapping ────────────────────────────────────────

private data class KwMapping(val keywords: Set<String>, val category: String)

private val DEFAULT_MAPPINGS = listOf(
    KwMapping(
        setOf(
            "food", "lunch", "breakfast", "dinner", "tea", "coffee",
            "snacks", "snack", "milk", "egg", "eggs", "paneer", "chicken",
            "dosa", "chapati", "chapathi", "groceries", "grocery",
            "biryani", "pizza", "burger", "idli", "vada", "juice",
            "restaurant", "cafe", "canteen", "hotel", "swiggy", "zomato"
        ), "Food"
    ),
    KwMapping(setOf("zepto"), "Zepto"),
    KwMapping(
        setOf("haircut", "grooming", "salon", "personal", "spa", "barber", "gym", "fitness"),
        "Personal"
    ),
    KwMapping(setOf("petrol", "diesel", "fuel", "gas"), "Fuel"),
    KwMapping(
        setOf("bus", "train", "auto", "taxi", "uber", "ola", "metro", "rickshaw", "cab", "rapido", "flight"),
        "Travel"
    ),
    KwMapping(setOf("rent", "flat", "apartment"), "Rent"),
    KwMapping(
        setOf("bill", "bills", "electricity", "recharge", "wifi", "internet", "mobile", "postpaid", "broadband", "dth", "cable"),
        "Bills"
    ),
    KwMapping(
        setOf("medicine", "hospital", "doctor", "pharmacy", "medical", "clinic", "health"),
        "Health"
    ),
    KwMapping(
        setOf("movie", "cinema", "game", "games", "netflix", "spotify", "hotstar", "ott", "entertainment"),
        "Entertainment"
    ),
    KwMapping(
        setOf("shopping", "amazon", "flipkart", "myntra", "meesho", "clothes", "clothing", "shoes", "accessories"),
        "Shopping"
    ),
    KwMapping(
        setOf("salary", "income", "received", "credited", "refund", "bonus"),
        "Income"
    )
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun padded(n: Int): String = n.toString().padStart(2, '0')
private fun makeDate(y: Int, m: Int, d: Int): String = "$y-${padded(m)}-${padded(d)}"
private fun isValidDay(d: Int) = d in 1..31
private fun isValidMonth(m: Int) = m in 1..12
private fun isValidYear(y: Int) = y in 2000..2099
private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

private fun yesterdayStr(todayDateStr: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    try { cal.time = sdf.parse(todayDateStr) ?: cal.time } catch (_: Exception) {}
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return sdf.format(cal.time)
}

// ─── Phase 1: Currency-marked amount extraction ───────────────────────────────

private data class AmountResult(
    val amount: Double?,
    val usedIndices: Set<Int>,
    val currency: String?
)

/**
 * Scans the token list for currency-attached amount patterns.
 * Returns the first currency-marked amount found with consumed indices.
 */
private fun parseCurrencyAmount(tokens: List<String>): AmountResult {
    for (i in tokens.indices) {
        val tok = tokens[i]

        // ₹20 or ₹20.50
        val rupeePrefix = Regex("^₹(\\d+(?:\\.\\d+)?)$").find(tok)
        if (rupeePrefix != null) {
            val n = rupeePrefix.groupValues[1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i), "INR")
        }

        // rs20, rs.20
        val rsPrefix = Regex("^rs\\.?(\\d+(?:\\.\\d+)?)$", RegexOption.IGNORE_CASE).find(tok)
        if (rsPrefix != null) {
            val n = rsPrefix.groupValues[1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i), "INR")
        }

        // inr20
        val inrPrefix = Regex("^inr(\\d+(?:\\.\\d+)?)$", RegexOption.IGNORE_CASE).find(tok)
        if (inrPrefix != null) {
            val n = inrPrefix.groupValues[1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i), "INR")
        }

        // 20rs, 20rs.
        val rsSuffix = Regex("^(\\d+(?:\\.\\d+)?)rs\\.?$", RegexOption.IGNORE_CASE).find(tok)
        if (rsSuffix != null) {
            val n = rsSuffix.groupValues[1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i), "INR")
        }

        // 20inr
        val inrSuffix = Regex("^(\\d+(?:\\.\\d+)?)inr$", RegexOption.IGNORE_CASE).find(tok)
        if (inrSuffix != null) {
            val n = inrSuffix.groupValues[1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i), "INR")
        }

        // Two-token: "₹" then number
        if (tok == "₹" && i + 1 < tokens.size) {
            val n = tokens[i + 1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i, i + 1), "INR")
        }

        // Two-token: "rs" / "rs." / "inr" then number
        if (Regex("^rs\\.?|inr$", RegexOption.IGNORE_CASE).matches(tok) && i + 1 < tokens.size) {
            val n = tokens[i + 1].toDoubleOrNull()
            if (n != null && n > 0) return AmountResult(n, setOf(i, i + 1), "INR")
        }

        // Two-token: number then "rupees" / "rupee" / "rs" / "inr"
        if (Regex("^\\d+(?:\\.\\d+)?$").matches(tok) && i + 1 < tokens.size) {
            val next = tokens[i + 1].lowercase(Locale.US)
            if (Regex("^rupees?|rs\\.?|inr$").matches(next)) {
                val n = tok.toDoubleOrNull()
                if (n != null && n > 0) return AmountResult(n, setOf(i, i + 1), "INR")
            }
        }
    }

    return AmountResult(null, emptySet(), null)
}

// ─── Phase 2: Natural date extraction ────────────────────────────────────────

private data class DateResult(
    val dateStr: String, // yyyy-MM-dd
    val usedIndices: Set<Int>
)

/**
 * Tries to find a date in the token list using priority order:
 *  1. yyyy-MM-dd (ISO)
 *  2. dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy
 *  3. MonthName Day [Year]
 *  4. Day MonthName [Year]
 *  5. today / current / yesterday
 *  6. fallback → today
 *
 * [excludedIndices] are indices already consumed by the amount scan.
 */
private fun parseNaturalDate(
    tokens: List<String>,
    excludedIndices: Set<Int>,
    todayDateStr: String
): DateResult {
    val yr = currentYear()
    val available: (Int) -> Boolean = { i -> !excludedIndices.contains(i) }

    // ── 1. ISO yyyy-MM-dd ──────────────────────────────────────────────────
    for (i in tokens.indices) {
        if (!available(i)) continue
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(tokens[i]) ?: continue
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        if (isValidYear(y) && isValidMonth(mo) && isValidDay(d)) {
            return DateResult(makeDate(y, mo, d), setOf(i))
        }
    }

    // ── 2. dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy ─────────────────────────────
    for (i in tokens.indices) {
        if (!available(i)) continue
        val m = Regex("^(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{4})$").find(tokens[i]) ?: continue
        val d = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val y = m.groupValues[3].toInt()
        if (isValidDay(d) && isValidMonth(mo) && isValidYear(y)) {
            return DateResult(makeDate(y, mo, d), setOf(i))
        }
    }

    // ── 3. MonthName Day [Year] ────────────────────────────────────────────
    for (i in tokens.indices) {
        if (!available(i)) continue

        var mi = i
        if (tokens[mi] == "on" && mi + 1 < tokens.size) mi++

        val monthNum = MONTH_MAP[tokens[mi]] ?: continue

        val dayIdx = mi + 1
        if (dayIdx >= tokens.size || !available(dayIdx)) continue
        val dayStr = tokens[dayIdx]
        if (!Regex("^\\d{1,2}$").matches(dayStr)) continue
        val dayNum = dayStr.toInt()
        if (!isValidDay(dayNum)) continue

        val usedIdx = mutableSetOf(mi, dayIdx)
        if (mi != i) usedIdx.add(i) // "on" consumed

        val yearIdx = mi + 2
        var y = yr
        if (yearIdx < tokens.size && available(yearIdx)) {
            val maybeYear = tokens[yearIdx].toIntOrNull()
            if (maybeYear != null && isValidYear(maybeYear) &&
                Regex("^\\d{4}$").matches(tokens[yearIdx])) {
                y = maybeYear
                usedIdx.add(yearIdx)
            }
        }

        return DateResult(makeDate(y, monthNum, dayNum), usedIdx)
    }

    // ── 4. Day MonthName [Year] ────────────────────────────────────────────
    for (i in tokens.indices) {
        if (!available(i)) continue

        var si = i
        if (tokens[si] == "on" && si + 1 < tokens.size) si++

        if (!Regex("^\\d{1,2}$").matches(tokens[si])) continue
        val dayNum = tokens[si].toInt()
        if (!isValidDay(dayNum)) continue

        val monthIdx = si + 1
        if (monthIdx >= tokens.size || !available(monthIdx)) continue
        val monthNum = MONTH_MAP[tokens[monthIdx]] ?: continue

        val usedIdx = mutableSetOf(si, monthIdx)
        if (si != i) usedIdx.add(i) // "on" consumed

        val yearIdx = si + 2
        var y = yr
        if (yearIdx < tokens.size && available(yearIdx)) {
            val maybeYear = tokens[yearIdx].toIntOrNull()
            if (maybeYear != null && isValidYear(maybeYear) &&
                Regex("^\\d{4}$").matches(tokens[yearIdx])) {
                y = maybeYear
                usedIdx.add(yearIdx)
            }
        }

        return DateResult(makeDate(y, monthNum, dayNum), usedIdx)
    }

    // ── 5. Relative words ──────────────────────────────────────────────────
    for (i in tokens.indices) {
        if (!available(i)) continue
        when (tokens[i]) {
            "today", "current", "now" ->
                return DateResult(todayDateStr, setOf(i))
            "yesterday" ->
                return DateResult(yesterdayStr(todayDateStr), setOf(i))
        }
    }

    // ── 6. Fallback ────────────────────────────────────────────────────────
    return DateResult(todayDateStr, emptySet())
}

// ─── Main parser ──────────────────────────────────────────────────────────────

/**
 * Parses a natural-language expense/income note into a [ParsedTransactionDraft].
 *
 * Handles natural amount formats: ₹20, 20rs, rs 20, 20 rupees, 20.50
 * Handles natural date formats: may 20, 20 may 2026, 20/05/2026, 2026-05-20, today, yesterday
 * Correctly resolves "lunch 20 may 20" as amount=20, date=May 20 current year.
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

    // ── Tokenise ──────────────────────────────────────────────────────────────
    // Separate currency symbols so ₹20 becomes ["₹", "20"] temporarily only for
    // the currency-amount scanner. We keep the raw lowercased tokens for the rest.
    val tokens = trimmed
        .lowercase(Locale.US)
        .replace("₹", " ₹ ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(" ")
        .filter { it.isNotEmpty() }

    // ── Phase 1: Currency-marked amount ──────────────────────────────────────
    val amtResult = parseCurrencyAmount(tokens)
    var amount: Double? = amtResult.amount
    val detectedCurrency = amtResult.currency ?: defaultCurrency
    val amountUsed = amtResult.usedIndices.toMutableSet()

    // ── Phase 2: Natural date ─────────────────────────────────────────────────
    val dateResult = parseNaturalDate(tokens, amountUsed, todayDateStr)
    val dateStr = dateResult.dateStr
    val dateUsed = dateResult.usedIndices.toMutableSet()

    // ── Phase 3: Bare number fallback ─────────────────────────────────────────
    if (amount == null) {
        for (i in tokens.indices) {
            if (amountUsed.contains(i) || dateUsed.contains(i)) continue
            val tok = tokens[i]
            if (!Regex("^\\d+(?:\\.\\d+)?$").matches(tok)) continue
            val n = tok.toDoubleOrNull() ?: continue
            // Reject year-like values (2000–2099, 4 digits)
            if (n > 0 && !(n >= 2000 && n <= 2099 && tok.length == 4)) {
                amount = n
                amountUsed.add(i)
                break
            }
        }
    }

    if (amount == null) {
        throw IllegalArgumentException("Please include amount. Example: food 60")
    }

    // ── Build semantic token list ─────────────────────────────────────────────
    val allUsed = amountUsed + dateUsed
    val semanticTokens = tokens.filterIndexed { i, tok ->
        !allUsed.contains(i) &&
        !CURRENCY_WORDS.contains(tok) &&
        !DATE_NOISE.contains(tok)
    }

    // ── Time words → note ──────────────────────────────────────────────────────
    val noteWords = semanticTokens.filter { it in TIME_WORDS }
    val keywordTokens = semanticTokens.filter { it !in TIME_WORDS }

    // ── Transaction type ──────────────────────────────────────────────────────
    val type = when {
        keywordTokens.any { it in INCOME_WORDS } ||
        tokens.containsAll(listOf("payment", "received")) ->
            TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    // ── Category ──────────────────────────────────────────────────────────────
    var detectedCategory = ""
    var detectedKeyword: String? = null

    // Step 1: user profile categories
    if (availableCategories.isNotEmpty()) {
        outer@ for (token in keywordTokens) {
            if (token.length < 3) continue
            if (token.toDoubleOrNull() != null) continue
            for (cat in availableCategories) {
                val catLower = cat.name.lowercase(Locale.US)
                if (token == catLower ||
                    (token.length >= 4 && catLower.contains(token)) ||
                    (catLower.length >= 4 && token.contains(catLower))
                ) {
                    detectedCategory = cat.name
                    detectedKeyword = token
                    break@outer
                }
            }
        }
    }

    // Step 2: default keyword mappings
    if (detectedCategory.isBlank()) {
        outer@ for (mapping in DEFAULT_MAPPINGS) {
            for (token in keywordTokens) {
                if (token in mapping.keywords) {
                    detectedCategory = mapping.category
                    detectedKeyword = token
                    break@outer
                }
            }
        }
    }

    // Step 3: fallback
    if (detectedCategory.isBlank()) {
        detectedCategory = if (type == TransactionType.INCOME) "Income" else "Other"
    }

    // ── Title ─────────────────────────────────────────────────────────────────
    val title: String = when {
        type == TransactionType.INCOME && keywordTokens.any { it == "salary" } -> "Salary"
        type == TransactionType.INCOME && keywordTokens.any { it == "refund" }  -> "Refund"
        type == TransactionType.INCOME && keywordTokens.any { it == "bonus" }   -> "Bonus"
        type == TransactionType.INCOME                                           -> "Income"
        detectedKeyword != null ->
            detectedKeyword!!.replaceFirstChar { it.uppercase() }
        else ->
            keywordTokens.firstOrNull { tok ->
                tok.toDoubleOrNull() == null && tok.length >= 2
            }?.replaceFirstChar { it.uppercase() } ?: "Expense"
    }

    // ── Note: original input for context ─────────────────────────────────────
    val note = if (noteWords.isNotEmpty()) noteWords.joinToString(" ") else trimmed

    return ParsedTransactionDraft(
        type     = type,
        title    = title,
        category = detectedCategory,
        amount   = amount,
        currency = detectedCurrency,
        dateStr  = dateStr,
        note     = note
    )
}
