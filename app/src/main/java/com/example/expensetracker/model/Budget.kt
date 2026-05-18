package com.example.expensetracker.model

import java.util.UUID

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val profileId: String = "",
    val category: String = "ALL",   // "ALL" = overall monthly budget
    val period: String = "monthly",
    val limitAmount: Double = 0.0,
    val currency: String = "INR"
)
