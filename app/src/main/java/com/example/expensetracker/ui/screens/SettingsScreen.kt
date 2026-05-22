package com.example.expensetracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.notifications.ReminderReceiver
import com.example.expensetracker.notifications.ReminderScheduler
import com.example.expensetracker.ui.components.CurrencyDialog
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.ui.theme.AppStyles
import com.example.expensetracker.utils.DisplayRefreshRateUtils
import com.example.expensetracker.utils.PdfSplitType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    appData: ExpenseAppData,
    onCurrencySelected: (String) -> Unit,
    onExportPdf: (PdfSplitType) -> Unit,
    onExportCsv: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit = {},
    onApplyRefreshRate: (mode: String, hz: Float) -> Unit = { _, _ -> },
    isDarkMode: Boolean = false,
    onDarkModeToggle: (Boolean) -> Unit = {},
    repository: com.example.expensetracker.data.SupabaseRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePassword by rememberSaveable { mutableStateOf(false) }
    var showPdfSplitDialog by remember { mutableStateOf(false) }
    var selectedSplitType  by remember { mutableStateOf(PdfSplitType.NONE) }

    // ── Smooth Display state ───────────────────────────────────────────────────
    val availableHz = remember { DisplayRefreshRateUtils.getAvailableRefreshRates(context) }
    val currentHz  = remember { DisplayRefreshRateUtils.getCurrentRefreshRate(context) }
    var refreshMode by rememberSaveable {
        mutableStateOf(DisplayRefreshRateUtils.loadSavedMode(context))
    }
    var refreshHz by rememberSaveable {
        mutableStateOf(DisplayRefreshRateUtils.loadSavedHz(context))
    }
    var refreshDropExpanded  by remember { mutableStateOf(false) }
    var refreshMessage       by rememberSaveable { mutableStateOf("") }

    // Daily reminder state
    val localSettings = remember { ReminderScheduler.loadLocal(context) }
    var reminderEnabled by rememberSaveable { mutableStateOf(localSettings.first) }
    // Internal 24-hour values
    var reminderHour by rememberSaveable { mutableStateOf(localSettings.second) }
    var reminderMinute by rememberSaveable { mutableStateOf(localSettings.third) }
    // Derived 12-hour display values
    var hour12 by rememberSaveable {
        mutableStateOf(
            when {
                localSettings.second == 0 -> 12
                localSettings.second <= 12 -> localSettings.second
                else -> localSettings.second - 12
            }
        )
    }
    var selectedMinute by rememberSaveable { mutableStateOf(localSettings.third) }
    var isPm by rememberSaveable { mutableStateOf(localSettings.second >= 12) }

    var reminderSaving by rememberSaveable { mutableStateOf(false) }
    var reminderMessage by rememberSaveable { mutableStateOf("") }
    var notifPermDenied by rememberSaveable { mutableStateOf(false) }

    // Fetch reminder settings from Supabase on first load
    LaunchedEffect(Unit) {
        try {
            val remoteSettings = repository.fetchReminderSettings()
            remoteSettings?.let {
                reminderEnabled = it.dailyReminderEnabled
                reminderHour = it.reminderHour
                reminderMinute = it.reminderMinute
                hour12 = when {
                    it.reminderHour == 0 -> 12
                    it.reminderHour <= 12 -> it.reminderHour
                    else -> it.reminderHour - 12
                }
                selectedMinute = it.reminderMinute
                isPm = it.reminderHour >= 12
            }
        } catch (e: Exception) {
            // fallback to local is fine
        }
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, proceed to enable and schedule
            reminderEnabled = true
            ReminderScheduler.saveLocally(context, true, reminderHour, reminderMinute)
            ReminderScheduler.scheduleReminder(context, reminderHour, reminderMinute)
            scope.launch {
                try { repository.upsertReminderSettings(true, reminderHour, reminderMinute) } catch (_: Exception) {}
            }
            reminderMessage = "Daily reminder enabled ✓"
        } else {
            notifPermDenied = true
        }
    }

    fun hasNotifPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun applyReminderSettings(enabled: Boolean) {
        if (enabled && !hasNotifPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        reminderEnabled = enabled
        ReminderScheduler.saveLocally(context, enabled, reminderHour, reminderMinute)
        if (enabled) {
            ReminderScheduler.scheduleReminder(context, reminderHour, reminderMinute)
        } else {
            ReminderScheduler.cancelReminder(context)
        }
        scope.launch {
            try { repository.upsertReminderSettings(enabled, reminderHour, reminderMinute) } catch (_: Exception) {}
        }
        reminderMessage = if (enabled) "Daily reminder enabled ✓" else "Daily reminder disabled"
    }

    /** Convert 12-hour picker values to 24-hour and update reminder state */
    fun commitTimeSelection() {
        reminderHour = when {
            !isPm && hour12 == 12 -> 0   // 12 AM = 0
            isPm && hour12 != 12 -> hour12 + 12  // 1-11 PM = 13-23
            else -> hour12  // 12 PM = 12, 1-11 AM = 1-11
        }
        reminderMinute = selectedMinute
    }

    /** Format for display e.g. "09:00 PM" */
    fun formatDisplayTime(): String {
        val h = hour12.toString().padStart(2, '0')
        val m = selectedMinute.toString().padStart(2, '0')
        val ampm = if (isPm) "PM" else "AM"
        return "$h:$m $ampm"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.PrimaryText)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Appearance — Dark Mode toggle
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Appearance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.PrimaryText
                        )
                        Text(
                            "Choose your preferred theme",
                            fontSize = 13.sp,
                            color = AppColors.SecondaryText
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Dark Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.PrimaryText
                                )
                                Text(
                                    if (isDarkMode) "Dark theme is active" else "Using light theme",
                                    fontSize = 13.sp,
                                    color = AppColors.SecondaryText
                                )
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = onDarkModeToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AppColors.CardBackground,
                                    checkedTrackColor = AppColors.Primary,
                                    uncheckedThumbColor = AppColors.SecondaryText,
                                    uncheckedTrackColor = AppColors.SurfaceSoft
                                )
                            )
                        }
                    }
                }
            }

            // Currency
            item {
                SettingsCard(
                    title = "Global Currency",
                    subtitle = "Current: ${appData.currencyCode}",
                    onClick = { showCurrencyDialog = true }
                )
            }

            // Budget limits
            item {
                SettingsCard(
                    title = "Budget Limits",
                    subtitle = "Set monthly spending limits per category",
                    onClick = onNavigateToBudgets
                )
            }

            // Recurring
            item {
                SettingsCard(
                    title = "🔄 Recurring Expenses",
                    subtitle = "Manage repeating bills, EMI, subscriptions",
                    onClick = onNavigateToRecurring
                )
            }

            // Daily Reminder section
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("🔔 Daily Reminder", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Get a daily notification to record your expenses.", fontSize = 14.sp, color = AppColors.SecondaryText)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Enable toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable daily reminder", modifier = Modifier.weight(1f), fontSize = 15.sp)
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { applyReminderSettings(it) }
                            )
                        }

                        if (notifPermDenied) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "⚠️ Notification permission is required for daily reminders.",
                                color = AppColors.ExpenseError, fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Dropdown time picker ─────────────────────────────
                        Text("Reminder time", fontSize = 14.sp, color = AppColors.SecondaryText)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hour dropdown (1–12)
                            var hourMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { hourMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Hour: ${hour12.toString().padStart(2, '0')}") }
                                DropdownMenu(
                                    expanded = hourMenuExpanded,
                                    onDismissRequest = { hourMenuExpanded = false }
                                ) {
                                    (1..12).forEach { h ->
                                        DropdownMenuItem(
                                            text = { Text(h.toString().padStart(2, '0')) },
                                            onClick = { hour12 = h; hourMenuExpanded = false; commitTimeSelection() }
                                        )
                                    }
                                }
                            }

                            // Minute dropdown (00, 05, 10 … 55)
                            var minMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { minMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Min: ${selectedMinute.toString().padStart(2, '0')}") }
                                DropdownMenu(
                                    expanded = minMenuExpanded,
                                    onDismissRequest = { minMenuExpanded = false }
                                ) {
                                    listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m.toString().padStart(2, '0')) },
                                            onClick = { selectedMinute = m; minMenuExpanded = false; commitTimeSelection() }
                                        )
                                    }
                                }
                            }

                            // AM/PM dropdown
                            var amPmMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { amPmMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(if (isPm) "PM" else "AM") }
                                DropdownMenu(
                                    expanded = amPmMenuExpanded,
                                    onDismissRequest = { amPmMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("AM") },
                                        onClick = { isPm = false; amPmMenuExpanded = false; commitTimeSelection() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("PM") },
                                        onClick = { isPm = true; amPmMenuExpanded = false; commitTimeSelection() }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Selected: ${formatDisplayTime()}",
                            fontSize = 13.sp, color = AppColors.Primary, fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                reminderSaving = true
                                reminderMessage = ""
                                commitTimeSelection()
                                ReminderScheduler.saveLocally(context, reminderEnabled, reminderHour, reminderMinute)
                                if (reminderEnabled) {
                                    ReminderScheduler.cancelReminder(context)
                                    ReminderScheduler.scheduleReminder(context, reminderHour, reminderMinute)
                                }
                                scope.launch {
                                    try {
                                        repository.upsertReminderSettings(reminderEnabled, reminderHour, reminderMinute)
                                        reminderMessage = "Reminder settings saved ✓"
                                    } catch (e: Exception) {
                                        reminderMessage = "Saved locally (sync failed)"
                                    }
                                    reminderSaving = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryAccent)
                        ) {
                            if (reminderSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Save Reminder Settings", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Send Test Notification button
                        OutlinedButton(
                            onClick = {
                                if (!hasNotifPermission()) {
                                    reminderMessage = "⚠️ Notification permission required."
                                } else {
                                    ReminderReceiver.showReminderNotification(context)
                                    reminderMessage = "Test notification sent ✓"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("🔔 Send Test Notification", fontWeight = FontWeight.Bold)
                        }

                        if (reminderMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(reminderMessage, fontSize = 13.sp,
                                color = if (reminderMessage.contains("✓")) AppColors.SuccessIncome else AppColors.SecondaryText)
                        }

                        // Debug info
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = AppColors.Border)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Debug", fontSize = 12.sp, color = AppColors.SecondaryText, fontWeight = FontWeight.Bold)
                        Text("Reminder enabled: ${if (reminderEnabled) "Yes" else "No"}",
                            fontSize = 12.sp, color = AppColors.SecondaryText)
                        Text("Reminder time: ${formatDisplayTime()} (24h: ${reminderHour.toString().padStart(2,'0')}:${reminderMinute.toString().padStart(2,'0')})",
                            fontSize = 12.sp, color = AppColors.SecondaryText)
                        Text("Notification permission: ${if (hasNotifPermission()) "Granted" else "Denied"}",
                            fontSize = 12.sp, color = if (hasNotifPermission()) AppColors.SuccessIncome else AppColors.ExpenseError)
                    }
                }
            }

            // Smooth Display
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "⚡ Smooth Display",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Set preferred display refresh rate for smoother scrolling.",
                            fontSize = 14.sp, color = AppColors.SecondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Current: ${currentHz.toInt()}Hz",
                                fontSize = 13.sp, color = AppColors.Primary, fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Available: ${availableHz.joinToString(", ") { "${it.toInt()}Hz" }}",
                                fontSize = 13.sp, color = AppColors.SecondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dropdown selector
                        Box {
                            OutlinedButton(
                                onClick = { refreshDropExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    DisplayRefreshRateUtils.modeLabel(refreshMode, refreshHz, availableHz),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("▾", fontSize = 14.sp, color = AppColors.SecondaryText)
                            }
                            DropdownMenu(
                                expanded = refreshDropExpanded,
                                onDismissRequest = { refreshDropExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Auto (system default)") },
                                    onClick = {
                                        refreshMode = DisplayRefreshRateUtils.MODE_AUTO
                                        refreshDropExpanded = false
                                        refreshMessage = ""
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("Highest Available (${(availableHz.maxOrNull() ?: 60f).toInt()}Hz)")
                                    },
                                    onClick = {
                                        refreshMode = DisplayRefreshRateUtils.MODE_HIGHEST
                                        refreshDropExpanded = false
                                        refreshMessage = ""
                                    }
                                )
                                availableHz.forEach { hz ->
                                    DropdownMenuItem(
                                        text = { Text("${hz.toInt()}Hz") },
                                        onClick = {
                                            refreshMode = DisplayRefreshRateUtils.MODE_CUSTOM
                                            refreshHz   = hz
                                            refreshDropExpanded = false
                                            refreshMessage = ""
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                DisplayRefreshRateUtils.saveMode(context, refreshMode, refreshHz)
                                onApplyRefreshRate(refreshMode, refreshHz)
                                refreshMessage = when (refreshMode) {
                                    DisplayRefreshRateUtils.MODE_AUTO ->
                                        "Display set to Auto (system default)."
                                    DisplayRefreshRateUtils.MODE_HIGHEST ->
                                        "Using highest available (${(availableHz.maxOrNull() ?: 60f).toInt()}Hz)."
                                    DisplayRefreshRateUtils.MODE_CUSTOM -> {
                                        val supported = availableHz.any {
                                            it >= refreshHz - 1f && it <= refreshHz + 1f
                                        }
                                        if (supported) "Refresh rate set to ${refreshHz.toInt()}Hz."
                                        else "${refreshHz.toInt()}Hz is not available on this device."
                                    }
                                    else -> ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryAccent)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (refreshMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                refreshMessage,
                                fontSize = 13.sp,
                                color = if (refreshMessage.contains("not available"))
                                    AppColors.ExpenseError
                                else
                                    AppColors.SuccessIncome
                            )
                        }
                    }
                }
            }

            // Export
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Export Data", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Download a PDF or CSV report for the active profile", fontSize = 14.sp, color = AppColors.SecondaryText)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showPdfSplitDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryAccent)
                        ) { Text("Export PDF Report", fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onExportCsv, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.SuccessIncome)
                        ) { Text("Export CSV / Excel", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }

            // Account
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showChangePassword = true },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryAccent)
                        ) { Text("Change Password", fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.ExpenseError)
                        ) { Text("Logout", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }

            // About
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyles.CardCornerRadius),
                    colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expense Tracker App", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version 1.0", fontSize = 14.sp, color = AppColors.SecondaryText)
                    }
                }
            }
        }
    }

    if (showCurrencyDialog) {
        CurrencyDialog(
            currentCurrency = appData.currencyCode,
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { selectedCurrency ->
                onCurrencySelected(selectedCurrency)
                showCurrencyDialog = false
            }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            repository = repository,
            onDismiss = { showChangePassword = false }
        )
    }

    // ── PDF split-type selection dialog ────────────────────────────────────────
    if (showPdfSplitDialog) {
        AlertDialog(
            onDismissRequest = { showPdfSplitDialog = false },
            title = { Text("Export PDF Report", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Choose how to split the report:",
                        fontSize = 14.sp, color = AppColors.SecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PdfSplitType.values().forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSplitType = type }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedSplitType == type,
                                onClick = { selectedSplitType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(type.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                val desc = when (type) {
                                    PdfSplitType.NONE  -> "All transactions in one list"
                                    PdfSplitType.DAY   -> "Group by day with daily totals"
                                    PdfSplitType.WEEK  -> "Group by week with weekly totals"
                                    PdfSplitType.MONTH -> "Group by month with monthly totals"
                                }
                                Text(desc, fontSize = 11.sp, color = AppColors.SecondaryText)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPdfSplitDialog = false
                    onExportPdf(selectedSplitType)
                }) {
                    Text("Export", fontWeight = FontWeight.Bold, color = AppColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfSplitDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(AppStyles.CardCornerRadius),
        colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppStyles.CardElevation)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.PrimaryText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 14.sp, color = AppColors.SecondaryText)
            }
            Text("›", fontSize = 24.sp, color = AppColors.TextMuted)
        }
    }
}
