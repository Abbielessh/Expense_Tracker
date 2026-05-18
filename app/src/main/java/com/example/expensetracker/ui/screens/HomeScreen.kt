package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.formatMoney
import com.example.expensetracker.utils.getConvertedAmount
import java.util.Calendar

// ── Filter definitions ────────────────────────────────────────────────────────
enum class TxFilter(val label: String) {
    ALL("All"),
    EXPENSE("Expense"),
    INCOME("Income"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appData: ExpenseAppData,
    activeProfile: ExpenseProfile,
    recurringList: List<RecurringTransaction> = emptyList(),
    onUpdateTransaction: (MoneyTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var transactionToEdit by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf(TxFilter.ALL) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Apply all filters + search
    val now = Calendar.getInstance()
    val filtered = activeProfile.transactions
        .sortedByDescending { it.dateMillis }
        .filter { tx ->
            // Type / time filter
            when (activeFilter) {
                TxFilter.ALL -> true
                TxFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                TxFilter.INCOME -> tx.type == TransactionType.INCOME
                TxFilter.TODAY -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
                TxFilter.THIS_WEEK -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
                TxFilter.THIS_MONTH -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
            }
        }
        .filter { tx ->
            // Category filter
            categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)
        }
        .filter { tx ->
            // Search query
            if (searchQuery.isBlank()) true
            else {
                val q = searchQuery.lowercase()
                tx.title.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                tx.amount.toString().contains(q)
            }
        }

    // Upcoming recurring (next 3, sorted by due date)
    val upcoming = recurringList
        .filter { it.isActive && it.nextDueDate.isNotBlank() }
        .sortedBy { it.nextDueDate }
        .take(3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFF8FBFF), Color(0xFFFFFFFF))))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
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
                        Text("Welcome back", fontSize = 14.sp, color = Color(0xFF667085))
                        Text(
                            activeProfile.name, fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold, color = Color(0xFF101828)
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

            // Upcoming Recurring card (only shown if there are upcoming items)
            if (upcoming.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFFBEB)),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔄", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Upcoming Recurring",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            upcoming.forEach { rec ->
                                val icon = CategoryIconUtils.iconForCategory(rec.category, null)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(icon, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(rec.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Text("${rec.frequency} • ${rec.nextDueDate}", fontSize = 11.sp, color = Color(0xFF667085))
                                        }
                                    }
                                    Text(
                                        "- ${formatMoney(rec.amount, appData.currencyCode)}",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search transactions…") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }

            // Filter chips row
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TxFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    // Category chips with icons
                    activeProfile.categories.forEach { cat ->
                        val icon = CategoryIconUtils.iconForCategory(cat, null)
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                            label = { Text("$icon $cat", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Transactions header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transactions", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF101828)
                    )
                    if (filtered.isNotEmpty()) {
                        Text("${filtered.size} results", fontSize = 12.sp, color = Color(0xFF667085))
                    }
                }
            }

            // Transaction list
            if (filtered.isEmpty()) {
                item {
                    if (activeProfile.transactions.isEmpty()) {
                        EmptyStateCard()
                    } else {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔍 No matching transactions found.", fontSize = 14.sp, color = Color(0xFF667085))
                                if (searchQuery.isNotBlank() || categoryFilter != null) {
                                    TextButton(onClick = { searchQuery = ""; categoryFilter = null; activeFilter = TxFilter.ALL }) {
                                        Text("Clear filters")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currencyCode = appData.currencyCode,
                        isDeleting = deletingId == transaction.id,
                        onEdit = { transactionToEdit = transaction },
                        onDelete = {
                            deletingId = transaction.id
                            onDeleteTransaction(transaction.id)
                        }
                    )
                }
            }
        }
    }

    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            profile = activeProfile,
            currencyCode = appData.currencyCode,
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTransaction ->
                onUpdateTransaction(updatedTransaction)
                transactionToEdit = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BalanceSummaryCard(profile: ExpenseProfile, currencyCode: String) {
    val totalExpense = profile.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.getConvertedAmount(currencyCode) }
    val totalIncome = profile.transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.getConvertedAmount(currencyCode) }
    val balance = totalIncome - totalExpense

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Overview", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMiniCard("Income", formatMoney(totalIncome, currencyCode), Color(0xFF059669), Modifier.weight(1f))
                SummaryMiniCard("Expense", formatMoney(totalExpense, currencyCode), Color(0xFFDC2626), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            SummaryMiniCard(
                "Balance", formatMoney(balance, currencyCode),
                if (balance >= 0) Color(0xFF2563EB) else Color(0xFFDC2626),
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
        onDismissRequest = onDismiss,
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
                                        val emoji = CategoryIconUtils.emojiForKey(cat.iconKey)
                                        DropdownMenuItem(
                                            text = { Text("$emoji  ${cat.name}") },
                                            onClick = { categoryText = cat.name; categoryMenuExpanded = false }
                                        )
                                    }
                                } else {
                                    profile.categories.forEach { category ->
                                        val emoji = CategoryIconUtils.iconForCategory(category, null)
                                        DropdownMenuItem(
                                            text = { Text("$emoji  $category") },
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
            Button(onClick = {
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
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
