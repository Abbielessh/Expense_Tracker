package com.example.expensetracker.utils

import com.example.expensetracker.model.MoneyTransaction
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatMoney(amount: Double, currencyCode: String): String {
    return try {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = Currency.getInstance(currencyCode.uppercase())
        formatter.format(amount)
    } catch (_: Exception) {
        "${currencyCode.uppercase()} ${String.format(Locale.US, "%.2f", amount)}"
    }
}

fun sumBetween(
    transactions: List<MoneyTransaction>,
    startMillis: Long,
    endMillis: Long,
    currencyCode: String
): Double {
    return transactions
        .filter { it.dateMillis in startMillis..endMillis }
        .sumOf { it.getConvertedAmount(currencyCode) }
}
