package com.example.expensetracker.utils

import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.TransactionType

suspend fun createCsvContent(
    profile: ExpenseProfile,
    displayCurrency: String,
    convertFn: suspend (Double, String, String) -> Double
): String {
    val builder = StringBuilder()
    
    // Header
    builder.append("Profile,Date,Type,Title,Category,Base Amount,Base Currency,Converted Amount,Display Currency,Note\n")
    
    val safeProfileName = profile.name.replace("\"", "\"\"")
    
    for (t in profile.transactions) {
        val date = formatDisplayDate(t.dateMillis)
        val type = t.type.name
        val title = t.title.replace("\"", "\"\"")
        val category = t.category.replace("\"", "\"\"")
        val note = t.note.replace("\"", "\"\"")
        
        val safeTitle = if (title.contains(",") || title.contains("\n") || title.contains("\"")) "\"$title\"" else title
        val safeCategory = if (category.contains(",") || category.contains("\n") || category.contains("\"")) "\"$category\"" else category
        val safeNote = if (note.contains(",") || note.contains("\n") || note.contains("\"")) "\"$note\"" else note
        
        val sign = if (t.type == TransactionType.EXPENSE) "-" else ""
        val baseAmount = "$sign${t.amount}"
        val convertedValue = convertFn(t.amount, t.baseCurrency, displayCurrency)
        val convertedAmountStr = "$sign$convertedValue"
        
        builder.append("\"$safeProfileName\",")
        builder.append("$date,")
        builder.append("$type,")
        builder.append("$safeTitle,")
        builder.append("$safeCategory,")
        builder.append("$baseAmount,")
        builder.append("${t.baseCurrency},")
        builder.append("$convertedAmountStr,")
        builder.append("$displayCurrency,")
        builder.append("$safeNote\n")
    }
    
    return builder.toString()
}
