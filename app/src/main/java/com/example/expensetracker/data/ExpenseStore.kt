package com.example.expensetracker.data

import android.content.Context
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.model.defaultCategories
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ExpenseStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("expense_tracker_store", Context.MODE_PRIVATE)

    fun load(): ExpenseAppData {
        val raw = prefs.getString("app_data", null) ?: return ExpenseAppData()

        return try {
            val root = JSONObject(raw)
            val currencyCode = root.optString("currencyCode", "INR")
            val activeProfileIndex = root.optInt("activeProfileIndex", 0)
            val profilesJson = root.optJSONArray("profiles") ?: JSONArray()

            val profiles = mutableListOf<ExpenseProfile>()

            for (i in 0 until profilesJson.length()) {
                val profileJson = profilesJson.getJSONObject(i)

                val name = profileJson.optString("name", "Profile ${i + 1}")

                val categoriesJson = profileJson.optJSONArray("categories") ?: JSONArray()
                val categories = mutableListOf<String>()

                for (c in 0 until categoriesJson.length()) {
                    categories.add(categoriesJson.optString(c))
                }

                val transactionsJson = profileJson.optJSONArray("transactions") ?: JSONArray()
                val transactions = mutableListOf<MoneyTransaction>()

                for (t in 0 until transactionsJson.length()) {
                    val transactionJson = transactionsJson.getJSONObject(t)

                    val typeString = transactionJson.optString("type", "EXPENSE")
                    val type = try {
                        TransactionType.valueOf(typeString)
                    } catch (_: Exception) {
                        TransactionType.EXPENSE
                    }

                    transactions.add(
                        MoneyTransaction(
                            id = transactionJson.optString("id", UUID.randomUUID().toString()),
                            type = type,
                            title = transactionJson.optString("title", ""),
                            category = transactionJson.optString("category", "Other"),
                            amount = transactionJson.optDouble("amount", 0.0),
                            dateMillis = transactionJson.optLong("dateMillis", System.currentTimeMillis()),
                            note = transactionJson.optString("note", "")
                        )
                    )
                }

                profiles.add(
                    ExpenseProfile(
                        name = name,
                        categories = if (categories.isEmpty()) defaultCategories() else categories,
                        transactions = transactions
                    )
                )
            }

            if (profiles.isEmpty()) {
                ExpenseAppData()
            } else {
                ExpenseAppData(
                    currencyCode = currencyCode,
                    activeProfileIndex = activeProfileIndex.coerceIn(0, profiles.lastIndex),
                    profiles = profiles
                )
            }
        } catch (_: Exception) {
            ExpenseAppData()
        }
    }

    fun save(data: ExpenseAppData) {
        val root = JSONObject()
        root.put("currencyCode", data.currencyCode)
        root.put("activeProfileIndex", data.activeProfileIndex)

        val profilesJson = JSONArray()

        data.profiles.forEach { profile ->
            val profileJson = JSONObject()
            profileJson.put("name", profile.name)

            val categoriesJson = JSONArray()
            profile.categories.forEach { categoriesJson.put(it) }
            profileJson.put("categories", categoriesJson)

            val transactionsJson = JSONArray()

            profile.transactions.forEach { transaction ->
                val transactionJson = JSONObject()
                transactionJson.put("id", transaction.id)
                transactionJson.put("type", transaction.type.name)
                transactionJson.put("title", transaction.title)
                transactionJson.put("category", transaction.category)
                transactionJson.put("amount", transaction.amount)
                transactionJson.put("dateMillis", transaction.dateMillis)
                transactionJson.put("note", transaction.note)
                transactionsJson.put(transactionJson)
            }

            profileJson.put("transactions", transactionsJson)
            profilesJson.put(profileJson)
        }

        root.put("profiles", profilesJson)

        prefs.edit()
            .putString("app_data", root.toString())
            .apply()
    }
}
