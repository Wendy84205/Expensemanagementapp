package com.example.financeapp.viewmodel.ai

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.BuildConfig
import com.example.financeapp.FinanceApp
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

// ==================== DATA CLASSES ====================

/**
 * Data class cho dự báo chi tiêu
 */
data class SpendingForecast(
    val estimatedSpending: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val confidenceLevel: Int,
    val recommendations: List<String>,
    val warning: String = ""
)

/**
 * Data class cho dự báo chi tiêu nâng cao
 */
data class AdvancedSpendingForecast(
    val estimatedSpending: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val confidenceLevel: Int,
    val recommendations: List<String>,
    val warning: String = "",
    val categoryForecasts: List<CategoryForecast> = emptyList(),
    val algorithmUsed: String = "linear_regression"
)

data class CategoryForecast(
    val category: String,
    val forecast: Double,
    val confidence: Double,
    val trend: String // up, down, stable
)

/**
 * Data class cho phân tích chi tiêu chi tiết
 */
data class DetailedSpendingAnalysis(
    val totalSpending: Double,
    val averageSpending: Double,
    val transactionCount: Int,
    val categoryBreakdown: Map<String, CategoryAnalysis>,
    val dailySpending: Map<String, Double>,
    val largestTransaction: Transaction?,
    val unusualTransactions: List<Transaction>,
    val savingsOpportunity: Double,
    val hasUnusualSpending: Boolean
)

data class CategoryAnalysis(
    val total: Double,
    val count: Int,
    val average: Double,
    val percentage: Double
)

/**
 * Data class cho phân tích xu hướng nâng cao
 */
data class AdvancedTrendAnalysis(
    val mainTrends: List<String>,
    val changes: List<String>,
    val signals: List<String>,
    val actions: List<String>,
    val charts: Map<String, List<Double>> = emptyMap(),
    val predictions: List<String> = emptyList()
)

/**
 * Data class cho phân tích xu hướng
 */
data class TrendAnalysis(
    val mainTrends: List<String>,
    val changes: List<String>,
    val signals: List<String>,
    val actions: List<String>
)

/**
 * Data class cho đề xuất ngân sách cá nhân hóa
 */
data class PersonalizedBudgetRecommendations(
    val allocation: List<String>,
    val goals: List<String>,
    val advice: List<String>,
    val personalizedAllocation: Map<String, Double> = emptyMap(),
    val riskAssessment: String = "medium"
)

/**
 * Data class cho đề xuất ngân sách
 */
data class BudgetRecommendations(
    val allocation: List<String>,
    val goals: List<String>,
    val advice: List<String>
)

/**
 * Data class cho phân tích sức khỏe tài chính nâng cao
 */
data class AdvancedFinancialHealth(
    val score: Int,
    val level: String,
    val components: Map<String, Double>,
    val recommendations: List<String>,
    val improvementAreas: List<String>,
    val timeline: Map<String, String>
)

/**
 * Data class cho kết quả lệnh AI
 */
data class AICommandResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Data class cho phân tích chi tiêu
 */
data class SpendingAnalysis(
    val totalSpending: Double,
    val averageSpending: Double,
    val transactionCount: Int,
    val categoryBreakdown: List<Pair<String, Double>>,
    val period: String
)

/**
 * Data class cho điểm sức khỏe tài chính
 */
data class FinancialHealthScore(
    val score: Int,
    val savingsRate: Double,
    val expenseRatio: Double,
    val recommendations: List<String>
)

/**
 * Data class cho tin nhắn chat
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isProactive: Boolean = false
)

/**
 * Data class cho ngữ cảnh proactive
 */
data class ProactiveContext(
    val currentHour: Int,
    val currentDay: Int,
    val lastUserMessage: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val monthExpense: Double,
    val monthIncome: Double,
    val hasOverBudget: Boolean,
    val overBudgetCount: Int,
    val recentTransactionCount: Int,
    val userEngagementLevel: Int,
    val favoriteCategories: Set<String>,
    val mostUsedCommands: Map<String, Int>
)

/**
 * Data class cho profile hành vi người dùng
 */
data class UserBehaviorProfile(
    var engagementScore: Int = 0,
    var preferredCategories: MutableSet<String> = mutableSetOf(),
    var commonCommands: MutableMap<String, Int> = mutableMapOf(),
    var responseTimes: MutableList<Long> = mutableListOf(),
    var ignoredSuggestions: MutableSet<String> = mutableSetOf(),
    var acceptedSuggestions: MutableSet<String> = mutableSetOf(),
    var lastActiveTime: Long = System.currentTimeMillis(),
    var totalInteractions: Int = 0
)

/**
 * Data class cho dữ liệu real-time
 */
data class RealTimeData(
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val overBudgetCount: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis()
)

/**
 * Data class cho phát hiện bất thường
 */
data class AnomalyDetection(
    val anomalies: List<TransactionAnomaly>,
    val severity: String,
    val recommendations: List<String>,
    val confidence: Double
)

data class TransactionAnomaly(
    val transaction: Transaction,
    val anomalyType: String,
    val severity: String,
    val explanation: String,
    val suggestedAction: String
)

/**
 * Data class cho phân tích mẫu chi tiêu nâng cao
 */
data class AdvancedSpendingPattern(
    val monthlyAverage: Double,
    val weeklyPattern: Map<String, Double>, // Chi tiêu theo ngày trong tuần
    val seasonalTrend: String,
    val topCategories: List<Pair<String, Pair<Int, Double>>>,
    val consistencyScore: Int,
    val peakSpendingDays: List<LocalDate> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList()
)

data class RecurringTransaction(
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String, // daily, weekly, monthly
    val nextDate: LocalDate,
    val confidence: Double
)

/**
 * Data class cho kế hoạch tiết kiệm
 */
data class SavingsPlan(
    val goalName: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val monthlyRequired: Double,
    val timelineMonths: Int,
    val riskLevel: String,
    val investmentSuggestions: List<InvestmentSuggestion>
)

data class InvestmentSuggestion(
    val type: String,
    val name: String,
    val expectedReturn: Double,
    val risk: String,
    val minAmount: Double,
    val description: String
)

/**
 * Data class cho cảnh báo chi tiêu
 */
data class SpendingAlert(
    val type: String,
    val message: String,
    val severity: String, // info, warning, critical
    val category: String?,
    val amount: Double?,
    val threshold: Double?,
    val recommendation: String
)

/**
 * Data class cho Regression Forecast
 */
data class RegressionForecast(
    val forecast: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val rSquared: Double,
    val hasSeasonality: Boolean
)

/**
 * Enum cho trạng thái AI
 */
enum class AIState {
    IDLE, PROCESSING, ERROR, LEARNING
}

/**
 * Data class cho mẫu chi tiêu
 */
data class SpendingPattern(
    val monthlyAverages: Double,
    val seasonalTrend: String,
    val topCategories: List<Pair<String, Pair<Int, Double>>>,
    val consistencyScore: Int
)

// ==================== AI COMMANDS ====================

/**
 * Sealed class cho tất cả các lệnh AI (Mở rộng)
 */
sealed class AICommand {
    // Lệnh giao dịch
    data class AddTransaction(
        val title: String,
        val amount: Double,
        val category: String,
        val wallet: String,
        val isIncome: Boolean = false,
        val date: String? = null
    ) : AICommand()

    data class AnalyzeSpending(
        val period: String,
        val category: String? = null
    ) : AICommand()

    data class ListTransactions(
        val date: String? = null,
        val period: String = "today",
        val category: String? = null,
        val wallet: String? = null,
        val limit: Int = 20
    ) : AICommand()

    data class GetDailySummary(
        val date: String? = null
    ) : AICommand()

    data class ExportTransactions(
        val period: String = "month",
        val format: String = "text"
    ) : AICommand()

    data class ComparePeriods(
        val currentPeriod: String,
        val previousPeriod: String
    ) : AICommand()

    data class SearchTransactionsByKeyword(
        val keyword: String,
        val period: String? = null
    ) : AICommand()

    data class AnalyzeSpendingTrend(
        val period: String,
        val compareWithPrevious: Boolean = false
    ) : AICommand()

    // Lệnh ngân sách
    data class SetBudget(
        val category: String,
        val amount: Double,
        val period: String = "monthly"
    ) : AICommand()

    data class CreateBudget(
        val categoryId: String,
        val amount: Double,
        val periodType: String = "month",
        val note: String? = null
    ) : AICommand()

    data class UpdateBudget(
        val budgetId: String? = null,
        val categoryId: String? = null,
        val newAmount: Double? = null
    ) : AICommand()

    data class DeleteBudget(
        val budgetId: String? = null,
        val categoryId: String? = null
    ) : AICommand()

    data class ListBudgets(
        val periodType: String? = null,
        val categoryId: String? = null
    ) : AICommand()

    data class GetBudgetStatus(
        val categoryId: String? = null
    ) : AICommand()

    // Lệnh phân tích nâng cao (MỚI)
    data class GetSpendingForecast(
        val period: String = "month",
        val confidenceLevel: Double = 0.75
    ) : AICommand()

    data class GetBudgetRecommendations(
        val income: Double? = null,
        val riskTolerance: String = "medium"
    ) : AICommand()

    data class GetCategoryInsights(
        val category: String,
        val period: String = "month"
    ) : AICommand()

    data class GenerateReport(
        val period: String,
        val reportType: String = "summary"
    ) : AICommand()

    data class GetSpendingAlerts(
        val threshold: Double? = null,
        val severity: String = "all"
    ) : AICommand()

    data class DetectAnomalies(
        val sensitivity: Double = 2.0
    ) : AICommand()

    data class GetSavingsPlan(
        val goalId: String? = null,
        val monthlySavings: Double? = null
    ) : AICommand()

    // Lệnh cơ bản
    object GetFinancialHealthScore : AICommand()
    object ShowSummary : AICommand()
    object GetQuickTips : AICommand()
    object GetMonthlyOverview : AICommand()
    object GetWeeklyReport : AICommand()
    object GetSavingsAdvice : AICommand()
    object GetInvestmentTips : AICommand()
    object UnknownCommand : AICommand()
}

// ==================== NATURAL LANGUAGE PARSER ====================

/**
 * Class xử lý phân tích ngôn ngữ tự nhiên
 */
