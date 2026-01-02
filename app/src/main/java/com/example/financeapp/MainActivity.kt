package com.example.financeapp

import android.Manifest
import android.content.Intent
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
import com.example.financeapp.navigation.NavGraph
import com.example.financeapp.screen.main.dashboard.UserSession
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.utils.work.AIButlerWorker
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.auth.AuthViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.savings.SavingsViewModel
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

    private val authViewModel: AuthViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val languageViewModel: LanguageViewModel by viewModels()
    private val aiViewModel: AIViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()
    private val categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel by viewModels()
    private val recurringExpenseViewModel: RecurringExpenseViewModel by viewModels()
    private val savingsViewModel: SavingsViewModel by viewModels()

    private lateinit var userPrefs: UserPreferencesDataStore
    private val notificationHelper by lazy { NotificationHelper }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            lifecycleScope.launch {
                delay(3000)
                startAIBackgroundMonitoring()
            }
        } else {
            lifecycleScope.launch {
                delay(3000)
                tryScheduleWorkerWithoutPermission()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        userPrefs = UserPreferencesDataStore(this)
        initializeNotificationSystem()

        transactionViewModel.connectToSavingsViewModel(savingsViewModel)

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

                    delay(2000)

                    processRecurringExpensesOnAppStart()
                    initializeSavingsViewModel()

                    isLoading = false
                    checkAndStartAIWorker()

                    delay(1000)
                    checkNotificationIntent(intent)
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
                                budgetViewModel = budgetViewModel,
                                savingsViewModel = savingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initializeSavingsViewModel() {
        lifecycleScope.launch {
            try {
                savingsViewModel.loadSavingsGoals()
                delay(1000)
                savingsViewModel.calculateAutoSavings()
            } catch (e: Exception) {
            }
        }
    }

    private fun initializeNotificationSystem() {
        try {
            NotificationHelper.createChannel(this)
            requestNotificationPermission()
            initializeFirebaseMessaging()
        } catch (e: Exception) {
        }
    }

    private fun initializeFirebaseMessaging() {
        try {
        } catch (e: Exception) {
        }
    }

    private fun checkNotificationIntent(intent: Intent?) {
        lifecycleScope.launch {
            try {
                if (intent?.hasExtra("from_notification") == true) {
                    val notificationType = intent.getStringExtra("notification_type")
                    val data = intent.getStringExtra("notification_data")

                    when (notificationType) {
                        "budget_alert" -> {
                        }
                        "savings_alert" -> {
                        }
                        "recurring_expense" -> {
                        }
                    }
                }

                intent?.extras?.let { extras ->
                    if (extras.containsKey("gcm.notification.title") || extras.containsKey("title")) {
                        val title = extras.getString("gcm.notification.title") ?: extras.getString("title") ?: ""
                        val body = extras.getString("gcm.notification.body") ?: extras.getString("body") ?: ""

                        if (title.isNotEmpty() && body.isNotEmpty()) {
                            notificationHelper.showNotification(
                                context = this@MainActivity,
                                title = title,
                                message = body
                            )
                        }
                    }
                }

            } catch (e: Exception) {
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                lifecycleScope.launch {
                    delay(2000)
                    startAIBackgroundMonitoring()
                }
            }
        } else {
            lifecycleScope.launch {
                delay(2000)
                startAIBackgroundMonitoring()
            }
        }
    }

    private fun processRecurringExpensesOnAppStart() {
        lifecycleScope.launch {
            try {
                delay(1000)

                recurringExpenseViewModel.processDueRecurringExpenses(
                    context = this@MainActivity,
                    onTransactionCreated = { expense ->
                        createAndAddTransactionFromRecurring(expense)
                    }
                )

            } catch (e: Exception) {
            }
        }
    }

    private fun createAndAddTransactionFromRecurring(expense: com.example.financeapp.data.models.RecurringExpense) {
        lifecycleScope.launch {
            try {
                transactionViewModel.addTransactionFromRecurringExpense(expense, budgetViewModel)
                sendRecurringExpenseNotification(expense)
            } catch (e: Exception) {
            }
        }
    }

    private fun processRecurringExpensesOnAppResume() {
        lifecycleScope.launch {
            try {
                recurringExpenseViewModel.processDueRecurringExpenses(
                    context = this@MainActivity,
                    onTransactionCreated = { expense ->
                        createAndAddTransactionFromRecurring(expense)
                    }
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun sendRecurringExpenseNotification(expense: com.example.financeapp.data.models.RecurringExpense) {
        lifecycleScope.launch {
            try {
                NotificationHelper.showNotification(
                    context = this@MainActivity,
                    title = "Đã tạo giao dịch định kỳ",
                    message = "${expense.title}: ${formatCurrency(expense.amount)}\n" +
                            "Ngày: ${getTodayDateForDisplay()}\n" +
                            "Tần suất: ${getFrequencyDisplayName(expense.getFrequencyEnum())}"
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun checkAndStartAIWorker() {
        lifecycleScope.launch {
            try {
                val allowBackground = shouldAllowAIBackground()

                if (allowBackground) {
                    startAIBackgroundMonitoring()
                }

            } catch (e: Exception) {
            }
        }
    }

    private fun startAIBackgroundMonitoring() {
        lifecycleScope.launch {
            try {
                val isAlreadyScheduled = AIButlerWorker.isScheduled(this@MainActivity)

                if (isAlreadyScheduled) {
                    return@launch
                }

                val success = AIButlerWorker.schedule(this@MainActivity)

                if (success) {
                    runInitialAICheck()
                    saveAIWorkerState(true)
                }

            } catch (e: Exception) {
            }
        }
    }

    private fun runInitialAICheck() {
        lifecycleScope.launch {
            try {
                delay(5000)
                checkInitialConditions()
                checkSavingsConditions()
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun checkInitialConditions() {
        try {
            val exceededBudgets = budgetViewModel.budgets.value.filter { it.isActive && it.isOverBudget }

            if (exceededBudgets.isNotEmpty()) {
                sendImmediateBudgetNotification(exceededBudgets)
            }

            val warningBudgets = budgetViewModel.budgets.value.filter { budget ->
                budget.isActive &&
                        budget.amount > 0 &&
                        budget.spentAmount / budget.amount >= 0.8 &&
                        budget.spentAmount / budget.amount < 1.0
            }

            if (warningBudgets.isNotEmpty()) {
                sendBudgetWarningNotification(warningBudgets)
            }

        } catch (e: Exception) {
        }
    }

    private suspend fun checkSavingsConditions() {
        try {
            val currentMonthIncome = transactionViewModel.getCurrentMonthIncome()
            val savingsGoals = savingsViewModel.savingsGoals.value

            if (currentMonthIncome > 0 && savingsGoals.isEmpty()) {
                sendSavingsSuggestionNotification(currentMonthIncome)
            }

            val upcomingGoals = savingsViewModel.getActiveGoals().filter { goal ->
                goal.deadline > 0 && goal.deadline - System.currentTimeMillis() < 30L * 24 * 60 * 60 * 1000
            }

            if (upcomingGoals.isNotEmpty()) {
                sendUpcomingGoalNotification(upcomingGoals)
            }

            val autoGoals = savingsViewModel.getActiveGoals().filter { it.autoCalculate }
            if (autoGoals.isNotEmpty() && currentMonthIncome > 0) {
                savingsViewModel.calculateAutoSavings()
            }

        } catch (e: Exception) {
        }
    }

    private fun sendSavingsSuggestionNotification(income: Long) {
        lifecycleScope.launch {
            try {
                NotificationHelper.showNotification(
                    context = this@MainActivity,
                    title = "Tạo mục tiêu tiết kiệm",
                    message = "Bạn có ${formatCurrency(income.toDouble())} thu nhập tháng này.\n" +
                            "Hãy tạo mục tiêu tiết kiệm đầu tiên!"
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun sendUpcomingGoalNotification(goals: List<com.example.financeapp.data.models.SavingsGoal>) {
        lifecycleScope.launch {
            try {
                goals.forEach { goal ->
                    val daysLeft = (goal.deadline - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                    val remainingAmount = goal.targetAmount - goal.currentAmount

                    if (remainingAmount > 0 && daysLeft > 0) {
                        NotificationHelper.showNotification(
                            context = this@MainActivity,
                            title = "${goal.name} sắp đến hạn",
                            message = "Còn $daysLeft ngày\n" +
                                    "Cần thêm ${formatCurrency(remainingAmount.toDouble())}\n" +
                                    "Tiến độ: ${goal.progress.toInt()}%"
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun sendImmediateBudgetNotification(budgets: List<com.example.financeapp.data.models.Budget>) {
        lifecycleScope.launch {
            try {
                val categoryNames = budgets.mapNotNull { budget ->
                    categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                }.distinct().joinToString(", ")

                if (categoryNames.isNotEmpty()) {
                    val exceededAmount = budgets.first().spentAmount - budgets.first().amount

                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        title = "VƯỢT NGÂN SÁCH!",
                        message = "Bạn đã vượt ngân sách cho: $categoryNames\n" +
                                "Vượt quá: ${formatCurrency(exceededAmount)}"
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun sendBudgetWarningNotification(budgets: List<com.example.financeapp.data.models.Budget>) {
        lifecycleScope.launch {
            try {
                val topBudget = budgets.maxByOrNull { it.spentAmount / it.amount }
                topBudget?.let { budget ->
                    val categoryName = categoryViewModel.categories.value
                        .find { it.id == budget.categoryId }?.name ?: "Không xác định"

                    val percentage = (budget.spentAmount / budget.amount * 100).toInt()

                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        title = "Ngân sách sắp vượt!",
                        message = "$categoryName đã dùng $percentage% ngân sách\n" +
                                "Đã chi: ${formatCurrency(budget.spentAmount)} / ${formatCurrency(budget.amount)}"
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun tryScheduleWorkerWithoutPermission() {
        lifecycleScope.launch {
            try {
                val success = AIButlerWorker.schedule(this@MainActivity)
            } catch (e: Exception) {
            }
        }
    }

    private fun shouldAllowAIBackground(): Boolean {
        return try {
            val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            prefs.getBoolean("allow_ai_background", true)
        } catch (e: Exception) {
            true
        }
    }

    private fun saveAIWorkerState(isEnabled: Boolean) {
        try {
            val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("ai_worker_enabled", isEnabled)
                .putLong("ai_worker_last_start", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            delay(1500)
            processRecurringExpensesOnAppResume()
            checkAIWorkerStatus()
            checkSavingsConditions()
            savingsViewModel.calculateAutoSavings()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkNotificationIntent(intent)
    }

    private fun checkAIWorkerStatus() {
        lifecycleScope.launch {
            try {
                val isScheduled = AIButlerWorker.isScheduled(this@MainActivity)

                if (!isScheduled && shouldAllowAIBackground()) {
                    startAIBackgroundMonitoring()
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        transactionViewModel.disconnectSavingsViewModel()
        saveLastDestroyTime()
    }

    private fun saveLastDestroyTime() {
        try {
            val prefs = getSharedPreferences("ai_settings", MODE_PRIVATE)
            prefs.edit()
                .putLong("last_destroy_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = NumberFormat.getInstance(Locale.getDefault())
            "${formatter.format(amount)}đ"
        } catch (e: Exception) {
            "${amount.toInt()}đ"
        }
    }

    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        return prefs.getString("language_code", "vi") ?: "vi"
    }

    private fun getTodayDateForDisplay(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getFrequencyDisplayName(frequency: com.example.financeapp.data.models.RecurringFrequency): String {
        return when (frequency) {
            com.example.financeapp.data.models.RecurringFrequency.DAILY -> "Hàng ngày"
            com.example.financeapp.data.models.RecurringFrequency.WEEKLY -> "Hàng tuần"
            com.example.financeapp.data.models.RecurringFrequency.MONTHLY -> "Hàng tháng"
            com.example.financeapp.data.models.RecurringFrequency.QUARTERLY -> "Hàng quý"
            com.example.financeapp.data.models.RecurringFrequency.YEARLY -> "Hàng năm"
        }
    }
}