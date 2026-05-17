package com.example.expensetracker

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.ui.screens.AddTransactionScreen
import com.example.expensetracker.ui.screens.HomeScreen
import com.example.expensetracker.ui.screens.ProfilesScreen
import com.example.expensetracker.ui.screens.ReportsScreen
import com.example.expensetracker.ui.screens.SettingsScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.utils.createExpensePdf

class MainActivity : ComponentActivity() {

    private var pendingPdfBytes: ByteArray? = null

    private val pdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && pendingPdfBytes != null) {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(pendingPdfBytes)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpenseTrackerTheme {
                val context = LocalContext.current
                val store = remember { ExpenseStore(context) }
                var appData by remember { mutableStateOf(store.load()) }

                fun updateData(newData: ExpenseAppData) {
                    appData = newData
                    store.save(newData)
                }

                val onExportPdf = {
                    val activeProfile = appData.profiles[
                        appData.activeProfileIndex.coerceIn(0, appData.profiles.lastIndex)
                    ]

                    val pdfBytes = createExpensePdf(
                        context = context,
                        appData = appData,
                        profile = activeProfile
                    )

                    pendingPdfBytes = pdfBytes

                    val safeProfileName = activeProfile.name
                        .replace(" ", "_")
                        .replace("/", "_")

                    pdfLauncher.launch("expense_report_$safeProfileName.pdf")
                }

                ExpenseAppNavigation(
                    appData = appData,
                    onDataChange = { updateData(it) },
                    onExportPdf = { onExportPdf() }
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val iconResId: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_nav_home)
    object Add : Screen("add", "Add", R.drawable.ic_nav_add)
    object Reports : Screen("reports", "Reports", R.drawable.ic_nav_reports)
    object Profiles : Screen("profiles", "Profiles", R.drawable.ic_nav_profiles)
    object Settings : Screen("settings", "Settings", R.drawable.ic_nav_settings)
}

@Composable
fun ExpenseAppNavigation(
    appData: ExpenseAppData,
    onDataChange: (ExpenseAppData) -> Unit,
    onExportPdf: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Add,
        Screen.Reports,
        Screen.Profiles,
        Screen.Settings
    )

    val activeProfileIndex = appData.activeProfileIndex.coerceIn(0, appData.profiles.lastIndex)
    val activeProfile = appData.profiles[activeProfileIndex]

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = screen.iconResId),
                                contentDescription = screen.title,
                                modifier = Modifier.size(26.dp)
                            ) 
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color(0xFF2563EB),
                            indicatorColor = Color(0xFFEAF2FF), // light blue pill background
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color(0xFF667085)
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    appData = appData,
                    activeProfile = activeProfile,
                    onUpdateTransaction = { updatedTx ->
                        val updatedTransactions = activeProfile.transactions.map {
                            if (it.id == updatedTx.id) updatedTx else it
                        }
                        
                        // Check if the category is new and needs to be added
                        val isNewCategory = updatedTx.type == TransactionType.EXPENSE && 
                            !activeProfile.categories.any { it.equals(updatedTx.category, ignoreCase = true) }
                        
                        val newCategories = if (isNewCategory) {
                            activeProfile.categories + updatedTx.category.trim()
                        } else {
                            activeProfile.categories
                        }

                        val updatedProfile = activeProfile.copy(
                            transactions = updatedTransactions,
                            categories = newCategories
                        )
                        val updatedProfiles = appData.profiles.toMutableList()
                        updatedProfiles[activeProfileIndex] = updatedProfile
                        onDataChange(appData.copy(profiles = updatedProfiles))
                    },
                    onDeleteTransaction = { transactionId ->
                        val updatedProfile = activeProfile.copy(
                            transactions = activeProfile.transactions.filterNot {
                                it.id == transactionId
                            }
                        )
                        val updatedProfiles = appData.profiles.toMutableList()
                        updatedProfiles[activeProfileIndex] = updatedProfile
                        onDataChange(appData.copy(profiles = updatedProfiles))
                    }
                )
            }
            composable(Screen.Add.route) {
                AddTransactionScreen(
                    profile = activeProfile,
                    currencyCode = appData.currencyCode,
                    onAddTransaction = { transaction ->
                        val updatedProfile = activeProfile.copy(
                            transactions = activeProfile.transactions + transaction
                        )
                        val updatedProfiles = appData.profiles.toMutableList()
                        updatedProfiles[activeProfileIndex] = updatedProfile
                        onDataChange(appData.copy(profiles = updatedProfiles))
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onAddCategory = { category ->
                        val cleaned = category.trim()
                        if (cleaned.isNotEmpty()) {
                            val exists = activeProfile.categories.any {
                                it.equals(cleaned, ignoreCase = true)
                            }
                            if (!exists) {
                                val updatedProfile = activeProfile.copy(
                                    categories = activeProfile.categories + cleaned
                                )
                                val updatedProfiles = appData.profiles.toMutableList()
                                updatedProfiles[activeProfileIndex] = updatedProfile
                                onDataChange(appData.copy(profiles = updatedProfiles))
                            }
                        }
                    }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    profile = activeProfile,
                    currencyCode = appData.currencyCode
                )
            }
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
                            val updatedProfiles = appData.profiles + ExpenseProfile(name = cleanName)
                            onDataChange(
                                appData.copy(
                                    profiles = updatedProfiles,
                                    activeProfileIndex = updatedProfiles.lastIndex
                                )
                            )
                        }
                    },
                    onEditProfile = { index, newName ->
                        val cleanName = newName.trim()
                        if (cleanName.isNotEmpty()) {
                            val updatedProfile = appData.profiles[index].copy(name = cleanName)
                            val updatedProfiles = appData.profiles.toMutableList()
                            updatedProfiles[index] = updatedProfile
                            onDataChange(appData.copy(profiles = updatedProfiles))
                        }
                    },
                    onDeleteProfile = { index ->
                        if (appData.profiles.size > 1) {
                            val updatedProfiles = appData.profiles.toMutableList()
                            updatedProfiles.removeAt(index)
                            
                            val newActiveIndex = if (index == appData.activeProfileIndex) {
                                0
                            } else if (index < appData.activeProfileIndex) {
                                appData.activeProfileIndex - 1
                            } else {
                                appData.activeProfileIndex
                            }
                            
                            onDataChange(
                                appData.copy(
                                    profiles = updatedProfiles,
                                    activeProfileIndex = newActiveIndex.coerceIn(0, updatedProfiles.lastIndex)
                                )
                            )
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    appData = appData,
                    onCurrencySelected = { selectedCurrency ->
                        onDataChange(appData.copy(currencyCode = selectedCurrency.uppercase()))
                    },
                    onExportPdf = onExportPdf
                )
            }
        }
    }
}