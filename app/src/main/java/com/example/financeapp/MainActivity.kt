package com.example.financeapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.components.theme.FinanceAppTheme
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.navigation.NavGraph
import com.example.financeapp.screen.main.dashboard.UserSession
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.utils.work.AIButlerWorker
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.auth.AuthViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.example.financeapp.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    // ViewModels
    private val authViewModel: AuthViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val languageViewModel: LanguageViewModel by viewModels()
    private val aiViewModel: AIViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()
    private val categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel by viewModels()
    private val recurringExpenseViewModel: RecurringExpenseViewModel by viewModels()

    // DataStore
    private lateinit var userPrefs: UserPreferencesDataStore

    // Permission launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            println("Notification permission granted")
            // Khởi động AI Butler Worker khi có permission
            AIButlerWorker.schedule(this)
        } else {
            // Permission denied
            println("Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Initialize DataStore
        userPrefs = UserPreferencesDataStore(this)

        // Initialize notification system
        initializeNotificationSystem()

        // Set Compose content
        setContent {
            val navController = rememberNavController()

            var savedSession by remember { mutableStateOf<UserSession?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            // Load user data and settings
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

    /**
     * Initialize the notification system
     */
    private fun initializeNotificationSystem() {
        try {
            // 1. Create notification channel (required for Android 8.0+)
            NotificationHelper.createChannel(this)

            // 2. Request notification permission (Android 13+)
            requestNotificationPermission()

            println("Notification system initialized")
        } catch (e: Exception) {
            println("Lỗi khi khởi tạo hệ thống notification: ${e.message}")
        }
    }

    /**
     * Request notification permission for Android 13+
     */
    private fun requestNotificationPermission() {
        // Only required for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Permission already granted, schedule worker
                println("Notification permission already granted")
                AIButlerWorker.schedule(this)
            }
        } else {
            // For Android < 13, no runtime permission needed
            println("Notification permission not required (Android < 13)")
            AIButlerWorker.schedule(this)
        }
    }

    /**
     * Process recurring expenses
     */
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

    /**
     * Create transaction from recurring expense
     */
    private fun createTransactionFromRecurring(expense: com.example.financeapp.data.models.RecurringExpense) {
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

    /**
     * Get today's date in yyyy-MM-dd format
     * @return Today's date as string
     */
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Get day of week from date string
     * @param date Date string in yyyy-MM-dd format
     * @return Day of week in Vietnamese
     */
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

    /**
     * Format currency in VND format
     * @param amount Amount to format
     * @return Formatted currency string
     */
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
    }

    /**
     * Get saved language preference from SharedPreferences
     * @return Language code (default: "vi")
     */
    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return prefs.getString("language_code", "vi") ?: "vi"
    }
}
