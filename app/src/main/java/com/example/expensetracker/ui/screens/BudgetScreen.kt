package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.Budget
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.utils.formatMoney
import com.example.expensetracker.utils.getConvertedAmount
import java.util.Calendar

@Composable
fun BudgetScreen(
    appData: ExpenseAppData,
    activeProfile: ExpenseProfile,
    budgets: List<Budget>,
    onUpsertBudget: (Budget) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editBudget by remember { mutableStateOf<Budget?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }

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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Budget Limits", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF101828))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A3FF))
                    ) {
                        Text("+ Add", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (budgets.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No budgets set", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667085))
                            Text("Tap + Add to set a spending limit.", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                        }
                    }
                }
            }

            items(budgets, key = { it.id }) { budget ->
                BudgetCard(
                    budget = budget,
                    activeProfile = activeProfile,
                    currencyCode = appData.currencyCode,
                    isDeleting = deletingId == budget.id,
                    onEdit = { editBudget = budget },
                    onDelete = {
                        deletingId = budget.id
                        onDeleteBudget(budget.id)
                    }
                )
            }
        }
    }

    if (showAddDialog || editBudget != null) {
        BudgetFormDialog(
            existing = editBudget,
            activeProfile = activeProfile,
            currencyCode = appData.currencyCode,
            onDismiss = { showAddDialog = false; editBudget = null },
            onSave = { budget ->
                onUpsertBudget(budget)
                showAddDialog = false
                editBudget = null
            }
        )
    }
}

@Composable
private fun BudgetCard(
    budget: Budget,
    activeProfile: ExpenseProfile,
    currencyCode: String,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Calculate spent this month for this budget category
    val now = Calendar.getInstance()
    val spent = activeProfile.transactions
        .filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
            tx.type == TransactionType.EXPENSE &&
            (budget.category == "ALL" || tx.category.equals(budget.category, ignoreCase = true))
        }
        .sumOf { tx -> tx.getConvertedAmount(currencyCode) }

    val pctRaw = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
    val pct = if (pctRaw > 1.0) 1.0 else if (pctRaw < 0.0) 0.0 else pctRaw
    val remaining = budget.limitAmount - spent
    val isOver = spent > budget.limitAmount
    val isWarning = pct >= 0.8 && !isOver

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isOver -> Color(0xFFFFF1F1)
                isWarning -> Color(0xFFFFFDE7)
                else -> Color.White
            }
        ), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = if (budget.category == "ALL") "Overall Monthly Budget" else budget.category,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828)
                    )
                    Text("Limit: ${formatMoney(budget.limitAmount, currencyCode)}", fontSize = 13.sp, color = Color(0xFF667085))
                }
                if (isOver) Text("⚠️ Over", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                else if (isWarning) Text("⚠️ 80%+", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = when {
                    isOver -> Color(0xFFDC2626)
                    isWarning -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                },
                trackColor = Color(0xFFE5E7EB)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent: ${formatMoney(spent, currencyCode)}", fontSize = 12.sp, color = if (isOver) Color(0xFFDC2626) else Color(0xFF374151))
                Text(
                    text = if (isOver) "Over by ${formatMoney(-remaining, currencyCode)}" else "Left: ${formatMoney(remaining, currencyCode)}",
                    fontSize = 12.sp,
                    color = if (isOver) Color(0xFFDC2626) else Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Edit") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFFDC2626), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deleting...")
                    } else {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormDialog(
    existing: Budget?,
    activeProfile: ExpenseProfile,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    val categoryOptions = listOf("ALL") + activeProfile.categories
    var selectedCategory by remember { mutableStateOf(existing?.category ?: "ALL") }
    var limitText by remember { mutableStateOf(existing?.limitAmount?.toString() ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Budget" else "Add Budget Limit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory == "ALL") "All Categories (Monthly Total)" else selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (cat == "ALL") "All Categories (Monthly Total)" else cat) },
                                onClick = { selectedCategory = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Limit Amount ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(validationError!!, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (limit == null || limit <= 0) {
                        validationError = "Please enter a valid amount greater than 0."
                        return@Button
                    }
                    onSave(
                        (existing ?: Budget()).copy(
                            profileId = activeProfile.id,
                            category = selectedCategory,
                            period = "monthly",
                            limitAmount = limit,
                            currency = currencyCode
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
