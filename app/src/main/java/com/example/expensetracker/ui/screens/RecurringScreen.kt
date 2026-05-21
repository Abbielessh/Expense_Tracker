package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.RecurringTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.formatMoney
import java.text.SimpleDateFormat
import java.util.*

private val DATE_FMT_REC = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@Composable
fun RecurringScreen(
    profile: ExpenseProfile,
    currencyCode: String,
    recurringList: List<RecurringTransaction>,
    onAdd: (RecurringTransaction) -> Unit,
    onUpdate: (RecurringTransaction) -> Unit,
    onDelete: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    deletingId: String?
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringTransaction?>(null) }

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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF101828))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) { Text("+ Add") }
                }
            }

            val active = recurringList.filter { it.isActive }
            val inactive = recurringList.filter { !it.isActive }

            if (active.isNotEmpty()) {
                item {
                    Text("Active", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                }
                items(active, key = { it.id }) { rec ->
                    RecurringCard(
                        rec = rec,
                        currencyCode = currencyCode,
                        isDeleting = deletingId == rec.id,
                        onEdit = { editingItem = rec },
                        onDelete = { onDelete(rec.id) },
                        onToggle = { onToggleActive(rec.id, !rec.isActive) }
                    )
                }
            }

            if (inactive.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Inactive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF667085))
                }
                items(inactive, key = { it.id }) { rec ->
                    RecurringCard(
                        rec = rec,
                        currencyCode = currencyCode,
                        isDeleting = deletingId == rec.id,
                        onEdit = { editingItem = rec },
                        onDelete = { onDelete(rec.id) },
                        onToggle = { onToggleActive(rec.id, !rec.isActive) }
                    )
                }
            }

            if (recurringList.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔄 No recurring transactions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Add recurring expenses like Rent, Netflix, EMI.", fontSize = 14.sp, color = Color(0xFF667085))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecurringFormDialog(
            profile = profile,
            currencyCode = currencyCode,
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { rec ->
                onAdd(rec)
                showAddDialog = false
            }
        )
    }

    if (editingItem != null) {
        RecurringFormDialog(
            profile = profile,
            currencyCode = currencyCode,
            existing = editingItem,
            onDismiss = { editingItem = null },
            onSave = { rec ->
                onUpdate(rec)
                editingItem = null
            }
        )
    }
}

@Composable
private fun RecurringCard(
    rec: RecurringTransaction,
    currencyCode: String,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val isExpense = rec.type == TransactionType.EXPENSE
    val icon = CategoryIconUtils.iconForCategory(rec.category, null)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (rec.isActive) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(rec.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                    Text(
                        "${rec.category} • ${rec.frequency}",
                        fontSize = 12.sp, color = Color(0xFF667085)
                    )
                    Text(
                        "Next: ${rec.nextDueDate}",
                        fontSize = 12.sp, color = Color(0xFF2563EB)
                    )
                }
                Text(
                    text = if (isExpense) "- ${formatMoney(rec.amount, currencyCode)}"
                           else "+ ${formatMoney(rec.amount, currencyCode)}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = if (isExpense) Color(0xFFDC2626) else Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active toggle chip
                FilterChip(
                    selected = rec.isActive,
                    onClick = onToggle,
                    label = { Text(if (rec.isActive) "Active" else "Inactive", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.White
                    )
                )
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                ) { Text("Edit") }
                OutlinedButton(
                    onClick = { if (!isDeleting) showDeleteDialog = true },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFFDC2626), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deleting...")
                    } else Text("Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recurring") },
            text = { Text("Delete recurring transaction \"${rec.title}\"? This will not delete past transactions.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormDialog(
    profile: ExpenseProfile,
    currencyCode: String,
    existing: RecurringTransaction?,
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var amountText by rememberSaveable { mutableStateOf(existing?.amount?.toString() ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var frequency by rememberSaveable { mutableStateOf(existing?.frequency ?: "MONTHLY") }
    var startDate by rememberSaveable { mutableStateOf(existing?.startDate ?: DATE_FMT_REC.format(Date())) }
    var isActive by rememberSaveable { mutableStateOf(existing?.isActive ?: true) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var freqMenuExpanded by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    fun calcNextDue(start: String, freq: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.time = DATE_FMT_REC.parse(start) ?: return start
            val today = DATE_FMT_REC.format(Date())
            if (start >= today) return start
            // Advance to next future date
            while (DATE_FMT_REC.format(cal.time) < today) {
                when (freq.uppercase()) {
                    "DAILY"   -> cal.add(Calendar.DAY_OF_MONTH, 1)
                    "WEEKLY"  -> cal.add(Calendar.DAY_OF_MONTH, 7)
                    "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                }
            }
            DATE_FMT_REC.format(cal.time)
        } catch (e: Exception) { start }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Recurring" else "Edit Recurring") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type chips
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = type == TransactionType.EXPENSE,
                            onClick = { type = TransactionType.EXPENSE; category = "" },
                            label = { Text("Expense") }
                        )
                        FilterChip(
                            selected = type == TransactionType.INCOME,
                            onClick = { type = TransactionType.INCOME; category = "Income" },
                            label = { Text("Income") }
                        )
                    }
                }
                // Title
                item {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                // Amount
                item {
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text("Amount ($currencyCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                // Category
                if (type == TransactionType.EXPENSE) {
                    item {
                        OutlinedTextField(
                            value = category, onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedButton(onClick = { categoryMenuExpanded = true }) { Text("Select Category") }
                            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                                profile.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            val icon = CategoryIconUtils.iconForCategory(cat, null)
                                            Text("$icon $cat")
                                        },
                                        onClick = { category = cat; categoryMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                // Note
                item {
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth()
                    )
                }
                // Frequency
                item {
                    Box {
                        OutlinedButton(
                            onClick = { freqMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Frequency: $frequency") }
                        DropdownMenu(expanded = freqMenuExpanded, onDismissRequest = { freqMenuExpanded = false }) {
                            listOf("DAILY", "WEEKLY", "MONTHLY").forEach { f ->
                                DropdownMenuItem(text = { Text(f) }, onClick = { frequency = f; freqMenuExpanded = false })
                            }
                        }
                    }
                }
                // Start date
                item {
                    com.example.expensetracker.ui.components.AppDatePickerField(
                        label = "Start Date",
                        selectedDate = startDate,
                        onDateSelected = { startDate = it }
                    )
                }
                // Active toggle
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Active", modifier = Modifier.weight(1f))
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }
                if (error.isNotBlank()) {
                    item { Text(error, color = Color(0xFFDC2626), fontSize = 13.sp) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                val finalCategory = if (type == TransactionType.INCOME) "Income" else category.ifBlank { "Other" }
                val finalTitle = title.ifBlank { if (type == TransactionType.INCOME) "Income" else "Expense" }
                if (amount == null || amount <= 0) {
                    error = "Please enter a valid amount."
                    return@Button
                }
                if (startDate.isBlank()) {
                    error = "Please select a start date."
                    return@Button
                }
                val nextDue = calcNextDue(startDate, frequency)
                val rec = RecurringTransaction(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    userId = existing?.userId ?: "",
                    profileId = existing?.profileId ?: profile.id,
                    type = type,
                    title = finalTitle,
                    category = finalCategory,
                    amount = amount,
                    baseCurrency = currencyCode,
                    note = note,
                    frequency = frequency,
                    startDate = startDate,
                    nextDueDate = nextDue,
                    isActive = isActive
                )
                onSave(rec)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
