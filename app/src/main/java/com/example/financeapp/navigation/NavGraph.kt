package com.example.financeapp.navigation

import android.app.Activity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.financeapp.screen.settings.AccountSettingsScreen
import com.example.financeapp.screen.main.budget.AddBudgetScreen
import com.example.financeapp.screen.features.category.AddCategoryScreen
import com.example.financeapp.screen.features.recurring.AddRecurringExpenseScreen
import com.example.financeapp.screen.main.transaction.AddTransactionScreen
import com.example.financeapp.screen.features.savings.SavingsGoalsScreen
import com.example.financeapp.screen.features.savings.AddSavingsGoalScreen
import com.example.financeapp.screen.auth.AuthScreen
import com.example.financeapp.screen.main.budget.BudgetScreen
import com.example.financeapp.screen.features.CalendarScreen
import com.example.financeapp.screen.features.category.CategoryScreen
import com.example.financeapp.screen.settings.ExtensionsScreen
import com.example.financeapp.screen.main.dashboard.HomeScreen
import com.example.financeapp.screen.settings.LanguageSettingsScreen
import com.example.financeapp.screen.features.recurring.RecurringExpenseScreen
import com.example.financeapp.screen.auth.RegisterScreen
import com.example.financeapp.screen.settings.SettingsScreen
import com.example.financeapp.screen.main.statistics.StatisticsScreen
import com.example.financeapp.screen.main.transaction.TransactionScreen
import com.example.financeapp.utils.notification.NotificationPreferences
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.auth.AuthViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.savings.SavingsViewModel // THÊM DÒNG NÀY
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.example.financeapp.screen.settings.HelpScreen
import com.example.financeapp.components.ui.CategorySelectionScreen
import com.example.financeapp.screen.features.invoice.InvoiceScannerScreen
import com.example.financeapp.screen.features.ai.ChatAIScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.viewmodel.transaction.CategoryViewModel



