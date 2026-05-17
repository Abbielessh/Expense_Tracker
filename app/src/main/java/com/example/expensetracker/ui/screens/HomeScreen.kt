package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.ui.components.EmptyStateCard
import com.example.expensetracker.ui.components.SummaryMiniCard
import com.example.expensetracker.ui.components.TransactionRow
import com.example.expensetracker.utils.formatMoney
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun HomeScreen(
    appData: ExpenseAppData,
    activeProfile: ExpenseProfile,
    onUpdateTransaction: (MoneyTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var transactionToEdit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<MoneyTransaction?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEAF2FF),
                        Color(0xFFF8FBFF),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back",
                            fontSize = 14.sp,
                            color = Color(0xFF667085)
                        )
                        Text(
                            text = activeProfile.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF101828)
                        )
                    }
                    
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.expensetracker.R.drawable.app_logo_expense),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            item {
                BalanceSummaryCard(
                    profile = activeProfile,
                    currencyCode = appData.currencyCode
                )
            }

            item {
                Text(
                    text = "Recent Transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (activeProfile.transactions.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                items(
                    activeProfile.transactions.sortedByDescending { it.dateMillis }.take(10),
                    key = { it.id }
                ) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currencyCode = appData.currencyCode,
                        onEdit = { transactionToEdit = transaction },
                        onDelete = { onDeleteTransaction(transaction.id) }
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

@Composable
fun BalanceSummaryCard(
    profile: ExpenseProfile,
    currencyCode: String
) {
    val totalExpense = profile.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    val totalIncome = profile.transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }

    val balance = totalIncome - totalExpense

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMiniCard(
                    title = "Income",
                    value = formatMoney(totalIncome, currencyCode),
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f)
                )

                SummaryMiniCard(
                    title = "Expense",
                    value = formatMoney(totalExpense, currencyCode),
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SummaryMiniCard(
                title = "Balance",
                value = formatMoney(balance, currencyCode),
                color = if (balance >= 0) Color(0xFF2563EB) else Color(0xFFDC2626),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
                        FilterChip(
                            selected = type == TransactionType.EXPENSE,
                            onClick = { type = TransactionType.EXPENSE },
                            label = { Text("Expense") }
                        )

                        FilterChip(
                            selected = type == TransactionType.INCOME,
                            onClick = { type = TransactionType.INCOME },
                            label = { Text("Income") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($currencyCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                if (type == TransactionType.EXPENSE) {
                    item {
                        OutlinedTextField(
                            value = categoryText,
                            onValueChange = { categoryText = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { categoryMenuExpanded = true }
                            ) {
                                Text("Select Category")
                            }

                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                profile.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            categoryText = category
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val parsedDate = com.example.expensetracker.utils.parseDateStart(dateText)

                    if (amount != null && amount > 0 && parsedDate != null) {
                        val finalCategory = if (type == TransactionType.INCOME) {
                            "Income"
                        } else {
                            categoryText.ifBlank { "Other" }
                        }

                        val finalTitle = title.ifBlank {
                            if (type == TransactionType.EXPENSE) "Expense" else "Income"
                        }

                        onSave(
                            transaction.copy(
                                type = type,
                                title = finalTitle,
                                category = finalCategory,
                                amount = amount,
                                dateMillis = parsedDate,
                                note = note
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
