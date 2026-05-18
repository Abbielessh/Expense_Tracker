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
import com.example.expensetracker.notifications.ReminderScheduler
import com.example.expensetracker.ui.components.CurrencyDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    appData: ExpenseAppData,
    onCurrencySelected: (String) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecurring: () -> Unit = {},
    repository: com.example.expensetracker.data.SupabaseRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePassword by rememberSaveable { mutableStateOf(false) }

    // Daily reminder state
    val localSettings = remember { ReminderScheduler.loadLocal(context) }
    var reminderEnabled by rememberSaveable { mutableStateOf(localSettings.first) }
    var reminderHour by rememberSaveable { mutableStateOf(localSettings.second) }
    var reminderMinute by rememberSaveable { mutableStateOf(localSettings.third) }
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

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFF8FBFF), Color(0xFFFFFFFF)))
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF101828))
                Spacer(modifier = Modifier.height(10.dp))
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("🔔 Daily Reminder", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Get a daily notification to record your expenses.", fontSize = 14.sp, color = Color(0xFF667085))
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
                                color = Color(0xFFDC2626), fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Time picker row
                        Text("Reminder time", fontSize = 14.sp, color = Color(0xFF667085))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour picker
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hour: $reminderHour")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Slider(
                                    value = reminderHour.toFloat(),
                                    onValueChange = { reminderHour = it.toInt() },
                                    valueRange = 0f..23f,
                                    steps = 22,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF2563EB))
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Min: ${reminderMinute.toString().padStart(2, '0')}")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Slider(
                                    value = reminderMinute.toFloat(),
                                    onValueChange = { reminderMinute = it.toInt() },
                                    valueRange = 0f..59f,
                                    steps = 58,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF2563EB))
                                )
                            }
                        }

                        Text(
                            "Set to: ${reminderHour.toString().padStart(2,'0')}:${reminderMinute.toString().padStart(2,'0')}",
                            fontSize = 13.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                reminderSaving = true
                                reminderMessage = ""
                                // Save time and reschedule if enabled
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            if (reminderSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Save Reminder Settings", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (reminderMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(reminderMessage, fontSize = 13.sp,
                                color = if (reminderMessage.contains("✓")) Color(0xFF059669) else Color(0xFF667085))
                        }
                    }
                }
            }

            // Export
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Export Data", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Download a PDF or CSV report for the active profile", fontSize = 14.sp, color = Color(0xFF667085))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onExportPdf, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A3FF))
                        ) { Text("Export PDF Report", fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onExportCsv, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) { Text("Export CSV / Excel", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }

            // Account
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showChangePassword = true },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) { Text("Change Password", fontWeight = FontWeight.Bold, color = Color.White) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                        ) { Text("Logout", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }

            // About
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expense Tracker App", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version 1.0", fontSize = 14.sp, color = Color(0xFF667085))
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
}

@Composable
private fun SettingsCard(title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101828))
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 14.sp, color = Color(0xFF667085))
            }
            Text("›", fontSize = 24.sp, color = Color(0xFF9E9E9E))
        }
    }
}