class NaturalLanguageParser(
    private val categoryViewModel: CategoryViewModel
) {
    private companion object {
        private const val TAG = "NaturalLanguageParser"
    }

    /**
     * Phân tích câu lệnh từ tin nhắn người dùng
     */
    fun parseCommand(message: String): AICommand {
        val lowerMessage = message.lowercase().trim()

        Log.d(TAG, "Phân tích câu lệnh: '$lowerMessage'")

        return when {
            isAddTransactionCommand(lowerMessage) -> {
                Log.d(TAG, "Nhận diện: ADD TRANSACTION")
                parseAddCommand(lowerMessage)
            }
            isListTransactionsCommand(lowerMessage) -> {
                Log.d(TAG, "Nhận diện: LIST TRANSACTIONS")
                parseListTransactions(lowerMessage)
            }
            containsAny(lowerMessage, listOf("phân tích", "analytics", "thống kê", "xem chi tiêu")) -> {
                Log.d(TAG, "Nhận diện: ANALYZE SPENDING")
                AICommand.AnalyzeSpending(period = extractPeriod(lowerMessage))
            }
            containsAny(lowerMessage, listOf("tổng quan", "summary", "tổng hợp", "tình hình")) -> {
                Log.d(TAG, "Nhận diện: SHOW SUMMARY")
                AICommand.ShowSummary
            }
            containsAny(lowerMessage, listOf("sức khỏe", "health", "điểm", "tình trạng")) -> {
                Log.d(TAG, "Nhận diện: FINANCIAL HEALTH")
                AICommand.GetFinancialHealthScore
            }
            containsAny(lowerMessage, listOf("mẹo", "tip", "advice", "khuyên", "gợi ý")) -> {
                Log.d(TAG, "Nhận diện: QUICK TIPS")
                AICommand.GetQuickTips
            }
            containsAny(lowerMessage, listOf("báo cáo", "report", "báo cáo tuần", "weekly")) -> {
                Log.d(TAG, "Nhận diện: WEEKLY REPORT")
                AICommand.GetWeeklyReport
            }
            containsAny(lowerMessage, listOf("tiết kiệm", "savings", "tiết kiệm tiền")) -> {
                Log.d(TAG, "Nhận diện: SAVINGS ADVICE")
                AICommand.GetSavingsAdvice
            }
            containsAny(lowerMessage, listOf("đầu tư", "investment", "đầu tư tiền")) -> {
                Log.d(TAG, "Nhận diện: INVESTMENT TIPS")
                AICommand.GetInvestmentTips
            }
            containsAny(lowerMessage, listOf("dự báo", "forecast", "dự đoán", "ước tính")) -> {
                Log.d(TAG, "Nhận diện: SPENDING FORECAST")
                AICommand.GetSpendingForecast(period = extractPeriod(lowerMessage))
            }
            containsAny(lowerMessage, listOf("cảnh báo", "alert", "warning", "thông báo")) -> {
                Log.d(TAG, "Nhận diện: SPENDING ALERTS")
                AICommand.GetSpendingAlerts()
            }
            containsAny(lowerMessage, listOf("bất thường", "anomaly", "phát hiện")) -> {
                Log.d(TAG, "Nhận diện: DETECT ANOMALIES")
                AICommand.DetectAnomalies()
            }
            else -> {
                Log.d(TAG, "Nhận diện: UNKNOWN COMMAND")
                AICommand.UnknownCommand
            }
        }
    }

    /**
     * Kiểm tra xem có phải lệnh thêm giao dịch không
     */
    private fun isAddTransactionCommand(message: String): Boolean {
        val addKeywords = listOf(
            "chi tiêu", "chi", "mua", "thanh toán", "trả", "tốn", "tiêu",
            "thu nhập", "thu", "nhận", "lương", "thưởng", "thêm", "add", "tạo"
        )

        val amountPattern = """(\d+([.,]\d+)?)\s*(k|triệu|tr|nghìn|nghin)?"""
        val hasAmount = Regex(amountPattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
        val hasKeyword = containsAny(message, addKeywords)

        Log.d(TAG, "isAddTransaction - hasKeyword: $hasKeyword, hasAmount: $hasAmount")

        return hasKeyword && hasAmount
    }

    /**
     * Kiểm tra xem có phải lệnh xem giao dịch không
     */
    private fun isListTransactionsCommand(message: String): Boolean {
        val listKeywords = listOf(
            "xem giao dịch", "xem chi tiêu", "danh sách", "liệt kê",
            "giao dịch", "lịch sử", "xem lại", "hiển thị"
        )

        return containsAny(message, listKeywords) &&
                !message.contains("thêm") &&
                !message.contains("tạo")
    }

    /**
     * Phân tích lệnh thêm giao dịch
     */
    private fun parseAddCommand(message: String): AICommand {
        val amount = extractAmount(message)
        val isIncome = isIncomeCommand(message)
        val category = extractCategory(message, isIncome)

        return AICommand.AddTransaction(
            title = extractTransactionTitle(message, isIncome),
            amount = amount,
            category = category,
            wallet = "",
            isIncome = isIncome
        )
    }

    /**
     * Phân tích lệnh xem giao dịch
     */
    private fun parseListTransactions(message: String): AICommand {
        val period = extractPeriod(message)
        val category = extractCategory(message, false)

        return AICommand.ListTransactions(
            period = period,
            category = category
        )
    }

    /**
     * Kiểm tra có phải thu nhập không
     */
    private fun isIncomeCommand(message: String): Boolean {
        val incomeKeywords = listOf(
            "thu nhập", "thu thập", "income", "lương", "tiền vào", "nhận được",
            "thưởng", "lãi", "tiền thêm", "cho thêm", "nạp tiền", "nhận", "được"
        )

        val expenseKeywords = listOf(
            "chi tiêu", "chi", "mua", "thanh toán", "trả", "tốn",
            "trừ tiền", "chi ra", "tiêu", "mất", "xuất", "tiêu dùng"
        )

        val lowerMessage = message.lowercase()

        if (incomeKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        if (expenseKeywords.any { lowerMessage.contains(it) }) {
            return false
        }

        return false
    }

    /**
     * Trích xuất tiêu đề giao dịch
     */
    private fun extractTransactionTitle(message: String, isIncome: Boolean): String {
        val lowerMessage = message.lowercase()

        if (isIncome) {
            return when {
                lowerMessage.contains("lương") -> "Tiền lương"
                lowerMessage.contains("thưởng") -> "Tiền thưởng"
                lowerMessage.contains("lãi") -> "Tiền lãi"
                lowerMessage.contains("thu nhập phụ") -> "Thu nhập phụ"
                lowerMessage.contains("nhận được") -> "Tiền nhận được"
                else -> "Thu nhập"
            }
        } else {
            return when {
                lowerMessage.contains("ăn uống") -> "Ăn uống"
                lowerMessage.contains("mua sắm") -> "Mua sắm"
                lowerMessage.contains("giải trí") -> "Giải trí"
                lowerMessage.contains("y tế") -> "Y tế"
                lowerMessage.contains("giáo dục") -> "Giáo dục"
                lowerMessage.contains("nhà ở") -> "Nhà ở"
                lowerMessage.contains("đi lại") -> "Đi lại"
                lowerMessage.contains("hóa đơn") -> "Hóa đơn"
                else -> "Chi tiêu"
            }
        }
    }

    /**
     * Trích xuất số tiền từ tin nhắn
     */
    private fun extractAmount(message: String): Double {
        val lowerMessage = message.lowercase()

        val patterns = listOf(
            Regex("""(\d+([.,]\d+)?)\s*(triệu|tr|million|m)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+([.,]\d+)?)\s*(nghìn|nghin|ngàn|ngan|k|thousand)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+([.,]\d+)?)\s*(đ|dong|vnd|vnđ)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+([.,]\d+)?)\s*(k|triệu|tr|nghìn)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(triệu|tr)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(nghìn|nghin|ngàn|ngan|k)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+([.,]\d+)?)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(lowerMessage)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", ".")
                val amount = amountStr.toDoubleOrNull() ?: 0.0

                val unit = match.groupValues.getOrNull(3)?.lowercase() ?: ""

                return when {
                    unit.contains("triệu") || unit.contains("tr") || unit.contains("million") || unit.contains("m") ->
                        amount * 1000000
                    unit.contains("nghìn") || unit.contains("nghin") || unit.contains("ngàn") ||
                            unit.contains("ngan") || unit.contains("k") || unit.contains("thousand") ->
                        amount * 1000
                    amount > 1000 && unit.isEmpty() -> amount
                    amount < 1000 && unit.isEmpty() -> amount * 1000
                    else -> amount
                }
            }
        }

        val simpleNumberPattern = Regex("""\b(\d+)\b""")
        val numbers = simpleNumberPattern.findAll(lowerMessage).toList()
        if (numbers.isNotEmpty()) {
            val number = numbers.last().value.toDoubleOrNull() ?: 0.0
            return if (number > 1000) number else number * 1000
        }

        return 0.0
    }

    /**
     * Trích xuất danh mục từ tin nhắn
     */
    private fun extractCategory(message: String, isIncome: Boolean = false): String {
        val availableCategories = if (isIncome) {
            categoryViewModel.getIncomeCategories()
        } else {
            categoryViewModel.getExpenseCategories()
        }

        val lowerMessage = message.lowercase()

        val matchedCategory = availableCategories.find { category ->
            val categoryNameLower = category.name.lowercase()
            lowerMessage.contains(categoryNameLower) ||
                    containsAnyKeyword(lowerMessage, getCategoryKeywords(category.name))
        }

        return matchedCategory?.name ?: getDefaultCategory(isIncome)
    }

    /**
     * Lấy từ khóa cho danh mục
     */
    private fun getCategoryKeywords(categoryName: String): List<String> {
        return when (categoryName.lowercase()) {
            "ăn uống" -> listOf("ăn", "uống", "cafe", "nhà hàng", "food", "restaurant", "cơm", "cháo", "phở", "bún", "buffet")
            "mua sắm" -> listOf("mua sắm", "shopping", "mua quần áo", "trung tâm thương mại", "mall", "mua đồ")
            "giải trí" -> listOf("xem phim", "game", "giải trí", "entertainment", "cafe", "cà phê", "karaoke", "pub", "bar")
            "y tế" -> listOf("bệnh viện", "phòng khám", "thuốc", "sức khỏe", "health", "hospital", "khám bệnh")
            "giáo dục" -> listOf("học", "trường", "sách", "giáo dục", "education", "khóa học", "đào tạo")
            "nhà ở" -> listOf("tiền nhà", "thuê nhà", "mortgage", "nhà cửa", "sửa nhà", "điện", "nước")
            "đi lại" -> listOf("xe", "xăng", "dầu", "taxi", "grab", "transport", "đi lại", "di chuyển", "bus", "máy bay")
            "lương" -> listOf("lương", "salary", "tiền lương", "lương tháng", "payroll")
            "thưởng" -> listOf("thưởng", "bonus", "tiền thưởng", "thưởng tết")
            else -> emptyList()
        }
    }

    /**
     * Kiểm tra có từ khóa nào không
     */
    private fun containsAnyKeyword(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    /**
     * Lấy danh mục mặc định
     */
    private fun getDefaultCategory(isIncome: Boolean): String {
        return if (isIncome) "Lương" else "Chi phí phát sinh"
    }

    /**
     * Trích xuất khoảng thời gian
     */
    private fun extractPeriod(message: String): String {
        return when {
            message.contains("tuần") || message.contains("week") -> "week"
            message.contains("tháng") || message.contains("month") -> "month"
            message.contains("năm") || message.contains("year") -> "year"
            message.contains("hôm qua") || message.contains("yesterday") -> "yesterday"
            else -> "today"
        }
    }

    /**
     * Kiểm tra có chứa bất kỳ từ nào trong danh sách không
     */
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}

// ==================== AI COMMAND EXECUTOR NÂNG CAO ====================

/**
 * AI Command Executor với tất cả tính năng nâng cao (Gộp cũ + mới)
 */
class AdvancedAICommandExecutor(
    private val transactionViewModel: TransactionViewModel,
    private val budgetViewModel: BudgetViewModel,
    private val categoryViewModel: CategoryViewModel,
    private val recurringExpenseViewModel: RecurringExpenseViewModel? = null
) {
    companion object {
        private const val TAG = "AdvancedAICommandExecutor"
        private val DATE_FORMATTER = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    /**
     * Thực thi lệnh AI - Tích hợp tất cả tính năng
     */
    suspend fun executeCommand(command: AICommand): AICommandResult {
        Log.d(TAG, "Thực thi lệnh nâng cao: ${command::class.simpleName}")

        return try {
            when (command) {
                // === CƠ BẢN ===
                is AICommand.AddTransaction -> addTransactionWithSmartFeatures(command)
                is AICommand.ListTransactions -> listTransactionsWithInsights(command)
                is AICommand.GetDailySummary -> getDailySummaryWithTrends(command)
                is AICommand.ExportTransactions -> exportTransactionsWithFormat(command)
                is AICommand.ComparePeriods -> comparePeriodsWithAnalysis(command)
                is AICommand.SearchTransactionsByKeyword -> searchTransactionsWithAI(command)

                // === NGÂN SÁCH ===
                is AICommand.CreateBudget -> createBudgetWithOptimization(command)
                is AICommand.UpdateBudget -> updateBudgetWithIntelligence(command)
                is AICommand.DeleteBudget -> deleteBudgetWithSafeguard(command)
                is AICommand.GetBudgetStatus -> getBudgetStatusWithPredictions(command)
                is AICommand.SetBudget -> createBudgetWithAutoAdjust(command)

                // === PHÂN TÍCH NÂNG CAO ===
                is AICommand.GetSpendingForecast -> getAdvancedSpendingForecast(command)
                is AICommand.GetBudgetRecommendations -> getPersonalizedBudgetRecommendations(command)
                is AICommand.AnalyzeSpending -> analyzeSpendingWithML(command)
                is AICommand.AnalyzeSpendingTrend -> analyzeSpendingTrendWithAI(command)
                is AICommand.GetCategoryInsights -> getCategoryInsights(command)
                is AICommand.GenerateReport -> generateReport(command)

                // === TÍN HIỆU & CẢNH BÁO ===
                is AICommand.GetSpendingAlerts -> getSmartSpendingAlerts(command)
                is AICommand.DetectAnomalies -> detectAnomaliesWithML(command)

                // === KẾ HOẠCH TÀI CHÍNH ===
                is AICommand.GetSavingsPlan -> getPersonalizedSavingsPlan(command)

                // === AI ADVICE ===
                is AICommand.GetFinancialHealthScore -> getAdvancedFinancialHealthScore()
                is AICommand.ShowSummary -> showSummaryWithInsights()
                is AICommand.GetQuickTips -> getContextualQuickTips()
                is AICommand.GetWeeklyReport -> getWeeklyReportWithPredictions()
                is AICommand.GetSavingsAdvice -> getPersonalizedSavingsAdvice()
                is AICommand.GetInvestmentTips -> getPersonalizedInvestmentTips()
                is AICommand.GetMonthlyOverview -> getMonthlyOverview()
                is AICommand.UnknownCommand -> AICommandResult(false, "Tôi chưa hiểu yêu cầu của bạn. Hãy thử:\n• 'Thêm chi tiêu 50k cho ăn uống'\n• 'Xem giao dịch hôm nay'\n• 'Phân tích chi tiêu tháng này'")

                else -> AICommandResult(false, "Lệnh chưa được hỗ trợ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi thực thi lệnh nâng cao: ${e.message}", e)
            AICommandResult(false, "Có lỗi xảy ra: ${e.message}")
        }
    }

    // ==================== CÁC PHƯƠNG THỨC CƠ BẢN NÂNG CAO ====================

    /**
     * Thêm giao dịch với tính năng thông minh
     */
    private suspend fun addTransactionWithSmartFeatures(command: AICommand.AddTransaction): AICommandResult {
        return try {
            Log.d(TAG, "Thêm giao dịch thông minh: $command")

            // Phân tích danh mục thông minh
            val smartCategory = analyzeAndSuggestCategory(command.title, command.amount, command.isIncome)

            // Kiểm tra ngân sách trước khi thêm
            val budgetCheck = checkBudgetBeforeAdding(smartCategory, command.amount)

            // Phát hiện bất thường
            val anomalyCheck = detectTransactionAnomaly(command.title, command.amount, smartCategory)

            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                title = command.title,
                amount = command.amount,
                category = smartCategory,
                wallet = command.wallet,
                isIncome = command.isIncome,
                date = command.date ?: getCurrentDate(),
                categoryId = "",
                dayOfWeek = getDayOfWeek(),
                group = if (command.isIncome) "Thu nhập" else "Chi tiêu",
                description = "Tạo bởi AI Assistant - ${budgetCheck.message}",
                categoryIcon = getCategoryIcon(smartCategory),
                categoryColor = getCategoryColor(smartCategory),
                isAutoGenerated = false,
                recurringSourceId = ""
            )

            transactionViewModel.addTransactionFromAI(
                transaction = transaction,
                budgetViewModel = budgetViewModel
            )

            delay(500)

            val message = buildTransactionAddedMessage(command, smartCategory, budgetCheck, anomalyCheck)

            AICommandResult(
                success = true,
                message = message,
                data = mapOf(
                    "transaction" to transaction,
                    "budget_check" to budgetCheck,
                    "anomaly_check" to anomalyCheck
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi thêm giao dịch thông minh: ${e.message}", e)
            AICommandResult(
                success = false,
                message = "Lỗi thêm giao dịch: ${e.message}"
            )
        }
    }

    /**
     * Xem danh sách giao dịch với insights
     */
    private suspend fun listTransactionsWithInsights(command: AICommand.ListTransactions): AICommandResult {
        return try {
            val transactions = getFilteredTransactions(command)

            if (transactions.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "Không có giao dịch nào trong khoảng thời gian này!",
                    data = emptyList<Transaction>()
                )
            }

            // Phân tích nâng cao
            val spendingAnalysis = performDetailedSpendingAnalysis(transactions, command.period)
            val anomalies = detectTransactionAnomalies(transactions)
            val insights = detectSpendingInsights(spendingAnalysis)

            val message = buildTransactionsListWithInsights(
                transactions,
                command.period,
                spendingAnalysis,
                anomalies,
                insights
            )

            AICommandResult(
                success = true,
                message = message,
                data = mapOf(
                    "transactions" to transactions,
                    "analysis" to spendingAnalysis,
                    "anomalies" to anomalies,
                    "insights" to insights
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xem giao dịch với insights: ${e.message}", e)
            AICommandResult(false, "Lỗi khi lấy danh sách giao dịch: ${e.message}")
        }
    }

    /**
     * Xem tổng quan với insights
     */
    private suspend fun showSummaryWithInsights(): AICommandResult {
        val totalBalance = transactionViewModel.getTotalIncome() - transactionViewModel.getTotalExpense()
        val totalIncome = transactionViewModel.getTotalIncome()
        val totalExpense = transactionViewModel.getTotalExpense()
        val savings = totalIncome - totalExpense

        // Phân tích chi tiết
        val spendingPattern = analyzeSpendingPattern()
        val budgetStatus = analyzeBudgetStatus()
        val financialHealth = calculateComprehensiveFinancialHealth()
        val predictions = generateShortTermPredictions()

        val message = """
            📊 TỔNG QUAN TÀI CHÍNH THÔNG MINH
            
            💰 TỔNG SỐ:
            • Số dư: ${formatCurrency(totalBalance)} ${getTrendIndicator(totalBalance)}
            • Tổng thu: ${formatCurrency(totalIncome)} ${getMonthlyTrend("income")}
            • Tổng chi: ${formatCurrency(totalExpense)} ${getMonthlyTrend("expense")}
            • Tiết kiệm: ${formatCurrency(savings)} (${if (totalIncome > 0) "%.1f".format(savings/totalIncome*100) else "0"}%)
            
            📈 PHÂN TÍCH:
            • Sức khỏe tài chính: ${financialHealth.level} (${financialHealth.score}/100)
            • Mẫu chi tiêu: ${spendingPattern.consistencyScore}/100 nhất quán
            • Ngân sách: ${budgetStatus.activeBudgets} hoạt động, ${budgetStatus.overBudget} vượt
            
            🔮 DỰ BÁO 30 NGÀY:
            ${predictions.joinToString("\n") { "• $it" }}
            
            ${if (totalBalance < 0) "⚠️ CẢNH BÁO: Chi tiêu đang vượt quá thu nhập!"
        else if (financialHealth.score < 50) "💡 GỢI Ý: ${financialHealth.recommendations.firstOrNull()}"
        else "✅ Tài chính đang ổn định!"}
        """.trimIndent()

        return AICommandResult(
            success = true,
            message = message,
            data = mapOf(
                "balance" to totalBalance,
                "income" to totalIncome,
                "expense" to totalExpense,
                "health_score" to financialHealth.score
            )
        )
    }

    /**
     * Lấy mẹo ngữ cảnh
     */
    private suspend fun getContextualQuickTips(): AICommandResult {
        val tips = listOf(
            "💡 Chi tiêu ít hơn 50% thu nhập cho nhu cầu thiết yếu",
            "💰 Tiết kiệm ít nhất 20% thu nhập mỗi tháng",
            "📱 Theo dõi chi tiêu hàng ngày để kiểm soát ngân sách",
            "🎯 Đặt mục tiêu tài chính ngắn hạn và dài hạn",
            "🛒 So sánh giá trước khi mua sắm lớn",
            "💳 Tránh nợ thẻ tín dụng lãi suất cao"
        )

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val contextualTip = when {
            currentHour in 6..9 -> "🌅 Buổi sáng: Kiểm tra ngân sách ngày mới!"
            currentHour in 17..20 -> "🌇 Cuối ngày: Ghi lại chi tiêu hôm nay!"
            else -> tips.random()
        }

        return AICommandResult(success = true, message = contextualTip)
    }

    /**
     * Lấy báo cáo tuần với dự đoán
     */
    private suspend fun getWeeklyReportWithPredictions(): AICommandResult {
        return try {
            Log.d(TAG, "Lấy báo cáo tuần")

            val weekTransactions = getTransactionsForPeriod("week")
            val lastWeekTransactions = getTransactionsForPeriod("previous_week")

            val weekIncome = weekTransactions.filter { it.isIncome }.sumOf { it.amount }
            val weekExpense = weekTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val weekBalance = weekIncome - weekExpense

            val lastWeekIncome = lastWeekTransactions.filter { it.isIncome }.sumOf { it.amount }
            val lastWeekExpense = lastWeekTransactions.filter { !it.isIncome }.sumOf { it.amount }

            val incomeChange = if (lastWeekIncome > 0) ((weekIncome - lastWeekIncome) / lastWeekIncome * 100) else 0.0
            val expenseChange = if (lastWeekExpense > 0) ((weekExpense - lastWeekExpense) / lastWeekExpense * 100) else 0.0

            val topCategories = weekTransactions
                .filter { !it.isIncome }
                .groupBy { it.category }
                .mapValues { (_, trans) -> trans.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            // Dự đoán tuần tới
            val weeklyForecast = calculateWeeklyForecast(weekTransactions, lastWeekTransactions)

            val message = """
            📅 BÁO CÁO TUẦN
            --------------------
            
            📊 TỔNG QUAN:
            • Thu nhập: ${formatCurrency(weekIncome)} ${getChangeSymbol(incomeChange)}${"%.1f".format(abs(incomeChange))}%
            • Chi tiêu: ${formatCurrency(weekExpense)} ${getChangeSymbol(expenseChange)}${"%.1f".format(abs(expenseChange))}%
            • Số dư: ${formatCurrency(weekBalance)}
            • Số giao dịch: ${weekTransactions.size}
            
            ${if (topCategories.isNotEmpty()) {
                "🏆 TOP CHI TIÊU:\n" + topCategories.joinToString("\n") {
                        (cat, amount) -> "• $cat: ${formatCurrency(amount)}"
                }
            } else ""}
            
            🔮 DỰ BÁO TUẦN TỚI:
            • Ước tính chi tiêu: ${formatCurrency(weeklyForecast.forecast)}
            • Khoảng dao động: ${formatCurrency(weeklyForecast.lowerBound)} - ${formatCurrency(weeklyForecast.upperBound)}
            • Độ tin cậy: ${"%.0f".format(weeklyForecast.rSquared * 100)}%
            
            💡 ĐÁNH GIÁ:
            ${getWeeklyAssessment(weekBalance, weekExpense, weekIncome)}
            
            🎯 MỤC TIÊU TUẦN TỚI:
            ${getWeeklyGoals(weekExpense, weekIncome)}
        """.trimIndent()

            AICommandResult(
                success = true,
                message = message,
                data = mapOf(
                    "weekly_income" to weekIncome,
                    "weekly_expense" to weekExpense,
                    "forecast" to weeklyForecast
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy báo cáo tuần: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy báo cáo tuần: ${e.message}")
        }
    }

    // ==================== PHÂN TÍCH NÂNG CAO ====================

    /**
     * Dự báo chi tiêu với ML
     */
    private suspend fun getAdvancedSpendingForecast(command: AICommand.GetSpendingForecast): AICommandResult {
        return try {
            Log.d(TAG, "Dự báo chi tiêu nâng cao: $command")

            // Lấy dữ liệu lịch sử thực tế
            val historicalData = getRealHistoricalSpendingData(command.period)

            if (historicalData.size < 3) {
                return AICommandResult(
                    success = false,
                    message = "Cần ít nhất 3 kỳ dữ liệu để dự báo chính xác"
                )
            }

            // Sử dụng Linear Regression
            val forecast = calculateLinearRegressionForecast(historicalData)

            // Dự báo theo danh mục
            val categoryForecasts = calculateCategoryBasedForecasts(command.period)

            // Tạo khuyến nghị cá nhân hóa
            val recommendations = generatePersonalizedForecastRecommendations(
                forecast.forecast,
                historicalData.last(),
                command.confidenceLevel
            )

            val forecastResult = AdvancedSpendingForecast(
                estimatedSpending = forecast.forecast,
                lowerBound = forecast.lowerBound,
                upperBound = forecast.upperBound,
                confidenceLevel = (command.confidenceLevel * 100).toInt(),
                recommendations = recommendations,
                warning = if (forecast.forecast > historicalData.average() * 1.3)
                    "⚠️ Dự báo chi tiêu cao hơn 30% so với trung bình" else "",
                categoryForecasts = categoryForecasts,
                algorithmUsed = "linear_regression_with_seasonality"
            )

            val message = buildForecastMessage(forecastResult, command.period)

            AICommandResult(
                success = true,
                message = message,
                data = forecastResult,
                metadata = mapOf(
                    "historical_data_points" to historicalData.size,
                    "r_squared" to forecast.rSquared,
                    "seasonality_detected" to forecast.hasSeasonality
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi dự báo chi tiêu nâng cao: ${e.message}", e)
            AICommandResult(false, "Lỗi dự báo chi tiêu: ${e.message}")
        }
    }

    /**
     * Phân tích chi tiêu với Machine Learning
     */
    private suspend fun analyzeSpendingWithML(command: AICommand.AnalyzeSpending): AICommandResult {
        return try {
            Log.d(TAG, "Phân tích chi tiêu với ML: $command")

            val transactions = when (command.period.lowercase()) {
                "week" -> getTransactionsForPeriod("week")
                "month" -> getTransactionsForPeriod("month")
                "year" -> getTransactionsForPeriod("year")
                else -> transactionViewModel.transactions.value
            }.filter { !it.isIncome }

            val filteredTransactions = if (command.category != null) {
                transactions.filter { it.category.equals(command.category, true) }
            } else {
                transactions
            }

            if (filteredTransactions.isEmpty()) {
                return AICommandResult(
                    success = false,
                    message = "Không có dữ liệu chi tiêu trong kỳ này"
                )
            }

            // Phân tích chi tiết với ML
            val detailedAnalysis = performDetailedSpendingAnalysis(filteredTransactions, command.period)

            // Phát hiện insights
            val insights = detectSpendingInsights(detailedAnalysis)

            // So sánh với kỳ trước
            val comparison = compareWithPreviousPeriod(command.period, detailedAnalysis.totalSpending)

            // Tìm mẫu chi tiêu
            val patterns = findSpendingPatterns(filteredTransactions)

            val message = buildDetailedAnalysisMessage(detailedAnalysis, insights, comparison, patterns, command.period)

            AICommandResult(
                success = true,
                message = message,
                data = detailedAnalysis,
                metadata = mapOf(
                    "insights_count" to insights.size,
                    "has_unusual_spending" to detailedAnalysis.hasUnusualSpending,
                    "savings_opportunity" to detailedAnalysis.savingsOpportunity,
                    "patterns_found" to patterns.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích chi tiêu với ML: ${e.message}", e)
            AICommandResult(false, "Lỗi phân tích chi tiêu: ${e.message}")
        }
    }

    /**
     * Phân tích xu hướng với AI
     */
    private suspend fun analyzeSpendingTrendWithAI(command: AICommand.AnalyzeSpendingTrend): AICommandResult {
        return try {
            Log.d(TAG, "Phân tích xu hướng với AI: $command")

            val currentData = getTransactionsForPeriod(command.period)
            val previousData = getPreviousPeriodData(command.period)

            // Phân tích xu hướng nâng cao
            val trendAnalysis = performAdvancedTrendAnalysis(currentData, previousData, command.compareWithPrevious)

            // Dự đoán tương lai
            val predictions = generateTrendPredictions(currentData)

            // Tạo biểu đồ dữ liệu
            val charts = generateTrendCharts(currentData, previousData)

            val message = buildAdvancedTrendAnalysisMessage(trendAnalysis, predictions, command.period)

            AICommandResult(
                success = true,
                message = message,
                data = AdvancedTrendAnalysis(
                    mainTrends = trendAnalysis.mainTrends,
                    changes = trendAnalysis.changes,
                    signals = trendAnalysis.signals,
                    actions = trendAnalysis.actions,
                    charts = charts,
                    predictions = predictions
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích xu hướng với AI: ${e.message}", e)
            AICommandResult(false, "Lỗi phân tích xu hướng: ${e.message}")
        }
    }

    /**
     * Lấy điểm sức khỏe tài chính nâng cao
     */
    private suspend fun getAdvancedFinancialHealthScore(): AICommandResult {
        return try {
            val financialHealth = calculateComprehensiveFinancialHealth()

            val message = buildFinancialHealthMessage(financialHealth)

            AICommandResult(
                success = true,
                message = message,
                data = financialHealth,
                metadata = mapOf(
                    "improvement_timeline" to financialHealth.timeline,
                    "priority_areas" to financialHealth.improvementAreas
                )
            )
        } catch (e: Exception) {
            AICommandResult(false, "Lỗi tính điểm sức khỏe: ${e.message}")
        }
    }

    /**
     * Lấy đề xuất ngân sách cá nhân hóa
     */
    private suspend fun getPersonalizedBudgetRecommendations(command: AICommand.GetBudgetRecommendations): AICommandResult {
        return try {
            Log.d(TAG, "Đề xuất ngân sách cá nhân hóa: $command")

            val income = command.income ?: transactionViewModel.getTotalIncome()
            val spendingPattern = analyzeAdvancedSpendingPattern()
            val riskTolerance = command.riskTolerance

            val recommendations = generatePersonalizedBudgetRecommendations(income, spendingPattern, riskTolerance)

            val message = buildPersonalizedBudgetMessage(recommendations, income)

            AICommandResult(
                success = true,
                message = message,
                data = recommendations,
                metadata = mapOf(
                    "risk_assessment" to recommendations.riskAssessment,
                    "personalized_categories" to recommendations.personalizedAllocation.keys.size
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi đề xuất ngân sách cá nhân hóa: ${e.message}", e)
            AICommandResult(false, "Lỗi đề xuất ngân sách: ${e.message}")
        }
    }

    /**
     * Lấy insights danh mục
     */
    private suspend fun getCategoryInsights(command: AICommand.GetCategoryInsights): AICommandResult {
        return try {
            val transactions = getTransactionsForPeriod(command.period)
                .filter { !it.isIncome && it.category.equals(command.category, ignoreCase = true) }

            if (transactions.isEmpty()) {
                return AICommandResult(false, "Không có dữ liệu cho danh mục '${command.category}'")
            }

            val analysis = performDetailedSpendingAnalysis(transactions, command.period)
            val insights = generateCategoryInsights(analysis, command.category)

            val message = """
                📊 INSIGHTS DANH MỤC: ${command.category.uppercase()}
                
                TỔNG QUAN ${command.period.uppercase()}:
                • Tổng chi tiêu: ${formatCurrency(analysis.totalSpending)}
                • Số giao dịch: ${analysis.transactionCount}
                • Chi tiêu trung bình: ${formatCurrency(analysis.averageSpending)}
                
                🔍 PHÂN TÍCH:
                ${insights.joinToString("\n") { "• $it" }}
                
                💰 SO VỚI CÁC DANH MỤC KHÁC:
                ${compareCategoryWithOthers(command.category, analysis.totalSpending, command.period)}
            """.trimIndent()

            AICommandResult(success = true, message = message, data = analysis)

        } catch (e: Exception) {
            AICommandResult(false, "Lỗi lấy insights danh mục: ${e.message}")
        }
    }

    /**
     * Tạo báo cáo
     */
    private suspend fun generateReport(command: AICommand.GenerateReport): AICommandResult {
        return try {
            val transactions = getTransactionsForPeriod(command.period)
            val analysis = performDetailedSpendingAnalysis(transactions, command.period)

            val report = when (command.reportType) {
                "summary" -> generateSummaryReport(analysis, command.period)
                "detailed" -> generateDetailedReport(analysis, command.period)
                "export" -> generateExportReport(transactions, command.period)
                else -> generateSummaryReport(analysis, command.period)
            }

            AICommandResult(success = true, message = report, data = analysis)

        } catch (e: Exception) {
            AICommandResult(false, "Lỗi tạo báo cáo: ${e.message}")
        }
    }

    // ==================== TÍN HIỆU & CẢNH BÁO NÂNG CAO ====================

    /**
     * Lấy cảnh báo chi tiêu thông minh
     */
    private suspend fun getSmartSpendingAlerts(command: AICommand.GetSpendingAlerts): AICommandResult {
        return try {
            Log.d(TAG, "Lấy cảnh báo chi tiêu thông minh: $command")

            val alerts = mutableListOf<SpendingAlert>()

            // Kiểm tra ngân sách
            alerts.addAll(checkBudgetAlerts(command.threshold))

            // Kiểm tra chi tiêu bất thường
            alerts.addAll(checkSpendingAnomalies())

            // Kiểm cash flow
            alerts.addAll(checkCashFlowAlerts())

            // Kiểm tra cơ hội tiết kiệm
            alerts.addAll(checkSavingsOpportunities())

            val message = buildAlertsMessage(alerts)

            AICommandResult(
                success = true,
                message = message,
                data = alerts,
                metadata = mapOf(
                    "total_alerts" to alerts.size,
                    "critical_alerts" to alerts.count { it.severity == "critical" },
                    "warning_alerts" to alerts.count { it.severity == "warning" }
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy cảnh báo: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy cảnh báo: ${e.message}")
        }
    }

    /**
     * Phát hiện bất thường với ML
     */
    private suspend fun detectAnomaliesWithML(command: AICommand.DetectAnomalies): AICommandResult {
        return try {
            Log.d(TAG, "Phát hiện bất thường với ML: $command")

            val transactions = transactionViewModel.transactions.value
            val anomalies = detectAdvancedAnomalies(transactions, command.sensitivity)

            val message = buildAnomaliesMessage(anomalies)

            AICommandResult(
                success = true,
                message = message,
                data = anomalies,
                metadata = mapOf(
                    "total_anomalies" to anomalies.anomalies.size,
                    "severity" to anomalies.severity,
                    "confidence" to anomalies.confidence
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phát hiện bất thường: ${e.message}", e)
            AICommandResult(false, "Lỗi phát hiện bất thường: ${e.message}")
        }
    }

    // ==================== KẾ HOẠCH TÀI CHÍNH NÂNG CAO ====================

    /**
     * Lấy kế hoạch tiết kiệm cá nhân hóa
     */
    private suspend fun getPersonalizedSavingsPlan(command: AICommand.GetSavingsPlan): AICommandResult {
        return try {
            Log.d(TAG, "Lấy kế hoạch tiết kiệm cá nhân hóa: $command")

            val monthlyIncome = transactionViewModel.getTotalIncome() / 12 // Ước tính thu nhập hàng tháng
            val monthlyExpense = transactionViewModel.getTotalExpense() / 12 // Ước tính chi tiêu hàng tháng

            val savingsCapacity = monthlyIncome - monthlyExpense
            val goalAmount = command.monthlySavings ?: (savingsCapacity * 0.2) // Mặc định 20% tiết kiệm

            val plan = createPersonalizedSavingsPlan(goalAmount, command.goalId)

            val message = buildSavingsPlanMessage(plan, monthlyIncome, monthlyExpense)

            AICommandResult(
                success = true,
                message = message,
                data = plan,
                metadata = mapOf(
                    "monthly_income" to monthlyIncome,
                    "monthly_expense" to monthlyExpense,
                    "savings_capacity" to savingsCapacity,
                    "target_achievable" to (savingsCapacity >= plan.monthlyRequired)
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy kế hoạch tiết kiệm: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy kế hoạch tiết kiệm: ${e.message}")
        }
    }

    /**
     * Lấy lời khuyên tiết kiệm cá nhân hóa
     */
    private suspend fun getPersonalizedSavingsAdvice(): AICommandResult {
        return try {
            Log.d(TAG, "Lấy lời khuyên tiết kiệm cá nhân hóa")

            val totalIncome = transactionViewModel.getTotalIncome()
            val totalExpense = transactionViewModel.getTotalExpense()
            val savings = totalIncome - totalExpense
            val savingsRate = if (totalIncome > 0) (savings / totalIncome * 100) else 0.0

            val spendingPattern = analyzeAdvancedSpendingPattern()
            val budgetStatus = analyzeBudgetStatus()

            val advice = generatePersonalizedSavingsAdvice(savingsRate, spendingPattern, budgetStatus)

            val message = buildPersonalizedSavingsAdviceMessage(advice, savingsRate, totalIncome, totalExpense)

            AICommandResult(success = true, message = message, data = advice)

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy lời khuyên tiết kiệm: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy lời khuyên tiết kiệm: ${e.message}")
        }
    }

    /**
     * Lấy mẹo đầu tư cá nhân hóa
     */
    private suspend fun getPersonalizedInvestmentTips(): AICommandResult {
        return try {
            Log.d(TAG, "Lấy mẹo đầu tư cá nhân hóa")

            val totalIncome = transactionViewModel.getTotalIncome()
            val totalExpense = transactionViewModel.getTotalExpense()
            val availableForInvestment = totalIncome - totalExpense

            val riskProfile = analyzeRiskProfile()
            val investmentKnowledge = estimateInvestmentKnowledge()

            val tips = generatePersonalizedInvestmentTips(availableForInvestment, riskProfile, investmentKnowledge)

            val message = buildInvestmentTipsMessage(tips, availableForInvestment, riskProfile)

            AICommandResult(success = true, message = message, data = tips)

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy mẹo đầu tư: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy mẹo đầu tư: ${e.message}")
        }
    }

    /**
     * Lấy tổng quan tháng
     */
    private suspend fun getMonthlyOverview(): AICommandResult {
        return try {
            val monthTransactions = getTransactionsForPeriod("month")
            val monthIncome = monthTransactions.filter { it.isIncome }.sumOf { it.amount }
            val monthExpense = monthTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val monthBalance = monthIncome - monthExpense

            val lastMonthTransactions = getTransactionsForPeriod("previous_month")
            val lastMonthIncome = lastMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
            val lastMonthExpense = lastMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }

            val incomeChange = if (lastMonthIncome > 0) ((monthIncome - lastMonthIncome) / lastMonthIncome * 100) else 0.0
            val expenseChange = if (lastMonthExpense > 0) ((monthExpense - lastMonthExpense) / lastMonthExpense * 100) else 0.0

            val message = """
                📅 TỔNG QUAN THÁNG
                
                📊 HIỆN TẠI:
                • Thu nhập: ${formatCurrency(monthIncome)} ${getChangeSymbol(incomeChange)}${"%.1f".format(abs(incomeChange))}%
                • Chi tiêu: ${formatCurrency(monthExpense)} ${getChangeSymbol(expenseChange)}${"%.1f".format(abs(expenseChange))}%
                • Số dư: ${formatCurrency(monthBalance)}
                • Số giao dịch: ${monthTransactions.size}
                
                🎯 MỤC TIÊU THÁNG:
                • Tiết kiệm: ${formatCurrency(monthIncome * 0.2)} (20% thu nhập)
                • Chi tiêu thiết yếu: < ${formatCurrency(monthIncome * 0.5)}
                
                ${if (monthExpense > monthIncome) "⚠️ CẢNH BÁO: Chi tiêu vượt thu nhập!"
            else if (monthBalance < monthIncome * 0.1) "💡 LƯU Ý: Tiết kiệm thấp, cần cải thiện"
            else "✅ Đang đi đúng hướng!"}
            """.trimIndent()

            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            AICommandResult(false, "Lỗi lấy tổng quan tháng: ${e.message}")
        }
    }

    // ==================== CÁC PHƯƠNG THỨC HỖ TRỢ NÂNG CAO ====================

    /**
     * Phân tích và đề xuất danh mục thông minh
     */
    private suspend fun analyzeAndSuggestCategory(
        title: String,
        amount: Double,
        isIncome: Boolean
    ): String {
        val lowerTitle = title.lowercase()

        // Phân tích từ khóa
        val categoryKeywords = mapOf(
            "ăn uống" to listOf("ăn", "uống", "cafe", "nhà hàng", "food", "restaurant", "cơm"),
            "mua sắm" to listOf("mua", "sắm", "shopping", "quần áo", "đồ", "tiệm"),
            "giải trí" to listOf("xem phim", "game", "giải trí", "karaoke", "pub", "bar"),
            "y tế" to listOf("bệnh viện", "thuốc", "sức khỏe", "khám"),
            "giáo dục" to listOf("học", "trường", "sách", "khóa học"),
            "nhà ở" to listOf("tiền nhà", "thuê", "điện", "nước", "internet"),
            "đi lại" to listOf("xe", "xăng", "taxi", "grab", "bus")
        )

        // Tìm danh mục phù hợp nhất
        var bestCategory = if (isIncome) "Lương" else "Chi phí phát sinh"
        var bestScore = 0

        for ((category, keywords) in categoryKeywords) {
            val score = keywords.count { lowerTitle.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestCategory = category
            }
        }

        // Phân tích dựa trên số tiền
        if (!isIncome) {
            when {
                amount > 1000000 -> {
                    if (!listOf("nhà ở", "giáo dục", "y tế").contains(bestCategory)) {
                        bestCategory = "Chi phí lớn"
                    }
                }
                amount < 50000 -> {
                    bestCategory = "Chi phí nhỏ"
                }
            }
        }

        return bestCategory
    }

    /**
     * Kiểm tra ngân sách trước khi thêm giao dịch
     */
    private suspend fun checkBudgetBeforeAdding(category: String, amount: Double): BudgetCheckResult {
        return try {
            val budgets = budgetViewModel.budgets.value
            val categoryId = categoryViewModel.findCategoryByName(category)?.id ?: ""

            val relevantBudget = budgets.find { it.categoryId == categoryId && it.isActive }

            return if (relevantBudget != null) {
                val newSpent = relevantBudget.spentAmount + amount
                val remaining = relevantBudget.amount - newSpent
                val percentage = (newSpent / relevantBudget.amount) * 100

                when {
                    percentage > 100 -> BudgetCheckResult(
                        isOverBudget = true,
                        message = "⚠️ Vượt ngân sách ${formatCurrency(amount - remaining)}",
                        severity = "critical",
                        remaining = 0.0
                    )
                    percentage > 90 -> BudgetCheckResult(
                        isOverBudget = false,
                        message = "⚠️ Sắp vượt ngân sách! Còn ${formatCurrency(remaining)}",
                        severity = "warning",
                        remaining = remaining
                    )
                    percentage > 80 -> BudgetCheckResult(
                        isOverBudget = false,
                        message = "📊 Ngân sách còn ${formatCurrency(remaining)} (${"%.0f".format(100 - percentage)}%)",
                        severity = "info",
                        remaining = remaining
                    )
                    else -> BudgetCheckResult(
                        isOverBudget = false,
                        message = "✅ Ngân sách ổn định",
                        severity = "success",
                        remaining = remaining
                    )
                }
            } else {
                BudgetCheckResult(
                    isOverBudget = false,
                    message = "ℹ️ Chưa có ngân sách cho danh mục này",
                    severity = "info",
                    remaining = 0.0
                )
            }
        } catch (e: Exception) {
            BudgetCheckResult(
                isOverBudget = false,
                message = "Không thể kiểm tra ngân sách",
                severity = "error",
                remaining = 0.0
            )
        }
    }

    data class BudgetCheckResult(
        val isOverBudget: Boolean,
        val message: String,
        val severity: String,
        val remaining: Double
    )

    /**
     * Phát hiện giao dịch bất thường
     */
    private suspend fun detectTransactionAnomaly(
        title: String,
        amount: Double,
        category: String
    ): AnomalyCheckResult {
        val recentTransactions = getTransactionsForPeriod("month")
            .filter { it.category == category && !it.isIncome }

        if (recentTransactions.size < 5) {
            return AnomalyCheckResult(
                isAnomaly = false,
                message = "Không đủ dữ liệu để phân tích",
                severity = "normal"  // THÊM DÒNG NÀY
            )
        }

        val amounts = recentTransactions.map { it.amount }
        val mean = amounts.average()
        val stdDev = sqrt(amounts.map { (it - mean).pow(2) }.average())

        val zScore = if (stdDev > 0) abs(amount - mean) / stdDev else 0.0

        return when {
            zScore > 3.0 -> AnomalyCheckResult(
                isAnomaly = true,
                message = "🚨 BẤT THƯỜNG: Giao dịch này lớn hơn ${"%.1f".format(zScore)} lần so với trung bình",
                severity = "critical",
                zScore = zScore
            )
            zScore > 2.0 -> AnomalyCheckResult(
                isAnomaly = true,
                message = "⚠️ KHÁC BIỆT: Giao dịch này lớn hơn ${"%.1f".format(zScore)} lần so với trung bình",
                severity = "warning",
                zScore = zScore
            )
            else -> AnomalyCheckResult(
                isAnomaly = false,
                message = "✅ Bình thường",
                severity = "normal",
                zScore = zScore
            )
        }
    }
    data class AnomalyCheckResult(
        val isAnomaly: Boolean,
        val message: String,
        val severity: String,
        val zScore: Double = 0.0
    )

    /**
     * Tính toán Linear Regression Forecast
     */
    private fun calculateLinearRegressionForecast(
        data: List<Double>
    ): RegressionForecast {
        val n = data.size
        val x = (1..n).map { it.toDouble() }
        val y = data

        // Tính mean
        val xMean = x.average()
        val yMean = y.average()

        // Tính slope (b1) và intercept (b0)
        var numerator = 0.0
        var denominator = 0.0

        for (i in 0 until n) {
            numerator += (x[i] - xMean) * (y[i] - yMean)
            denominator += (x[i] - xMean).pow(2)
        }

        val slope = numerator / denominator
        val intercept = yMean - slope * xMean

        // Dự báo cho kỳ tiếp theo
        val nextX = n + 1
        val forecast = intercept + slope * nextX

        // Tính R-squared
        var ssTotal = 0.0
        var ssResidual = 0.0

        for (i in 0 until n) {
            val predicted = intercept + slope * x[i]
            ssTotal += (y[i] - yMean).pow(2)
            ssResidual += (y[i] - predicted).pow(2)
        }

        val rSquared = 1 - (ssResidual / ssTotal)

        // Tính khoảng tin cậy
        val standardError = sqrt(ssResidual / (n - 2))
        val marginOfError = 1.96 * standardError * sqrt(1 + 1.0/n +
                (nextX - xMean).pow(2) / denominator)

        // Phát hiện tính mùa vụ
        val hasSeasonality = detectSeasonality(data)

        return RegressionForecast(
            forecast = forecast,
            lowerBound = forecast - marginOfError,
            upperBound = forecast + marginOfError,
            rSquared = rSquared,
            hasSeasonality = hasSeasonality
        )
    }

    /**
     * Phát hiện tính mùa vụ
     */
    private fun detectSeasonality(data: List<Double>): Boolean {
        if (data.size < 8) return false

        // Kiểm tra sự lặp lại
        val halfSize = data.size / 2
        val firstHalf = data.take(halfSize)
        val secondHalf = data.takeLast(halfSize)

        val mean1 = firstHalf.average()
        val mean2 = secondHalf.average()

        val variance1 = firstHalf.map { (it - mean1).pow(2) }.average()
        val variance2 = secondHalf.map { (it - mean2).pow(2) }.average()

        return abs(variance1 - variance2) / max(variance1, variance2) > 0.3
    }

    /**
     * Tính toán dự báo theo danh mục
     */
    private suspend fun calculateCategoryBasedForecasts(period: String): List<CategoryForecast> {
        val transactions = getTransactionsForPeriod(period)
        val categories = transactions
            .filter { !it.isIncome }
            .groupBy { it.category }

        return categories.map { (category, catTransactions) ->
            val amounts = catTransactions.map { it.amount }
            val forecast = if (amounts.size >= 3) {
                calculateLinearRegressionForecast(amounts).forecast
            } else {
                amounts.average()
            }

            // Phân tích xu hướng
            val trend = analyzeAmountTrend(amounts)

            CategoryForecast(
                category = category,
                forecast = forecast,
                confidence = min(0.95, amounts.size.toDouble() / 10),
                trend = trend
            )
        }.sortedByDescending { it.forecast }
    }

    /**
     * Phân tích xu hướng số tiền
     */
    private fun analyzeAmountTrend(data: List<Double>): String {
        if (data.size < 2) return "stable"

        val recent = data.takeLast(3).average()
        val previous = if (data.size >= 6) data.take(3).average() else data.first()

        return when {
            recent > previous * 1.2 -> "up"
            recent < previous * 0.8 -> "down"
            else -> "stable"
        }
    }

    /**
     * Tạo khuyến nghị dự báo cá nhân hóa
     */
    private fun generatePersonalizedForecastRecommendations(
        forecast: Double,
        lastPeriod: Double,
        confidenceLevel: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val change = ((forecast - lastPeriod) / lastPeriod * 100).toInt()

        when {
            change > 20 -> {
                recommendations.add("Dự báo chi tiêu tăng ${abs(change)}%, cần kiểm soát chặt chẽ hơn")
                recommendations.add("Xem xét cắt giảm chi phí không cần thiết")
            }
            change < -10 -> {
                recommendations.add("Chi tiêu dự kiến giảm ${abs(change)}%, đây là dấu hiệu tốt")
                recommendations.add("Có thể tăng tiết kiệm hoặc đầu tư thêm")
            }
            else -> {
                recommendations.add("Chi tiêu ổn định, duy trì thói quen hiện tại")
            }
        }

        if (confidenceLevel < 0.7) {
            recommendations.add("Độ tin cậy dự báo thấp, cần thêm dữ liệu để chính xác hơn")
        }

        recommendations.add("Kiểm tra lại ngân sách hàng tháng để đảm bảo phù hợp")

        return recommendations.take(5)
    }

    /**
     * Phân tích chi tiêu chi tiết
     */
    private fun performDetailedSpendingAnalysis(
        transactions: List<Transaction>,
        period: String
    ): DetailedSpendingAnalysis {
        val totalSpending = transactions.sumOf { it.amount }
        val averageSpending = if (transactions.isNotEmpty()) totalSpending / transactions.size else 0.0

        // Phân phối theo danh mục chi tiết
        val categoryBreakdown = transactions
            .groupBy { it.category }
            .mapValues { (_, trans) ->
                CategoryAnalysis(
                    total = trans.sumOf { it.amount },
                    count = trans.size,
                    average = trans.sumOf { it.amount } / trans.size,
                    percentage = trans.sumOf { it.amount } / totalSpending * 100
                )
            }

        // Phân phối theo thời gian
        val dailySpending = transactions
            .groupBy { it.date }
            .mapValues { (_, trans) -> trans.sumOf { it.amount } }
            .toSortedMap()

        // Tìm giao dịch lớn nhất
        val largestTransaction = transactions.maxByOrNull { it.amount }

        // Phát hiện giao dịch bất thường bằng Z-score
        val unusualTransactions = detectUnusualTransactions(transactions)

        // Tính toán cơ hội tiết kiệm
        val savingsOpportunity = calculateSavingsOpportunity(categoryBreakdown)

        return DetailedSpendingAnalysis(
            totalSpending = totalSpending,
            averageSpending = averageSpending,
            transactionCount = transactions.size,
            categoryBreakdown = categoryBreakdown,
            dailySpending = dailySpending,
            largestTransaction = largestTransaction,
            unusualTransactions = unusualTransactions,
            savingsOpportunity = savingsOpportunity,
            hasUnusualSpending = unusualTransactions.isNotEmpty()
        )
    }

    /**
     * Phát hiện giao dịch bất thường
     */
    private fun detectUnusualTransactions(transactions: List<Transaction>): List<Transaction> {
        if (transactions.size < 5) return emptyList()

        val amounts = transactions.map { it.amount }
        val mean = amounts.average()
        val stdDev = sqrt(amounts.map { (it - mean).pow(2) }.average())

        if (stdDev == 0.0) return emptyList()

        return transactions.filter { transaction ->
            val zScore = abs(transaction.amount - mean) / stdDev
            zScore > 2.0 // Ngưỡng Z-score cho bất thường
        }
    }

    /**
     * Tính cơ hội tiết kiệm
     */
    private fun calculateSavingsOpportunity(
        categoryAnalysis: Map<String, CategoryAnalysis>
    ): Double {
        // Giả sử có thể cắt giảm 20% chi tiêu ở các danh mục không thiết yếu
        val nonEssentialCategories = listOf("Giải trí", "Mua sắm", "Ăn ngoài", "Chi phí phát sinh")

        return categoryAnalysis
            .filter { (category, _) ->
                nonEssentialCategories.any { cat ->
                    category.contains(cat, ignoreCase = true)
                }
            }
            .values
            .sumOf { it.total } * 0.2 // 20% tiết kiệm tiềm năng
    }

    /**
     * Phát hiện insights chi tiêu
     */
    private fun detectSpendingInsights(analysis: DetailedSpendingAnalysis): List<String> {
        val insights = mutableListOf<String>()

        // Insight 1: Danh mục chi tiêu lớn nhất
        val topCategory = analysis.categoryBreakdown.maxByOrNull { it.value.total }
        topCategory?.let { (category, data) ->
            insights.add("${category} chiếm ${"%.1f".format(data.percentage)}% tổng chi tiêu")
        }

        // Insight 2: Chi tiêu bất thường
        if (analysis.hasUnusualSpending) {
            insights.add("Có ${analysis.unusualTransactions.size} giao dịch bất thường cần kiểm tra")
        }

        // Insight 3: Cơ hội tiết kiệm
        if (analysis.savingsOpportunity > 100000) {
            insights.add("Có thể tiết kiệm ${formatCurrency(analysis.savingsOpportunity)} bằng cách cắt giảm chi phí không cần thiết")
        }

        // Insight 4: Xu hướng hàng ngày
        if (analysis.dailySpending.isNotEmpty()) {
            val maxSpendingDay = analysis.dailySpending.maxByOrNull { it.value }
            maxSpendingDay?.let { (date, amount) ->
                insights.add("Ngày chi tiêu nhiều nhất: $date - ${formatCurrency(amount)}")
            }
        }

        return insights
    }

    /**
     * So sánh với kỳ trước
     */
    private suspend fun compareWithPreviousPeriod(period: String, currentSpending: Double): String {
        val previousData = getPreviousPeriodData(period)
        val previousSpending = previousData.filter { !it.isIncome }.sumOf { it.amount }

        return if (previousSpending > 0) {
            val change = ((currentSpending - previousSpending) / previousSpending * 100).toInt()
            "So với kỳ trước: ${if (change >= 0) "tăng" else "giảm"} ${abs(change)}%"
        } else {
            "Không có dữ liệu để so sánh"
        }
    }

    /**
     * Tìm mẫu chi tiêu
     */
    private fun findSpendingPatterns(transactions: List<Transaction>): List<String> {
        val patterns = mutableListOf<String>()

        // Phân tích theo ngày trong tuần
        val dayOfWeekPattern = transactions
            .groupBy { it.dayOfWeek }
            .mapValues { (_, trans) -> trans.sumOf { it.amount } }

        val topDay = dayOfWeekPattern.maxByOrNull { it.value }
        topDay?.let { (day, amount) ->
            patterns.add("Chi tiêu nhiều nhất vào $day: ${formatCurrency(amount)}")
        }

        // Phân tích theo khoảng thời gian
        val timePatterns = detectTimePatterns(transactions)
        patterns.addAll(timePatterns)

        return patterns
    }

    /**
     * Phát hiện mẫu thời gian
     */
    private fun detectTimePatterns(transactions: List<Transaction>): List<String> {
        val patterns = mutableListOf<String>()

        // Giả sử có field time trong transaction
        // Thực tế cần điều chỉnh theo data structure của bạn
        val morningTransactions = transactions // Lọc theo thời gian
        val eveningTransactions = transactions // Lọc theo thời gian

        if (morningTransactions.size > eveningTransactions.size * 1.5) {
            patterns.add("Bạn có xu hướng chi tiêu nhiều hơn vào buổi sáng")
        } else if (eveningTransactions.size > morningTransactions.size * 1.5) {
            patterns.add("Bạn có xu hướng chi tiêu nhiều hơn vào buổi tối")
        }

        return patterns
    }

    /**
     * Lấy dữ liệu lịch sử thực tế
     */
    private suspend fun getRealHistoricalSpendingData(period: String): List<Double> {
        val historicalPeriods = when (period) {
            "week" -> 8
            "month" -> 6
            "year" -> 4
            else -> 4
        }

        val historicalData = mutableListOf<Double>()

        for (i in 0 until historicalPeriods) {
            val periodData = when (period) {
                "week" -> getTransactionsForPreviousWeek(i)
                "month" -> getTransactionsForPreviousMonth(i)
                "year" -> getTransactionsForPreviousYear(i)
                else -> emptyList()
            }

            val spending = periodData
                .filter { !it.isIncome }
                .sumOf { it.amount }

            historicalData.add(spending)
        }

        return historicalData
    }

    /**
     * Tính toán sức khỏe tài chính toàn diện
     */
    private suspend fun calculateComprehensiveFinancialHealth(): AdvancedFinancialHealth {
        val income = transactionViewModel.getTotalIncome()
        val expense = transactionViewModel.getTotalExpense()
        val savings = income - expense

        // Tính các chỉ số thành phần
        val savingsRate = if (income > 0) savings / income * 100 else 0.0
        val expenseRatio = if (income > 0) expense / income * 100 else 0.0
        val debtRatio = calculateDebtRatio()
        val emergencyFundScore = calculateEmergencyFundScore()
        val investmentDiversityScore = calculateInvestmentDiversityScore()
        val budgetAdherenceScore = calculateBudgetAdherenceScore()

        // Tính điểm tổng hợp (trọng số)
        val components = mapOf(
            "savings_rate" to savingsRate,
            "expense_ratio" to expenseRatio,
            "debt_ratio" to debtRatio,
            "emergency_fund" to emergencyFundScore,
            "investment_diversity" to investmentDiversityScore,
            "budget_adherence" to budgetAdherenceScore
        )

        val weightedScore = (
                savingsRate.coerceIn(0.0, 30.0) * 0.25 +
                        (100 - expenseRatio).coerceIn(0.0, 25.0) * 0.25 +
                        (100 - debtRatio).coerceIn(0.0, 15.0) * 0.15 +
                        emergencyFundScore * 0.15 +
                        investmentDiversityScore * 0.10 +
                        budgetAdherenceScore * 0.10
                ).toInt()

        val level = when {
            weightedScore >= 85 -> "Xuất sắc 🏆"
            weightedScore >= 70 -> "Tốt 👍"
            weightedScore >= 55 -> "Trung bình ⚖️"
            weightedScore >= 40 -> "Cần cải thiện 📈"
            else -> "Nguy hiểm ⚠️"
        }

        // Tạo khuyến nghị
        val recommendations = generateHealthRecommendations(
            weightedScore, savingsRate, expenseRatio, debtRatio
        )

        // Xác định khu vực cần cải thiện
        val improvementAreas = identifyImprovementAreas(components)

        // Timeline cải thiện
        val timeline = createImprovementTimeline(weightedScore, improvementAreas)

        return AdvancedFinancialHealth(
            score = weightedScore,
            level = level,
            components = components,
            recommendations = recommendations,
            improvementAreas = improvementAreas,
            timeline = timeline
        )
    }

    /**
     * Tính tỷ lệ nợ
     */
    private fun calculateDebtRatio(): Double {
        // Giả sử tính toán dựa trên dữ liệu có sẵn
        // Trong thực tế, cần tích hợp với dữ liệu nợ
        return 0.0 // Mặc định 0%
    }

    /**
     * Tính điểm quỹ khẩn cấp
     */
    private suspend fun calculateEmergencyFundScore(): Double {
        val monthlyExpense = transactionViewModel.getTotalExpense() / 12
        // Giả sử có dữ liệu về quỹ khẩn cấp
        // Trong thực tế, cần lấy từ dữ liệu tài khoản tiết kiệm
        val emergencyFund = 0.0

        return when {
            emergencyFund >= monthlyExpense * 6 -> 100.0
            emergencyFund >= monthlyExpense * 3 -> 70.0
            emergencyFund >= monthlyExpense -> 40.0
            else -> 10.0
        }
    }

    /**
     * Tính điểm đa dạng đầu tư
     */
    private fun calculateInvestmentDiversityScore(): Double {
        // Giả sử tính toán dựa trên danh mục đầu tư
        // Trong thực tế, cần tích hợp với dữ liệu đầu tư
        return 0.0 // Mặc định 0%
    }

    /**
     * Tính điểm tuân thủ ngân sách
     */
    private suspend fun calculateBudgetAdherenceScore(): Double {
        val budgets = budgetViewModel.budgets.value
        val activeBudgets = budgets.filter { it.isActive }

        if (activeBudgets.isEmpty()) return 0.0

        val adherenceScores = activeBudgets.map { budget ->
            if (budget.amount == 0.0) 100.0
            else max(0.0, 100 - ((budget.spentAmount / budget.amount) * 100))
        }

        return adherenceScores.average()
    }

    /**
     * Tạo khuyến nghị sức khỏe
     */
    private fun generateHealthRecommendations(
        score: Int,
        savingsRate: Double,
        expenseRatio: Double,
        debtRatio: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            score >= 85 -> {
                recommendations.add("Xuất sắc! Tiếp tục duy trì thói quen tài chính tốt")
                recommendations.add("Xem xét đầu tư để tăng trưởng tài sản")
            }
            score >= 70 -> {
                recommendations.add("Khá tốt! Mục tiêu tiếp theo: tăng tỷ lệ tiết kiệm lên 25%")
                recommendations.add("Đa dạng hóa các kênh đầu tư")
            }
            score >= 55 -> {
                recommendations.add("Cần cải thiện một số khu vực")
                if (savingsRate < 10) recommendations.add("Ưu tiên tiết kiệm ít nhất 10% thu nhập")
                if (expenseRatio > 90) recommendations.add("Giảm chi tiêu không cần thiết")
            }
            else -> {
                recommendations.add("Cần hành động ngay lập tức")
                recommendations.add("Tạo ngân sách và theo dõi chi tiêu hàng ngày")
                recommendations.add("Ưu tiên trả nợ lãi suất cao")
                recommendations.add("Xây dựng quỹ khẩn cấp cơ bản")
            }
        }

        return recommendations.take(5)
    }

    /**
     * Xác định khu vực cần cải thiện
     */
    private fun identifyImprovementAreas(components: Map<String, Double>): List<String> {
        val areas = mutableListOf<String>()

        components.forEach { (component, value) ->
            when (component) {
                "savings_rate" -> if (value < 10) areas.add("Tỷ lệ tiết kiệm thấp")
                "expense_ratio" -> if (value > 90) areas.add("Tỷ lệ chi tiêu cao")
                "debt_ratio" -> if (value > 30) areas.add("Tỷ lệ nợ cao")
                "emergency_fund" -> if (value < 50) areas.add("Quỹ khẩn cấp yếu")
                "investment_diversity" -> if (value < 30) areas.add("Đa dạng hóa đầu tư kém")
                "budget_adherence" -> if (value < 60) areas.add("Tuân thủ ngân sách kém")
            }
        }

        return areas.take(3)
    }

    /**
     * Tạo timeline cải thiện
     */
    private fun createImprovementTimeline(score: Int, areas: List<String>): Map<String, String> {
        val timeline = mutableMapOf<String, String>()

        when {
            score < 40 -> {
                timeline["1 tuần"] = "Tạo ngân sách cơ bản"
                timeline["1 tháng"] = "Theo dõi chi tiêu hàng ngày"
                timeline["3 tháng"] = "Xây dựng quỹ khẩn cấp 1 tháng"
            }
            score < 55 -> {
                timeline["1 tháng"] = "Cải thiện 1-2 khu vực yếu nhất"
                timeline["3 tháng"] = "Đạt tỷ lệ tiết kiệm 10%"
                timeline["6 tháng"] = "Quỹ khẩn cấp 3 tháng"
            }
            else -> {
                timeline["3 tháng"] = "Duy trì điểm số hiện tại"
                timeline["6 tháng"] = "Cải thiện thêm 10 điểm"
                timeline["1 năm"] = "Đạt điểm sức khỏe > 80"
            }
        }

        return timeline
    }

    // ==================== CÁC PHƯƠNG THỨC HỖ TRỢ ====================

    /**
     * Xây dựng tin nhắn đã thêm giao dịch
     */
    private fun buildTransactionAddedMessage(
        command: AICommand.AddTransaction,
        smartCategory: String,
        budgetCheck: BudgetCheckResult,
        anomalyCheck: AnomalyCheckResult
    ): String {
        return """
            ✅ ĐÃ THÊM ${if (command.isIncome) "THU NHẬP" else "CHI TIÊU"} THÀNH CÔNG
            
            📝 Chi tiết:
            • Mô tả: ${command.title}
            • Số tiền: ${formatCurrency(command.amount)}
            • Danh mục: $smartCategory (AI đề xuất)
            • Loại: ${if (command.isIncome) "Thu nhập" else "Chi tiêu"}
            
            🔍 KIỂM TRA THÔNG MINH:
            • Ngân sách: ${budgetCheck.message}
            • Phân tích: ${anomalyCheck.message}
            
            ${if (budgetCheck.isOverBudget) "⚠️ LƯU Ý: Đã vượt ngân sách cho danh mục này!" else ""}
            ${if (anomalyCheck.isAnomaly) "🔍 LƯU Ý: Giao dịch này có dấu hiệu bất thường!" else ""}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn danh sách với insights
     */
    private fun buildTransactionsListWithInsights(
        transactions: List<Transaction>,
        period: String,
        analysis: DetailedSpendingAnalysis,
        anomalies: List<Transaction>,
        insights: List<String>
    ): String {
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val net = totalIncome - totalExpense

        val periodTitle = when (period) {
            "today" -> "HÔM NAY"
            "yesterday" -> "HÔM QUA"
            "week" -> "TUẦN NÀY"
            "month" -> "THÁNG NÀY"
            else -> "GIAO DỊCH"
        }

        val header = """
            📊 DANH SÁCH GIAO DỊCH $periodTitle
            
            💰 TỔNG SỐ:
            • Tổng thu: ${formatCurrency(totalIncome)}
            • Tổng chi: ${formatCurrency(totalExpense)}
            • Số dư: ${formatCurrency(net)} ${if (net >= 0) "✅" else "⚠️"}
            • Số giao dịch: ${transactions.size}
        """.trimIndent()

        val insightsSection = if (insights.isNotEmpty()) {
            "\n\n🔍 INSIGHTS PHÂN TÍCH:\n" + insights.joinToString("\n") { "• $it" }
        } else ""

        val anomaliesSection = if (anomalies.isNotEmpty()) {
            "\n\n🚨 GIAO DỊCH BẤT THƯỜNG:\n" + anomalies.take(3).joinToString("\n\n") { anomaly ->
                "• ${anomaly.title}\n  ${formatCurrency(anomaly.amount)} - ${anomaly.category}"
            }
        } else ""

        val topCategoriesSection = if (analysis.categoryBreakdown.isNotEmpty()) {
            val topCategories = analysis.categoryBreakdown.toList()
                .sortedByDescending { it.second.total }
                .take(3)

            "\n\n🏆 TOP DANH MỤC:\n" + topCategories.joinToString("\n") { (category, data) ->
                "• $category: ${formatCurrency(data.total)} (${data.count} giao dịch)"
            }
        } else ""

        val transactionsText = transactions.take(10).joinToString("\n\n") { transaction ->
            buildTransactionItemText(transaction)
        }

        val footer = if (transactions.size > 10) {
            "\n\n... và ${transactions.size - 10} giao dịch khác"
        } else ""

        return header + insightsSection + anomaliesSection + topCategoriesSection + "\n\n📋 CHI TIẾT:\n" + transactionsText + footer
    }

    /**
     * Xây dựng text cho từng giao dịch
     */
    private fun buildTransactionItemText(transaction: Transaction): String {
        val type = if (transaction.isIncome) "📥 THU" else "📤 CHI"
        val walletInfo = if (transaction.wallet.isNotBlank()) " • Ví: ${transaction.wallet}" else ""

        return """
            $type ${transaction.title}
            • Số tiền: ${formatCurrency(transaction.amount)}
            • Danh mục: ${transaction.category}
            • Ngày: ${transaction.date}$walletInfo
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn dự báo
     */
    private fun buildForecastMessage(
        forecast: AdvancedSpendingForecast,
        period: String
    ): String {
        val periodName = when (period) {
            "week" -> "tuần tới"
            "month" -> "tháng tới"
            "year" -> "năm tới"
            else -> "kỳ tới"
        }

        val categoryForecastsText = if (forecast.categoryForecasts.isNotEmpty()) {
            "\n\n📈 DỰ BÁO THEO DANH MỤC:\n" +
                    forecast.categoryForecasts.take(3).joinToString("\n") { category ->
                        "• ${category.category}: ${formatCurrency(category.forecast)} (${category.trend} ${"%.0f".format(category.confidence*100)}%)"
                    }
        } else ""

        return """
            🔮 DỰ BÁO CHI TIÊU $periodName
            
            📊 ƯỚC TÍNH:
            • Chi tiêu dự kiến: ${formatCurrency(forecast.estimatedSpending)}
            • Khoảng dao động: ${formatCurrency(forecast.lowerBound)} - ${formatCurrency(forecast.upperBound)}
            • Độ tin cậy: ${forecast.confidenceLevel}%
            • Thuật toán: ${forecast.algorithmUsed}
            $categoryForecastsText
            
            💡 KIẾN NGHỊ:
            ${forecast.recommendations.joinToString("\n") { "• $it" }}
            
            ${if (forecast.warning.isNotEmpty()) "⚠️ ${forecast.warning}" else ""}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn phân tích chi tiết
     */
    private fun buildDetailedAnalysisMessage(
        analysis: DetailedSpendingAnalysis,
        insights: List<String>,
        comparison: String,
        patterns: List<String>,
        period: String
    ): String {
        val periodName = when (period) {
            "week" -> "TUẦN"
            "month" -> "THÁNG"
            "year" -> "NĂM"
            else -> period.uppercase()
        }

        val patternsText = if (patterns.isNotEmpty()) {
            "\n\n🔄 MẪU CHI TIÊU:\n" + patterns.joinToString("\n") { "• $it" }
        } else ""

        return """
            📊 PHÂN TÍCH CHI TIÊU $periodName
            
            📈 TỔNG QUAN:
            • Tổng chi tiêu: ${formatCurrency(analysis.totalSpending)}
            • Chi tiêu trung bình: ${formatCurrency(analysis.averageSpending)}
            • Số giao dịch: ${analysis.transactionCount}
            • $comparison
            
            🏆 DANH MỤC HÀNG ĐẦU:
            ${analysis.categoryBreakdown.toList()
            .sortedByDescending { it.second.total }
            .take(3)
            .joinToString("\n") { (category, data) ->
                "• $category: ${formatCurrency(data.total)} (${"%.1f".format(data.percentage)}%)"
            }}
            
            🔍 INSIGHTS:
            ${insights.joinToString("\n") { "• $it" }}
            $patternsText
            
            💰 CƠ HỘI TIẾT KIỆM:
            • Có thể tiết kiệm: ${formatCurrency(analysis.savingsOpportunity)}
            • Bằng cách: Cắt giảm 20% chi phí không thiết yếu
            
            ${if (analysis.hasUnusualSpending) "🚨 CẢNH BÁO: Có ${analysis.unusualTransactions.size} giao dịch bất thường" else ""}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn phân tích xu hướng
     */
    private fun buildAdvancedTrendAnalysisMessage(
        analysis: TrendAnalysis,
        predictions: List<String>,
        period: String
    ): String {
        return """
            📈 PHÂN TÍCH XU HƯỚNG CHI TIÊU ${period.uppercase()}
            
            🔄 XU HƯỚNG CHÍNH:
            ${analysis.mainTrends.joinToString("\n") { "• $it" }}
            
            📉 BIẾN ĐỘNG:
            ${analysis.changes.joinToString("\n") { "• $it" }}
            
            ⚠️ DẤU HIỆU QUAN TRỌNG:
            ${analysis.signals.joinToString("\n") { "• $it" }}
            
            🔮 DỰ ĐOÁN:
            ${predictions.joinToString("\n") { "• $it" }}
            
            🎯 HÀNH ĐỘNG ĐỀ XUẤT:
            ${analysis.actions.joinToString("\n") { "• $it" }}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn sức khỏe tài chính
     */
    private fun buildFinancialHealthMessage(
        health: AdvancedFinancialHealth
    ): String {
        val componentsText = health.components.entries.joinToString("\n") { (component, value) ->
            when (component) {
                "savings_rate" -> "• Tỷ lệ tiết kiệm: ${"%.1f".format(value)}%"
                "expense_ratio" -> "• Tỷ lệ chi tiêu: ${"%.1f".format(value)}%"
                "debt_ratio" -> "• Tỷ lệ nợ: ${"%.1f".format(value)}%"
                "emergency_fund" -> "• Quỹ khẩn cấp: ${"%.0f".format(value)}/100"
                "investment_diversity" -> "• Đa dạng đầu tư: ${"%.0f".format(value)}/100"
                "budget_adherence" -> "• Tuân thủ ngân sách: ${"%.0f".format(value)}/100"
                else -> "• $component: ${"%.1f".format(value)}"
            }
        }

        val timelineText = if (health.timeline.isNotEmpty()) {
            "\n\n📅 LỘ TRÌNH CẢI THIỆN:\n" +
                    health.timeline.entries.joinToString("\n") { (time, action) ->
                        "• $time: $action"
                    }
        } else ""

        return """
            💪 ĐIỂM SỨC KHỎE TÀI CHÍNH: ${health.score}/100
            Mức độ: ${health.level}
            
            📊 CHỈ SỐ THÀNH PHẦN:
            $componentsText
            
            💡 KHUYẾN NGHỊ:
            ${health.recommendations.joinToString("\n") { "• $it" }}
            
            🎯 KHU VỰC CẦN CẢI THIỆN:
            ${health.improvementAreas.joinToString("\n") { "• $it" }}
            $timelineText
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn ngân sách cá nhân hóa
     */
    private fun buildPersonalizedBudgetMessage(
        recommendations: PersonalizedBudgetRecommendations,
        income: Double
    ): String {
        val personalizedAllocationText = if (recommendations.personalizedAllocation.isNotEmpty()) {
            "\n\n🎯 PHÂN BỔ CÁ NHÂN HÓA:\n" +
                    recommendations.personalizedAllocation.entries.joinToString("\n") { (category, amount) ->
                        "• $category: ${formatCurrency(amount)} (${"%.0f".format(amount/income*100)}%)"
                    }
        } else ""

        return """
            💰 GỢI Ý NGÂN SÁCH CÁ NHÂN HÓA
            
            📊 PHÂN BỔ LÝ TƯỞNG (dựa trên thu nhập ${formatCurrency(income)}):
            ${recommendations.allocation.joinToString("\n") { "• $it" }}
            $personalizedAllocationText
            
            🎯 MỤC TIÊU:
            ${recommendations.goals.joinToString("\n") { "• $it" }}
            
            💡 LỜI KHUYÊN:
            ${recommendations.advice.joinToString("\n") { "• $it" }}
            
            ⚠️ ĐÁNH GIÁ RỦI RO: ${recommendations.riskAssessment.uppercase()}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn cảnh báo
     */
    private fun buildAlertsMessage(alerts: List<SpendingAlert>): String {
        if (alerts.isEmpty()) {
            return "✅ KHÔNG CÓ CẢNH BÁO NÀO HIỆN TẠI"
        }

        val criticalAlerts = alerts.filter { it.severity == "critical" }
        val warningAlerts = alerts.filter { it.severity == "warning" }
        val infoAlerts = alerts.filter { it.severity == "info" }

        val criticalSection = if (criticalAlerts.isNotEmpty()) {
            "🔴 CẢNH BÁO NGUY HIỂM:\n" + criticalAlerts.joinToString("\n\n") { alert ->
                "• ${alert.message}\n  ${alert.recommendation}"
            } + "\n"
        } else ""

        val warningSection = if (warningAlerts.isNotEmpty()) {
            "🟡 CẢNH BÁO CẦN LƯU Ý:\n" + warningAlerts.joinToString("\n\n") { alert ->
                "• ${alert.message}\n  ${alert.recommendation}"
            } + "\n"
        } else ""

        val infoSection = if (infoAlerts.isNotEmpty()) {
            "🔵 THÔNG TIN:\n" + infoAlerts.joinToString("\n\n") { alert ->
                "• ${alert.message}"
            }
        } else ""

        return """
        ⚠️ DANH SÁCH CẢNH BÁO CHI TIÊU
        
        $criticalSection
        $warningSection
        $infoSection
        
        Tổng cộng: ${alerts.size} cảnh báo (${criticalAlerts.size} nguy hiểm, ${warningAlerts.size} cần lưu ý)
    """.trimIndent()
    }
    /**
     * Xây dựng tin nhắn bất thường
     */
    private fun buildAnomaliesMessage(anomalies: AnomalyDetection): String {
        if (anomalies.anomalies.isEmpty()) {
            return "✅ KHÔNG PHÁT HIỆN GIAO DỊCH BẤT THƯỜNG"
        }

        return """
            🔍 PHÁT HIỆN BẤT THƯỜNG
            
            Mức độ nghiêm trọng: ${anomalies.severity.uppercase()}
            Độ tin cậy: ${"%.0f".format(anomalies.confidence * 100)}%
            
            📋 DANH SÁCH BẤT THƯỜNG:
            ${anomalies.anomalies.take(5).joinToString("\n\n") { anomaly ->
            """
                • ${anomaly.transaction.title}
                  Số tiền: ${formatCurrency(anomaly.transaction.amount)}
                  Danh mục: ${anomaly.transaction.category}
                  Loại: ${anomaly.anomalyType}
                  Mức độ: ${anomaly.severity}
                  Giải thích: ${anomaly.explanation}
                  Hành động: ${anomaly.suggestedAction}
                """.trimIndent()
        }}
            
            ${if (anomalies.anomalies.size > 5) "... và ${anomalies.anomalies.size - 5} bất thường khác" else ""}
            
            💡 KIẾN NGHỊ:
            ${anomalies.recommendations.joinToString("\n") { "• $it" }}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn kế hoạch tiết kiệm
     */
    private fun buildSavingsPlanMessage(
        plan: SavingsPlan,
        monthlyIncome: Double,
        monthlyExpense: Double
    ): String {
        val progress = if (plan.targetAmount > 0) (plan.currentAmount / plan.targetAmount * 100).toInt() else 0

        return """
            🎯 KẾ HOẠCH TIẾT KIỆM: ${plan.goalName}
            
            📊 TỔNG QUAN:
            • Mục tiêu: ${formatCurrency(plan.targetAmount)}
            • Hiện tại: ${formatCurrency(plan.currentAmount)} ($progress%)
            • Cần thêm: ${formatCurrency(plan.targetAmount - plan.currentAmount)}
            
            💰 KẾ HOẠCH HÀNG THÁNG:
            • Cần tiết kiệm: ${formatCurrency(plan.monthlyRequired)}/tháng
            • Thời gian: ${plan.timelineMonths} tháng
            • Tỷ lệ so với thu nhập: ${"%.1f".format(plan.monthlyRequired/monthlyIncome*100)}%
            
            📈 SO SÁNH VỚI HIỆN TẠI:
            • Thu nhập/tháng: ${formatCurrency(monthlyIncome)}
            • Chi tiêu/tháng: ${formatCurrency(monthlyExpense)}
            • Tiết kiệm hiện tại: ${formatCurrency(monthlyIncome - monthlyExpense)}/tháng
            • ${if (plan.monthlyRequired <= monthlyIncome - monthlyExpense) "✅ CÓ THỂ ĐẠT ĐƯỢC" else "⚠️ CẦN ĐIỀU CHỈNH"}
            
            💡 ĐỀ XUẤT ĐẦU TƯ:
            ${plan.investmentSuggestions.take(3).joinToString("\n\n") { suggestion ->
            """
                • ${suggestion.name} (${suggestion.type})
                  Lợi nhuận kỳ vọng: ${"%.1f".format(suggestion.expectedReturn)}%/năm
                  Rủi ro: ${suggestion.risk}
                  Số tiền tối thiểu: ${formatCurrency(suggestion.minAmount)}
                  ${suggestion.description}
                """.trimIndent()
        }}
            
            🎯 MỨC ĐỘ RỦI RO: ${plan.riskLevel.uppercase()}
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn lời khuyên tiết kiệm
     */
    private fun buildPersonalizedSavingsAdviceMessage(
        advice: List<String>,
        savingsRate: Double,
        totalIncome: Double,
        totalExpense: Double
    ): String {
        return """
            💰 LỜI KHUYÊN TIẾT KIỆM CÁ NHÂN HÓA
            
            📊 TÌNH HÌNH HIỆN TẠI:
            • Tỷ lệ tiết kiệm: ${"%.1f".format(savingsRate)}%
            • Thu nhập: ${formatCurrency(totalIncome)}
            • Chi tiêu: ${formatCurrency(totalExpense)}
            • Tiết kiệm thực tế: ${formatCurrency(totalIncome - totalExpense)}
            
            💡 KHUYẾN NGHỊ CÁ NHÂN HÓA:
            ${advice.take(5).joinToString("\n") { "• $it" }}
            
            🎯 MỤC TIÊU ĐỀ XUẤT:
            • Tiết kiệm ít nhất ${formatCurrency(totalIncome * 0.1)}/tháng (10% thu nhập)
            • Xây dựng quỹ khẩn cấp 3-6 tháng chi phí sinh hoạt
            • Tăng dần tỷ lệ tiết kiệm lên 20%
            
            ⚡ MẸO TIẾT KIỆM THÔNG MINH:
            • Áp dụng quy tắc 24 giờ trước khi mua sắm lớn
            • Sử dụng ứng dụng so sánh giá
            • Tận dụng các chương trình giảm giá, khuyến mãi
            • Học cách sửa chữa thay vì mua mới
        """.trimIndent()
    }

    /**
     * Xây dựng tin nhắn mẹo đầu tư
     */
    private fun buildInvestmentTipsMessage(
        tips: List<String>,
        availableForInvestment: Double,
        riskProfile: String
    ): String {
        return """
            📈 MẸO ĐẦU TƯ THÔNG MINH
            
            💰 TÌNH HÌNH TÀI CHÍNH:
            • Tiền có thể đầu tư: ${formatCurrency(availableForInvestment)}
            • Hồ sơ rủi ro: ${riskProfile.uppercase()}
            
            🎯 KHUYẾN NGHỊ PHÙ HỢP:
            ${tips.take(6).joinToString("\n") { "• $it" }}
            
            ⚠️ NGUYÊN TẮC VÀNG:
            • Luôn có kế hoạch đầu tư rõ ràng
            • Kiểm soát cảm xúc khi thị trường biến động
            • Tái đầu tư lợi nhuận để tăng trưởng vốn
            • Cập nhật kiến thức tài chính thường xuyên
            
            🚨 LƯU Ý QUAN TRỌNG:
            • Đầu tư luôn đi kèm rủi ro
            • Hiệu quả trong quá khứ không đảm bảo tương lai
            • Tham khảo ý kiến chuyên gia trước khi quyết định lớn
            • Chỉ đầu tư số tiền bạn có thể chấp nhận mất
        """.trimIndent()
    }

    // ==================== CÁC PHƯƠNG THỨC GETTER ====================

    /**
     * Lấy giao dịch theo khoảng thời gian
     */
    private suspend fun getTransactionsForPeriod(period: String): List<Transaction> {
        val allTransactions = transactionViewModel.transactions.value

        return when (period) {
            "today" -> allTransactions.filter { it.date == getCurrentDate() }
            "yesterday" -> allTransactions.filter { it.date == getYesterdayDate() }
            "week" -> allTransactions.filter { isInCurrentWeek(it.date) }
            "month" -> allTransactions.filter { isInCurrentMonth(it.date) }
            "previous_week" -> allTransactions.filter { isInPreviousWeek(it.date) }
            "previous_month" -> allTransactions.filter { isInPreviousMonth(it.date) }
            else -> allTransactions
        }.sortedByDescending { parseDate(it.date) }
    }

    /**
     * Lấy danh sách giao dịch đã lọc
     */
    private suspend fun getFilteredTransactions(command: AICommand.ListTransactions): List<Transaction> {
        val allTransactions = transactionViewModel.transactions.value

        return allTransactions.filter { transaction ->
            val matchesDate = when (command.period) {
                "today" -> transaction.date == getCurrentDate()
                "yesterday" -> transaction.date == getYesterdayDate()
                "week" -> isInCurrentWeek(transaction.date)
                "month" -> isInCurrentMonth(transaction.date)
                else -> command.date == null || transaction.date == command.date
            }

            val matchesCategory = command.category == null ||
                    transaction.category.equals(command.category, ignoreCase = true)
            val matchesWallet = command.wallet == null ||
                    transaction.wallet.equals(command.wallet, ignoreCase = true)

            matchesDate && matchesCategory && matchesWallet
        }.sortedByDescending { parseDate(it.date) }
            .take(command.limit)
    }

    /**
     * Lấy giao dịch tuần trước
     */
    private suspend fun getTransactionsForPreviousWeek(weeksAgo: Int = 1): List<Transaction> {
        val allTransactions = transactionViewModel.transactions.value
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        val targetWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val targetYear = calendar.get(Calendar.YEAR)

        return allTransactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.WEEK_OF_YEAR) == targetWeek &&
                    transactionCalendar.get(Calendar.YEAR) == targetYear
        }
    }

    /**
     * Lấy giao dịch tháng trước
     */
    private suspend fun getTransactionsForPreviousMonth(monthsAgo: Int = 1): List<Transaction> {
        val allTransactions = transactionViewModel.transactions.value
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -monthsAgo)
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)

        return allTransactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.MONTH) == targetMonth &&
                    transactionCalendar.get(Calendar.YEAR) == targetYear
        }
    }

    /**
     * Lấy giao dịch năm trước
     */
    private suspend fun getTransactionsForPreviousYear(yearsAgo: Int = 1): List<Transaction> {
        val allTransactions = transactionViewModel.transactions.value
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -yearsAgo)
        val targetYear = calendar.get(Calendar.YEAR)

        return allTransactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.YEAR) == targetYear
        }
    }

    /**
     * Lấy dữ liệu kỳ trước
     */
    private suspend fun getPreviousPeriodData(period: String): List<Transaction> {
        return when (period) {
            "week" -> getTransactionsForPeriod("previous_week")
            "month" -> getTransactionsForPeriod("previous_month")
            else -> emptyList()
        }
    }

    // ==================== CÁC PHƯƠNG THỨC TIỆN ÍCH ====================

    /**
     * Định dạng tiền tệ
     */
    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }

    /**
     * Parse ngày từ string
     */
    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * Lấy ngày hiện tại
     */
    private fun getCurrentDate(): String = DATE_FORMATTER.format(Date())

    /**
     * Lấy ngày hôm qua
     */
    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return DATE_FORMATTER.format(calendar.time)
    }

    /**
     * Lấy thứ trong tuần
     */
    private fun getDayOfWeek(): String {
        val days = arrayOf("Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7")
        return days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
    }

    /**
     * Kiểm tra có trong tuần hiện tại không
     */
    private fun isInCurrentWeek(dateString: String): Boolean {
        return try {
            val transactionDate = parseDate(dateString)
            val calendar = Calendar.getInstance()
            val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
            val currentYear = calendar.get(Calendar.YEAR)

            calendar.time = transactionDate
            val transactionWeek = calendar.get(Calendar.WEEK_OF_YEAR)
            val transactionYear = calendar.get(Calendar.YEAR)

            currentWeek == transactionWeek && currentYear == transactionYear
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kiểm tra có trong tháng hiện tại không
     */
    private fun isInCurrentMonth(dateString: String): Boolean {
        return try {
            val transactionDate = parseDate(dateString)
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            calendar.time = transactionDate
            val transactionMonth = calendar.get(Calendar.MONTH)
            val transactionYear = calendar.get(Calendar.YEAR)

            currentMonth == transactionMonth && currentYear == transactionYear
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kiểm tra có trong năm hiện tại không
     */
    private fun isInCurrentYear(dateString: String): Boolean {
        return try {
            val transactionDate = parseDate(dateString)
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)

            calendar.time = transactionDate
            val transactionYear = calendar.get(Calendar.YEAR)

            currentYear == transactionYear
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kiểm tra có trong tuần trước không
     */
    private fun isInPreviousWeek(dateString: String): Boolean {
        return try {
            val transactionDate = parseDate(dateString)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            val previousWeek = calendar.get(Calendar.WEEK_OF_YEAR)
            val previousYear = calendar.get(Calendar.YEAR)

            calendar.time = transactionDate
            val transactionWeek = calendar.get(Calendar.WEEK_OF_YEAR)
            val transactionYear = calendar.get(Calendar.YEAR)

            previousWeek == transactionWeek && previousYear == transactionYear
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kiểm tra có trong tháng trước không
     */
    private fun isInPreviousMonth(dateString: String): Boolean {
        return try {
            val transactionDate = parseDate(dateString)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            val previousMonth = calendar.get(Calendar.MONTH)
            val previousYear = calendar.get(Calendar.YEAR)

            calendar.time = transactionDate
            val transactionMonth = calendar.get(Calendar.MONTH)
            val transactionYear = calendar.get(Calendar.YEAR)

            previousMonth == transactionMonth && previousYear == transactionYear
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lấy icon cho danh mục
     */
    private fun getCategoryIcon(category: String): String {
        return when (category.lowercase()) {
            "ăn uống", "food" -> "🍽️"
            "mua sắm", "shopping" -> "🛍️"
            "giải trí", "entertainment" -> "🎬"
            "y tế", "health" -> "🏥"
            "giáo dục", "education" -> "📚"
            "nhà ở", "housing" -> "🏠"
            "đi lại", "transport" -> "🚗"
            else -> "💰"
        }
    }

    /**
     * Lấy màu cho danh mục
     */
    private fun getCategoryColor(category: String): String {
        return when (category.lowercase()) {
            "ăn uống" -> "#FF6B6B"
            "mua sắm" -> "#4ECDC4"
            "giải trí" -> "#45B7D1"
            "y tế" -> "#96CEB4"
            "giáo dục" -> "#FFEAA7"
            "nhà ở" -> "#DDA0DD"
            "đi lại" -> "#98D8C8"
            else -> "#F7DC6F"
        }
    }

    /**
     * Tính ngày kết thúc ngân sách
     */
    private fun calculateBudgetEndDate(startDate: LocalDate, periodType: BudgetPeriodType): LocalDate {
        return when (periodType) {
            BudgetPeriodType.WEEK -> startDate.plusWeeks(1)
            BudgetPeriodType.MONTH -> startDate.plusMonths(1)
            BudgetPeriodType.QUARTER -> startDate.plusMonths(3)
            BudgetPeriodType.YEAR -> startDate.plusYears(1)
        }
    }

    /**
     * Lấy tên chu kỳ
     */
    private fun getPeriodName(periodType: BudgetPeriodType): String {
        return when (periodType) {
            BudgetPeriodType.WEEK -> "tuần"
            BudgetPeriodType.MONTH -> "tháng"
            BudgetPeriodType.QUARTER -> "quý"
            BudgetPeriodType.YEAR -> "năm"
        }
    }

    /**
     * Lấy chỉ báo xu hướng
     */
    private fun getTrendIndicator(value: Double): String {
        return when {
            value > 0 -> "📈"
            value < 0 -> "📉"
            else -> "➡️"
        }
    }

    /**
     * Lấy ký hiệu thay đổi
     */
    private fun getChangeSymbol(change: Double): String {
        return when {
            change > 0 -> "↑"
            change < 0 -> "↓"
            else -> "→"
        }
    }

    /**
     * Lấy xu hướng tháng
     */
    private suspend fun getMonthlyTrend(type: String): String {
        val currentMonth = getTransactionsForPeriod("month")
        val previousMonth = getTransactionsForPeriod("previous_month")

        val currentAmount = if (type == "income") {
            currentMonth.filter { it.isIncome }.sumOf { it.amount }
        } else {
            currentMonth.filter { !it.isIncome }.sumOf { it.amount }
        }

        val previousAmount = if (type == "income") {
            previousMonth.filter { it.isIncome }.sumOf { it.amount }
        } else {
            previousMonth.filter { !it.isIncome }.sumOf { it.amount }
        }

        return if (previousAmount > 0) {
            val change = ((currentAmount - previousAmount) / previousAmount * 100).toInt()
            "${if (change >= 0) "+" else ""}${change}%"
        } else {
            "N/A"
        }
    }

    // ==================== CÁC PHƯƠNG THỨC CHƯA TRIỂN KHAI (cần implement) ====================

    /**
     * Tổng quan hàng ngày với xu hướng
     */
    private suspend fun getDailySummaryWithTrends(command: AICommand.GetDailySummary): AICommandResult {
        return try {
            val targetDate = command.date ?: getCurrentDate()
            val allTransactions = transactionViewModel.transactions.value

            val dayTransactions = allTransactions.filter { it.date == targetDate }
            if (dayTransactions.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "Hôm nay ($targetDate) bạn chưa có giao dịch nào."
                )
            }

            val income = dayTransactions.filter { it.isIncome }.sumOf { it.amount }
            val expense = dayTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val balanceChange = income - expense

            val topCategories = dayTransactions
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)

            val message = buildString {
                appendLine("📅 TỔNG KẾT NGÀY $targetDate")
                appendLine()
                appendLine("• Thu: ${formatCurrency(income)}")
                appendLine("• Chi: ${formatCurrency(expense)}")
                appendLine("• Thay đổi số dư: ${formatCurrency(balanceChange)}")
                if (topCategories.isNotEmpty()) {
                    appendLine()
                    appendLine("Danh mục nổi bật:")
                    topCategories.forEach { (cat, amount) ->
                        appendLine("• $cat: ${formatCurrency(amount)}")
                    }
                }
                appendLine()
                append(
                    when {
                        expense == 0.0 && income > 0.0 ->
                            "👏 Tuyệt vời! Hôm nay bạn chỉ có thu nhập, chưa chi tiêu gì."
                        expense > income ->
                            "⚠️ Hôm nay chi tiêu nhiều hơn thu nhập, hãy chú ý cân đối nhé."
                        else ->
                            "✅ Tài chính hôm nay khá ổn, hãy tiếp tục duy trì thói quen này."
                    }
                )
            }

            AICommandResult(
                success = true,
                message = message,
                data = mapOf(
                    "date" to targetDate,
                    "income" to income,
                    "expense" to expense,
                    "balance_change" to balanceChange,
                    "top_categories" to topCategories
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tổng quan ngày: ${e.message}", e)
            AICommandResult(false, "Không thể lấy tổng quan ngày: ${e.message}")
        }
    }

    /**
     * Xuất giao dịch với định dạng
     */
    private suspend fun exportTransactionsWithFormat(command: AICommand.ExportTransactions): AICommandResult {
        return try {
            val transactions = getTransactionsForPeriod(command.period)
            if (transactions.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "Không có giao dịch nào trong kỳ được chọn để xuất."
                )
            }

            // Hiện tại: xuất dạng văn bản xem trước trong chat
            val previewLimit = 20
            val preview = transactions.take(previewLimit)

            val header = "📄 Xuất ${transactions.size} giao dịch ($previewLimit đầu tiên hiển thị bên dưới):"
            val lines = preview.joinToString("\n") { t ->
                val type = if (t.isIncome) "Thu" else "Chi"
                "• [$type] ${t.date} - ${t.title} (${t.category}): ${formatCurrency(t.amount)}"
            }

            val message = "$header\n\n$lines"

            AICommandResult(
                success = true,
                message = message,
                data = transactions
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xuất giao dịch: ${e.message}", e)
            AICommandResult(false, "Không thể xuất giao dịch: ${e.message}")
        }
    }

    /**
     * So sánh kỳ với phân tích
     */
    private suspend fun comparePeriodsWithAnalysis(command: AICommand.ComparePeriods): AICommandResult {
        return try {
            val currentData = getTransactionsForPeriod(command.currentPeriod)
            val previousData = getPreviousPeriodData(command.currentPeriod)

            if (currentData.isEmpty() && previousData.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "Không có dữ liệu giao dịch để so sánh giữa các kỳ."
                )
            }

            fun sumExpense(list: List<Transaction>) =
                list.filter { !it.isIncome }.sumOf { it.amount }

            val currentExpense = sumExpense(currentData)
            val previousExpense = sumExpense(previousData)
            val diff = currentExpense - previousExpense
            val percentChange =
                if (previousExpense > 0) (diff / previousExpense * 100).coerceIn(-500.0, 500.0) else null

            val message = buildString {
                appendLine("📊 So sánh chi tiêu giữa các kỳ")
                appendLine("• Kỳ hiện tại (${command.currentPeriod}): ${formatCurrency(currentExpense)}")
                appendLine("• Kỳ trước (${command.previousPeriod}): ${formatCurrency(previousExpense)}")
                appendLine("• Chênh lệch tuyệt đối: ${formatCurrency(diff)}")
                percentChange?.let {
                    appendLine("• Thay đổi tương đối: ${"%.1f".format(it)}%")
                }
                appendLine()
                append(
                    when {
                        percentChange == null ->
                            "Hiện chưa có đủ dữ liệu kỳ trước để so sánh chi tiết."
                        percentChange > 20 ->
                            "⚠️ Chi tiêu đang tăng khá mạnh so với kỳ trước, hãy xem lại các khoản chi lớn."
                        percentChange < -10 ->
                            "✅ Chi tiêu đã giảm so với kỳ trước, bạn đang quản lý khá tốt!"
                        else ->
                            "💡 Chi tiêu không thay đổi quá nhiều so với kỳ trước."
                    }
                )
            }

            AICommandResult(
                success = true,
                message = message,
                data = mapOf(
                    "current_expense" to currentExpense,
                    "previous_expense" to previousExpense,
                    "diff" to diff,
                    "percent_change" to (percentChange ?: 0.0)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi so sánh kỳ: ${e.message}", e)
            AICommandResult(false, "Không thể so sánh các kỳ: ${e.message}")
        }
    }

    /**
     * Tìm kiếm giao dịch với AI
     */
    private suspend fun searchTransactionsWithAI(command: AICommand.SearchTransactionsByKeyword): AICommandResult {
        return try {
            val keyword = command.keyword.trim()
            if (keyword.isBlank()) {
                return AICommandResult(false, "Từ khóa tìm kiếm không hợp lệ.")
            }

            val allTransactions = transactionViewModel.transactions.value
            val filtered = allTransactions.filter { t ->
                val inText = t.title.contains(keyword, ignoreCase = true) ||
                        (t.description?.contains(keyword, ignoreCase = true) == true)
                val inPeriod = when (command.period?.lowercase()) {
                    "today" -> t.date == getCurrentDate()
                    "week" -> isInCurrentWeek(t.date)
                    "month" -> isInCurrentMonth(t.date)
                    "year" -> isInCurrentYear(t.date)
                    else -> true
                }
                inText && inPeriod
            }

            if (filtered.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "Không tìm thấy giao dịch nào với từ khóa '$keyword'."
                )
            }

            val income = filtered.filter { it.isIncome }.sumOf { it.amount }
            val expense = filtered.filter { !it.isIncome }.sumOf { it.amount }

            val preview = filtered.take(15).joinToString("\n") { t ->
                val type = if (t.isIncome) "Thu" else "Chi"
                "• [$type] ${t.date} - ${t.title}: ${formatCurrency(t.amount)} (${t.category})"
            }

            val message = buildString {
                appendLine("🔎 Kết quả tìm kiếm cho '$keyword': ${filtered.size} giao dịch")
                appendLine("• Tổng thu: ${formatCurrency(income)}")
                appendLine("• Tổng chi: ${formatCurrency(expense)}")
                appendLine()
                appendLine("Một số giao dịch:")
                append(preview)
            }

            AICommandResult(
                success = true,
                message = message,
                data = filtered
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tìm kiếm giao dịch: ${e.message}", e)
            AICommandResult(false, "Không thể tìm kiếm giao dịch: ${e.message}")
        }
    }

    /**
     * Tạo ngân sách với tối ưu hóa
     */
    private suspend fun createBudgetWithOptimization(command: AICommand.CreateBudget): AICommandResult {
        return try {
            val categoryId = command.categoryId
            if (categoryId.isBlank()) {
                return AICommandResult(false, "Danh mục để tạo ngân sách không hợp lệ.")
            }

            val periodType = when (command.periodType.lowercase()) {
                "week", "weekly" -> BudgetPeriodType.WEEK
                "year", "yearly" -> BudgetPeriodType.YEAR
                else -> BudgetPeriodType.MONTH
            }

            val newBudget = budgetViewModel.createNewBudget(
                categoryId = categoryId,
                amount = command.amount,
                periodType = periodType,
                note = command.note
            )

            budgetViewModel.addBudget(newBudget)

            val message = "🎯 Đã tạo ngân sách ${formatCurrency(command.amount)} cho danh mục $categoryId (${periodType.name.lowercase()})."

            AICommandResult(
                success = true,
                message = message,
                data = newBudget
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tạo ngân sách từ AI: ${e.message}", e)
            AICommandResult(false, "Không thể tạo ngân sách: ${e.message}")
        }
    }

    /**
     * Cập nhật ngân sách với thông minh
     */
    private suspend fun updateBudgetWithIntelligence(command: AICommand.UpdateBudget): AICommandResult {
        return try {
            val targetBudget = when {
                !command.budgetId.isNullOrBlank() ->
                    budgetViewModel.getBudgetById(command.budgetId)
                !command.categoryId.isNullOrBlank() ->
                    budgetViewModel.getBudgetForCategory(command.categoryId)
                else -> null
            }

            if (targetBudget == null) {
                return AICommandResult(false, "Không tìm thấy ngân sách để cập nhật.")
            }

            val newAmount = command.newAmount ?: targetBudget.amount
            val updated = targetBudget.copy(amount = newAmount)

            budgetViewModel.updateFullBudget(updated)

            val message = "✅ Đã cập nhật ngân sách danh mục ${targetBudget.categoryId} từ ${formatCurrency(targetBudget.amount)} lên ${formatCurrency(newAmount)}."

            AICommandResult(
                success = true,
                message = message,
                data = updated
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi cập nhật ngân sách từ AI: ${e.message}", e)
            AICommandResult(false, "Không thể cập nhật ngân sách: ${e.message}")
        }
    }

    /**
     * Xóa ngân sách với bảo vệ
     */
    private suspend fun deleteBudgetWithSafeguard(command: AICommand.DeleteBudget): AICommandResult {
        return try {
            val targetBudget = when {
                !command.budgetId.isNullOrBlank() ->
                    budgetViewModel.getBudgetById(command.budgetId)
                !command.categoryId.isNullOrBlank() ->
                    budgetViewModel.getBudgetForCategory(command.categoryId)
                else -> null
            }

            if (targetBudget == null) {
                return AICommandResult(false, "Không tìm thấy ngân sách để xóa.")
            }

            budgetViewModel.deleteBudget(targetBudget.id)

            val message = "🗑️ Đã xóa ngân sách cho danh mục ${targetBudget.categoryId} với hạn mức ${formatCurrency(targetBudget.amount)}."

            AICommandResult(
                success = true,
                message = message,
                data = targetBudget
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xóa ngân sách từ AI: ${e.message}", e)
            AICommandResult(false, "Không thể xóa ngân sách: ${e.message}")
        }
    }

    /**
     * Lấy trạng thái ngân sách với dự đoán
     */
    private suspend fun getBudgetStatusWithPredictions(command: AICommand.GetBudgetStatus): AICommandResult {
        return try {
            val budgets = if (command.categoryId != null) {
                val b = budgetViewModel.getBudgetForCategory(command.categoryId)
                if (b != null) listOf(b) else emptyList()
            } else {
                budgetViewModel.getActiveBudgets()
            }

            if (budgets.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "Hiện tại bạn chưa có ngân sách nào đang hoạt động cho yêu cầu này."
                )
            }

            val totalBudget = budgets.sumOf { it.amount }
            val totalSpent = budgets.sumOf { it.spentAmount }
            val usageRate = if (totalBudget > 0) (totalSpent / totalBudget * 100) else 0.0

            val nearExceeded = budgetViewModel.getNearExceededBudgets()
            val exceeded = budgets.filter { it.isOverBudget }

            val message = buildString {
                if (command.categoryId != null) {
                    appendLine("📊 Trạng thái ngân sách cho danh mục ${command.categoryId}:")
                } else {
                    appendLine("📊 Tổng quan ngân sách hiện tại:")
                }
                appendLine("• Tổng hạn mức: ${formatCurrency(totalBudget)}")
                appendLine("• Đã chi: ${formatCurrency(totalSpent)} (${String.format(Locale.getDefault(), "%.1f", usageRate)}%)")
                appendLine("• Ngân sách đang vượt: ${exceeded.size}")
                appendLine("• Ngân sách sắp vượt (>80%): ${nearExceeded.size}")
                appendLine()
                append(
                    when {
                        exceeded.isNotEmpty() ->
                            "⚠️ Một số ngân sách đã vượt hạn mức, bạn nên xem lại chi tiêu các danh mục này."
                        nearExceeded.isNotEmpty() ->
                            "⚠️ Một số ngân sách sắp vượt, hãy hạn chế chi thêm vào các danh mục đó."
                        else ->
                            "✅ Các ngân sách hiện tại vẫn trong vùng an toàn."
                    }
                )
            }

            AICommandResult(
                success = true,
                message = message,
                data = budgets
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy trạng thái ngân sách: ${e.message}", e)
            AICommandResult(false, "Không thể lấy trạng thái ngân sách: ${e.message}")
        }
    }

    /**
     * Tạo ngân sách với tự động điều chỉnh
     */
    private suspend fun createBudgetWithAutoAdjust(command: AICommand.SetBudget): AICommandResult {
        return try {
            // Từ tên category (tiếng Việt) -> categoryId
            val categories = categoryViewModel.categories.value
            val matchedCategory = categories.firstOrNull { cat ->
                cat.name.equals(command.category, ignoreCase = true)
            }

            if (matchedCategory == null) {
                return AICommandResult(
                    success = false,
                    message = "Không tìm thấy danh mục '${command.category}' để đặt ngân sách."
                )
            }

            val periodType = when (command.period.lowercase()) {
                "week", "weekly" -> BudgetPeriodType.WEEK
                "year", "yearly" -> BudgetPeriodType.YEAR
                else -> BudgetPeriodType.MONTH
            }

            val newBudget = budgetViewModel.createNewBudget(
                categoryId = matchedCategory.id,
                amount = command.amount,
                periodType = periodType,
                note = "Tạo bởi Wendy AI"
            )

            budgetViewModel.addBudget(newBudget)

            val message =
                "🎯 Đã đặt ngân sách ${formatCurrency(command.amount)} cho danh mục '${matchedCategory.name}' theo chu kỳ ${periodType.name.lowercase()}."

            AICommandResult(
                success = true,
                message = message,
                data = newBudget
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tạo ngân sách tự động: ${e.message}", e)
            AICommandResult(false, "Không thể đặt ngân sách: ${e.message}")
        }
    }

    /**
     * Phân tích mẫu chi tiêu nâng cao
     */
    private suspend fun analyzeAdvancedSpendingPattern(): AdvancedSpendingPattern {
        // TODO: Implement
        return AdvancedSpendingPattern(
            monthlyAverage = 0.0,
            weeklyPattern = emptyMap(),
            seasonalTrend = "stable",
            topCategories = emptyList(),
            consistencyScore = 0,
            peakSpendingDays = emptyList(),
            recurringTransactions = emptyList()
        )
    }

    /**
     * Phân tích trạng thái ngân sách
     */
    private suspend fun analyzeBudgetStatus(): BudgetStatus {
        return try {
            val active = budgetViewModel.getActiveBudgets()
            val over = active.count { it.isOverBudget }
            val near = budgetViewModel.getNearExceededBudgets().size
            val safe = active.size - over - near

            BudgetStatus(
                activeBudgets = active.size,
                overBudget = over,
                nearBudget = near,
                safeBudgets = safe.coerceAtLeast(0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích trạng thái ngân sách: ${e.message}", e)
            BudgetStatus(
                activeBudgets = 0,
                overBudget = 0,
                nearBudget = 0,
                safeBudgets = 0
            )
        }
    }

    data class BudgetStatus(
        val activeBudgets: Int,
        val overBudget: Int,
        val nearBudget: Int,
        val safeBudgets: Int
    )

    /**
     * Tạo dự đoán ngắn hạn
     */
    private fun generateShortTermPredictions(): List<String> {
        // TODO: Implement
        return listOf(
            "Dự kiến chi tiêu ổn định trong 30 ngày tới",
            "Tiếp tục theo dõi các danh mục chi tiêu lớn"
        )
    }

    /**
     * Tính dự báo tuần
     */
    private fun calculateWeeklyForecast(
        currentWeek: List<Transaction>,
        lastWeek: List<Transaction>
    ): RegressionForecast {
        // TODO: Implement
        return RegressionForecast(
            forecast = 0.0,
            lowerBound = 0.0,
            upperBound = 0.0,
            rSquared = 0.0,
            hasSeasonality = false
        )
    }

    /**
     * Lấy đánh giá tuần
     */
    private fun getWeeklyAssessment(balance: Double, expense: Double, income: Double): String {
        return when {
            balance > 0 && expense < income * 0.7 -> "TUYỆT VỜI! Bạn đang quản lý chi tiêu rất tốt và có tiết kiệm"
            balance > 0 && expense < income * 0.9 -> "TỐT! Bạn đang sống trong khả năng tài chính"
            balance < 0 && expense > income -> "CẢNH BÁO! Chi tiêu vượt thu nhập, cần điều chỉnh ngay"
            else -> "ỔN ĐỊNH! Tài chính đang trong tầm kiểm soát"
        }
    }

    /**
     * Lấy mục tiêu tuần tới
     */
    private fun getWeeklyGoals(expense: Double, income: Double): String {
        val suggestions = mutableListOf<String>()

        if (expense > income * 0.8) {
            suggestions.add("Giảm chi tiêu xuống dưới 80% thu nhập")
            suggestions.add("Cắt giảm ít nhất 1 danh mục chi tiêu không cần thiết")
        }

        suggestions.addAll(listOf(
            "Theo dõi chi tiêu hàng ngày",
            "Đặt ngân sách cho từng danh mục cụ thể",
            "Tiết kiệm ít nhất 10% thu nhập"
        ))

        return suggestions.joinToString("\n") { "• $it" }
    }

    /**
     * Phân tích xu hướng nâng cao
     */
    private fun performAdvancedTrendAnalysis(
        currentData: List<Transaction>,
        previousData: List<Transaction>,
        compare: Boolean
    ): TrendAnalysis {
        // TODO: Implement
        return TrendAnalysis(
            mainTrends = emptyList(),
            changes = emptyList(),
            signals = emptyList(),
            actions = emptyList()
        )
    }

    /**
     * Tạo dự đoán xu hướng
     */
    private fun generateTrendPredictions(currentData: List<Transaction>): List<String> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Tạo biểu đồ xu hướng
     */
    private fun generateTrendCharts(
        currentData: List<Transaction>,
        previousData: List<Transaction>
    ): Map<String, List<Double>> {
        // TODO: Implement
        return emptyMap()
    }

    /**
     * Tạo đề xuất ngân sách cá nhân hóa
     */
    private fun generatePersonalizedBudgetRecommendations(
        income: Double,
        pattern: AdvancedSpendingPattern,
        riskTolerance: String
    ): PersonalizedBudgetRecommendations {
        // TODO: Implement
        return PersonalizedBudgetRecommendations(
            allocation = emptyList(),
            goals = emptyList(),
            advice = emptyList(),
            personalizedAllocation = emptyMap(),
            riskAssessment = "medium"
        )
    }

    /**
     * Kiểm tra cảnh báo ngân sách
     */
    private suspend fun checkBudgetAlerts(threshold: Double?): List<SpendingAlert> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Kiểm tra chi tiêu bất thường
     */
    private suspend fun checkSpendingAnomalies(): List<SpendingAlert> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Kiểm tra cảnh báo cash flow
     */
    private suspend fun checkCashFlowAlerts(): List<SpendingAlert> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Kiểm tra cơ hội tiết kiệm
     */
    private suspend fun checkSavingsOpportunities(): List<SpendingAlert> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Phát hiện bất thường nâng cao
     */
    private suspend fun detectAdvancedAnomalies(
        transactions: List<Transaction>,
        sensitivity: Double
    ): AnomalyDetection {
        // TODO: Implement
        return AnomalyDetection(
            anomalies = emptyList(),
            severity = "low",
            recommendations = emptyList(),
            confidence = 0.0
        )
    }

    /**
     * Phát hiện bất thường trong giao dịch
     */
    private suspend fun detectTransactionAnomalies(transactions: List<Transaction>): List<Transaction> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Tạo kế hoạch tiết kiệm cá nhân hóa
     */
    private suspend fun createPersonalizedSavingsPlan(
        goalAmount: Double,
        goalId: String?
    ): SavingsPlan {
        // TODO: Implement
        return SavingsPlan(
            goalName = "Tiết kiệm mục tiêu",
            targetAmount = goalAmount,
            currentAmount = 0.0,
            monthlyRequired = goalAmount / 12,
            timelineMonths = 12,
            riskLevel = "medium",
            investmentSuggestions = emptyList()
        )
    }

    /**
     * Tạo lời khuyên tiết kiệm cá nhân hóa
     */
    private suspend fun generatePersonalizedSavingsAdvice(
        savingsRate: Double,
        pattern: AdvancedSpendingPattern,
        budgetStatus: BudgetStatus
    ): List<String> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Phân tích hồ sơ rủi ro
     */
    private suspend fun analyzeRiskProfile(): String {
        // TODO: Implement
        return "medium"
    }

    /**
     * Ước tính kiến thức đầu tư
     */
    private suspend fun estimateInvestmentKnowledge(): String {
        // TODO: Implement
        return "beginner"
    }

    /**
     * Tạo mẹo đầu tư cá nhân hóa
     */
    private suspend fun generatePersonalizedInvestmentTips(
        availableForInvestment: Double,
        riskProfile: String,
        investmentKnowledge: String
    ): List<String> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * Tạo insights danh mục
     */
    private suspend fun generateCategoryInsights(
        analysis: DetailedSpendingAnalysis,
        category: String
    ): List<String> {
        // TODO: Implement
        return emptyList()
    }

    /**
     * So sánh danh mục với các danh mục khác
     */
    private suspend fun compareCategoryWithOthers(
        category: String,
        categorySpending: Double,
        period: String
    ): String {
        // TODO: Implement
        return "Không có dữ liệu so sánh"
    }

    /**
     * Tạo báo cáo tóm tắt
     */
    private suspend fun generateSummaryReport(
        analysis: DetailedSpendingAnalysis,
        period: String
    ): String {
        // TODO: Implement
        return "Báo cáo tóm tắt"
    }

    /**
     * Tạo báo cáo chi tiết
     */
    private suspend fun generateDetailedReport(
        analysis: DetailedSpendingAnalysis,
        period: String
    ): String {
        // TODO: Implement
        return "Báo cáo chi tiết"
    }

    /**
     * Tạo báo cáo xuất
     */
    private suspend fun generateExportReport(
        transactions: List<Transaction>,
        period: String
    ): String {
        // TODO: Implement
        return "Báo cáo xuất"
    }

    /**
     * Phân tích mẫu chi tiêu
     */
    private suspend fun analyzeSpendingPattern(): SpendingPattern {
        return try {
            // Lấy dữ liệu chi tiêu 6 tháng gần nhất
            val allTransactions = transactionViewModel.transactions.value
            if (allTransactions.isEmpty()) {
                return SpendingPattern(
                    monthlyAverages = 0.0,
                    seasonalTrend = "stable",
                    topCategories = emptyList(),
                    consistencyScore = 0
                )
            }

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            val monthlyTotals = mutableMapOf<Pair<Int, Int>, Double>() // (year, month) -> totalExpense

            allTransactions
                .filter { !it.isIncome }
                .forEach { t ->
                    val date = parseDate(t.date)
                    val c = Calendar.getInstance().apply { time = date }
                    val y = c.get(Calendar.YEAR)
                    val m = c.get(Calendar.MONTH)
                    monthlyTotals[y to m] = (monthlyTotals[y to m] ?: 0.0) + t.amount
                }

            // Tập trung vào 6 tháng gần nhất
            val lastSixKeys = generateSequence(0) { it + 1 }
                .map { offset ->
                    val c = Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, currentMonth)
                        add(Calendar.MONTH, -offset)
                    }
                    c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
                }
                .take(6)
                .toList()

            val lastSixValues = lastSixKeys.map { key -> monthlyTotals[key] ?: 0.0 }
            val avg = if (lastSixValues.isNotEmpty()) lastSixValues.average() else 0.0

            // Xác định xu hướng đơn giản
            val seasonalTrend = when {
                lastSixValues.size < 2 -> "stable"
                lastSixValues.last() > lastSixValues.first() * 1.2 -> "up"
                lastSixValues.last() < lastSixValues.first() * 0.8 -> "down"
                else -> "stable"
            }

            // Top danh mục theo tổng chi và số lần trong 3 tháng gần nhất
            val threeMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -2)
            }

            val categoryStats = allTransactions
                .filter { !it.isIncome }
                .filter { t ->
                    val d = parseDate(t.date)
                    d.after(threeMonthsAgo.time) || d == threeMonthsAgo.time
                }
                .groupBy { it.category }
                .mapValues { (_, list) ->
                    val count = list.size
                    val total = list.sumOf { it.amount }
                    count to total
                }
                .toList()
                .sortedByDescending { it.second.second }
                .take(5)

            // Độ nhất quán: độ lệch chuẩn so với trung bình (càng thấp càng nhất quán)
            val consistencyScore = if (lastSixValues.size >= 2 && avg > 0) {
                val variance = lastSixValues
                    .map { (it - avg) * (it - avg) }
                    .average()
                val stdDev = kotlin.math.sqrt(variance)
                // Chuyển stdDev thành điểm 0-100 (ít dao động => điểm cao)
                val ratio = (stdDev / avg).coerceAtLeast(0.0)
                (100 - (ratio * 100)).coerceIn(0.0, 100.0).toInt()
            } else {
                0
            }

            SpendingPattern(
                monthlyAverages = avg,
                seasonalTrend = seasonalTrend,
                topCategories = categoryStats,
                consistencyScore = consistencyScore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích mẫu chi tiêu: ${e.message}", e)
            SpendingPattern(
                monthlyAverages = 0.0,
                seasonalTrend = "stable",
                topCategories = emptyList(),
                consistencyScore = 0
            )
        }
    }
}

// ==================== AI VIEWMODEL CHÍNH ====================

/**
 * ViewModel chính cho AI Assistant với tất cả tính năng nâng cao
 */
class AIViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AIViewModel"
        private const val MAX_CALLS_PER_MINUTE = 30
        private const val MAX_CONVERSATION_HISTORY = 50
        private const val CACHE_DURATION_MS = 500000

        // Cấu hình hệ thống thông báo
        private const val PROACTIVE_CHECK_INTERVAL = 60 * 1000L // 1 phút
        private const val MIN_TIME_BETWEEN_PROACTIVE = 2 * 60 * 1000L // 2 phút
        private const val INACTIVITY_THRESHOLD = 30 * 1000L // 30 giây
    }

    // Flow cho dữ liệu real-time
    private val _realTimeData = MutableStateFlow<RealTimeData>(RealTimeData())
    val realTimeData: StateFlow<RealTimeData> = _realTimeData.asStateFlow()

    // Flow cho messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Flow cho AI state
    private val _aiState = MutableStateFlow(AIState.IDLE)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    // State
    val isAITyping = mutableStateOf(false)
    val lastError = mutableStateOf<String?>(null)

    private val transactionViewModel: TransactionViewModel by lazy {
        (application as FinanceApp).transactionViewModel
    }

    private val categoryViewModel: CategoryViewModel by lazy {
        (application as FinanceApp).categoryViewModel
    }

    private val budgetViewModel: BudgetViewModel by lazy {
        (application as FinanceApp).budgetViewModel
    }

    private val recurringExpenseViewModel: RecurringExpenseViewModel by lazy {
        (application as FinanceApp).recurringExpenseViewModel
    }

    private val generativeModel: GenerativeModel by lazy {
        try {
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo Gemini: ${e.message}")
            throw e
        }
    }

    private val commandExecutor by lazy {
        AdvancedAICommandExecutor(
            transactionViewModel,
            budgetViewModel,
            categoryViewModel,
            recurringExpenseViewModel
        )
    }
    private val naturalLanguageParser by lazy { NaturalLanguageParser(categoryViewModel) }

    private val apiCallTimes = mutableListOf<Long>()
    private val conversationHistory = mutableListOf<String>()
    private var lastFinanceSummary: String? = null
    private var lastSummaryUpdateTime: Long = 0
    private var currentJob: Job? = null
    private val financialInsightsCache = mutableMapOf<String, Pair<String, Long>>()

    // Bộ não AI nâng cao
    private var lastUserActivityTime = System.currentTimeMillis()
    private var lastProactiveMessageTime = 0L
    private var userBehaviorProfile = UserBehaviorProfile()
    private var lastAnalysisTime = 0L
    private val analysisInterval = 1 * 60 * 1000L // 1 phút
    private var brainJob: Job? = null

    // Job theo dõi dữ liệu
    private var dataMonitoringJob: Job? = null
    private var lastTransactionCount = 0
    private var lastBudgetCount = 0
    private var lastTransactionData: List<Transaction> = emptyList()
    private var lastBudgetData: List<Budget> = emptyList()

    private val sentEvents = mutableSetOf<String>()
    private val eventCooldowns = mutableMapOf<String, Long>()

    // Coroutine scope riêng để quản lý lifecycle
    private val aiCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        Log.d(TAG, "AIViewModel khởi tạo với hệ thống học hỏi thông minh")
        initializeAIChat()

        viewModelScope.launch {
            connectDataSources()
            loadInitialInsights()
            startAIBrain()
            startDataMonitoring()
        }
    }

    // ==================== HỆ THỐNG THEO DÕI DỮ LIỆU REAL-TIME ====================

    /**
     * Bắt đầu theo dõi dữ liệu real-time
     */
    private fun startDataMonitoring() {
        dataMonitoringJob?.cancel()
        dataMonitoringJob = viewModelScope.launch {
            Log.d(TAG, "Bắt đầu theo dõi dữ liệu real-time...")

            // Theo dõi transactions
            launch {
                transactionViewModel.transactions.collect { transactions ->
                    if (transactions != lastTransactionData) {
                        Log.d(TAG, "Phát hiện transaction data thay đổi: ${transactions.size} giao dịch")
                        updateRealTimeData(transactions)
                        lastTransactionData = transactions

                        if (transactions.size > lastTransactionCount) {
                            val newCount = transactions.size - lastTransactionCount
                            if (newCount > 0 && newCount <= 5) {
                                pushProactiveMessage("Đã thêm $newCount giao dịch mới vào hệ thống!")
                            }
                            lastTransactionCount = transactions.size
                        }

                        checkSpendingAnomalies(transactions)
                    }
                }
            }

            // Theo dõi budgets
            launch {
                budgetViewModel.budgets.collect { budgets ->
                    if (budgets != lastBudgetData) {
                        Log.d(TAG, "Phát hiện budget data thay đổi: ${budgets.size} ngân sách")
                        updateBudgetData(budgets)
                        lastBudgetData = budgets

                        if (budgets.size != lastBudgetCount) {
                            val change = budgets.size - lastBudgetCount
                            if (change > 0) {
                                pushProactiveMessage("Đã thêm $change ngân sách mới!")
                            }
                            lastBudgetCount = budgets.size
                        }

                        checkBudgetWarnings(budgets)
                    }
                }
            }

            // Theo dõi sự kiện real-time khác
            while (isActive) {
                try {
                    updateAggregatedData()
                    delay(30 * 1000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi trong data monitoring: ${e.message}")
                    delay(60 * 1000L)
                }
            }
        }
    }

    /**
     * Cập nhật dữ liệu real-time
     */
    private fun updateRealTimeData(transactions: List<Transaction>) {
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        _realTimeData.value = RealTimeData(
            transactionCount = transactions.size,
            budgetCount = lastBudgetData.size,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance,
            overBudgetCount = lastBudgetData.count { it.isOverBudget },
            lastUpdate = System.currentTimeMillis()
        )

        Log.d(TAG, "Dữ liệu cập nhật: ${transactions.size} gd, Thu: ${formatCurrency(totalIncome)}, Chi: ${formatCurrency(totalExpense)}")
    }

    /**
     * Cập nhật dữ liệu ngân sách
     */
    private fun updateBudgetData(budgets: List<Budget>) {
        val overBudgetCount = budgets.count { it.isOverBudget }

        _realTimeData.value = _realTimeData.value.copy(
            budgetCount = budgets.size,
            overBudgetCount = overBudgetCount
        )

        if (overBudgetCount > 0) {
            Log.d(TAG, "Có $overBudgetCount ngân sách đang vượt")
        }
    }

    /**
     * Kiểm tra chi tiêu bất thường
     */
    private fun checkSpendingAnomalies(transactions: List<Transaction>) {
        try {
            if (transactions.size < 5) return

            val recentTransactions = transactions.takeLast(10)
            val recentSpending = recentTransactions
                .filter { !it.isIncome }
                .sumOf { it.amount }

            if (recentSpending > 5000000) {
                pushProactiveMessage("TÔI NHẬN THẤY: Bạn đã chi tiêu ${formatCurrency(recentSpending)} trong 10 giao dịch gần đây. Mọi thứ ổn chứ?")
            }

            val categorySpending = recentTransactions
                .filter { !it.isIncome }
                .groupBy { it.category }
                .mapValues { (_, trans) -> trans.sumOf { it.amount } }

            val highSpendingCategory = categorySpending.entries.find { it.value > 2000000 }
            highSpendingCategory?.let { (category, amount) ->
                pushProactiveMessage("LƯU Ý: Bạn đã chi ${formatCurrency(amount)} cho '$category' gần đây. Có cần xem xét lại không?")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra chi tiêu bất thường: ${e.message}")
        }
    }

    /**
     * Kiểm tra cảnh báo ngân sách
     */
    private fun checkBudgetWarnings(budgets: List<Budget>) {
        try {
            val criticalBudgets = budgets.filter {
                it.isActive && it.isOverBudget
            }

            if (criticalBudgets.isNotEmpty()) {
                val categoryNames = criticalBudgets.joinToString(", ") { budget ->
                    categoryViewModel.getCategoryById(budget.categoryId)?.name ?: budget.categoryId
                }
                pushProactiveMessage("CẢNH BÁO: Ngân sách vượt cho $categoryNames!")
            }

            val nearBudget = budgets.filter {
                it.isActive && !it.isOverBudget &&
                        it.amount > 0 && (it.spentAmount / it.amount) >= 0.8
            }

            if (nearBudget.isNotEmpty()) {
                pushProactiveMessage("LƯU Ý: Có ${nearBudget.size} ngân sách sắp vượt (>80%). Hãy kiểm tra!")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra cảnh báo ngân sách: ${e.message}")
        }
    }

    /**
     * Cập nhật dữ liệu tổng hợp
     */
    private suspend fun updateAggregatedData() {
        try {
            Log.d(TAG, "Đang cập nhật dữ liệu tổng hợp...")

            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
            }

            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value
            }

            updateFinanceSummary(transactions)
            checkTrends(transactions)
            updateUserProfileWithNewData(transactions, budgets)

            Log.d(TAG, "Đã cập nhật dữ liệu tổng hợp")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi cập nhật dữ liệu tổng hợp: ${e.message}")
        }
    }

    /**
     * Kiểm tra xu hướng
     */
    private suspend fun checkTrends(transactions: List<Transaction>) {
        try {
            val currentMonth = getCurrentMonthTransactions(transactions)
            val lastMonth = getLastMonthTransactions(transactions)

            if (currentMonth.isNotEmpty() && lastMonth.isNotEmpty()) {
                val currentSpending = currentMonth.filter { !it.isIncome }.sumOf { it.amount }
                val lastMonthSpending = lastMonth.filter { !it.isIncome }.sumOf { it.amount }

                if (lastMonthSpending > 0) {
                    val changePercent = ((currentSpending - lastMonthSpending) / lastMonthSpending * 100).toInt()

                    if (abs(changePercent) > 20) {
                        val trend = if (changePercent > 0) "tăng" else "giảm"
                        pushProactiveMessage("XU HƯỚNG: Chi tiêu tháng này ${trend} ${abs(changePercent)}% so với tháng trước!")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra xu hướng: ${e.message}")
        }
    }

    /**
     * Cập nhật user profile với dữ liệu mới
     */
    private fun updateUserProfileWithNewData(transactions: List<Transaction>, budgets: List<Budget>) {
        val recentTransactions = transactions.takeLast(20)
        val topCategories = recentTransactions
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, trans) -> trans.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        topCategories.forEach { (category, _) ->
            trackUserPreference("favorite_category", category)
        }

        userBehaviorProfile.engagementScore = calculateRealTimeEngagement()

        Log.d(TAG, "User profile updated: ${topCategories.size} favorite categories, engagement: ${userBehaviorProfile.engagementScore}")
    }

    /**
     * Tính toán engagement real-time
     */
    private fun calculateRealTimeEngagement(): Int {
        val now = System.currentTimeMillis()
        val lastHourActivity = _messages.value.count { now - it.timestamp < 60 * 60 * 1000 }

        return when {
            lastHourActivity > 10 -> 10
            lastHourActivity > 5 -> 7
            lastHourActivity > 2 -> 5
            else -> 3
        }
    }

    // ==================== HỆ THỐNG BỘ NÃO AI ====================

    /**
     * Khởi động bộ não AI
     */
    private fun startAIBrain() {
        brainJob?.cancel()
        brainJob = viewModelScope.launch {
            delay(3000)

            Log.d(TAG, "AI Brain đã khởi động - Phiên bản Real-time!")
            pushProactiveMessage("Chào bạn! Tôi là WendyAI. Tôi luôn theo dõi tài chính của bạn 24/7!")

            var checkCount = 0

            while (isActive) {
                try {
                    checkCount++
                    Log.d(TAG, "AI Brain - Lần kiểm tra thứ $checkCount")

                    val currentData = _realTimeData.value
                    Log.d(TAG, "Data snapshot: ${currentData.transactionCount} gd, ${currentData.overBudgetCount} vượt NS")

                    val timeSinceLastActivity = System.currentTimeMillis() - lastUserActivityTime

                    if (shouldSendProactiveMessage(timeSinceLastActivity)) {
                        Log.d(TAG, "Đủ điều kiện, bắt đầu gửi tin nhắn chủ động...")
                        sendProactiveMessage()
                    }

                    if (System.currentTimeMillis() - lastAnalysisTime > analysisInterval) {
                        analyzeFinancialSituation()
                        lastAnalysisTime = System.currentTimeMillis()
                    }

                    checkForSpecialEvents()
                    performQuickDataCheck()

                    Log.d(TAG, "Đợi ${PROACTIVE_CHECK_INTERVAL/1000}s...")
                    delay(PROACTIVE_CHECK_INTERVAL)

                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi AI Brain: ${e.message}", e)
                    delay(15 * 1000L)
                }
            }
        }
    }

    /**
     * Kiểm tra dữ liệu nhanh
     */
    private suspend fun performQuickDataCheck() {
        try {
            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value.takeLast(5)
            }

            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value
            }

            if (transactions.isNotEmpty()) {
                val latestTransaction = transactions.last()
                val timeSinceLatest = System.currentTimeMillis() - parseDate(latestTransaction.date).time

                if (timeSinceLatest < 5 * 60 * 1000) {
                    Log.d(TAG, "Có giao dịch mới trong 5 phút: ${latestTransaction.title}")
                }
            }

            val activeBudgets = budgets.filter { it.isActive }
            val urgentBudgets = activeBudgets.filter {
                it.amount > 0 && it.spentAmount / it.amount >= 0.9
            }

            if (urgentBudgets.isNotEmpty() && Random.nextInt(100) < 30) {
                pushProactiveMessage("CẤP BÁCH: Có ngân sách sắp vượt 90%!")
            }

        } catch (e: Exception) {
            // Bỏ qua lỗi nhỏ
        }
    }

    /**
     * Kiểm tra điều kiện gửi tin nhắn chủ động
     */
    private fun shouldSendProactiveMessage(timeSinceLastActivity: Long): Boolean {
        Log.d(TAG, "Kiểm tra điều kiện proactive...")

        if (_aiState.value == AIState.PROCESSING) {
            Log.d(TAG, "AI đang bận")
            return false
        }

        if (_messages.value.size <= 1) {
            Log.d(TAG, "Chưa đủ tin nhắn: ${_messages.value.size}")
            return false
        }

        if (timeSinceLastActivity < INACTIVITY_THRESHOLD) {
            Log.d(TAG, "Người dùng vừa hoạt động: ${timeSinceLastActivity/1000}s trước")
            return false
        }

        val timeSinceLastProactive = System.currentTimeMillis() - lastProactiveMessageTime
        if (timeSinceLastProactive < MIN_TIME_BETWEEN_PROACTIVE) {
            Log.d(TAG, "Vừa gửi tin nhắn: ${timeSinceLastProactive/1000}s trước")
            return false
        }

        val lastMessage = _messages.value.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser && lastMessage.isProactive) {
            Log.d(TAG, "Tin nhắn cuối đã là proactive")
            return false
        }

        val randomChance = Random.nextInt(100)
        if (randomChance < 50) {
            Log.d(TAG, "Random check passed: $randomChance >= 50")
            Log.d(TAG, "Đủ tất cả điều kiện gửi tin nhắn chủ động!")
            return true
        }

        Log.d(TAG, "Random check failed: $randomChance < 50")
        return false
    }

    /**
     * Gửi tin nhắn chủ động
     */
    private suspend fun sendProactiveMessage() {
        try {
            Log.d(TAG, "Bắt đầu gửi tin nhắn chủ động (Real-time)...")

            val currentData = _realTimeData.value
            val context = analyzeUserContext()

            val message = generateProactiveMessageByPriority(context, currentData)

            if (message != null) {
                Log.d(TAG, "Đã tạo tin nhắn real-time: ${message.take(50)}...")

                val randomDelay = Random.nextLong(800, 2000)
                Log.d(TAG, "Đợi ${randomDelay}ms...")
                delay(randomDelay)

                pushProactiveMessage(message)

                lastProactiveMessageTime = System.currentTimeMillis()
                userBehaviorProfile.totalInteractions++

                Log.d(TAG, "Đã gửi tin nhắn chủ động với dữ liệu real-time!")
            } else {
                Log.d(TAG, "Không tạo được tin nhắn phù hợp")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi tin nhắn chủ động: ${e.message}", e)
        }
    }

    /**
     * Tạo tin nhắn với real-time data
     */
    private suspend fun generateProactiveMessageByPriority(
        context: ProactiveContext,
        realTimeData: RealTimeData
    ): String? {
        val messageOptions = mutableListOf<Pair<Int, String>>()

        val realTimeAlert = generateRealTimeAlert(realTimeData)
        if (realTimeAlert != null) {
            messageOptions.add(50 to realTimeAlert)
        }

        val financialAlert = generateFinancialAlertMessage(context)
        if (financialAlert != null) {
            messageOptions.add(30 to financialAlert)
        }

        val timeBased = generateTimeBasedMessage(context)
        if (timeBased != null) {
            messageOptions.add(10 to timeBased)
        }

        val educational = generateEducationalMessage()
        if (educational != null) {
            messageOptions.add(5 to educational)
        }

        val randomTip = generateRandomTip()
        if (randomTip != null) {
            messageOptions.add(5 to randomTip)
        }

        if (messageOptions.isEmpty()) {
            return null
        }

        val totalWeight = messageOptions.sumOf { it.first }
        var randomValue = Random.nextInt(totalWeight)

        for ((weight, message) in messageOptions) {
            if (randomValue < weight) {
                Log.d(TAG, "Chọn tin nhắn real-time với weight: $weight")
                return message
            }
            randomValue -= weight
        }

        return messageOptions.first().second
    }

    /**
     * Tạo cảnh báo real-time
     */
    private fun generateRealTimeAlert(data: RealTimeData): String? {
        return when {
            data.overBudgetCount > 0 -> {
                "REAL-TIME: Đang có ${data.overBudgetCount} ngân sách vượt!"
            }
            data.balance < 0 -> {
                "REAL-TIME: Số dư âm ${formatCurrency(abs(data.balance))}!"
            }
            data.transactionCount > 0 && data.transactionCount % 10 == 0 -> {
                "REAL-TIME: Đã có ${data.transactionCount} giao dịch!"
            }
            else -> null
        }
    }

    /**
     * Phân tích tình hình tài chính với real-time data
     */
    private suspend fun analyzeFinancialSituation() {
        try {
            Log.d(TAG, "AI Brain: Đang phân tích tình hình tài chính (Real-time)...")

            val currentData = _realTimeData.value

            Log.d(TAG, "Real-time Analysis:")
            Log.d(TAG, "  • Giao dịch: ${currentData.transactionCount}")
            Log.d(TAG, "  • Thu nhập: ${formatCurrency(currentData.totalIncome)}")
            Log.d(TAG, "  • Chi tiêu: ${formatCurrency(currentData.totalExpense)}")
            Log.d(TAG, "  • Số dư: ${formatCurrency(currentData.balance)}")
            Log.d(TAG, "  • Ngân sách vượt: ${currentData.overBudgetCount}")

            if (currentData.overBudgetCount > 0 && currentData.overBudgetCount % 2 == 0) {
                pushProactiveMessage("CÓ ${currentData.overBudgetCount} NGÂN SÁCH ĐANG VƯỢT! HÃY KIỂM TRA NGAY!")
            }

            if (currentData.balance < -1000000) {
                pushProactiveMessage("SỐ DƯ ÂM ${formatCurrency(abs(currentData.balance))}! CẦN HÀNH ĐỘNG NGAY!")
            }

            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
            }

            val largeTransactions = transactions
                .filter { !it.isIncome && it.amount > 1000000 }
                .takeLast(3)

            if (largeTransactions.isNotEmpty()) {
                val totalLarge = largeTransactions.sumOf { it.amount }
                pushProactiveMessage("CÓ ${largeTransactions.size} GIAO DỊCH LỚN (${formatCurrency(totalLarge)}) GẦN ĐÂY!")
            }

            lastAnalysisTime = System.currentTimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích tài chính real-time: ${e.message}")
        }
    }

    /**
     * Kết nối data sources
     */
    private suspend fun connectDataSources() {
        try {
            Log.d(TAG, "Đang kết nối data sources...")

            coroutineScope {
                launch {
                    transactionViewModel.transactions.collect { transactions ->
                        Log.d(TAG, "Nhận ${transactions.size} transactions")
                    }
                }

                launch {
                    budgetViewModel.budgets.collect { budgets ->
                        Log.d(TAG, "Nhận ${budgets.size} budgets")
                    }
                }
            }

            Log.d(TAG, "Đã kết nối tất cả data sources")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kết nối data sources: ${e.message}")
        }
    }

    /**
     * Thông báo dữ liệu cập nhật
     */
    fun notifyDataUpdated(dataType: String) {
        viewModelScope.launch {
            Log.d(TAG, "Thông báo dữ liệu cập nhật: $dataType")

            when (dataType) {
                "transaction" -> {
                    val transactions = withContext(Dispatchers.Main) {
                        transactionViewModel.transactions.value
                    }
                    updateRealTimeData(transactions)
                }
                "budget" -> {
                    val budgets = withContext(Dispatchers.Main) {
                        budgetViewModel.budgets.value
                    }
                    updateBudgetData(budgets)
                }
            }

            if (shouldSendProactiveMessage(Long.MAX_VALUE)) {
                pushProactiveMessage("Hệ thống vừa cập nhật dữ liệu $dataType mới nhất!")
            }
        }
    }

    /**
     * Xử lý khi thêm transaction từ AI
     */
    fun onTransactionAdded(transaction: Transaction) {
        Log.d(TAG, "Transaction added via AI: ${transaction.title} - ${formatCurrency(transaction.amount)}")

        viewModelScope.launch {
            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
            }
            updateRealTimeData(transactions)

            if (shouldSendProactiveMessage(60000)) {
                pushProactiveMessage("Đã thêm giao dịch '${transaction.title}' thành công!")
            }
        }
    }

    /**
     * Xử lý khi cập nhật budget từ AI
     */
    fun onBudgetUpdated(budget: Budget) {
        Log.d(TAG, "Budget updated via AI: ${budget.categoryId} - ${formatCurrency(budget.amount)}")

        viewModelScope.launch {
            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value
            }
            updateBudgetData(budgets)
        }
    }

    /**
     * Cleanup khi ViewModel bị hủy
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Đang dọn dẹp AIViewModel...")

        currentJob?.cancel()
        brainJob?.cancel()
        dataMonitoringJob?.cancel()
        aiCoroutineScope.cancel()

        _messages.update { emptyList() }
        conversationHistory.clear()
        lastError.value = null
        financialInsightsCache.clear()

        Log.d(TAG, "AIViewModel đã được giải phóng hoàn toàn")
    }

    /**
     * Debug status
     */
    fun debugStatus(): String {
        return """
            AI STATUS:
            • Messages: ${_messages.value.size}
            • Real-time data: ${_realTimeData.value}
            • Last proactive: ${(System.currentTimeMillis() - lastProactiveMessageTime)/1000}s ago
            • User activity: ${(System.currentTimeMillis() - lastUserActivityTime)/1000}s ago
            • AI State: ${_aiState.value}
            • Jobs: brain=${brainJob?.isActive}, data=${dataMonitoringJob?.isActive}
        """.trimIndent()
    }

    /**
     * Force refresh data
     */
    fun forceRefreshData() {
        viewModelScope.launch {
            Log.d(TAG, "Force refreshing all data...")

            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
            }

            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value
            }

            updateRealTimeData(transactions)
            updateBudgetData(budgets)

            pushProactiveMessage("Đã làm mới toàn bộ dữ liệu thành công!")

            Log.d(TAG, "Force refresh completed")
        }
    }

    /**
     * Gửi tin nhắn từ người dùng
     */
    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        currentJob?.cancel()

        lastUserActivityTime = System.currentTimeMillis()

        val userMessage = ChatMessage(
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )

        _messages.update { currentMessages ->
            currentMessages + userMessage
        }

        conversationHistory.add("Người dùng: $text")

        if (conversationHistory.size > MAX_CONVERSATION_HISTORY) {
            conversationHistory.removeFirst()
        }

        if (!canMakeApiCall()) {
            showRateLimitMessage()
            return
        }

        if (isAITyping.value) {
            showAIBusyMessage()
            return
        }

        currentJob = processWithAI(text)
    }

    /**
     * Xử lý tin nhắn với AI
     */
    private fun processWithAI(userText: String): Job {
        return viewModelScope.launch {
            try {
                _aiState.value = AIState.PROCESSING
                isAITyping.value = true
                lastError.value = null

                Log.d(TAG, "Bắt đầu xử lý AI: '$userText'")

                if (isCommand(userText)) {
                    Log.d(TAG, "Nhận diện là COMMAND")
                    val command = naturalLanguageParser.parseCommand(userText)
                    Log.d(TAG, "Command parsed: ${command::class.simpleName}")

                    learnFromUserResponse(
                        ChatMessage(text = userText, isUser = true),
                        command
                    )

                    val result = commandExecutor.executeCommand(command)
                    handleCommandResult(result, userText)

                } else {
                    Log.d(TAG, "Nhận diện là QUESTION/CONVERSATION")
                    processWithGeminiAPI(userText)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi trong processWithAI: ${e.message}", e)
                handleAIResponse("Có lỗi xảy ra: ${e.message ?: "Vui lòng thử lại sau!"}")
            } finally {
                _aiState.value = AIState.IDLE
                isAITyping.value = false
            }
        }
    }

    /**
     * Xử lý kết quả command
     */
    private fun handleCommandResult(result: AICommandResult, userCommand: String) {
        if (result.success) {
            handleAIResponse(result.message)
            Log.d(TAG, "Command executed successfully")
        } else {
            val errorMessage = buildErrorMessage(result.message, userCommand)
            handleAIResponse(errorMessage)
            Log.w(TAG, "Command failed: ${result.message}")
        }
    }

    /**
     * Xử lý với Gemini API
     */
    private suspend fun processWithGeminiAPI(userText: String) {
        try {
            Log.d(TAG, "Gọi Gemini API với prompt: ${userText.take(50)}...")

            val prompt = buildSmartPrompt(userText)

            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }

            val aiResponse = response.text ?: "Xin lỗi, tôi chưa thể trả lời câu hỏi này ngay lúc này."

            handleAIResponse(aiResponse)
            Log.d(TAG, "Gemini API response received")

        } catch (e: CancellationException) {
            Log.d(TAG, "Gemini API call cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error: ${e.message}", e)
            handleAIResponse("Hiện tại tôi không thể kết nối đến AI. Bạn có thể thử các lệnh quản lý tài chính như:\n\n• 'Thêm chi tiêu 50k cho ăn uống'\n• 'Xem giao dịch hôm nay'\n• 'Phân tích chi tiêu tháng này'\n• 'Xem tổng quan tài chính'")
        }
    }

    /**
     * Xây dựng prompt thông minh
     */
    private fun buildSmartPrompt(userText: String): String {
        val financeContext = getCurrentFinanceContext()
        val userProfile = getUserProfileContext()

        return """
            Bạn là WendyAI - trợ lý tài chính thông minh người Việt. Bạn đang làm việc trong ứng dụng quản lý chi tiêu cá nhân.

            THÔNG TIN TÀI CHÍNH HIỆN TẠI CỦA NGƯỜI DÙNG:
            $financeContext

            THÔNG TIN HÀNH VI NGƯỜI DÙNG:
            $userProfile

            HÃY TRẢ LỜI CÂU HỎI: "$userText"

            QUY TẮC:
            - LUÔN dùng tiếng Việt tự nhiên, thân thiện
            - Tập trung vào tài chính cá nhân, quản lý chi tiêu
            - Đưa ra lời khuyên thực tế, có thể áp dụng ngay
            - Nếu liên quan đến dữ liệu trên, hãy tham chiếu cụ thể
            - Giữ câu trả lời ngắn gọn, dễ hiểu (50-100 từ)
            - Cá nhân hóa dựa trên thông tin hành vi nếu có

            Hãy trả lời như một người bạn am hiểu tài chính!
        """.trimIndent()
    }

    /**
     * Lấy context tài chính hiện tại
     */
    private fun getCurrentFinanceContext(): String {
        return try {
            val transactions = transactionViewModel.transactions.value
            val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
            val balance = totalIncome - totalExpense
            val recentTransactions = transactions.take(5)

            """
            • Tổng thu: ${formatCurrency(totalIncome)}
            • Tổng chi: ${formatCurrency(totalExpense)}
            • Số dư: ${formatCurrency(balance)}
            • Giao dịch gần đây: ${recentTransactions.size} giao dịch
            ${if (recentTransactions.isNotEmpty()) "• Mới nhất: ${recentTransactions.first().title} - ${formatCurrency(recentTransactions.first().amount)}" else ""}
            """.trimIndent()
        } catch (e: Exception) {
            "Chưa có đủ dữ liệu tài chính"
        }
    }

    /**
     * Lấy context user profile
     */
    private fun getUserProfileContext(): String {
        return """
            • Điểm engagement: ${userBehaviorProfile.engagementScore}/10
            • Danh mục yêu thích: ${userBehaviorProfile.preferredCategories.take(3).joinToString()}
            • Lệnh thường dùng: ${userBehaviorProfile.commonCommands.toList().sortedByDescending { it.second }.take(3).joinToString { it.first }}
            • Tổng tương tác: ${userBehaviorProfile.totalInteractions}
        """.trimIndent()
    }

    /**
     * Kiểm tra có phải command không
     */
    private fun isCommand(message: String): Boolean {
        val lowerMessage = message.lowercase().trim()

        val commandKeywords = listOf(
            "thêm", "tạo", "add", "create", "tao", "them",
            "chi tiêu", "chi", "mua", "thanh toán", "trả", "tốn", "tiêu",
            "thu nhập", "thu thập", "income", "lương", "thưởng", "nhận",
            "phân tích", "analytics", "thống kê", "xem", "tổng quan", "summary",
            "xem giao dịch", "xem giao dich", "liệt kê", "liet ke",
            "ngân sách", "ngan sach", "budget", "đặt ngân sách", "dat ngan sach", "set budget",
            "điểm sức khỏe", "diem suc khoe", "health score", "financial health",
            "báo cáo", "report", "báo cáo tuần", "weekly",
            "tiết kiệm", "savings", "tiết kiệm tiền",
            "đầu tư", "investment", "đầu tư tiền",
            "dự báo", "forecast", "dự đoán", "ước tính",
            "cảnh báo", "alert", "warning", "thông báo",
            "bất thường", "anomaly", "phát hiện"
        )

        val questionKeywords = listOf(
            "tại sao", "vi sao", "vì sao", "như thế nào", "nhu the nao", "cách", "cach",
            "làm sao", "lam sao", "bao nhiêu", "bao nhieu", "khi nào", "khi nao",
            "gì", "gi", "?",
            "how", "what", "why", "when", "where", "which",
            "hỏi", "hoi", "giải thích", "giai thich", "tư vấn", "tu van", "giúp", "giup"
        )

        if (questionKeywords.any { lowerMessage.contains(it) }) {
            Log.d(TAG, "Nhận diện là QUESTION vì có từ khóa hỏi")
            return false
        }

        if (commandKeywords.any { lowerMessage.contains(it) }) {
            Log.d(TAG, "Nhận diện là COMMAND vì có từ khóa lệnh")
            return true
        }

        val amountPattern = """(\d+([.,]\d+)?)\s*(k|triệu|tr|nghìn|nghin|ngàn|ngan|đ|dong|vnd)?"""
        val hasAmount = Regex(amountPattern, RegexOption.IGNORE_CASE).containsMatchIn(lowerMessage)

        if (hasAmount && !questionKeywords.any { lowerMessage.contains(it) }) {
            Log.d(TAG, "Phân loại: hasAmount=true -> COMMAND")
            return true
        }

        Log.d(TAG, "Phân loại mặc định: QUESTION")
        return false
    }

    /**
     * Khởi tạo chat AI
     */
    private fun initializeAIChat() {
        _messages.update { emptyList() }
        conversationHistory.clear()

        _messages.update {
            listOf(
                ChatMessage(
                    text = """
                🤖 WENDY AI - TRỢ LÝ TÀI CHÍNH THÔNG MINH
                
                Chào bạn! Tôi là WendyAI, trợ lý tài chính thông minh của bạn. 
                Tôi có thể giúp bạn:
                
                💰 Quản lý chi tiêu & thu nhập
                📊 Phân tích tài chính nâng cao
                🎯 Đặt ngân sách thông minh
                🔮 Dự báo chi tiêu
                ⚠️ Cảnh báo chi tiêu bất thường
                📈 Lập kế hoạch tiết kiệm & đầu tư
                🧠 Học hỏi từ thói quen của bạn
                
                Hãy thử nói: "Thêm chi tiêu 50k cho ăn uống" hoặc "Xem giao dịch hôm nay"
                """.trimIndent(),
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Xóa chat
     */
    fun clearChat() {
        currentJob?.cancel()
        brainJob?.cancel()

        _messages.update { emptyList() }
        conversationHistory.clear()
        lastError.value = null
        financialInsightsCache.clear()

        initializeAIChat()

        viewModelScope.launch {
            startAIBrain()
        }
    }

    /**
     * Lấy mẹo tài chính nhanh
     */
    fun getQuickFinancialTips(): List<String> {
        return listOf(
            "💡 Chi tiêu ít hơn 50% thu nhập cho nhu cầu thiết yếu",
            "💰 Tiết kiệm ít nhất 20% thu nhập mỗi tháng",
            "📱 Theo dõi chi tiêu hàng ngày để kiểm soát ngân sách",
            "🎯 Đặt mục tiêu tài chính ngắn hạn và dài hạn"
        )
    }

    /**
     * Xử lý phản hồi AI
     */
    private fun handleAIResponse(response: String) {
        _messages.update { currentMessages ->
            currentMessages + ChatMessage(
                text = response,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        }

        conversationHistory.add("AI: $response")
    }

    /**
     * Xây dựng thông báo lỗi
     */
    private fun buildErrorMessage(errorMessage: String, userCommand: String): String {
        val lowerCommand = userCommand.lowercase()

        val suggestion = when {
            lowerCommand.contains("ví") && errorMessage.contains("không tìm thấy") ->
                "\nGợi ý: Tính năng ví đã được đơn giản hóa trong phiên bản này"
            lowerCommand.contains("danh mục") && errorMessage.contains("không tìm thấy") ->
                "\nGợi ý: Hãy tạo danh mục trước bằng lệnh 'Tạo danh mục Ăn uống'"
            lowerCommand.contains("ngân sách") && errorMessage.contains("không tìm thấy") ->
                "\nGợi ý: Hãy tạo ngân sách bằng lệnh 'Đặt ngân sách 1 triệu cho Ăn uống'"
            errorMessage.contains("số tiền") || errorMessage.contains("amount") ->
                "\nGợi ý: Hãy nói rõ số tiền, ví dụ: 'Thêm chi tiêu 50 nghìn cho ăn uống'"
            else -> ""
        }

        return "$errorMessage$suggestion"
    }

    /**
     * Kiểm tra có thể gọi API không
     */
    private fun canMakeApiCall(): Boolean {
        val now = System.currentTimeMillis()
        apiCallTimes.removeAll { it < now - TimeUnit.MINUTES.toMillis(1) }
        return apiCallTimes.size < MAX_CALLS_PER_MINUTE
    }

    /**
     * Hiển thị thông báo rate limit
     */
    private fun showRateLimitMessage() {
        pushProactiveMessage("Bạn đang gửi tin nhắn hơi nhanh đó! Đợi tôi xíu rồi tiếp tục nhé!")
    }

    /**
     * Hiển thị thông báo AI đang bận
     */
    private fun showAIBusyMessage() {
        pushProactiveMessage("Tôi đang suy nghĩ về câu hỏi trước của bạn... Đợi xíu nhé!")
    }

    /**
     * Đẩy tin nhắn proactive
     */
    private fun pushProactiveMessage(text: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Đang đẩy tin nhắn: ${text.take(50)}...")

                if (_aiState.value == AIState.PROCESSING) {
                    Log.w(TAG, "Bỏ qua vì AI đang xử lý")
                    return@launch
                }

                val message = ChatMessage(
                    text = text,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isProactive = true
                )

                _messages.update { currentMessages ->
                    currentMessages + message
                }

                lastProactiveMessageTime = System.currentTimeMillis()

                Log.d(TAG, "Đã thêm tin nhắn chủ động vào danh sách")

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi pushProactiveMessage: ${e.message}")
            }
        }
    }

    /**
     * Kích hoạt tin nhắn proactive từ sự kiện
     */
    fun triggerProactiveMessage(trigger: String) {
        viewModelScope.launch {
            Log.d(TAG, "Trigger proactive message: $trigger")

            val message = when (trigger) {
                "new_transaction" -> "Bạn vừa thêm giao dịch mới. Muốn xem tổng quan không?"
                "budget_warning" -> "Có ngân sách sắp vượt. Cần kiểm tra ngay!"
                "low_balance" -> "Số dư đang thấp. Hãy cẩn thận chi tiêu!"
                "weekend" -> "Cuối tuần rồi! Đã lên kế hoạch chi tiêu chưa?"
                else -> null
            }

            if (message != null && shouldSendProactiveMessage(Long.MAX_VALUE)) {
                pushProactiveMessage(message)
            }
        }
    }

    // ==================== CÁC PHƯƠNG THỨC HỖ TRỢ ====================

    /**
     * Cập nhật tổng quan tài chính
     */
    private fun updateFinanceSummary(transactions: List<Transaction>) {
        try {
            lastFinanceSummary = null
            financialInsightsCache.clear()
            Log.d(TAG, "Dữ liệu cập nhật: ${transactions.size} giao dịch")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi cập nhật dữ liệu: ${e.message}")
        }
    }

    /**
     * Tải insights ban đầu
     */
    private suspend fun loadInitialInsights() {
        delay(1000)
        if (_messages.value.size == 1) {
            val quickTips = getQuickFinancialTips().random()

            _messages.update { currentMessages ->
                currentMessages + ChatMessage(
                    text = "💡 Mẹo nhanh: $quickTips\n\nHãy thử nhập: 'Thêm chi tiêu 50k cho ăn uống' hoặc 'Xem giao dịch hôm nay'",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Phân tích ngữ cảnh người dùng
     */
    private suspend fun analyzeUserContext(): ProactiveContext {
        return try {
            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
            }

            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value
            }

            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentDay = currentTime.get(Calendar.DAY_OF_WEEK)
            val recentMessages = _messages.value.takeLast(10)

            val lastUserMessage = recentMessages.findLast { it.isUser }?.text?.lowercase() ?: ""

            val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
            val balance = totalIncome - totalExpense

            val currentMonthTransactions = getCurrentMonthTransactions(transactions)
            val monthExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val monthIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }

            val activeBudgets = budgets.filter { it.isActive }
            val overBudgetCategories = activeBudgets.filter { it.isOverBudget }

            val favoriteCategories = userBehaviorProfile.preferredCategories.toSet()
            val mostUsedCommands = userBehaviorProfile.commonCommands.toMap()

            ProactiveContext(
                currentHour = currentHour,
                currentDay = currentDay,
                lastUserMessage = lastUserMessage,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                monthExpense = monthExpense,
                monthIncome = monthIncome,
                hasOverBudget = overBudgetCategories.isNotEmpty(),
                overBudgetCount = overBudgetCategories.size,
                recentTransactionCount = transactions.size,
                userEngagementLevel = calculateEngagementLevel(recentMessages),
                favoriteCategories = favoriteCategories,
                mostUsedCommands = mostUsedCommands
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích user context: ${e.message}")
            ProactiveContext(
                currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
                lastUserMessage = "",
                totalIncome = 0.0,
                totalExpense = 0.0,
                balance = 0.0,
                monthExpense = 0.0,
                monthIncome = 0.0,
                hasOverBudget = false,
                overBudgetCount = 0,
                recentTransactionCount = 0,
                userEngagementLevel = 5,
                favoriteCategories = emptySet(),
                mostUsedCommands = emptyMap()
            )
        }
    }

    /**
     * Tạo thông báo cảnh báo tài chính
     */
    private suspend fun generateFinancialAlertMessage(context: ProactiveContext): String? {
        return try {
            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value
            }

            if (context.hasOverBudget) {
                val overBudgetCategories = budgets
                    .filter { it.isOverBudget }
                    .joinToString(", ") { budget ->
                        val category = categoryViewModel.getCategoryById(budget.categoryId)
                        category?.name ?: budget.categoryId
                    }
                return "⚠️ CẢNH BÁO: Bạn đã vượt ngân sách cho: $overBudgetCategories. Hãy xem xét điều chỉnh chi tiêu!"
            }

            if (context.balance < 0) {
                return "📉 CHÚ Ý: Chi tiêu của bạn đang vượt quá thu nhập. Cần xem xét lại ngân sách!"
            }

            if (context.monthExpense > context.monthIncome * 0.8 && context.monthIncome > 0) {
                return "💡 LƯU Ý: Bạn đang chi tiêu ${(context.monthExpense/context.monthIncome*100).toInt()}% thu nhập. Mục tiêu lý tưởng là dưới 80%!"
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tạo thông báo tài chính: ${e.message}")
            null
        }
    }

    /**
     * Tạo tin nhắn theo thời gian
     */
    private fun generateTimeBasedMessage(context: ProactiveContext): String? {
        return when (context.currentHour) {
            in 6..9 -> "🌅 Chào buổi sáng! Bạn đã sẵn sàng cho một ngày tài chính thông minh chưa?"
            in 11..13 -> "🍽️ Đến giờ ăn trưa! Đây là thời điểm tốt để kiểm tra ngân sách ăn uống."
            in 17..19 -> "🌇 Cuối ngày rồi! Bạn có muốn xem tổng kết chi tiêu hôm nay không?"
            in 20..23 -> "🌙 Buổi tối yên tĩnh là thời điểm hoàn hảo để lên kế hoạch tài chính!"
            else -> null
        }
    }

    /**
     * Tạo tin nhắn giáo dục
     */
    private fun generateEducationalMessage(): String? {
        val tips = listOf(
            "💡 Mẹo hay: Luôn theo dõi chi tiêu nhỏ - chúng có thể chiếm tới 30% ngân sách!",
            "📊 Nguyên tắc 50/30/20: 50% cho nhu cầu, 30% cho muốn, 20% cho tiết kiệm!",
            "⏰ Nhắc nhở: Đặt ngân sách cho từng danh mục giúp kiểm soát chi tiêu tốt hơn!",
            "📈 Chiến lược: Xem lại chi tiêu cuối tuần giúp bạn điều chỉnh kịp thời!",
            "🧠 Bí quyết: Sử dụng tính năng phân tích để hiểu rõ thói quen chi tiêu!"
        )
        return tips.random()
    }

    /**
     * Tạo mẹo ngẫu nhiên
     */
    private fun generateRandomTip(): String? {
        val tips = listOf(
            "🤔 Bạn có biết: Ghi chép chi tiêu hàng ngày giúp tiết kiệm thêm 15-20% ngân sách?",
            "🎯 Mẹo hay: Đặt ngân sách riêng cho từng danh mục giúp kiểm soát chi tiêu tốt hơn!",
            "📅 Hãy thử: Xem lại chi tiêu cuối tuần để điều chỉnh kịp thời!",
            "💰 Bí quyết: Tự động hóa tiết kiệm giúp bạn không quên mục tiêu tài chính!",
            "📊 Nguyên tắc 50/30/20: 50% nhu cầu, 30% mong muốn, 20% tiết kiệm!"
        )
        return tips.random()
    }

    /**
     * Kiểm tra sự kiện đặc biệt
     */
    private suspend fun checkForSpecialEvents() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val today = SimpleDateFormat("ddMM", Locale.getDefault()).format(Date())

        // Cuối tháng (25-31)
        if (dayOfMonth in 25..31 && currentHour == 9) {
            if (!hasSentEventToday("end_of_month_$today")) {
                pushProactiveMessage("📅 Sắp kết thúc tháng! Đây là thời điểm tốt để xem xét lại ngân sách và lập kế hoạch cho tháng tới.")
                markEventSent("end_of_month_$today")
            }
        }

        // Đầu tháng (1-3)
        if (dayOfMonth in 1..3 && currentHour == 10) {
            if (!hasSentEventToday("start_of_month_$today")) {
                pushProactiveMessage("🎯 Đầu tháng mới! Hãy cùng thiết lập ngân sách và mục tiêu tài chính cho tháng này nhé!")
                markEventSent("start_of_month_$today")
            }
        }

        // Cuối tuần
        if (dayOfWeek == Calendar.SUNDAY && currentHour in 15..17) {
            if (!hasSentEventToday("weekend_review_$today")) {
                pushProactiveMessage("📊 Chủ nhật rồi! Hãy xem lại chi tiêu tuần vừa qua và lên kế hoạch cho tuần mới!")
                markEventSent("weekend_review_$today")
            }
        }
    }

    /**
     * Tính điểm engagement
     */
    private fun calculateEngagementLevel(recentMessages: List<ChatMessage>): Int {
        val userMessages = recentMessages.filter { it.isUser }
        val now = System.currentTimeMillis()
        val recentActivity = userMessages.count { now - it.timestamp < 24 * 60 * 60 * 1000 }

        return when {
            recentActivity > 10 -> 10
            recentActivity > 5 -> 7
            recentActivity > 2 -> 5
            else -> 3
        }
    }

    /**
     * Theo dõi sở thích người dùng
     */
    private fun trackUserPreference(type: String, value: String) {
        when (type) {
            "favorite_category" -> {
                userBehaviorProfile.preferredCategories.add(value)
                Log.d(TAG, "Đã ghi nhận danh mục yêu thích: $value")
            }
            "common_command" -> {
                userBehaviorProfile.commonCommands[value] =
                    userBehaviorProfile.commonCommands.getOrDefault(value, 0) + 1
                Log.d(TAG, "Đã ghi nhận lệnh thường dùng: $value")
            }
        }
    }

    /**
     * Học hỏi từ phản hồi người dùng
     */
    private fun learnFromUserResponse(message: ChatMessage, command: AICommand?) {
        if (message.isUser) {
            userBehaviorProfile.responseTimes.add(System.currentTimeMillis())

            command?.let {
                trackUserPreference("common_command", it::class.simpleName ?: "unknown")
            }
        }
    }

    /**
     * Kiểm tra đã gửi sự kiện hôm nay chưa
     */
    private fun hasSentEventToday(eventId: String): Boolean = sentEvents.contains(eventId)

    /**
     * Đánh dấu đã gửi sự kiện
     */
    private fun markEventSent(eventId: String) { sentEvents.add(eventId) }

    /**
     * Lấy giao dịch tháng hiện tại
     */
    private fun getCurrentMonthTransactions(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    /**
     * Lấy giao dịch tháng trước
     */
    private fun getLastMonthTransactions(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val lastMonth = calendar.get(Calendar.MONTH)
        val lastYear = calendar.get(Calendar.YEAR)

        return transactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.MONTH) == lastMonth &&
                    transactionCalendar.get(Calendar.YEAR) == lastYear
        }
    }

    /**
     * Parse ngày từ string
     */
    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * Định dạng tiền tệ
     */
    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }
}