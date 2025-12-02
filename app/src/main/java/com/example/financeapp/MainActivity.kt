package com.example.financeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.navigation.NavGraph
import com.example.financeapp.screen.UserSession
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.auth.AuthViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val languageViewModel: LanguageViewModel by viewModels()
    private val aiViewModel: AIViewModel by viewModels()

    private val budgetViewModel: BudgetViewModel by viewModels()
    private val categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel by viewModels()
    private val recurringExpenseViewModel: RecurringExpenseViewModel by viewModels()

    private lateinit var userPrefs: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        userPrefs = UserPreferencesDataStore(this)

        setContent {
            val navController = rememberNavController()

            var savedSession by remember { mutableStateOf<UserSession?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    val user = userPrefs.userFlow.first()
                    savedSession = user
                    val savedLanguage = getSavedLanguage()
                    languageViewModel.setLanguageFromCode(savedLanguage)
                    processRecurringExpenses()
                    isLoading = false
                }
            }

            FinanceAppTheme {
                CompositionLocalProvider(
                    LocalLanguageViewModel provides languageViewModel
                ) {
                    if (!isLoading) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavGraph(
                                navController = navController,
                                authViewModel = authViewModel,
                                transactionViewModel = transactionViewModel,
                                aiViewModel = aiViewModel,
                                activity = this@MainActivity,
                                languageViewModel = languageViewModel,
                                categoryViewModel = categoryViewModel,
                                recurringExpenseViewModel = recurringExpenseViewModel,
                                budgetViewModel = budgetViewModel
                            )
                        }
                    }
                }
            }
        }
    }
    private fun processRecurringExpenses() {
        lifecycleScope.launch {
            try {
                recurringExpenseViewModel.processDueRecurringExpenses(
                    onTransactionCreated = { expense ->
                        createTransactionFromRecurring(expense)
                    }
                )
                println("Đã kiểm tra và xử lý chi tiêu định kỳ")
            } catch (e: Exception) {
                println("Lỗi xử lý chi tiêu định kỳ: ${e.message}")
            }
        }
    }

    private fun createTransactionFromRecurring(expense: com.example.financeapp.model.RecurringExpense) {
        lifecycleScope.launch {
            try {
                val today = getTodayDate()
                val dayOfWeek = getDayOfWeek(today)

                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    title = expense.title,
                    date = today,
                    dayOfWeek = dayOfWeek,
                    category = expense.category,
                    categoryId = "",
                    amount = expense.amount,
                    isIncome = false,
                    group = "Chi tiêu định kỳ",
                    wallet = expense.wallet,
                    description = expense.description
                        ?: "Tự động từ chi tiêu định kỳ: ${expense.title}",
                    categoryIcon = expense.categoryIcon,
                    categoryColor = expense.categoryColor,
                    isAutoGenerated = true,
                    recurringSourceId = expense.id
                )

                transactionViewModel.addTransactionFromRecurring(
                    transaction = transaction,
                    budgetViewModel = budgetViewModel
                )
                println("Đã tạo và thêm transaction từ recurring: ${expense.title}")

            } catch (e: Exception) {
                println("Lỗi tạo transaction: ${e.message}")
            }
        }
    }
    
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
    private fun getDayOfWeek(date: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = sdf.parse(date)
            val calendar = Calendar.getInstance().apply { time = parsedDate!! }
            val dayNames = arrayOf("Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7")
            dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        } catch (e: Exception) {
            ""
        }
    }
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
    }
    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("language_code", "vi") ?: "vi"
    }
}