package com.example.expensetracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.RecurringTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.ui.components.EmptyStateCard
import com.example.expensetracker.ui.components.SummaryMiniCard
import com.example.expensetracker.ui.components.TransactionRow
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.ui.theme.AppStyles
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.formatMoney
import com.example.expensetracker.utils.getConvertedAmount
import java.util.Calendar

// ── Filter definitions (shared with AllTransactionsScreen) ───────────────────
enum class TxFilter(val label: String) {
    ALL("All"),
    EXPENSE("Expense"),
    INCOME("Income"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

private const val RECENT_LIMIT = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appData: ExpenseAppData,
    activeProfile: ExpenseProfile,
    recurringList: List<RecurringTransaction> = emptyList(),
    onUpdateTransaction: (MoneyTransaction, onDone: () -> Unit) -> Unit,
    onDeleteTransaction: (String, onDone: () -> Unit) -> Unit,
    onViewAllTransactions: () -> Unit
) {
    var transactionToEdit by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var isEditSaving by remember { mutableStateOf(false) }

    // Latest 5, newest first — cached, only recomputed when transactions change
    val recentTransactions = remember(activeProfile.transactions) {
        activeProfile.transactions
            .sortedByDescending { it.dateMillis }
            .take(RECENT_LIMIT)
    }
    val totalCount = activeProfile.transactions.size
    val hasMore = totalCount > RECENT_LIMIT

    // Upcoming recurring (next 3, sorted by due date) — cached
    val upcoming = remember(recurringList) {
        recurringList
            .filter { it.isActive && it.nextDueDate.isNotBlank() }
            .sortedBy { it.nextDueDate }
            .take(3)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome back", fontSize = 14.sp, color = AppColors.SecondaryText)
                        Text(
                            activeProfile.name, fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold, color = AppColors.PrimaryText
                        )
                    }
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = com.example.expensetracker.R.drawable.app_logo_expense
                        ),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(68.dp).clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Summary card
            item { BalanceSummaryCard(profile = activeProfile, currencyCode = appData.currencyCode) }

            // Upcoming Recurring card
            if (upcoming.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                        colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔄", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Upcoming Recurring",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Warning
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            upcoming.forEach { rec ->
                                val iconRes = CategoryIconUtils.iconResForCategory(rec.category, null)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = rec.category,
                                            modifier = Modifier.size(20.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(rec.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Text("${rec.frequency} • ${com.example.expensetracker.utils.formatDisplayDateStr(rec.nextDueDate)}", fontSize = 11.sp, color = AppColors.SecondaryText)
                                        }
                                    }
                                    Text(
                                        "- ${formatMoney(rec.amount, appData.currencyCode)}",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = AppColors.ExpenseError
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Recent Transactions", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = AppColors.PrimaryText
                        )
                        if (hasMore) {
                            Text(
                                "Showing latest $RECENT_LIMIT of $totalCount",
                                fontSize = 12.sp, color = AppColors.SecondaryText
                            )
                        }
                    }
                    if (totalCount > 0) {
                        Text("$totalCount total", fontSize = 12.sp, color = AppColors.SecondaryText)
                    }
                }
            }

            // Recent transaction rows
            if (recentTransactions.isEmpty()) {
                item { EmptyStateCard() }
            } else {
                items(recentTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currencyCode = appData.currencyCode,
                        isDeleting = deletingId == transaction.id,
                        onEdit = { transactionToEdit = transaction },
                        onDelete = {
                            deletingId = transaction.id
                            onDeleteTransaction(transaction.id) { deletingId = null }
                        }
                    )
                }
            }

            // "View All Transactions" button — only shown when total > 5
            if (hasMore) {
                item {
                    Button(
                        onClick = onViewAllTransactions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryAccent)
                    ) {
                        Text(
                            "View All Transactions  →",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            profile = activeProfile,
            currencyCode = appData.currencyCode,
            isSaving = isEditSaving,
            onDismiss = { if (!isEditSaving) transactionToEdit = null },
            onSave = { updatedTransaction ->
                isEditSaving = true
                onUpdateTransaction(updatedTransaction) {
                    isEditSaving = false
                    transactionToEdit = null
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BalanceSummaryCard(profile: ExpenseProfile, currencyCode: String) {
    val totalExpense = remember(profile.transactions, currencyCode) {
        profile.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.getConvertedAmount(currencyCode) }
    }
    val totalIncome = remember(profile.transactions, currencyCode) {
        profile.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.getConvertedAmount(currencyCode) }
    }
    val balance = totalIncome - totalExpense

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppStyles.CardCornerRadius),
        colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Overview", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMiniCard("Income", formatMoney(totalIncome, currencyCode), AppColors.SuccessIncome, Modifier.weight(1f))
                SummaryMiniCard("Expense", formatMoney(totalExpense, currencyCode), AppColors.ExpenseError, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            SummaryMiniCard(
                "Balance", formatMoney(balance, currencyCode),
                if (balance >= 0) AppColors.SuccessIncome else AppColors.ExpenseError,
                Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EditTransactionDialog(
    transaction: MoneyTransaction,
    profile: ExpenseProfile,
    currencyCode: String,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (MoneyTransaction) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(transaction.type) }
    var title by rememberSaveable { mutableStateOf(transaction.title) }
    var amountText by rememberSaveable { mutableStateOf(transaction.amount.toString()) }
    var categoryText by rememberSaveable { mutableStateOf(transaction.category) }
    var note by rememberSaveable { mutableStateOf(transaction.note) }
    var dateText by rememberSaveable { mutableStateOf(com.example.expensetracker.utils.formatDate(transaction.dateMillis)) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit Transaction") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE }, label = { Text("Expense") })
                        FilterChip(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME }, label = { Text("Income") })
                    }
                }
                item {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                item {
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text("Amount ($currencyCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                if (type == TransactionType.EXPENSE) {
                    item {
                        OutlinedTextField(value = categoryText, onValueChange = { categoryText = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedButton(onClick = { categoryMenuExpanded = true }) { Text("Select Category") }
                            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                                // Use rich category objects if available
                                val richCats = profile.categoryObjects
                                if (richCats.isNotEmpty()) {
                                    richCats.forEach { cat ->
                                        val catIconRes = CategoryIconUtils.drawableForKey(cat.iconKey)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = catIconRes),
                                                        contentDescription = cat.name,
                                                        modifier = Modifier.size(20.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                    Text(cat.name)
                                                }
                                            },
                                            onClick = { categoryText = cat.name; categoryMenuExpanded = false }
                                        )
                                    }
                                } else {
                                    profile.categories.forEach { category ->
                                        val catIconRes = CategoryIconUtils.iconResForCategory(category, null)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = catIconRes),
                                                        contentDescription = category,
                                                        modifier = Modifier.size(20.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                    Text(category)
                                                }
                                            },
                                            onClick = { categoryText = category; categoryMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    com.example.expensetracker.ui.components.AppDatePickerField(
                        label = "Date", selectedDate = dateText, onDateSelected = { dateText = it }
                    )
                }
                item {
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val parsedDate = com.example.expensetracker.utils.parseDateStart(dateText)
                    if (amount != null && amount > 0 && parsedDate != null) {
                        onSave(
                            transaction.copy(
                                type = type,
                                title = title.ifBlank { if (type == TransactionType.EXPENSE) "Expense" else "Income" },
                                category = if (type == TransactionType.INCOME) "Income" else categoryText.ifBlank { "Other" },
                                amount = amount, dateMillis = parsedDate, note = note
                            )
                        )
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Updating...")
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } }
    )
}
