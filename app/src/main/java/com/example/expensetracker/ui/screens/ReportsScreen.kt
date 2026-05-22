package com.example.expensetracker.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.ui.components.SummaryMiniCard
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.ui.theme.AppStyles
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.endOfDayMillis
import com.example.expensetracker.utils.formatDate
import com.example.expensetracker.utils.formatMoney
import com.example.expensetracker.utils.getConvertedAmount
import com.example.expensetracker.utils.monthStartMillis
import com.example.expensetracker.utils.parseDateEnd
import com.example.expensetracker.utils.parseDateStart
import com.example.expensetracker.utils.sumBetween
import com.example.expensetracker.utils.todayStartMillis
import com.example.expensetracker.utils.weekStartMillis

@Composable
fun ReportsScreen(
    profile: ExpenseProfile,
    currencyCode: String
) {
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var startDateText by rememberSaveable { mutableStateOf(formatDate(monthStartMillis())) }
    var endDateText by rememberSaveable { mutableStateOf(formatDate(System.currentTimeMillis())) }
    // Hoisted so graph calculations can use it in remember() keys
    var selectedGraphRange by rememberSaveable { mutableStateOf("Month") }

    val reportCategories = remember(profile.categories) { listOf("All") + profile.categories }

    // Period boundaries — computed once per session (stable within a session)
    val todayStart = remember { todayStartMillis() }
    val todayEnd = remember(todayStart) { endOfDayMillis(todayStart) }
    val weekStart = remember { weekStartMillis() }
    val weekEnd = remember { endOfDayMillis(System.currentTimeMillis()) }
    val monthStart = remember { monthStartMillis() }
    val monthEnd = remember { endOfDayMillis(System.currentTimeMillis()) }

    val customStart = remember(startDateText) { parseDateStart(startDateText) }
    val customEnd = remember(endDateText) { parseDateEnd(endDateText) }

    val expenseTransactions = remember(profile.transactions, selectedCategory) {
        profile.transactions.filter {
            it.type == TransactionType.EXPENSE &&
                    (selectedCategory == "All" || it.category == selectedCategory)
        }
    }

    val dailyTotal = remember(expenseTransactions, currencyCode) { sumBetween(expenseTransactions, todayStart, todayEnd, currencyCode) }
    val weeklyTotal = remember(expenseTransactions, currencyCode) { sumBetween(expenseTransactions, weekStart, weekEnd, currencyCode) }
    val monthlyTotal = remember(expenseTransactions, currencyCode) { sumBetween(expenseTransactions, monthStart, monthEnd, currencyCode) }

    val customTotal = remember(expenseTransactions, currencyCode, customStart, customEnd) {
        if (customStart != null && customEnd != null) {
            sumBetween(expenseTransactions, customStart, customEnd, currencyCode)
        } else {
            0.0
        }
    }

    // Graph computations — only recalculated when range/transactions/currency changes
    val graphTransactions = remember(profile.transactions) {
        profile.transactions.filter { it.type == TransactionType.EXPENSE }
    }
    val filteredForGraph = remember(graphTransactions, selectedGraphRange, todayStart, todayEnd, weekStart, weekEnd, monthStart, monthEnd, customStart, customEnd) {
        when (selectedGraphRange) {
            "Today" -> graphTransactions.filter { it.dateMillis in todayStart..todayEnd }
            "Week"  -> graphTransactions.filter { it.dateMillis in weekStart..weekEnd }
            "Month" -> graphTransactions.filter { it.dateMillis in monthStart..monthEnd }
            "Custom" -> if (customStart != null && customEnd != null)
                graphTransactions.filter { it.dateMillis in customStart..customEnd }
            else emptyList()
            else -> emptyList()
        }
    }
    val categoryTotals = remember(filteredForGraph, currencyCode) {
        filteredForGraph
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.getConvertedAmount(currencyCode) } }
            .filterValues { it > 0.0 }
            .toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
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
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Category Expense Report",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box {
                            OutlinedButton(
                                onClick = { categoryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Report Category: $selectedCategory")
                            }

                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                reportCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            selectedCategory = category
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryMiniCard(
                                title = "Today",
                                value = formatMoney(dailyTotal, currencyCode),
                                color = AppColors.ExpenseError,
                                modifier = Modifier.weight(1f)
                            )

                            SummaryMiniCard(
                                title = "This Week",
                                value = formatMoney(weeklyTotal, currencyCode),
                                color = AppColors.Warning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SummaryMiniCard(
                            title = "This Month",
                            value = formatMoney(monthlyTotal, currencyCode),
                            color = AppColors.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Custom Date Range",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            com.example.expensetracker.ui.components.AppDatePickerField(
                                label = "From",
                                selectedDate = startDateText,
                                modifier = Modifier.weight(1f),
                                onDateSelected = { startDateText = it }
                            )

                            com.example.expensetracker.ui.components.AppDatePickerField(
                                label = "To",
                                selectedDate = endDateText,
                                modifier = Modifier.weight(1f),
                                onDateSelected = { endDateText = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SummaryMiniCard(
                            title = "Selected Range Total",
                            value = formatMoney(customTotal, currencyCode),
                            color = AppColors.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Expense Graph Report",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // selectedGraphRange is hoisted to function level for remember() keys
                        val ranges = listOf("Today", "Week", "Month", "Custom")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ranges.forEach { range ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedGraphRange == range,
                                    onClick = { selectedGraphRange = range },
                                    label = { Text(range) },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.SelectedNavBackground,
                                        selectedLabelColor = AppColors.SelectedNavIconText
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // categoryTotals is pre-computed at function level using remember()
                        ExpensePieChart(categoryTotals = categoryTotals, currencyCode = currencyCode)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensePieChart(
    categoryTotals: Map<String, Double>,
    currencyCode: String
) {
    if (categoryTotals.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No expense data available for graph.",
                color = AppColors.SecondaryText,
                fontSize = 14.sp
            )
        }
        return
    }

    val total = categoryTotals.values.sum()
    
    val colors = AppColors.ChartColors

    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp), 
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            var startAngle = -90f
            var index = 0
            categoryTotals.forEach { (_, amount) ->
                val sweepAngle = (amount / total * 360f).toFloat()
                val color = colors[index % colors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 40.dp.toPx(), 
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt
                    )
                )
                startAngle += sweepAngle
                index++
            }
        }
        
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("Total", fontSize = 12.sp, color = AppColors.SecondaryText)
            Text(
                text = formatMoney(total, currencyCode), 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp,
                color = AppColors.PrimaryText
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var index = 0
        categoryTotals.forEach { (category, amount) ->
            val color = colors[index % colors.size]
            val percentage = (amount / total) * 100
            val catIconRes = CategoryIconUtils.iconResForCategory(category, null)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = catIconRes),
                        contentDescription = category,
                        modifier = Modifier.size(18.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        color = AppColors.PrimaryText
                    )
                }
                
                Text(
                    text = "${formatMoney(amount, currencyCode)} (${String.format("%.1f", percentage)}%)", 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = AppColors.PrimaryText
                )
            }
            index++
        }
    }
}
