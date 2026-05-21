package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.formatDisplayDate
import com.example.expensetracker.utils.formatMoney

@Composable
fun SummaryMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFF7FAFF)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF667085)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: MoneyTransaction,
    currencyCode: String,
    isDeleting: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Determine emoji icon from category name (iconKey not stored in MoneyTransaction,
    // so we guess from the category name)
    val categoryIcon = CategoryIconUtils.iconForCategory(transaction.category, null)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category icon
                Text(
                    text = categoryIcon,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("${transaction.category} • ${formatDisplayDate(transaction.dateMillis)}", fontSize = 13.sp, color = Color(0xFF667085))
                    if (transaction.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(transaction.note, fontSize = 12.sp, color = Color(0xFF98A2B3))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isExpense) "- ${formatMoney(transaction.amount, currencyCode)}"
                               else "+ ${formatMoney(transaction.amount, currencyCode)}",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (isExpense) Color(0xFFDC2626) else Color(0xFF059669)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onEdit,
                    enabled = !isDeleting,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                ) { Text("Edit") }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { if (!isDeleting) showDeleteDialog = true },
                    enabled = !isDeleting,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    if (isDeleting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(14.dp), color = Color(0xFFDC2626), strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deleting...")
                    } else {
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun EmptyStateCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No transactions yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add your first expense or income entry.",
                fontSize = 14.sp,
                color = Color(0xFF667085)
            )
        }
    }
}

@Composable
fun CurrencyDialog(
    currentCurrency: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit
) {
    val currencies = listOf(
        "INR", "USD", "EUR", "GBP", "AED", "SAR", "JPY", "CAD",
        "AUD", "SGD", "CHF", "CNY", "MYR", "THB", "LKR", "BDT",
        "PKR", "NPR", "ZAR", "BRL"
    )

    var customCurrency by rememberSaveable { mutableStateOf(currentCurrency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Global Currency")
        },
        text = {
            Column {
                Text(
                    text = "Choose common currency or type any currency code.",
                    fontSize = 14.sp,
                    color = Color(0xFF667085)
                )

                Spacer(modifier = Modifier.height(12.dp))

                currencies.chunked(4).forEach { rowCurrencies ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCurrencies.forEach { currency ->
                            AssistChip(
                                onClick = { onCurrencySelected(currency) },
                                label = { Text(currency) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customCurrency,
                    onValueChange = { customCurrency = it.uppercase() },
                    label = { Text("Custom currency code") },
                    placeholder = { Text("Example: INR, USD, AED") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customCurrency.isNotBlank()) {
                        onCurrencySelected(customCurrency.trim().uppercase())
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Enhanced AddCategoryDialog with icon picker.
 * Returns both name and selected icon_key via onAdd callback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onAddWithIcon: ((name: String, iconKey: String) -> Unit)? = null
) {
    var category by rememberSaveable { mutableStateOf("") }
    var selectedIconKey by rememberSaveable { mutableStateOf("other") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        // Auto-suggest icon based on typed name
                        val guessed = CategoryIconUtils.guessIconKeyFromName(it)
                        if (guessed != "other") selectedIconKey = guessed
                    },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Icon", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344054))
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryIconUtils.allIconKeys.forEach { key ->
                        val emoji = CategoryIconUtils.emojiForKey(key)
                        FilterChip(
                            selected = selectedIconKey == key,
                            onClick = { selectedIconKey = key },
                            label = { Text("$emoji $key", fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (category.isNotBlank()) {
                        if (onAddWithIcon != null) {
                            onAddWithIcon(category.trim(), selectedIconKey)
                        } else {
                            onAdd(category.trim())
                        }
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var profileName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Profile") },
        text = {
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text("Profile name") },
                placeholder = { Text("Example: Me, Brother, Roommate") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (profileName.isNotBlank()) {
                        onAdd(profileName.trim())
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
