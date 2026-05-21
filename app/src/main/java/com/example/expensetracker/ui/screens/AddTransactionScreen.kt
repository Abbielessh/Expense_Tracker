package com.example.expensetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.ui.components.AddCategoryDialog
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.formatDate
import com.example.expensetracker.utils.parseDateStart

@Composable
fun AddTransactionScreen(
    profile: ExpenseProfile,
    currencyCode: String,
    onAddTransaction: (MoneyTransaction, onDone: () -> Unit) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var categoryText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    // remember (not rememberSaveable) so the date always resets to today when the
    // screen re-enters the composition (e.g. returning to the Add tab after saving).
    var dateText by remember { mutableStateOf(formatDate(System.currentTimeMillis())) }
    var isSaving by remember { mutableStateOf(false) }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

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
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add Entry",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showQuickAdd = true },
                                shape  = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7C3AED),
                                    contentColor   = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 12.dp, vertical = 6.dp
                                )
                            ) {
                                Text("✨ Easy Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = type == TransactionType.EXPENSE,
                                onClick = { type = TransactionType.EXPENSE },
                                label = { Text("Expense") }
                            )

                            FilterChip(
                                selected = type == TransactionType.INCOME,
                                onClick = { type = TransactionType.INCOME },
                                label = { Text("Income Optional") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(if (type == TransactionType.EXPENSE) "Expense title" else "Income title") },
                            placeholder = { Text("Example: Lunch, Salary, Petrol") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount in $currencyCode") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        AnimatedVisibility(visible = type == TransactionType.EXPENSE) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))

                                // Show icon preview next to category text
                                val iconPreview = CategoryIconUtils.iconForCategory(categoryText, null)
                                OutlinedTextField(
                                    value = categoryText,
                                    onValueChange = { categoryText = it },
                                    label = { Text("Category") },
                                    placeholder = { Text("Type manually or select from dropdown") },
                                    leadingIcon = if (categoryText.isNotBlank()) {
                                        { Text(iconPreview, fontSize = 18.sp) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box {
                                        OutlinedButton(
                                            onClick = { categoryMenuExpanded = true }
                                        ) {
                                            Text("Category Dropdown")
                                        }

                                        DropdownMenu(
                                            expanded = categoryMenuExpanded,
                                            onDismissRequest = { categoryMenuExpanded = false }
                                        ) {
                                            // Prefer rich category objects if available
                                            val richCats = profile.categoryObjects
                                            if (richCats.isNotEmpty()) {
                                                richCats.forEach { cat ->
                                                    val emoji = CategoryIconUtils.emojiForKey(cat.iconKey)
                                                    DropdownMenuItem(
                                                        text = { Text("$emoji  ${cat.name}") },
                                                        onClick = {
                                                            categoryText = cat.name
                                                            categoryMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            } else {
                                                profile.categories.forEach { category ->
                                                    val emoji = CategoryIconUtils.iconForCategory(category, null)
                                                    DropdownMenuItem(
                                                        text = { Text("$emoji  $category") },
                                                        onClick = {
                                                            categoryText = category
                                                            categoryMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { showAddCategoryDialog = true },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("+ Add")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        com.example.expensetracker.ui.components.AppDatePickerField(
                            label = "Date",
                            selectedDate = dateText,
                            onDateSelected = { dateText = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note optional") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                val parsedDate = parseDateStart(dateText)

                                if (amount != null && amount > 0 && parsedDate != null && !isSaving) {
                                    val finalCategory = if (type == TransactionType.INCOME) {
                                        "Income"
                                    } else {
                                        categoryText.ifBlank { "Other" }
                                    }

                                    val finalTitle = title.ifBlank {
                                        if (type == TransactionType.EXPENSE) "Expense" else "Income"
                                    }

                                    isSaving = true
                                    onAddTransaction(
                                        MoneyTransaction(
                                            type = type,
                                            title = finalTitle,
                                            category = finalCategory,
                                            amount = amount,
                                            baseCurrency = currencyCode,
                                            displayCurrency = currencyCode,
                                            dateMillis = parsedDate,
                                            note = note
                                        )
                                    ) {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Saving...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(text = "Save Entry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { category ->
                onAddCategory(category)
                categoryText = category
                showAddCategoryDialog = false
            }
        )
    }

    if (showQuickAdd) {
        EasyAddDialog(
            profile          = profile,
            currencyCode     = currencyCode,
            onAddTransaction = onAddTransaction,
            onAddCategory    = onAddCategory,
            onEditDetails    = { draft ->
                // Pre-fill the main form with parsed draft and close dialog
                type         = draft.type
                title        = draft.title
                amountText   = draft.amount.toBigDecimal().stripTrailingZeros().toPlainString()
                categoryText = if (draft.type == TransactionType.EXPENSE) draft.category else ""
                dateText     = draft.dateStr
                note         = draft.note
                showQuickAdd = false
            },
            onDismiss = { showQuickAdd = false }
        )
    }
}
