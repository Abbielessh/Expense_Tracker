package com.example.expensetracker.model

import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME
}

data class MoneyTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class ExpenseProfile(
    val name: String = "Profile 1",
    val categories: List<String> = defaultCategories(),
    val transactions: List<MoneyTransaction> = emptyList()
)

data class ExpenseAppData(
    val currencyCode: String = "INR",
    val activeProfileIndex: Int = 0,
    val profiles: List<ExpenseProfile> = listOf(
        ExpenseProfile("Profile 1"),
        ExpenseProfile("Profile 2")
    )
)

fun defaultCategories(): List<String> {
    return listOf(
        "Food",
        "Travel",
        "Shopping",
        "Rent",
        "Bills",
        "Health",
        "Education",
        "Entertainment",
        "Fuel",
        "Other"
    )
}
