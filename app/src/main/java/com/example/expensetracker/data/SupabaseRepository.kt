package com.example.expensetracker.data

import android.util.Log
import com.example.expensetracker.model.Budget
import com.example.expensetracker.model.Category
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.RecurringTransaction
import com.example.expensetracker.model.ReminderSettings
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.model.defaultCategories
import com.example.expensetracker.model.defaultCategoryObjects
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "ExpenseSupabase"
private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

// ─────────────────────────────────────────────────────────────────────────────
// DTOs — must match Supabase column names exactly
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ProfileDto(
    val id: String? = null,
    val user_id: String,
    val name: String
)

@Serializable
data class CategoryDto(
    val id: String? = null,
    val user_id: String,
    val profile_id: String,
    val name: String,
    val icon_key: String? = "other"
)

@Serializable
data class TransactionDto(
    val id: String? = null,
    val user_id: String,
    val profile_id: String,
    val type: String,
    val title: String,
    val category: String,
    val amount: Double,
    val base_currency: String,
    val display_currency: String,
    val date: String,           // "yyyy-MM-dd"
    val note: String
)

@Serializable
data class UserSettingsDto(
    val user_id: String,
    val default_currency: String
)

@Serializable
data class BudgetDto(
    val id: String? = null,
    val user_id: String,
    val profile_id: String,
    val category: String,
    val period: String,
    val limit_amount: Double,
    val currency: String
)

@Serializable
data class RecurringTransactionDto(
    val id: String? = null,
    val user_id: String,
    val profile_id: String,
    val type: String,
    val title: String,
    val category: String,
    val amount: Double,
    val base_currency: String,
    val note: String,
    val frequency: String,
    val start_date: String,
    val next_due_date: String,
    val is_active: Boolean
)

