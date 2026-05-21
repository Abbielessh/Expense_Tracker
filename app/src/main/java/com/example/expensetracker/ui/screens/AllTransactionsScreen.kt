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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.ui.components.EmptyStateCard
import com.example.expensetracker.ui.components.TransactionRow
import com.example.expensetracker.utils.CategoryIconUtils
import java.util.Calendar

private const val PAGE_SIZE = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    appData: ExpenseAppData,
    activeProfile: ExpenseProfile,
    onUpdateTransaction: (MoneyTransaction, onDone: () -> Unit) -> Unit,
    onDeleteTransaction: (String, onDone: () -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var transactionToEdit by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var isEditSaving by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf(TxFilter.ALL) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var currentPage by rememberSaveable { mutableStateOf(1) }

    // Reset to page 1 whenever filters or search change
    LaunchedEffect(searchQuery, activeFilter, categoryFilter) {
        currentPage = 1
    }

    // Full filtered list (newest first) — recomputed only when inputs change
    val filtered = remember(activeProfile.transactions, searchQuery, activeFilter, categoryFilter) {
        val now = Calendar.getInstance()
        activeProfile.transactions
            .sortedByDescending { it.dateMillis }
            .filter { tx ->
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
            .filter { tx -> categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true) }
            .filter { tx ->
                if (searchQuery.isBlank()) true
                else {
                    val q = searchQuery.lowercase()
                    tx.title.lowercase().contains(q) ||
                    tx.category.lowercase().contains(q) ||
                    tx.note.lowercase().contains(q) ||
                    tx.amount.toString().contains(q)
                }
            }
    }

    val totalPages = remember(filtered) { maxOf(1, (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE) }
    // Clamp page in case deletion reduces total pages
    val safePage = currentPage.coerceIn(1, totalPages)
    if (safePage != currentPage) currentPage = safePage

    val pagedItems = remember(filtered, currentPage) {
        filtered.drop((currentPage - 1) * PAGE_SIZE).take(PAGE_SIZE)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFF8FBFF), Color(0xFFFFFFFF))))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, color = Color(0xFF2563EB))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "All Transactions", fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold, color = Color(0xFF101828)
                        )
                        Text(
                            activeProfile.name, fontSize = 13.sp, color = Color(0xFF667085)
                        )
                    }
                }
            }

            // ── Search bar ───────────────────────────────────────────────────
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

            // ── Filter chips ─────────────────────────────────────────────────
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

            // ── Result count + clear ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filtered.size} result${if (filtered.size != 1) "s" else ""}",
                        fontSize = 13.sp, color = Color(0xFF667085)
                    )
                    if (searchQuery.isNotBlank() || categoryFilter != null || activeFilter != TxFilter.ALL) {
                        TextButton(onClick = {
                            searchQuery = ""
                            categoryFilter = null
                            activeFilter = TxFilter.ALL
                        }) {
                            Text("Clear filters", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Transaction rows ────────────────────────────────────────────
            if (pagedItems.isEmpty()) {
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
                            }
                        }
                    }
                }
            } else {
                items(pagedItems, key = { it.id }) { transaction ->
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

            // ── Pagination controls ──────────────────────────────────────────
            if (filtered.size > PAGE_SIZE) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("← Prev")
                            }

                            Text(
                                "Page $currentPage of $totalPages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF101828),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            OutlinedButton(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next →")
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit dialog
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
