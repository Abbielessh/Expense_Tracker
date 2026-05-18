package com.example.expensetracker.utils

import com.example.expensetracker.data.CurrencyRepository
import com.example.expensetracker.model.MoneyTransaction

fun MoneyTransaction.getConvertedAmount(targetCurrency: String): Double {
    return amount * CurrencyRepository.getRateSync(baseCurrency, targetCurrency)
}