@Serializable
data class ReminderSettingsDto(
    val user_id: String,
    val daily_reminder_enabled: Boolean,
    val reminder_hour: Int,
    val reminder_minute: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────────────────────

class SupabaseRepository {
    private val client = SupabaseClientProvider.client

    // ── Auth helpers ──────────────────────────────────────────────────────────

    fun isUserLoggedIn(): Boolean = client.auth.currentSessionOrNull() != null

    fun getUserId(): String? = client.auth.currentSessionOrNull()?.user?.id

    suspend fun login(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(
            provider = Email,
            redirectUrl = "expensetracker://auth-callback"
        ) {
            this.email = email
            this.password = password
        }
    }

    suspend fun resendConfirmationEmail(email: String) {
        client.auth.resendEmail(type = OtpType.Email.SIGNUP, email = email)
    }

    suspend fun logout() { client.auth.signOut() }

    suspend fun refreshSession(): Boolean = try {
        client.auth.refreshCurrentSession(); true
    } catch (e: Exception) { e.printStackTrace(); false }

    // ── Full fetch on login ───────────────────────────────────────────────────

    suspend fun fetchAppData(): ExpenseAppData {
        val uid = getUserId() ?: return ExpenseAppData()
        Log.d(TAG, "fetchAppData uid=$uid")

        // 1. Profiles
        val profileDtos = client.postgrest["profiles"]
            .select { filter { eq("user_id", uid) } }
            .decodeList<ProfileDto>()
        Log.d(TAG, "profiles fetched: ${profileDtos.size}")

        // 2. If none → create default
        val resolvedProfiles: List<ProfileDto> = if (profileDtos.isEmpty()) {
            val created = client.postgrest["profiles"]
                .insert(ProfileDto(user_id = uid, name = "Profile 1")) {
                    select()
                }.decodeSingle<ProfileDto>()
            Log.d(TAG, "Created default profile id=${created.id}")
            listOf(created)
        } else profileDtos

        // 3. User settings
        val settingsDtos = client.postgrest["user_settings"]
            .select { filter { eq("user_id", uid) } }
            .decodeList<UserSettingsDto>()
        val currencyCode = settingsDtos.firstOrNull()?.default_currency ?: "INR"

        // 4. Build ExpenseProfile list
        val profiles = resolvedProfiles.map { p ->
            val profileId = p.id ?: return@map ExpenseProfile(name = p.name)

            val catDtos = client.postgrest["categories"]
                .select { filter { eq("profile_id", profileId) } }
                .decodeList<CategoryDto>()

            val categoryObjects = if (catDtos.isEmpty()) {
                defaultCategoryObjects()
            } else {
                catDtos.map { dto ->
                    Category(
                        id = dto.id ?: "",
                        name = dto.name,
                        iconKey = dto.icon_key ?: com.example.expensetracker.utils.CategoryIconUtils.guessIconKeyFromName(dto.name)
                    )
                }
            }
            val categories = if (catDtos.isEmpty()) defaultCategories() else catDtos.map { it.name }

            val txDtos = client.postgrest["transactions"]
                .select { filter { eq("profile_id", profileId) } }
                .decodeList<TransactionDto>()
            Log.d(TAG, "profile $profileId → ${txDtos.size} transactions")

            val transactions = txDtos.map { t ->
                MoneyTransaction(
                    id = t.id ?: "",
                    type = try { TransactionType.valueOf(t.type) } catch (_: Exception) { TransactionType.EXPENSE },
                    title = t.title,
                    category = t.category,
                    amount = t.amount,
                    baseCurrency = t.base_currency,
                    displayCurrency = t.display_currency,
                    dateMillis = DATE_FMT.parse(t.date)?.time ?: System.currentTimeMillis(),
                    note = t.note
                )
            }

            ExpenseProfile(
                id = profileId,
                name = p.name,
                categories = categories,
                categoryObjects = categoryObjects,
                transactions = transactions
            )
        }

        return ExpenseAppData(
            currencyCode = currencyCode,
            activeProfileIndex = 0,
            profiles = profiles
        )
    }

    // ── Individual transaction operations ────────────────────────────────────

    suspend fun insertTransaction(transaction: MoneyTransaction, profileId: String): MoneyTransaction {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "insertTransaction uid=$uid profileId=$profileId title=${transaction.title}")

        val dto = TransactionDto(
            id = null,                          // let Supabase generate UUID
            user_id = uid,
            profile_id = profileId,
            type = transaction.type.name,
            title = transaction.title,
            category = transaction.category,
            amount = transaction.amount,
            base_currency = transaction.baseCurrency,
            display_currency = transaction.displayCurrency,
            date = DATE_FMT.format(Date(transaction.dateMillis)),
            note = transaction.note
        )

        val inserted = client.postgrest["transactions"]
            .insert(dto) { select() }
            .decodeSingle<TransactionDto>()

        Log.d(TAG, "insertTransaction → Supabase id=${inserted.id}")
        return transaction.copy(id = inserted.id ?: transaction.id)
    }

    suspend fun updateTransaction(transaction: MoneyTransaction) {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "updateTransaction id=${transaction.id}")

        client.postgrest["transactions"].update(
            {
                set("type", transaction.type.name)
                set("title", transaction.title)
                set("category", transaction.category)
                set("amount", transaction.amount)
                set("base_currency", transaction.baseCurrency)
                set("display_currency", transaction.displayCurrency)
                set("date", DATE_FMT.format(Date(transaction.dateMillis)))
                set("note", transaction.note)
            }
        ) {
            filter { eq("id", transaction.id) }
        }
        Log.d(TAG, "updateTransaction done")
    }

    suspend fun deleteTransaction(transactionId: String) {
        Log.d(TAG, "deleteTransaction id=$transactionId")
        client.postgrest["transactions"].delete {
            filter { eq("id", transactionId) }
        }
        Log.d(TAG, "deleteTransaction done")
    }

    // ── Profile operations ────────────────────────────────────────────────────

    suspend fun insertProfile(name: String): ExpenseProfile {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "insertProfile name=$name uid=$uid")
        val dto = client.postgrest["profiles"]
            .insert(ProfileDto(user_id = uid, name = name)) { select() }
            .decodeSingle<ProfileDto>()
        Log.d(TAG, "insertProfile → id=${dto.id}")
        return ExpenseProfile(id = dto.id ?: "", name = dto.name)
    }

    suspend fun updateProfileName(profileId: String, newName: String) {
        Log.d(TAG, "updateProfileName id=$profileId newName=$newName")
        client.postgrest["profiles"].update({ set("name", newName) }) {
            filter { eq("id", profileId) }
        }
    }

    suspend fun deleteProfile(profileId: String) {
        Log.d(TAG, "deleteProfile id=$profileId")
        // Delete child records first (cascading may not be set)
        client.postgrest["transactions"].delete { filter { eq("profile_id", profileId) } }
        client.postgrest["categories"].delete { filter { eq("profile_id", profileId) } }
        client.postgrest["profiles"].delete { filter { eq("id", profileId) } }
    }

    // ── Category operations ───────────────────────────────────────────────────

    suspend fun insertCategory(name: String, profileId: String, iconKey: String = "other"): Category {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "insertCategory name=$name profileId=$profileId iconKey=$iconKey")
        val inserted = client.postgrest["categories"].insert(
            CategoryDto(user_id = uid, profile_id = profileId, name = name, icon_key = iconKey)
        ) { select() }.decodeSingle<CategoryDto>()
        return Category(id = inserted.id ?: "", name = inserted.name, iconKey = inserted.icon_key ?: iconKey)
    }

    suspend fun updateCategoryIcon(categoryId: String, iconKey: String) {
        Log.d(TAG, "updateCategoryIcon id=$categoryId iconKey=$iconKey")
        client.postgrest["categories"].update({ set("icon_key", iconKey) }) {
            filter { eq("id", categoryId) }
        }
    }

    // ── Settings operations ───────────────────────────────────────────────────

    suspend fun updateCurrency(currencyCode: String) {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "updateCurrency uid=$uid code=$currencyCode")
        client.postgrest["user_settings"].upsert(
            UserSettingsDto(user_id = uid, default_currency = currencyCode)
        )
    }

    // ── Password operations ───────────────────────────────────────────────────

    /** Sends a password reset email with deep link back to app. */
    suspend fun sendPasswordResetEmail(email: String) {
        Log.d(TAG, "sendPasswordResetEmail email=$email")
        client.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "expensetracker://auth-callback"
        )
    }

    /** Updates the authenticated user's password (requires active session). */
    suspend fun updatePassword(newPassword: String) {
        Log.d(TAG, "updatePassword")
        client.auth.updateUser {
            password = newPassword
        }
    }

    // ── Budget operations ─────────────────────────────────────────────────────

    suspend fun fetchBudgets(profileId: String): List<Budget> {
        val uid = getUserId() ?: return emptyList()
        Log.d(TAG, "fetchBudgets profileId=$profileId")
        return client.postgrest["budgets"]
            .select { filter { eq("profile_id", profileId) } }
            .decodeList<BudgetDto>()
            .map { dto ->
                Budget(
                    id = dto.id ?: "",
                    userId = dto.user_id,
                    profileId = dto.profile_id,
                    category = dto.category,
                    period = dto.period,
                    limitAmount = dto.limit_amount,
                    currency = dto.currency
                )
            }
    }

    suspend fun upsertBudget(budget: Budget): Budget {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "upsertBudget category=${budget.category} limit=${budget.limitAmount}")
        val dto = BudgetDto(
            id = budget.id.ifEmpty { null },
            user_id = uid,
            profile_id = budget.profileId,
            category = budget.category,
            period = budget.period,
            limit_amount = budget.limitAmount,
            currency = budget.currency
        )
        val result = client.postgrest["budgets"]
            .upsert(dto) { select() }
            .decodeSingle<BudgetDto>()
        return budget.copy(id = result.id ?: budget.id)
    }

    suspend fun deleteBudget(budgetId: String) {
        Log.d(TAG, "deleteBudget id=$budgetId")
        client.postgrest["budgets"].delete {
            filter { eq("id", budgetId) }
        }
    }

    // ── Recurring transaction operations ──────────────────────────────────────

    suspend fun fetchRecurringTransactions(profileId: String): List<RecurringTransaction> {
        val uid = getUserId() ?: return emptyList()
        Log.d(TAG, "fetchRecurringTransactions profileId=$profileId")
        return try {
            client.postgrest["recurring_transactions"]
                .select { filter { eq("profile_id", profileId) } }
                .decodeList<RecurringTransactionDto>()
                .map { dto ->
                    RecurringTransaction(
                        id = dto.id ?: "",
                        userId = dto.user_id,
                        profileId = dto.profile_id,
                        type = try { TransactionType.valueOf(dto.type) } catch (_: Exception) { TransactionType.EXPENSE },
                        title = dto.title,
                        category = dto.category,
                        amount = dto.amount,
                        baseCurrency = dto.base_currency,
                        note = dto.note,
                        frequency = dto.frequency,
                        startDate = dto.start_date,
                        nextDueDate = dto.next_due_date,
                        isActive = dto.is_active
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "fetchRecurringTransactions failed", e)
            emptyList()
        }
    }

    suspend fun addRecurringTransaction(rec: RecurringTransaction): RecurringTransaction {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "addRecurringTransaction title=${rec.title}")
        val dto = RecurringTransactionDto(
            id = null,
            user_id = uid,
            profile_id = rec.profileId,
            type = rec.type.name,
            title = rec.title,
            category = rec.category,
            amount = rec.amount,
            base_currency = rec.baseCurrency,
            note = rec.note,
            frequency = rec.frequency,
            start_date = rec.startDate,
            next_due_date = rec.nextDueDate,
            is_active = rec.isActive
        )
        val result = client.postgrest["recurring_transactions"]
            .insert(dto) { select() }
            .decodeSingle<RecurringTransactionDto>()
        return rec.copy(id = result.id ?: rec.id)
    }

    suspend fun updateRecurringTransaction(rec: RecurringTransaction) {
        Log.d(TAG, "updateRecurringTransaction id=${rec.id}")
        val uid = getUserId() ?: error("Not logged in")
        client.postgrest["recurring_transactions"].update({
            set("type", rec.type.name)
            set("title", rec.title)
            set("category", rec.category)
            set("amount", rec.amount)
            set("base_currency", rec.baseCurrency)
            set("note", rec.note)
            set("frequency", rec.frequency)
            set("start_date", rec.startDate)
            set("next_due_date", rec.nextDueDate)
            set("is_active", rec.isActive)
        }) {
            filter { eq("id", rec.id) }
        }
    }

    suspend fun deleteRecurringTransaction(id: String) {
        Log.d(TAG, "deleteRecurringTransaction id=$id")
        client.postgrest["recurring_transactions"].delete {
            filter { eq("id", id) }
        }
    }

    suspend fun toggleRecurringActive(id: String, isActive: Boolean) {
        Log.d(TAG, "toggleRecurringActive id=$id isActive=$isActive")
        client.postgrest["recurring_transactions"].update({ set("is_active", isActive) }) {
            filter { eq("id", id) }
        }
    }

    /**
     * Process all due recurring transactions for the given profile.
     * Creates normal transactions for due items, advances next_due_date.
     * Returns list of newly created transactions so caller can update UI.
     */
    suspend fun processDueRecurringTransactions(profileId: String): List<MoneyTransaction> {
        val uid = getUserId() ?: return emptyList()
        val today = DATE_FMT.format(Date())
        Log.d(TAG, "processDueRecurringTransactions profileId=$profileId today=$today")

        val created = mutableListOf<MoneyTransaction>()

        try {
            val actives = client.postgrest["recurring_transactions"]
                .select { filter {
                    eq("profile_id", profileId)
                    eq("is_active", true)
                }}
                .decodeList<RecurringTransactionDto>()

            for (rec in actives) {
                if (rec.next_due_date.isNullOrBlank()) continue
                if (rec.next_due_date > today) continue  // Not due yet

                // Check duplicate: look for transaction with same note marker
                val marker = "Auto-created from recurring: ${rec.id}"
                val dueDateStr = rec.next_due_date

                // Advance next_due_date to a future date, creating only ONE transaction
                // for the most recent missed due date to avoid bulk creation
                val nextDue = advanceDate(dueDateStr, rec.frequency)
                val skipCreateIfExists = try {
                    val existing = client.postgrest["transactions"]
                        .select { filter {
                            eq("profile_id", profileId)
                            eq("note", marker)
                            eq("date", dueDateStr)
                        }}
                        .decodeList<TransactionDto>()
                    existing.isNotEmpty()
                } catch (e: Exception) { false }

                if (!skipCreateIfExists) {
                    // Create the normal transaction
                    try {
                        val txDto = TransactionDto(
                            id = null,
                            user_id = uid,
                            profile_id = profileId,
                            type = rec.type,
                            title = rec.title,
                            category = rec.category,
                            amount = rec.amount,
                            base_currency = rec.base_currency,
                            display_currency = rec.base_currency,
                            date = dueDateStr,
                            note = marker
                        )
                        val inserted = client.postgrest["transactions"]
                            .insert(txDto) { select() }
                            .decodeSingle<TransactionDto>()
                        Log.d(TAG, "Created recurring tx: ${rec.title} for $dueDateStr")
                        created.add(
                            MoneyTransaction(
                                id = inserted.id ?: "",
                                type = try { TransactionType.valueOf(rec.type) } catch (_: Exception) { TransactionType.EXPENSE },
                                title = rec.title,
                                category = rec.category,
                                amount = rec.amount,
                                baseCurrency = rec.base_currency,
                                displayCurrency = rec.base_currency,
                                dateMillis = DATE_FMT.parse(dueDateStr)?.time ?: System.currentTimeMillis(),
                                note = marker
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create recurring tx ${rec.title}", e)
                    }
                }

                // Always advance next_due_date even if we skipped creating (to avoid re-check loops)
                val newNextDue = if (nextDue <= today) {
                    // If still in past, keep advancing until future
                    advanceToFuture(nextDue, rec.frequency, today)
                } else nextDue

                try {
                    client.postgrest["recurring_transactions"].update({ set("next_due_date", newNextDue) }) {
                        filter { eq("id", rec.id ?: "") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to advance next_due_date for ${rec.title}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "processDueRecurringTransactions failed", e)
        }

        return created
    }

    private fun advanceDate(dateStr: String, frequency: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.time = DATE_FMT.parse(dateStr) ?: return dateStr
            when (frequency.uppercase()) {
                "DAILY"   -> cal.add(Calendar.DAY_OF_MONTH, 1)
                "WEEKLY"  -> cal.add(Calendar.DAY_OF_MONTH, 7)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            }
            DATE_FMT.format(cal.time)
        } catch (e: Exception) { dateStr }
    }

    private fun advanceToFuture(dateStr: String, frequency: String, today: String): String {
        var current = dateStr
        var safety = 0
        while (current <= today && safety < 500) {
            current = advanceDate(current, frequency)
            safety++
        }
        return current
    }

    // ── Reminder settings operations ─────────────────────────────────────────

    suspend fun fetchReminderSettings(): ReminderSettings? {
        val uid = getUserId() ?: return null
        Log.d(TAG, "fetchReminderSettings uid=$uid")
        return try {
            val list = client.postgrest["reminder_settings"]
                .select { filter { eq("user_id", uid) } }
                .decodeList<ReminderSettingsDto>()
            list.firstOrNull()?.let { dto ->
                ReminderSettings(
                    userId = dto.user_id,
                    dailyReminderEnabled = dto.daily_reminder_enabled,
                    reminderHour = dto.reminder_hour,
                    reminderMinute = dto.reminder_minute
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchReminderSettings failed", e)
            null
        }
    }

    suspend fun upsertReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        val uid = getUserId() ?: error("Not logged in")
        Log.d(TAG, "upsertReminderSettings enabled=$enabled hour=$hour minute=$minute")
        client.postgrest["reminder_settings"].upsert(
            ReminderSettingsDto(
                user_id = uid,
                daily_reminder_enabled = enabled,
                reminder_hour = hour,
                reminder_minute = minute
            )
        )
    }
}
