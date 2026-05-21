package com.example.expensetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.data.ExpenseStore
import com.example.expensetracker.data.SupabaseClientProvider
import com.example.expensetracker.data.SupabaseRepository
import com.example.expensetracker.model.Budget
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.RecurringTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.notifications.ReminderScheduler
import com.example.expensetracker.ui.screens.AddTransactionScreen
import com.example.expensetracker.ui.screens.AllTransactionsScreen
import com.example.expensetracker.ui.screens.BudgetScreen
import com.example.expensetracker.ui.screens.HomeScreen
import com.example.expensetracker.ui.screens.LoginScreen
import com.example.expensetracker.ui.screens.ProfilesScreen
import com.example.expensetracker.ui.screens.RecurringScreen
import com.example.expensetracker.ui.screens.ReportsScreen
import com.example.expensetracker.ui.screens.SettingsScreen
import com.example.expensetracker.ui.screens.SignupScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.utils.NetworkUtils
import com.example.expensetracker.utils.createExpensePdf
import com.example.expensetracker.utils.PdfSplitType
import com.example.expensetracker.utils.DisplayRefreshRateUtils
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingPdfBytes: ByteArray? = null
    private var pendingCsvBytes: ByteArray? = null

    private val pdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && pendingPdfBytes != null) {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(pendingPdfBytes)
            }
        }
    }

    private val csvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && pendingCsvBytes != null) {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(pendingCsvBytes)
            }
        }
    }

    // Shared state: set to true when the deep link confirms the email
    private val _emailConfirmed = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle deep link if app is launched via the confirmation email (PKCE token exchange)
        SupabaseClientProvider.client.handleDeeplinks(intent)
        // If the deep link established a new session, flag it so the UI reacts
        if (SupabaseClientProvider.client.auth.currentSessionOrNull() != null) {
            _emailConfirmed.value = true
        }

        // Apply saved display refresh rate preference immediately
        DisplayRefreshRateUtils.applyRefreshRate(
            this,
            DisplayRefreshRateUtils.loadSavedMode(this),
            DisplayRefreshRateUtils.loadSavedHz(this)
        )

        setContent {
            ExpenseTrackerTheme {
                val context = LocalContext.current
                val store = remember { ExpenseStore(context) }
                val repository = remember { SupabaseRepository() }
                
                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }  // null = checking session
                var emailConfirmed by remember { _emailConfirmed }
                var appData by remember { mutableStateOf<ExpenseAppData?>(null) }
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                // On first launch check for an existing session.
                // hasStoredSession() reads SharedPreferences directly — it is a fast,
                // offline check that works even before the Auth plugin has loaded the
                // session into memory asynchronously.
                LaunchedEffect(Unit) {
                    val hasStored = SupabaseClientProvider.hasStoredSession(context)
                    val loggedIn = when {
                        // Fast-path: session already loaded in memory
                        repository.isUserLoggedIn() -> true
                        // Session exists in storage — try to refresh the access token
                        hasStored -> {
                            val refreshed = try { repository.refreshSession() } catch (_: Exception) { false }
                            // If refresh failed (e.g. network error) keep the user logged in;
                            // fetchAppData() will fall back to local cache on failure.
                            refreshed || repository.isUserLoggedIn()
                        }
                        // Truly no stored session
                        else -> false
                    }
                    isLoggedIn = loggedIn
                }

                // When email is confirmed via deep link, refresh session & show banner
                LaunchedEffect(emailConfirmed) {
                    if (emailConfirmed) {
                        _emailConfirmed.value = false   // reset
                        try {
                            repository.refreshSession()
                        } catch (_: Exception) { }
                        isLoggedIn = repository.isUserLoggedIn()
                        snackbarHostState.showSnackbar("✅ Email confirmed! Welcome.")
                    }
                }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn == true) {
                        try {
                            appData = repository.fetchAppData()
                        } catch (e: Exception) {
                            appData = store.load()
                        }
                    } else if (isLoggedIn == false) {
                        appData = null
                    }
                }

                fun updateData(newData: ExpenseAppData) {
                    appData = newData
                    store.save(newData)   // local cache only — Supabase ops are done individually
                }

                val onExportPdf: (PdfSplitType) -> Unit = { splitType ->
                    appData?.let { data ->
                        val activeProfile = data.profiles[
                            data.activeProfileIndex.coerceIn(0, data.profiles.lastIndex)
                        ]

                        val pdfBytes = createExpensePdf(
                            context = context,
                            appData = data,
                            profile = activeProfile,
                            splitType = splitType
                        )

                        pendingPdfBytes = pdfBytes

                        val safeProfileName = activeProfile.name
                            .replace(" ", "_")
                            .replace("/", "_")

                        pdfLauncher.launch("expense_report_$safeProfileName.pdf")
                    }
                }

                val onExportCsv = {
                    scope.launch {
                        appData?.let { data ->
                            val activeProfile = data.profiles[
                                data.activeProfileIndex.coerceIn(0, data.profiles.lastIndex)
                            ]

                            val csvString = com.example.expensetracker.utils.createCsvContent(
                                profile = activeProfile,
                                displayCurrency = data.currencyCode,
                                convertFn = { amount, from, to ->
                                    com.example.expensetracker.data.CurrencyRepository.convertAmount(amount, from, to)
                                }
                            )

                            pendingCsvBytes = csvString.toByteArray(Charsets.UTF_8)

                            val safeProfileName = activeProfile.name
                                .replace(" ", "_")
                                .replace("/", "_")

                            csvLauncher.launch("expense_report_$safeProfileName.csv")
                        }
                    }
                }

                // Wrap everything in a Scaffold to host the Snackbar
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            )
                        }
                    }
                ) { innerPad ->
                    Box(modifier = Modifier.padding(innerPad)) {
                        if (isLoggedIn == null) {
                            // Session check in progress — show "Checking session..."
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Checking session...", color = Color(0xFF667085))
                                }
                            }
                        } else if (isLoggedIn == false) {
                            var showSignup by remember { mutableStateOf(false) }
                            if (showSignup) {
                                SignupScreen(
                                    onSignupSuccess = { isLoggedIn = true },
                                    onNavigateToLogin = { showSignup = false },
                                    repository = repository
                                )
                            } else {
                                LoginScreen(
                                    onLoginSuccess = { isLoggedIn = true },
                                    onNavigateToSignup = { showSignup = true },
                                    repository = repository
                                )
                            }
                        } else if (appData == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            ExpenseAppNavigation(
                                appData = appData!!,
                                onDataChange = { updateData(it) },
                                onExportPdf = onExportPdf,
                                onExportCsv = { onExportCsv() },
                                repository = repository,
                                onApplyRefreshRate = { mode, hz ->
                                    DisplayRefreshRateUtils.applyRefreshRate(this@MainActivity, mode, hz)
                                },
                                onLogout = {
                                    scope.launch {
                                        repository.logout()
                                        isLoggedIn = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle deep link when app is already running (e.g. brought from background)
        SupabaseClientProvider.client.handleDeeplinks(intent)
        if (SupabaseClientProvider.client.auth.currentSessionOrNull() != null) {
            _emailConfirmed.value = true
        }
    }
}

sealed class Screen(val route: String, val title: String, val iconResId: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_nav_home)
    object Add : Screen("add", "Add", R.drawable.ic_nav_add)
    object Reports : Screen("reports", "Reports", R.drawable.ic_nav_reports)
    object Profiles : Screen("profiles", "Profiles", R.drawable.ic_nav_profiles)
    object Settings : Screen("settings", "Settings", R.drawable.ic_nav_settings)
    object Budget : Screen("budget", "Budget", R.drawable.ic_nav_settings)  // reuses settings icon
    object Recurring : Screen("recurring", "Recurring", R.drawable.ic_nav_settings) // reuses settings icon
    object AllTransactions : Screen("all_transactions", "All Transactions", R.drawable.ic_nav_home) // no nav bar entry
}

@Composable
fun ExpenseAppNavigation(
    appData: ExpenseAppData,
    onDataChange: (ExpenseAppData) -> Unit,
    onExportPdf: (PdfSplitType) -> Unit,
    onExportCsv: () -> Unit,
    repository: SupabaseRepository,
    onApplyRefreshRate: (mode: String, hz: Float) -> Unit = { _, _ -> },
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Budget state
    var budgets by remember { mutableStateOf<List<Budget>>(emptyList()) }

    // Recurring state
    var recurringList by remember { mutableStateOf<List<RecurringTransaction>>(emptyList()) }
    var recurringDeletingId by remember { mutableStateOf<String?>(null) }

    val items = listOf(
        Screen.Home,
        Screen.Add,
        Screen.Reports,
        Screen.Profiles,
        Screen.Settings
    )

    val activeProfileIndex = appData.activeProfileIndex.coerceIn(0, appData.profiles.lastIndex)
    val activeProfile = appData.profiles[activeProfileIndex]

    // Fetch budgets when active profile changes
    LaunchedEffect(activeProfile.id) {
        try { budgets = repository.fetchBudgets(activeProfile.id) } catch (_: Exception) {}
    }

    // Fetch recurring transactions when active profile changes
    LaunchedEffect(activeProfile.id) {
        try { recurringList = repository.fetchRecurringTransactions(activeProfile.id) } catch (_: Exception) {}
    }

    // Process due recurring transactions on home screen load
    LaunchedEffect(activeProfile.id) {
        try {
            val newTx = repository.processDueRecurringTransactions(activeProfile.id)
            if (newTx.isNotEmpty()) {
                val updatedTransactions = activeProfile.transactions + newTx
                val updatedProfile = activeProfile.copy(transactions = updatedTransactions)
                val updatedProfiles = appData.profiles.toMutableList()
                updatedProfiles[activeProfileIndex] = updatedProfile
                onDataChange(appData.copy(profiles = updatedProfiles))
                // Refresh recurring list to get updated next_due_dates
                recurringList = repository.fetchRecurringTransactions(activeProfile.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpenseSupabase", "processDueRecurring failed", e)
        }
    }

    // Pre-fetch currency rates when profile/currency changes
    var ratesReady by remember { mutableStateOf(false) }
    LaunchedEffect(appData.currencyCode, activeProfile.transactions.size) {
        ratesReady = false
        val baseCurrencies = activeProfile.transactions.map { it.baseCurrency }.toSet()
        baseCurrencies.forEach { base ->
            com.example.expensetracker.data.CurrencyRepository.getRate(base, appData.currencyCode)
        }
        ratesReady = true
    }

    // Helper: show error snackbar
    fun showError(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // Network guard helper
    fun requireOnline(onOnline: () -> Unit) {
        if (!NetworkUtils.isOnline(context)) {
            showError("No internet connection. Please try again when online.")
        } else {
            onOnline()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                )
            }
        },
        // contentWindowInsets=0: the outer Scaffold (MainActivity) already consumed
        // the full systemBars insets via its default contentWindowInsets.  Its innerPad
        // shrinks our available area: top=statusBar, bottom=navBar.  Adding the same
        // insets again here would double-pad top and bottom, creating the large gaps.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // 64 dp white surface — no extra spacer needed.  The outer Scaffold's
            // innerPad already places us above the Android system nav buttons.
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = screen.iconResId),
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color(0xFF2563EB),
                                    indicatorColor = Color(0xFFEAF2FF),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color(0xFF667085)
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(durationMillis = 200)) },
            exitTransition = { fadeOut(animationSpec = tween(durationMillis = 200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 200)) },
            popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 200)) }
        ) {
            // ── Home ──────────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    appData = appData,
                    activeProfile = activeProfile,
                    recurringList = recurringList,
                    onViewAllTransactions = {
                        navController.navigate(Screen.AllTransactions.route)
                    },
                    onUpdateTransaction = { updatedTx, onDone ->
                        scope.launch {
                            try {
                                repository.updateTransaction(updatedTx)
                                // Update local state
                                val updatedTransactions = activeProfile.transactions.map {
                                    if (it.id == updatedTx.id) updatedTx else it
                                }
                                val updatedProfile = activeProfile.copy(transactions = updatedTransactions)
                                val updatedProfiles = appData.profiles.toMutableList()
                                updatedProfiles[activeProfileIndex] = updatedProfile
                                onDataChange(appData.copy(profiles = updatedProfiles))
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "updateTransaction failed", e)
                                showError("Failed to update transaction: ${e.message}")
                            } finally {
                                onDone()
                            }
                        }
                    },
                    onDeleteTransaction = { transactionId, onDone ->
                        scope.launch {
                            try {
                                repository.deleteTransaction(transactionId)
                                val updatedProfile = activeProfile.copy(
                                    transactions = activeProfile.transactions.filterNot { it.id == transactionId }
                                )
                                val updatedProfiles = appData.profiles.toMutableList()
                                updatedProfiles[activeProfileIndex] = updatedProfile
                                onDataChange(appData.copy(profiles = updatedProfiles))
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "deleteTransaction failed", e)
                                showError("Failed to delete transaction: ${e.message}")
                            } finally {
                                onDone()
                            }
                        }
                    }
                )
            }

            // ── Add Transaction ────────────────────────────────────────────────
            composable(Screen.Add.route) {
                AddTransactionScreen(
                    profile = activeProfile,
                    currencyCode = appData.currencyCode,
                    onAddTransaction = { transaction, onDone ->
                        if (!NetworkUtils.isOnline(context)) {
                            showError("No internet connection. Please try again when online.")
                            onDone()
                        } else {
                            scope.launch {
                                try {
                                    val savedTx = repository.insertTransaction(transaction, activeProfile.id)
                                    val updatedProfile = activeProfile.copy(transactions = activeProfile.transactions + savedTx)
                                    val updatedProfiles = appData.profiles.toMutableList()
                                    updatedProfiles[activeProfileIndex] = updatedProfile
                                    onDataChange(appData.copy(profiles = updatedProfiles))
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                                    onDone()
                                } catch (e: Exception) {
                                    android.util.Log.e("ExpenseSupabase", "insertTransaction failed", e)
                                    showError("Failed to save: ${NetworkUtils.classifyError(e)}")
                                    onDone()
                                }
                            }
                        }
                    },
                    onAddCategory = { category ->
                        val cleaned = category.trim()
                        if (cleaned.isNotEmpty()) {
                            val exists = activeProfile.categories.any { it.equals(cleaned, ignoreCase = true) }
                            if (!exists) {
                                scope.launch {
                                    try {
                                        val newCat = repository.insertCategory(cleaned, activeProfile.id)
                                        val updatedProfile = activeProfile.copy(
                                            categories = activeProfile.categories + cleaned,
                                            categoryObjects = activeProfile.categoryObjects + newCat
                                        )
                                        val updatedProfiles = appData.profiles.toMutableList()
                                        updatedProfiles[activeProfileIndex] = updatedProfile
                                        onDataChange(appData.copy(profiles = updatedProfiles))
                                    } catch (e: Exception) {
                                        android.util.Log.e("ExpenseSupabase", "insertCategory failed", e)
                                        // Still update local state even if Supabase fails
                                        val updatedProfile = activeProfile.copy(
                                            categories = activeProfile.categories + cleaned
                                        )
                                        val updatedProfiles = appData.profiles.toMutableList()
                                        updatedProfiles[activeProfileIndex] = updatedProfile
                                        onDataChange(appData.copy(profiles = updatedProfiles))
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // ── Reports ────────────────────────────────────────────────────────
            composable(Screen.Reports.route) {
                ReportsScreen(
                    profile = activeProfile,
                    currencyCode = appData.currencyCode
                )
            }

            // ── Profiles ───────────────────────────────────────────────────────
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    appData = appData,
                    onProfileSelected = { index ->
                        onDataChange(appData.copy(activeProfileIndex = index))
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onAddProfile = { profileName ->
                        val cleanName = profileName.trim()
                        if (cleanName.isNotEmpty()) {
                            scope.launch {
                                try {
                                    val newProfile = repository.insertProfile(cleanName)
                                    val updatedProfiles = appData.profiles + newProfile
                                    onDataChange(
                                        appData.copy(
                                            profiles = updatedProfiles,
                                            activeProfileIndex = updatedProfiles.lastIndex
                                        )
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("ExpenseSupabase", "insertProfile failed", e)
                                    showError("Failed to add profile: ${e.message}")
                                }
                            }
                        }
                    },
                    onEditProfile = { index, newName ->
                        val cleanName = newName.trim()
                        if (cleanName.isNotEmpty()) {
                            val profile = appData.profiles[index]
                            scope.launch {
                                try {
                                    repository.updateProfileName(profile.id, cleanName)
                                    val updatedProfile = profile.copy(name = cleanName)
                                    val updatedProfiles = appData.profiles.toMutableList()
                                    updatedProfiles[index] = updatedProfile
                                    onDataChange(appData.copy(profiles = updatedProfiles))
                                } catch (e: Exception) {
                                    android.util.Log.e("ExpenseSupabase", "updateProfile failed", e)
                                    showError("Failed to update profile: ${e.message}")
                                }
                            }
                        }
                    },
                    onDeleteProfile = { index ->
                        if (appData.profiles.size > 1) {
                            val profile = appData.profiles[index]
                            scope.launch {
                                try {
                                    repository.deleteProfile(profile.id)
                                    val updatedProfiles = appData.profiles.toMutableList()
                                    updatedProfiles.removeAt(index)
                                    val newActiveIndex = when {
                                        index == appData.activeProfileIndex -> 0
                                        index < appData.activeProfileIndex -> appData.activeProfileIndex - 1
                                        else -> appData.activeProfileIndex
                                    }
                                    onDataChange(
                                        appData.copy(
                                            profiles = updatedProfiles,
                                            activeProfileIndex = newActiveIndex.coerceIn(0, updatedProfiles.lastIndex)
                                        )
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("ExpenseSupabase", "deleteProfile failed", e)
                                    showError("Failed to delete profile: ${e.message}")
                                }
                            }
                        }
                    }
                )
            }

            // ── Settings ───────────────────────────────────────────────────────
            composable(Screen.Settings.route) {
                SettingsScreen(
                    appData = appData,
                    onCurrencySelected = { selectedCurrency ->
                        val code = selectedCurrency.uppercase()
                        scope.launch {
                            try { repository.updateCurrency(code) } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "updateCurrency failed", e)
                            }
                        }
                        onDataChange(appData.copy(currencyCode = code))
                    },
                    onExportPdf = onExportPdf,
                    onExportCsv = onExportCsv,
                    onLogout = onLogout,
                    onNavigateToBudgets = { navController.navigate(Screen.Budget.route) },
                    onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                    onApplyRefreshRate = onApplyRefreshRate,
                    repository = repository
                )
            }

            // ── Budget ─────────────────────────────────────────────────────────
            composable(Screen.Budget.route) {
                BudgetScreen(
                    appData = appData,
                    activeProfile = activeProfile,
                    budgets = budgets,
                    onUpsertBudget = { budget ->
                        scope.launch {
                            try {
                                val saved = repository.upsertBudget(budget)
                                budgets = budgets.filter { it.id != saved.id } + saved
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "upsertBudget failed", e)
                                showError("Failed to save budget: ${NetworkUtils.classifyError(e)}")
                            }
                        }
                    },
                    onDeleteBudget = { budgetId ->
                        scope.launch {
                            try {
                                repository.deleteBudget(budgetId)
                                budgets = budgets.filterNot { it.id == budgetId }
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "deleteBudget failed", e)
                                showError("Failed to delete budget: ${NetworkUtils.classifyError(e)}")
                            }
                        }
                    }
                )
            }

            // ── Recurring ──────────────────────────────────────────────────────
            composable(Screen.Recurring.route) {
                RecurringScreen(
                    profile = activeProfile,
                    currencyCode = appData.currencyCode,
                    recurringList = recurringList,
                    deletingId = recurringDeletingId,
                    onAdd = { rec ->
                        scope.launch {
                            try {
                                val saved = repository.addRecurringTransaction(
                                    rec.copy(profileId = activeProfile.id)
                                )
                                recurringList = recurringList + saved
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "addRecurring failed", e)
                                showError("Failed to add recurring: ${e.message}")
                            }
                        }
                    },
                    onUpdate = { rec ->
                        scope.launch {
                            try {
                                repository.updateRecurringTransaction(rec)
                                recurringList = recurringList.map { if (it.id == rec.id) rec else it }
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "updateRecurring failed", e)
                                showError("Failed to update recurring: ${e.message}")
                            }
                        }
                    },
                    onDelete = { id ->
                        recurringDeletingId = id
                        scope.launch {
                            try {
                                repository.deleteRecurringTransaction(id)
                                recurringList = recurringList.filterNot { it.id == id }
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "deleteRecurring failed", e)
                                showError("Failed to delete recurring: ${e.message}")
                            } finally {
                                recurringDeletingId = null
                            }
                        }
                    },
                    onToggleActive = { id, isActive ->
                        scope.launch {
                            try {
                                repository.toggleRecurringActive(id, isActive)
                                recurringList = recurringList.map {
                                    if (it.id == id) it.copy(isActive = isActive) else it
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "toggleRecurring failed", e)
                                showError("Failed to toggle recurring: ${e.message}")
                            }
                        }
                    }
                )
            }

            // ── All Transactions ──────────────────────────────────────────────
            composable(Screen.AllTransactions.route) {
                AllTransactionsScreen(
                    appData = appData,
                    activeProfile = activeProfile,
                    onUpdateTransaction = { updatedTx, onDone ->
                        scope.launch {
                            try {
                                repository.updateTransaction(updatedTx)
                                val updatedTransactions = activeProfile.transactions.map {
                                    if (it.id == updatedTx.id) updatedTx else it
                                }
                                val updatedProfile = activeProfile.copy(transactions = updatedTransactions)
                                val updatedProfiles = appData.profiles.toMutableList()
                                updatedProfiles[activeProfileIndex] = updatedProfile
                                onDataChange(appData.copy(profiles = updatedProfiles))
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "updateTransaction failed", e)
                                showError("Failed to update transaction: ${e.message}")
                            } finally {
                                onDone()
                            }
                        }
                    },
                    onDeleteTransaction = { transactionId, onDone ->
                        scope.launch {
                            try {
                                repository.deleteTransaction(transactionId)
                                val updatedProfile = activeProfile.copy(
                                    transactions = activeProfile.transactions.filterNot { it.id == transactionId }
                                )
                                val updatedProfiles = appData.profiles.toMutableList()
                                updatedProfiles[activeProfileIndex] = updatedProfile
                                onDataChange(appData.copy(profiles = updatedProfiles))
                            } catch (e: Exception) {
                                android.util.Log.e("ExpenseSupabase", "deleteTransaction failed", e)
                                showError("Failed to delete transaction: ${e.message}")
                            } finally {
                                onDone()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}