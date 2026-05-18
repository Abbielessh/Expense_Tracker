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
    val baseCurrency: String = "INR",
    val displayCurrency: String = "INR",
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

/** Category with optional icon support */
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val iconKey: String = "other"
)

data class ExpenseProfile(
    val id: String = UUID.randomUUID().toString(),   // mirrors Supabase profiles.id
    val name: String = "Profile 1",
    val categories: List<String> = defaultCategories(),
    val categoryObjects: List<Category> = emptyList(), // rich category objects with icons
    val transactions: List<MoneyTransaction> = emptyList()
)

data class ExpenseAppData(
    val currencyCode: String = "INR",
    val activeProfileIndex: Int = 0,
    val profiles: List<ExpenseProfile> = listOf(
        ExpenseProfile(name = "Profile 1"),
        ExpenseProfile(name = "Profile 2")
    )
)

/** Recurring transaction model */
data class RecurringTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val profileId: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val baseCurrency: String = "INR",
    val note: String = "",
    val frequency: String = "MONTHLY",   // DAILY / WEEKLY / MONTHLY
    val startDate: String = "",          // yyyy-MM-dd
    val nextDueDate: String = "",        // yyyy-MM-dd
    val isActive: Boolean = true
)

/** Daily reminder settings */
data class ReminderSettings(
    val userId: String = "",
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
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

fun defaultCategoryObjects(): List<Category> {
    return listOf(
        Category(name = "Food", iconKey = "food"),
        Category(name = "Travel", iconKey = "travel"),
        Category(name = "Shopping", iconKey = "shopping"),
        Category(name = "Rent", iconKey = "rent"),
        Category(name = "Bills", iconKey = "bills"),
        Category(name = "Health", iconKey = "health"),
        Category(name = "Education", iconKey = "education"),
        Category(name = "Entertainment", iconKey = "entertainment"),
        Category(name = "Fuel", iconKey = "fuel"),
        Category(name = "Other", iconKey = "other")
    )
}