@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    aiViewModel: AIViewModel,
    activity: Activity,
    languageViewModel: LanguageViewModel,
    categoryViewModel: CategoryViewModel,
    recurringExpenseViewModel: RecurringExpenseViewModel,
    budgetViewModel: BudgetViewModel,
    savingsViewModel: SavingsViewModel // THÊM DÒNG NÀY
) {
    var userId by remember { mutableStateOf<String?>(null) }
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val transactions by transactionViewModel.transactions.collectAsState()
    val recurringExpenses by recurringExpenseViewModel.recurringExpenses.collectAsState()
    val startDestination = if (currentUser != null) "home" else "auth"

    // Create NotificationPreferences instance
    val context = LocalContext.current
    val notificationPrefs = remember { NotificationPreferences(context) }

    NavHost(navController = navController, startDestination = startDestination) {

        // 🔹 Đăng nhập
        composable("auth") {
            AuthScreen(
                navController = navController,
                authViewModel = authViewModel,
                activity = activity
            )
        }

        // 🔹 Đăng ký
        composable("register") {
            RegisterScreen(onBack = { navController.popBackStack() }, authViewModel = authViewModel)
        }

        // 🔹 Trang chủ
        composable("home") {
            // Convert UserSession sang User
            val userData = remember(currentUser) {
                currentUser?.let { userSession ->
                    com.example.financeapp.data.models.User(
                        id = userSession.id,
                        email = userSession.email,
                        name = userSession.name
                    )
                }
            }

            HomeScreen(
                navController = navController,
                onAddTransaction = { navController.navigate("add_transaction") },
                currentUser = userData, // Truyền User đã convert
                transactions = transactions,
                onCalendarClick = {
                    // Xử lý calendar click
                    navController.navigate("calendar")
                },
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                savingsViewModel = savingsViewModel // THÊM VÀO HOME SCREEN
            )
        }

        // 🔹 Giao dịch
        composable("transactions") {
            TransactionScreen(
                navController = navController,
                onAddTransaction = { navController.navigate("add_transaction") },
                onTransactionClick = { transaction ->
                    navController.navigate("add_edit_transaction?transactionId=${transaction.id}")
                },
                transactionViewModel = transactionViewModel,
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                savingsViewModel = savingsViewModel
            )
        }

        // 🔹 Thêm giao dịch
        composable("add_transaction") {
            AddTransactionScreen(
                navController = navController,
                transactionViewModel = transactionViewModel,
                onBack = { navController.popBackStack() },
                onSave = { transaction ->
                    transactionViewModel.addTransaction(
                        transaction = transaction,
                        budgetViewModel = budgetViewModel
                    )
                },
                savingsViewModel = savingsViewModel // THÊM VÀO ADD TRANSACTION
            )
        }

        composable(
            route = "add_edit_transaction?transactionId={transactionId}",
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            )
        ) { entry ->
            val transactionId = entry.arguments?.getString("transactionId")
            val coroutineScope = rememberCoroutineScope()

            val existingTransaction = remember(transactionId, transactions) {
                transactionId?.let { id ->
                    transactions.find { it.id == id }
                }
            }

            AddTransactionScreen(
                navController = navController,
                existingTransaction = existingTransaction,
                transactionViewModel = transactionViewModel,
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                savingsViewModel = savingsViewModel,
                onBack = { navController.popBackStack() },
                onSave = { transaction ->
                    if (existingTransaction == null) {
                        transactionViewModel.addTransaction(
                            transaction = transaction,
                            budgetViewModel = budgetViewModel
                        )
                    } else {
                        transactionViewModel.updateTransaction(
                            updatedTransaction = transaction,
                            budgetViewModel = budgetViewModel
                        )
                    }
                },
                onDelete = existingTransaction?.let { transaction ->
                    {
                        transactionViewModel.deleteTransaction(
                            transactionId = transaction.id,
                            budgetViewModel = budgetViewModel
                        )
                    }
                }
            )
        }

        // 🔹 Thống kê
        composable("statistics") {
            StatisticsScreen(
                navController = navController,
                transactions = transactions,
                categoryViewModel = categoryViewModel,
                savingsViewModel = savingsViewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                savingsViewModel = savingsViewModel // THÊM VÀO SETTINGS
            )
        }

        // 🔹 Các màn hình khác
        composable("account_settings") {
            AccountSettingsScreen(navController = navController)
        }

        composable("language_settings") {
            LanguageSettingsScreen(
                navController = navController,
                languageViewModel = languageViewModel
            )
        }

        composable("chat_ai") {
            ChatAIScreen(
                navController = navController,
                aiViewModel = aiViewModel,
                savingsViewModel = savingsViewModel
            )
        }

        composable(
            route = "categories?transactionType={transactionType}&returnTo={returnTo}",
            arguments = listOf(
                navArgument("transactionType") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
                navArgument("returnTo") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            )
        ) { entry ->
            val transactionType = entry.arguments?.getString("transactionType")
            val returnTo = entry.arguments?.getString("returnTo")
            CategorySelectionScreen(
                navController = navController,
                categoryViewModel = categoryViewModel,
                transactionType = transactionType,
                returnTo = returnTo,
                onCategorySelected = { category ->
                    // ✅ Lưu category đã chọn vào NavController để truyền về AddTransactionScreen
                    navController.previousBackStackEntry?.savedStateHandle?.set("selectedCategoryId", category.id)
                    navController.popBackStack()
                }
            )
        }

        composable("categories") {
            CategoryScreen(
                navController = navController,
                categoryViewModel = categoryViewModel
            )
        }

        composable(
            route = "add_category?type={type}&parentId={parentId}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "expense"
                    nullable = false
                },
                navArgument("parentId") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            )
        ) { entry ->
            val typeArg = entry.arguments?.getString("type") ?: "expense"
            val parentIdArg = entry.arguments?.getString("parentId")

            AddCategoryScreen(
                navController = navController,
                viewModel = categoryViewModel,
                initialType = typeArg,
                initialParentCategoryId = parentIdArg
            )
        }

        // 🔹 BUDGET ROUTES
        composable("budgets") {
            BudgetScreen(
                navController = navController,
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel
            )
        }

        composable("add_budget") {
            AddBudgetScreen(
                navController = navController,
                onBack = { navController.popBackStack() },
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel
            )
        }

        composable(
            route = "edit_budget/{budgetId}",
            arguments = listOf(
                navArgument("budgetId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val budgetId = entry.arguments?.getString("budgetId")
            val budgets by budgetViewModel.budgets.collectAsState()
            val existingBudget = remember(budgetId, budgets) {
                budgets.find { it.id == budgetId }
            }

            AddBudgetScreen(
                navController = navController,
                onBack = { navController.popBackStack() },
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                existingBudget = existingBudget
            )
        }

        composable("calendar") {
            val calendarTransactions by transactionViewModel.transactions.collectAsState()
            val calendarCategories by categoryViewModel.categories.collectAsState()

            CalendarScreen(
                navController = navController,
                transactions = calendarTransactions,
                categories = calendarCategories
            )
        }

        composable("extensions") {
            ExtensionsScreen(navController = navController)
        }

        // 🔹 Invoice Scanner (quét hóa đơn -> AddTransaction)
        composable("invoice_scanner") {
            InvoiceScannerScreen(
                navController = navController
            )
        }

        composable("help") {
            HelpScreen(navController = navController)
        }

        composable("recurring_expenses") {
            RecurringExpenseScreen(
                navController = navController,
                recurringExpenseViewModel = recurringExpenseViewModel
            )
        }

        composable("add_recurring_expense") {
            AddRecurringExpenseScreen(
                navController = navController,
                onBack = { navController.popBackStack() },
                recurringExpenseViewModel = recurringExpenseViewModel,
                categoryViewModel = categoryViewModel
            )
        }

        composable(
            route = "edit_recurring_expense/{expenseId}",
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val expenseId = entry.arguments?.getString("expenseId")
            val existingExpense = remember(expenseId, recurringExpenses) {
                recurringExpenses.find { it.id == expenseId }
            }

            AddRecurringExpenseScreen(
                navController = navController,
                onBack = { navController.popBackStack() },
                recurringExpenseViewModel = recurringExpenseViewModel,
                categoryViewModel = categoryViewModel,
                existingExpense = existingExpense
            )
        }

        // 🔹 SAVINGS GOALS ROUTES - CẬP NHẬT
        composable("savings_goals") {
            SavingsGoalsScreen(
                navController = navController,
                savingsViewModel = savingsViewModel
            )
        }

        composable("add_savings_goal/{goalId}") { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
            AddSavingsGoalScreen(
                navController = navController,
                goalId = goalId,
                savingsViewModel = savingsViewModel
            )
        }

        composable("add_savings_goal") {
            AddSavingsGoalScreen(
                navController = navController,
                goalId = "",
                savingsViewModel = savingsViewModel
            )
        }
    }
}

// LanguageSettingsScreen implementation - thêm vào cuối file hoặc tạo file riêng
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    navController: NavHostController,
    languageViewModel: LanguageViewModel
) {
    // TODO: Implement LanguageSettingsScreen
    // For now, we'll create a simple placeholder
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("Language Settings") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                "Language Settings Screen",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            androidx.compose.material3.Text(
                "This screen will allow users to select their preferred language",
                modifier = androidx.compose.ui.Modifier.padding(top = 16.dp)
            )
        }
    }
}