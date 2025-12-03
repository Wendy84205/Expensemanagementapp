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
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.example.financeapp.screen.settings.HelpScreen
import com.example.financeapp.components.ui.CategorySelectionScreen
import com.example.financeapp.screen.features.ai.ChatAIScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api


@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    aiViewModel: AIViewModel,
    activity: Activity,
    languageViewModel: LanguageViewModel,
    categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel,
    recurringExpenseViewModel: RecurringExpenseViewModel,
    budgetViewModel: BudgetViewModel
) {
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
                budgetViewModel = budgetViewModel
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
                categoryViewModel = categoryViewModel
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
                    navController.popBackStack()
                }
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
                onBack = { navController.popBackStack() },
                onSave = { transaction ->
                    // ✅ Sử dụng coroutine để đảm bảo transaction được lưu trước khi navigate
                    coroutineScope.launch {
                        if (existingTransaction == null) {
                            transactionViewModel.addTransaction(
                                transaction = transaction,
                                budgetViewModel = budgetViewModel
                            )
                            // ✅ Đợi đủ lâu để state được cập nhật, wallet balance được trừ, và UI refresh
                            delay(800)
                        } else {
                            transactionViewModel.updateTransaction(
                                updatedTransaction = transaction,
                                budgetViewModel = budgetViewModel
                            )
                            delay(800)
                        }
                        // ✅ Đảm bảo navigate sau khi mọi thứ đã được cập nhật
                        navController.popBackStack()
                    }
                },
                onDelete = existingTransaction?.let { transaction ->
                    {
                        transactionViewModel.deleteTransaction(
                            transactionId = transaction.id,
                            budgetViewModel = budgetViewModel
                        )
                        navController.popBackStack()
                    }
                }
            )
        }

        // 🔹 Thống kê
        composable("statistics") {
            StatisticsScreen(
                navController = navController,
                transactions = transactions,
                categoryViewModel = categoryViewModel
            )
        }

        // 🔹 Cài đặt - ĐÃ CẬP NHẬT VỚI notificationPrefs
        composable("settings") {
            SettingsScreen(
                navController = navController,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                }
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
                aiViewModel = aiViewModel
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

        composable("add_category") {
            AddCategoryScreen(
                navController = navController,
                viewModel = categoryViewModel
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
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                existingBudget = existingBudget
            )
        }

        composable("calendar") {
            CalendarScreen(
                navController = navController,
                transactions = transactions
            )
        }

        composable("extensions") {
            ExtensionsScreen(navController = navController)
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
                recurringExpenseViewModel = recurringExpenseViewModel,
                categoryViewModel = categoryViewModel,
                existingExpense = existingExpense
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
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
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