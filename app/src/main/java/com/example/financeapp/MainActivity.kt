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
import com.example.financeapp.data.models.isOverBudget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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

            // ĐỢI 3 GIÂY trước khi schedule worker để đảm bảo app ổn định
            lifecycleScope.launch {
                delay(3000)
                startAIBackgroundMonitoring()
            }
        } else {
            // Permission denied
            println("Notification permission denied")
            // Vẫn thử schedule worker nhưng có thể không gửi được notification
            lifecycleScope.launch {
                delay(3000)
                tryScheduleWorkerWithoutPermission()
            }
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

                    // ĐỢI THÊM 2 GIÂY để các ViewModel load xong dữ liệu
                    delay(2000)

                    isLoading = false

                    // Kiểm tra và khởi động AI Worker sau khi app đã load xong
                    checkAndStartAIWorker()
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
            println("Đang khởi tạo hệ thống notification...")

            // 1. Create notification channel (required for Android 8.0+)
            NotificationHelper.createChannel(this)
            println("Đã tạo notification channel")

            // 2. Request notification permission (Android 13+)
            requestNotificationPermission()

        } catch (e: Exception) {
            println("Lỗi khi khởi tạo hệ thống notification: ${e.message}")
            e.printStackTrace()
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
                println("📱 Android 13+ - Yêu cầu notification permission...")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Permission already granted, schedule worker
                println("Notification permission đã được cấp từ trước")
                // Đợi một chút trước khi schedule
                lifecycleScope.launch {
                    delay(2000)
                    startAIBackgroundMonitoring()
                }
            }
        } else {
            // For Android < 13, no runtime permission needed
            println("📱 Android < 13 - Không cần runtime permission")
            // Đợi một chút trước khi schedule
            lifecycleScope.launch {
                delay(2000)
                startAIBackgroundMonitoring()
            }
        }
    }

    /**
     * Kiểm tra và khởi động AI Worker
     */
    private fun checkAndStartAIWorker() {
        lifecycleScope.launch {
            try {
                println("Đang kiểm tra và khởi động AI Worker...")

                // Kiểm tra setting có cho phép background monitoring không
                val allowBackground = shouldAllowAIBackground()

                if (allowBackground) {
                    println("Cho phép AI background monitoring")
                    startAIBackgroundMonitoring()
                } else {
                    println("AI background monitoring bị tắt trong setting")
                }

            } catch (e: Exception) {
                println("Lỗi khi khởi động AI Worker: ${e.message}")
            }
        }
    }

    /**
     * Bắt đầu AI Background Monitoring
     */
    private fun startAIBackgroundMonitoring() {
        lifecycleScope.launch {
            try {
                println("Đang bắt đầu AI Background Monitoring...")

                // 1. Kiểm tra xem worker đã được schedule chưa
                val isAlreadyScheduled = AIButlerWorker.isScheduled(this@MainActivity)

                if (isAlreadyScheduled) {
                    println("AI Worker đã được lên lịch từ trước")
                    return@launch
                }

                // 2. Schedule worker
                val success = AIButlerWorker.schedule(this@MainActivity)

                if (success) {
                    println("Đã lên lịch AI Worker thành công")

                    // 3. Chạy kiểm tra ngay lập tức lần đầu
                    runInitialAICheck()

                    // 4. Lưu trạng thái
                    saveAIWorkerState(true)

                } else {
                    println("Không thể lên lịch AI Worker")
                }

            } catch (e: Exception) {
                println("Lỗi khi bắt đầu AI monitoring: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Chạy kiểm tra AI ngay lập tức (lần đầu)
     */
    private fun runInitialAICheck() {
        lifecycleScope.launch {
            try {
                println("Đang chạy kiểm tra AI lần đầu...")

                // Đợi để đảm bảo dữ liệu đã load
                delay(5000)

                // Kiểm tra các điều kiện chính
                checkInitialConditions()

                println("Đã hoàn thành kiểm tra AI lần đầu")

            } catch (e: Exception) {
                println("Lỗi khi chạy kiểm tra AI lần đầu: ${e.message}")
            }
        }
    }

    /**
     * Kiểm tra các điều kiện ban đầu
     */
    private suspend fun checkInitialConditions() {
        try {
            // 1. Kiểm tra budget vượt quá
            val exceededBudgets = budgetViewModel.budgets.value.filter { it.isActive && it.isOverBudget }
            println("Kiểm tra budget: ${exceededBudgets.size} budget vượt quá")

            if (exceededBudgets.isNotEmpty()) {
                // Gửi notification ngay lập tức
                sendImmediateBudgetNotification(exceededBudgets)
            }

            // 2. Kiểm tra budget sắp vượt (>80%)
            val warningBudgets = budgetViewModel.budgets.value.filter { budget ->
                budget.isActive &&
                        budget.amount > 0 &&
                        budget.spent / budget.amount >= 0.8 &&
                        budget.spent / budget.amount < 1.0
            }
            println("Kiểm tra budget: ${warningBudgets.size} budget sắp vượt (>80%)")

            if (warningBudgets.isNotEmpty()) {
                sendBudgetWarningNotification(warningBudgets)
            }

        } catch (e: Exception) {
            println("Lỗi khi kiểm tra điều kiện ban đầu: ${e.message}")
        }
    }

    /**
     * Gửi notification budget vượt quá ngay lập tức
     */
    private fun sendImmediateBudgetNotification(budgets: List<com.example.financeapp.data.models.Budget>) {
        lifecycleScope.launch {
            try {
                val categoryNames = budgets.mapNotNull { budget ->
                    categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                }.distinct().joinToString(", ")

                if (categoryNames.isNotEmpty()) {
                    val exceededAmount = budgets.first().spent - budgets.first().amount

                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        title = "VƯỢT NGÂN SÁCH!",
                        message = "Bạn đã vượt ngân sách cho: $categoryNames\n" +
                                "Vượt quá: ${formatCurrency(exceededAmount)}"
                    )

                    println("Đã gửi notification vượt ngân sách: $categoryNames")
                }

            } catch (e: Exception) {
                println("Lỗi khi gửi notification vượt ngân sách: ${e.message}")
            }
        }
    }

    /**
     * Gửi notification budget sắp vượt
     */
    private fun sendBudgetWarningNotification(budgets: List<com.example.financeapp.data.models.Budget>) {
        lifecycleScope.launch {
            try {
                val topBudget = budgets.maxByOrNull { it.spent / it.amount }
                topBudget?.let { budget ->
                    val categoryName = categoryViewModel.categories.value
                        .find { it.id == budget.categoryId }?.name ?: "Không xác định"

                    val percentage = (budget.spent / budget.amount * 100).toInt()

                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        title = "Ngân sách sắp vượt!",
                        message = "$categoryName đã dùng $percentage% ngân sách\n" +
                                "Đã chi: ${formatCurrency(budget.spent)} / ${formatCurrency(budget.amount)}"
                    )

                    println("Đã gửi notification budget sắp vượt: $categoryName ($percentage%)")
                }

            } catch (e: Exception) {
                println("Lỗi khi gửi notification budget sắp vượt: ${e.message}")
            }
        }
    }

    /**
     * Thử schedule worker không cần permission
     */
    private fun tryScheduleWorkerWithoutPermission() {
        lifecycleScope.launch {
            try {
                println("Đang thử schedule worker không cần permission...")
                val success = AIButlerWorker.schedule(this@MainActivity)

                if (success) {
                    println("Đã schedule worker (không có permission)")
                } else {
                    println("Không thể schedule worker (không có permission)")
                }

            } catch (e: Exception) {
                println("Lỗi khi schedule worker không permission: ${e.message}")
            }
        }
    }

    /**
     * Kiểm tra xem có nên cho phép AI background không
     */
    private fun shouldAllowAIBackground(): Boolean {
        return try {
            val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            prefs.getBoolean("allow_ai_background", true) // Mặc định là true
        } catch (e: Exception) {
            true // Mặc định cho phép
        }
    }

    /**
     * Lưu trạng thái AI Worker
     */
    private fun saveAIWorkerState(isEnabled: Boolean) {
        try {
            val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("ai_worker_enabled", isEnabled)
                .putLong("ai_worker_last_start", System.currentTimeMillis())
                .apply()

            println("Đã lưu trạng thái AI Worker: $isEnabled")
        } catch (e: Exception) {
            println("Lỗi khi lưu trạng thái AI Worker: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        println("📱 MainActivity onResume")

        // Kiểm tra lại AI Worker khi app quay lại foreground
        lifecycleScope.launch {
            delay(1000)
            checkAIWorkerStatus()
        }
    }

    /**
     * Kiểm tra trạng thái AI Worker
     */
    private fun checkAIWorkerStatus() {
        lifecycleScope.launch {
            try {
                val isScheduled = AIButlerWorker.isScheduled(this@MainActivity)
                println("Trạng thái AI Worker: ${if (isScheduled) "ĐANG CHẠY" else "KHÔNG CHẠY"}")

                if (!isScheduled && shouldAllowAIBackground()) {
                    println("AI Worker không chạy, đang khởi động lại...")
                    startAIBackgroundMonitoring()
                }

            } catch (e: Exception) {
                println("Lỗi khi kiểm tra trạng thái AI Worker: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        println("📱 MainActivity onDestroy")

        // KHÔNG cancel worker ở đây để nó tiếp tục chạy nền
        // Chỉ lưu lại thời gian destroy
        saveLastDestroyTime()
    }

    /**
     * Lưu thời gian destroy
     */
    private fun saveLastDestroyTime() {
        try {
            val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            prefs.edit()
                .putLong("last_destroy_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            println("Lỗi khi lưu thời gian destroy: ${e.message}")
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
     */
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Get day of week from date string
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
     */
    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = NumberFormat.getInstance(Locale.getDefault())
            "${formatter.format(amount)}đ"
        } catch (e: Exception) {
            "${amount.toInt()}đ"
        }
    }

    /**
     * Get saved language preference from SharedPreferences
     */
    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        return prefs.getString("language_code", "vi") ?: "vi"
    }
}