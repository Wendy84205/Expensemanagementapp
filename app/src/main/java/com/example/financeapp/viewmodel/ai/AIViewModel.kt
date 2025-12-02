package com.example.financeapp.viewmodel.ai

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.Budget
import com.example.financeapp.data.BudgetPeriodType
import com.example.financeapp.BuildConfig
import com.example.financeapp.FinanceApp
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.data.getDisplayName
import com.example.financeapp.data.isOverBudget
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random
import java.time.LocalDate
import kotlin.math.sqrt

// ==================== DATA CLASSES BỔ SUNG ====================
data class SpendingForecast(
    val estimatedSpending: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val confidenceLevel: Int,
    val recommendations: List<String>,
    val warning: String = ""
)

data class SpendingPattern(
    val monthlyAverages: Double,
    val seasonalTrend: String,
    val topCategories: List<Pair<String, Pair<Int, Double>>>,
    val consistencyScore: Int
)

data class BudgetRecommendations(
    val allocation: List<String>,
    val goals: List<String>,
    val advice: List<String>
)

data class TrendAnalysis(
    val mainTrends: List<String>,
    val changes: List<String>,
    val signals: List<String>,
    val actions: List<String>
)

// ==================== AI COMMANDS ====================
sealed class AICommand {
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

    data class SetBudget(
        val category: String,
        val amount: Double,
        val period: String = "monthly"
    ) : AICommand()

    data class TransferMoney(
        val fromWallet: String,
        val toWallet: String,
        val amount: Double
    ) : AICommand()

    data class CreateWallet(
        val name: String,
        val initialBalance: Double = 0.0,
        val type: String = "CASH"
    ) : AICommand()

    data class DeleteWallet(
        val name: String
    ) : AICommand()

    data class GetWalletBalance(
        val walletName: String
    ) : AICommand()

    data class AnalyzeSpendingTrend(
        val period: String,
        val compareWithPrevious: Boolean = false
    ) : AICommand()

    data class UpdateTransaction(
        val transactionId: String? = null,
        val newAmount: Double? = null,
        val newCategory: String? = null,
        val newDescription: String? = null
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

    // BUDGET COMMANDS
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

    // CATEGORY COMMANDS
    data class CreateCategory(
        val name: String,
        val type: String,
        val isMainCategory: Boolean = false,
        val parentCategoryId: String? = null,
        val icon: String = "💰"
    ) : AICommand()

    data class DeleteCategory(
        val categoryId: String
    ) : AICommand()

    data class ListCategories(
        val type: String? = null
    ) : AICommand()

    // RECURRING EXPENSE COMMANDS
    data class CreateRecurringExpense(
        val title: String,
        val amount: Double,
        val category: String,
        val wallet: String,
        val frequency: String,
        val startDate: String? = null,
        val endDate: String? = null,
        val description: String? = null
    ) : AICommand()

    data class DeleteRecurringExpense(
        val expenseId: String? = null,
        val title: String? = null
    ) : AICommand()

    data class ListRecurringExpenses(
        val isActive: Boolean? = null
    ) : AICommand()

    data class ToggleRecurringExpense(
        val expenseId: String? = null,
        val title: String? = null
    ) : AICommand()

    // ADVANCED ANALYSIS COMMANDS
    data class GetSpendingForecast(
        val period: String = "month"
    ) : AICommand()

    data class GetBudgetRecommendations(
        val income: Double? = null
    ) : AICommand()

    object GetFinancialHealthScore : AICommand()
    object ShowSummary : AICommand()
    object GetQuickTips : AICommand()
    object UnknownCommand : AICommand()
}

// ==================== DATA CLASSES ====================
data class AICommandResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)

data class SpendingAnalysis(
    val totalSpending: Double,
    val averageSpending: Double,
    val transactionCount: Int,
    val categoryBreakdown: List<Pair<String, Double>>,
    val period: String
)

data class FinancialHealthScore(
    val score: Int,
    val savingsRate: Double,
    val expenseRatio: Double,
    val recommendations: List<String>
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isProactive: Boolean = false
)

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

enum class AIState {
    IDLE, PROCESSING, ERROR
}

// ==================== NATURAL LANGUAGE PARSER ====================
class NaturalLanguageParser(
    private val categoryViewModel: CategoryViewModel
) {

    fun parseCommand(message: String): AICommand {
        val lowerMessage = message.lowercase().trim()

        Log.d("NaturalLanguageParser", "=== BẮT ĐẦU PARSE ===")
        Log.d("NaturalLanguageParser", "Input: '$lowerMessage'")

        return when {
            isAddTransactionCommand(lowerMessage) -> {
                Log.d("NaturalLanguageParser", "🎯 Nhận diện: ADD TRANSACTION")
                parseAddCommand(lowerMessage)
            }
            isListTransactionsCommand(lowerMessage) -> {
                Log.d("NaturalLanguageParser", "📋 Nhận diện: LIST TRANSACTIONS")
                parseListTransactions(lowerMessage)
            }
            containsAny(lowerMessage, listOf("phân tích", "analytics", "thống kê", "xem chi tiêu", "chi tiêu")) -> {
                Log.d("NaturalLanguageParser", "📈 Nhận diện: ANALYZE SPENDING")
                AICommand.AnalyzeSpending(period = extractPeriod(lowerMessage))
            }
            containsAny(lowerMessage, listOf("tổng quan", "summary", "tổng hợp", "tình hình")) -> {
                Log.d("NaturalLanguageParser", "📊 Nhận diện: SHOW SUMMARY")
                AICommand.ShowSummary
            }
            containsAny(lowerMessage, listOf("sức khỏe", "health", "điểm", "tình trạng")) -> {
                Log.d("NaturalLanguageParser", "🏥 Nhận diện: FINANCIAL HEALTH")
                AICommand.GetFinancialHealthScore
            }
            containsAny(lowerMessage, listOf("mẹo", "tip", "advice", "khuyên", "gợi ý")) -> {
                Log.d("NaturalLanguageParser", "💡 Nhận diện: QUICK TIPS")
                AICommand.GetQuickTips
            }
            else -> {
                Log.d("NaturalLanguageParser", "❓ Nhận diện: UNKNOWN COMMAND")
                AICommand.UnknownCommand
            }
        }
    }

    private fun isAddTransactionCommand(message: String): Boolean {
        val addKeywords = listOf(
            "chi tiêu", "chi", "mua", "thanh toán", "trả", "tốn", "tiêu",
            "thu nhập", "thu", "nhận", "lương", "thưởng", "thêm", "add", "tạo"
        )

        val amountPattern = """(\d+([.,]\d+)?)\s*(k|triệu|tr|nghìn|nghin)?"""
        val hasAmount = Regex(amountPattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
        val hasKeyword = containsAny(message, addKeywords)

        Log.d("NaturalLanguageParser", "isAddTransaction - hasKeyword: $hasKeyword, hasAmount: $hasAmount")

        return hasKeyword && hasAmount
    }

    private fun isListTransactionsCommand(message: String): Boolean {
        val listKeywords = listOf(
            "xem giao dịch", "xem chi tiêu", "danh sách", "liệt kê",
            "giao dịch", "lịch sử", "xem lại", "hiển thị"
        )

        return containsAny(message, listKeywords) &&
                !message.contains("thêm") &&
                !message.contains("tạo")
    }

    private fun parseAddCommand(message: String): AICommand {
        val amount = extractAmount(message)
        val isIncome = isIncomeCommand(message)
        val category = extractCategory(message, isIncome)
        val wallet = ""

        return AICommand.AddTransaction(
            title = extractTransactionTitle(message, isIncome),
            amount = amount,
            category = category,
            wallet = wallet,
            isIncome = isIncome
        )
    }

    private fun parseListTransactions(message: String): AICommand {
        val period = extractPeriod(message)
        val category = extractCategory(message, false)

        return AICommand.ListTransactions(
            period = period,
            category = category
        )
    }

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

    private fun containsAnyKeyword(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun getDefaultCategory(isIncome: Boolean): String {
        return if (isIncome) "Lương" else "Chi phí phát sinh"
    }

    private fun extractPeriod(message: String): String {
        return when {
            message.contains("tuần") || message.contains("week") -> "week"
            message.contains("tháng") || message.contains("month") -> "month"
            message.contains("năm") || message.contains("year") -> "year"
            message.contains("hôm qua") || message.contains("yesterday") -> "yesterday"
            else -> "today"
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}

// ==================== AI COMMAND EXECUTOR ====================
class AICommandExecutor(
    private val transactionViewModel: TransactionViewModel,
    private val budgetViewModel: BudgetViewModel,
    private val categoryViewModel: CategoryViewModel,
    private val recurringExpenseViewModel: RecurringExpenseViewModel
) {
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    suspend fun executeCommand(command: AICommand): AICommandResult {
        Log.d("AICommandExecutor", "Executing command: ${command::class.simpleName}")

        return try {
            when (command) {
                is AICommand.AddTransaction -> addTransaction(command)
                is AICommand.ListTransactions -> listTransactions(command)
                is AICommand.GetDailySummary -> getDailySummary(command)
                is AICommand.ExportTransactions -> exportTransactions(command)
                is AICommand.ComparePeriods -> comparePeriods(command)
                is AICommand.SearchTransactionsByKeyword -> searchTransactionsByKeyword(command)
                is AICommand.CreateBudget -> createBudget(command)
                is AICommand.UpdateBudget -> updateBudget(command)
                is AICommand.DeleteBudget -> deleteBudget(command)
                is AICommand.GetBudgetStatus -> getBudgetStatus(command)
                is AICommand.SetBudget -> createBudgetFromSet(command)
                is AICommand.CreateCategory -> createCategory(command)
                is AICommand.DeleteCategory -> deleteCategory(command)
                is AICommand.ListCategories -> listCategories(command)
                is AICommand.CreateRecurringExpense -> createRecurringExpense(command)
                is AICommand.DeleteRecurringExpense -> deleteRecurringExpense(command)
                is AICommand.ListRecurringExpenses -> listRecurringExpenses(command)
                is AICommand.ToggleRecurringExpense -> toggleRecurringExpense(command)
                is AICommand.GetSpendingForecast -> getSpendingForecast(command)
                is AICommand.GetBudgetRecommendations -> getBudgetRecommendations(command)
                is AICommand.GetFinancialHealthScore -> getFinancialHealthScore()
                is AICommand.AnalyzeSpending -> analyzeSpending(command)
                is AICommand.AnalyzeSpendingTrend -> analyzeSpendingTrend(command)
                is AICommand.ShowSummary -> showSummary()
                is AICommand.GetQuickTips -> getQuickTips()
                is AICommand.TransferMoney -> transferMoney(command)
                else -> AICommandResult(false, "Tính năng đang phát triển: ${command::class.simpleName}")
            }
        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error executing command: ${e.message}", e)
            AICommandResult(false, "Có lỗi xảy ra: ${e.message}")
        }
    }

    // 🔥 CÁC PHƯƠNG THỨC COMMAND CHÍNH
    private suspend fun addTransaction(command: AICommand.AddTransaction): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Adding transaction: $command")

            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                title = command.title,
                amount = command.amount,
                category = command.category,
                wallet = command.wallet,
                isIncome = command.isIncome,
                date = command.date ?: getCurrentDate(),
                categoryId = "",
                dayOfWeek = getDayOfWeek(),
                group = if (command.isIncome) "Thu nhập" else "Chi tiêu",
                description = "Tạo bởi AI Assistant",
                categoryIcon = getCategoryIcon(command.category),
                categoryColor = getCategoryColor(command.category),
                isAutoGenerated = false,
                recurringSourceId = ""
            )

            transactionViewModel.addTransactionFromAI(
                transaction = transaction,
                budgetViewModel = null
            )

            delay(500)

            AICommandResult(
                success = true,
                message = "✅ Đã thêm ${if (command.isIncome) "thu nhập" else "chi tiêu"} ${formatCurrency(command.amount)} cho '${command.title}' vào danh mục ${command.category}"
            )

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error adding transaction: ${e.message}", e)
            AICommandResult(
                success = false,
                message = "Lỗi thêm giao dịch: ${e.message}"
            )
        }
    }

    private suspend fun listTransactions(command: AICommand.ListTransactions): AICommandResult {
        return try {
            val transactions = getFilteredTransactions(command)

            if (transactions.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "📭 Không có giao dịch nào trong khoảng thời gian này!"
                )
            }

            val message = buildTransactionsListMessage(transactions, command.period)
            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error listing transactions: ${e.message}", e)
            AICommandResult(false, "Lỗi khi lấy danh sách giao dịch: ${e.message}")
        }
    }

    private suspend fun showSummary(): AICommandResult {
        val totalBalance = transactionViewModel.getTotalIncome() - transactionViewModel.getTotalExpense()
        val totalIncome = transactionViewModel.getTotalIncome()
        val totalExpense = transactionViewModel.getTotalExpense()

        val message = """
            📊 TỔNG QUAN TÀI CHÍNH
            
            💰 TỔNG SỐ:
            • Số dư: ${formatCurrency(totalBalance)}
            • Tổng thu: ${formatCurrency(totalIncome)}
            • Tổng chi: ${formatCurrency(totalExpense)}
            • Tiết kiệm: ${formatCurrency(totalIncome - totalExpense)}
            
            ${if (totalBalance < 0) "⚠️ CẢNH BÁO: Chi tiêu đang vượt quá thu nhập!" else "✅ Tài chính đang ổn định!"}
        """.trimIndent()

        return AICommandResult(success = true, message = message)
    }

    private suspend fun getQuickTips(): AICommandResult {
        val tips = listOf(
            "💡 Chi tiêu ít hơn 50% thu nhập cho nhu cầu thiết yếu",
            "💰 Tiết kiệm ít nhất 20% thu nhập mỗi tháng",
            "📊 Theo dõi chi tiêu hàng ngày để kiểm soát ngân sách",
            "🎯 Đặt mục tiêu tài chính ngắn hạn và dài hạn",
            "🛒 So sánh giá trước khi mua sắm lớn",
            "💳 Tránh nợ thẻ tín dụng lãi suất cao"
        )
        val randomTip = tips.random()
        return AICommandResult(success = true, message = randomTip)
    }

    private suspend fun getFinancialHealthScore(): AICommandResult {
        return try {
            val income = transactionViewModel.getTotalIncome()
            val expense = transactionViewModel.getTotalExpense()
            val balance = income - expense

            val savingsRate = if (income > 0) ((income - expense) / income * 100) else 0.0
            val expenseRatio = if (income > 0) (expense / income * 100) else 0.0

            val score = calculateHealthScore(savingsRate, expenseRatio)
            val healthLevel = getHealthLevel(score)

            val message = """
                🏥 ĐIỂM SỨC KHỎE TÀI CHÍNH: $score/100
                Mức độ: $healthLevel

                📊 CHỈ SỐ:
                • Tỷ lệ tiết kiệm: ${"%.1f".format(savingsRate)}%
                • Tỷ lệ chi tiêu: ${"%.1f".format(expenseRatio)}%
                • Số dư: ${formatCurrency(balance)}

                ${getHealthRecommendation(score, savingsRate)}
            """.trimIndent()

            AICommandResult(success = true, message = message)
        } catch (e: Exception) {
            AICommandResult(false, "Lỗi tính điểm sức khỏe: ${e.message}")
        }
    }

    private suspend fun analyzeSpending(command: AICommand.AnalyzeSpending): AICommandResult {
        return try {
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

            val totalSpending = filteredTransactions.sumOf { it.amount }
            val averageSpending = if (filteredTransactions.isNotEmpty()) totalSpending / filteredTransactions.size else 0.0

            val categoryBreakdown = filteredTransactions.groupBy { it.category }
                .mapValues { (_, trans) -> trans.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            val analysis = SpendingAnalysis(
                totalSpending = totalSpending,
                averageSpending = averageSpending,
                transactionCount = filteredTransactions.size,
                categoryBreakdown = categoryBreakdown,
                period = command.period
            )

            val message = """
            📈 PHÂN TÍCH CHI TIÊU ${command.period.uppercase()}:
            
            💰 Tổng chi tiêu: ${formatCurrency(analysis.totalSpending)}
            📊 Chi tiêu trung bình: ${formatCurrency(analysis.averageSpending)}
            🔢 Số giao dịch: ${analysis.transactionCount}
            
            ${if (analysis.categoryBreakdown.isNotEmpty()) {
                "🏷️ TOP DANH MỤC:\n" + analysis.categoryBreakdown.take(5).joinToString("\n") {
                        (cat, amount) -> "• $cat: ${formatCurrency(amount)}"
                }
            } else ""}
        """.trimIndent()

            AICommandResult(success = true, message = message)
        } catch (e: Exception) {
            AICommandResult(false, "Lỗi phân tích chi tiêu: ${e.message}")
        }
    }

    // 🔥 CÁC PHƯƠNG THỨC HỖ TRỢ
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

    private fun buildTransactionsListMessage(transactions: List<Transaction>, period: String): String {
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }

        val periodTitle = when (period) {
            "today" -> "HÔM NAY"
            "yesterday" -> "HÔM QUA"
            "week" -> "TUẦN NÀY"
            "month" -> "THÁNG NÀY"
            else -> "GIAO DỊCH"
        }

        val header = """
            📋 DANH SÁCH GIAO DỊCH $periodTitle
            💰 Tổng thu: ${formatCurrency(totalIncome)}
            💸 Tổng chi: ${formatCurrency(totalExpense)}
            🔢 Số giao dịch: ${transactions.size}
            
        """.trimIndent()

        val transactionsText = transactions.take(10).joinToString("\n\n") { transaction ->
            buildTransactionItemText(transaction)
        }

        val footer = if (transactions.size > 10) {
            "\n\n... và ${transactions.size - 10} giao dịch khác"
        } else ""

        return header + "\n\n" + transactionsText + footer
    }

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

    // 🔥 CÁC PHƯƠNG THỨC TIỆN ÍCH
    private fun getCurrentDate(): String = dateFormatter.format(Date())

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormatter.format(calendar.time)
    }

    private fun getDayOfWeek(): String {
        val days = arrayOf("Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7")
        return days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
    }

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

    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }

    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

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

    private fun calculateHealthScore(savingsRate: Double, expenseRatio: Double): Int {
        return when {
            savingsRate >= 20 && expenseRatio <= 80 -> (90..100).random()
            savingsRate >= 10 && expenseRatio <= 90 -> (70..89).random()
            savingsRate > 0 && expenseRatio <= 100 -> (50..69).random()
            else -> (0..49).random()
        }
    }

    private fun getHealthLevel(score: Int): String {
        return when {
            score >= 80 -> "Xuất sắc ⭐⭐⭐⭐⭐"
            score >= 60 -> "Tốt ⭐⭐⭐⭐"
            score >= 40 -> "Trung bình ⭐⭐⭐"
            else -> "Cần cải thiện ⭐⭐"
        }
    }

    private fun getHealthRecommendation(score: Int, savingsRate: Double): String {
        return when {
            score >= 80 -> "🎉 Tuyệt vời! Bạn đang quản lý tài chính rất tốt. Tiếp tục duy trì!"
            score >= 60 -> "👍 Khá tốt! Có thể cải thiện bằng cách tăng tỷ lệ tiết kiệm lên 20%"
            score >= 40 -> "💡 Cần quan tâm! Hãy xem xét giảm chi tiêu không cần thiết"
            else -> "⚠️ Cần hành động! Chi tiêu đang vượt quá thu nhập. Hãy lập ngân sách ngay!"
        }
    }

    private suspend fun getDailySummary(command: AICommand.GetDailySummary): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Getting daily summary: $command")

            val targetDate = command.date ?: getCurrentDate()
            val dailyTransactions = transactionViewModel.transactions.value
                .filter { it.date == targetDate }

            val dailyIncome = dailyTransactions.filter { it.isIncome }.sumOf { it.amount }
            val dailyExpense = dailyTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val dailyBalance = dailyIncome - dailyExpense

            val topCategories = dailyTransactions
                .filter { !it.isIncome }
                .groupBy { it.category }
                .mapValues { (_, trans) -> trans.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)

            val message = """
            📊 TỔNG QUAN NGÀY ${targetDate}
            
            💰 TỔNG SỐ:
            • Thu nhập: ${formatCurrency(dailyIncome)}
            • Chi tiêu: ${formatCurrency(dailyExpense)}
            • Số dư: ${formatCurrency(dailyBalance)}
            • Số giao dịch: ${dailyTransactions.size}
            
            ${if (topCategories.isNotEmpty()) {
                "🏆 TOP CHI TIÊU:\n" + topCategories.joinToString("\n") {
                        (cat, amount) -> "• $cat: ${formatCurrency(amount)}"
                }
            } else ""}
            
            ${if (dailyBalance < 0) "⚠️ CẢNH BÁO: Chi tiêu vượt quá thu nhập hôm nay!"
            else if (dailyBalance > 0) "✅ Tuyệt vời! Bạn đang có số dư dương."
            else "➖ Cân bằng thu chi."}
        """.trimIndent()

            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error getting daily summary: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy tổng quan ngày: ${e.message}")
        }
    }

    private suspend fun exportTransactions(command: AICommand.ExportTransactions): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Exporting transactions: $command")

            val transactions = getTransactionsForPeriod(command.period)

            if (transactions.isEmpty()) {
                return AICommandResult(
                    success = false,
                    message = "📭 Không có giao dịch nào để xuất trong khoảng thời gian này!"
                )
            }

            val exportData = buildExportData(transactions, command.format)

            AICommandResult(
                success = true,
                message = "📤 ĐÃ XUẤT DỮ LIỆU ($command.period):\n\n$exportData",
                data = exportData
            )

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error exporting transactions: ${e.message}", e)
            AICommandResult(false, "Lỗi xuất dữ liệu: ${e.message}")
        }
    }

    private suspend fun comparePeriods(command: AICommand.ComparePeriods): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Comparing periods: $command")

            val currentPeriodTransactions = getTransactionsForPeriod(command.currentPeriod)
            val previousPeriodTransactions = getTransactionsForPeriod(command.previousPeriod)

            val currentIncome = currentPeriodTransactions.filter { it.isIncome }.sumOf { it.amount }
            val currentExpense = currentPeriodTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val currentBalance = currentIncome - currentExpense

            val previousIncome = previousPeriodTransactions.filter { it.isIncome }.sumOf { it.amount }
            val previousExpense = previousPeriodTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val previousBalance = previousIncome - previousExpense

            val incomeChange = if (previousIncome > 0) ((currentIncome - previousIncome) / previousIncome * 100) else 0.0
            val expenseChange = if (previousExpense > 0) ((currentExpense - previousExpense) / previousExpense * 100) else 0.0
            val balanceChange = if (previousBalance != 0.0) ((currentBalance - previousBalance) / abs(previousBalance) * 100) else 0.0

            val message = """
            📊 SO SÁNH KỲ:
            • Hiện tại: ${command.currentPeriod.uppercase()}
            • Trước đó: ${command.previousPeriod.uppercase()}
            
            💰 THU NHẬP:
            • Hiện tại: ${formatCurrency(currentIncome)} ${getChangeSymbol(incomeChange)}${"%.1f".format(abs(incomeChange))}%
            • Trước đó: ${formatCurrency(previousIncome)}
            
            💸 CHI TIÊU:
            • Hiện tại: ${formatCurrency(currentExpense)} ${getChangeSymbol(expenseChange)}${"%.1f".format(abs(expenseChange))}%
            • Trước đó: ${formatCurrency(previousExpense)}
            
            ⚖️ SỐ DƯ:
            • Hiện tại: ${formatCurrency(currentBalance)} ${getChangeSymbol(balanceChange)}${"%.1f".format(abs(balanceChange))}%
            • Trước đó: ${formatCurrency(previousBalance)}
            
            ${getComparisonInsight(incomeChange, expenseChange, balanceChange)}
        """.trimIndent()

            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error comparing periods: ${e.message}", e)
            AICommandResult(false, "Lỗi so sánh kỳ: ${e.message}")
        }
    }

    private suspend fun searchTransactionsByKeyword(command: AICommand.SearchTransactionsByKeyword): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Searching transactions: $command")

            val allTransactions = transactionViewModel.transactions.value
            val filteredTransactions = allTransactions.filter { transaction ->
                val matchesKeyword = transaction.title.contains(command.keyword, ignoreCase = true) ||
                        transaction.category.contains(command.keyword, ignoreCase = true) ||
                        (transaction.description?.contains(command.keyword, ignoreCase = true) == true)

                val matchesPeriod = if (command.period != null) {
                    when (command.period) {
                        "today" -> transaction.date == getCurrentDate()
                        "week" -> isInCurrentWeek(transaction.date)
                        "month" -> isInCurrentMonth(transaction.date)
                        else -> true
                    }
                } else true

                matchesKeyword && matchesPeriod
            }.sortedByDescending { parseDate(it.date) }

            if (filteredTransactions.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "🔍 Không tìm thấy giao dịch nào với từ khóa '${command.keyword}'${if (command.period != null) " trong ${command.period}" else ""}"
                )
            }

            val message = buildSearchResultsMessage(filteredTransactions, command.keyword, command.period)
            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error searching transactions: ${e.message}", e)
            AICommandResult(false, "Lỗi tìm kiếm giao dịch: ${e.message}")
        }
    }

    private suspend fun createBudget(command: AICommand.CreateBudget): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Creating budget: $command")

            // Tìm category
            val category = categoryViewModel.getCategoryById(command.categoryId)
            if (category == null) {
                return AICommandResult(
                    success = false,
                    message = "❌ Không tìm thấy danh mục với ID: ${command.categoryId}"
                )
            }

            // Chuyển đổi periodType string sang enum
            val periodType = when (command.periodType.lowercase()) {
                "week", "tuần" -> BudgetPeriodType.WEEK
                "month", "tháng" -> BudgetPeriodType.MONTH
                "quarter", "quý" -> BudgetPeriodType.QUARTER
                "year", "năm" -> BudgetPeriodType.YEAR
                else -> BudgetPeriodType.MONTH
            }

            val startDate = LocalDate.now()
            val endDate = calculateBudgetEndDate(startDate, periodType)

            // Tạo budget object
            val budget = Budget(
                id = UUID.randomUUID().toString(),
                categoryId = command.categoryId,
                amount = command.amount,
                periodType = periodType,
                startDate = startDate,
                endDate = endDate,
                note = command.note,
                spentAmount = 0.0,
                isActive = true
            )

            // Gọi ViewModel
            budgetViewModel.addBudget(budget)

            delay(500)

            AICommandResult(
                success = true,
                message = "✅ Đã tạo ngân sách ${formatCurrency(command.amount)} ${periodType.getDisplayName()} cho '${category.name}'"
            )

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error creating budget: ${e.message}", e)
            AICommandResult(false, "Lỗi tạo ngân sách: ${e.message}")
        }
    }

    private suspend fun updateBudget(command: AICommand.UpdateBudget): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Updating budget: $command")

            val budgets = budgetViewModel.budgets.value
            val budgetToUpdate = if (command.budgetId != null) {
                budgets.find { it.id == command.budgetId }
            } else if (command.categoryId != null) {
                budgets.find { it.categoryId == command.categoryId && it.isActive }
            } else {
                null
            }

            if (budgetToUpdate == null) {
                return AICommandResult(
                    success = false,
                    message = "❌ Không tìm thấy ngân sách để cập nhật!"
                )
            }

            val updatedAmount = command.newAmount ?: budgetToUpdate.amount

            // Gọi ViewModel - SỬA THEO ĐÚNG PHƯƠNG THỨC CÓ SẴN
            budgetViewModel.updateFullBudget(
                budgetToUpdate.copy(amount = updatedAmount)
            )

            delay(500)

            val category = categoryViewModel.getCategoryById(budgetToUpdate.categoryId)
            val categoryName = category?.name ?: budgetToUpdate.categoryId

            AICommandResult(
                success = true,
                message = "✅ Đã cập nhật ngân sách cho '$categoryName' thành ${formatCurrency(updatedAmount)}"
            )

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error updating budget: ${e.message}", e)
            AICommandResult(false, "Lỗi cập nhật ngân sách: ${e.message}")
        }
    }

    private suspend fun deleteBudget(command: AICommand.DeleteBudget): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Deleting budget: $command")

            val budgets = budgetViewModel.budgets.value
            val budgetToDelete = if (command.budgetId != null) {
                budgets.find { it.id == command.budgetId }
            } else if (command.categoryId != null) {
                budgets.find { it.categoryId == command.categoryId && it.isActive }
            } else {
                null
            }

            if (budgetToDelete == null) {
                return AICommandResult(
                    success = false,
                    message = "❌ Không tìm thấy ngân sách để xóa!"
                )
            }

            // Gọi ViewModel
            budgetViewModel.deleteBudget(budgetToDelete.id)

            delay(500)

            val category = categoryViewModel.getCategoryById(budgetToDelete.categoryId)
            val categoryName = category?.name ?: budgetToDelete.categoryId

            AICommandResult(
                success = true,
                message = "✅ Đã xóa ngân sách cho '$categoryName'"
            )

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error deleting budget: ${e.message}", e)
            AICommandResult(false, "Lỗi xóa ngân sách: ${e.message}")
        }
    }

    private suspend fun createBudgetFromSet(command: AICommand.SetBudget): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Creating budget from set: $command")

            // Tìm category bằng name
            val category = categoryViewModel.findCategoryByName(command.category)
            if (category == null) {
                return AICommandResult(
                    success = false,
                    message = "❌ Không tìm thấy danh mục '${command.category}'. Hãy tạo danh mục trước!"
                )
            }

            // Chuyển đổi periodType
            val periodType = when (command.period.lowercase()) {
                "week", "tuần" -> BudgetPeriodType.WEEK
                "month", "tháng" -> BudgetPeriodType.MONTH
                "quarter", "quý" -> BudgetPeriodType.QUARTER
                "year", "năm" -> BudgetPeriodType.YEAR
                else -> BudgetPeriodType.MONTH
            }

            val startDate = LocalDate.now()
            val endDate = calculateBudgetEndDate(startDate, periodType)

            // Tạo budget object
            val budget = Budget(
                id = UUID.randomUUID().toString(),
                categoryId = category.id,
                amount = command.amount,
                periodType = periodType,
                startDate = startDate,
                endDate = endDate,
                note = "Tạo bởi AI Assistant",
                spentAmount = 0.0,
                isActive = true
            )

            // Gọi ViewModel
            budgetViewModel.addBudget(budget)

            delay(500)

            AICommandResult(
                success = true,
                message = "✅ Đã đặt ngân sách ${formatCurrency(command.amount)} ${periodType.getDisplayName()} cho '${command.category}'"
            )

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error creating budget from set: ${e.message}", e)
            AICommandResult(false, "Lỗi đặt ngân sách: ${e.message}")
        }
    }

    private suspend fun getBudgetStatus(command: AICommand.GetBudgetStatus): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Getting budget status: $command")

            val budgets = budgetViewModel.budgets.value
            val filteredBudgets = if (command.categoryId != null) {
                budgets.filter { it.categoryId == command.categoryId }
            } else {
                budgets
            }

            if (filteredBudgets.isEmpty()) {
                return AICommandResult(
                    success = true,
                    message = "📭 Không có ngân sách nào để hiển thị!"
                )
            }

            val message = buildBudgetStatusMessage(filteredBudgets)
            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error getting budget status: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy trạng thái ngân sách: ${e.message}")
        }
    }

    private suspend fun getSpendingForecast(command: AICommand.GetSpendingForecast): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Getting spending forecast: $command")

            val historicalData = getHistoricalSpendingData(command.period)
            val forecast = calculateSpendingForecast(historicalData)

            val message = """
            🔮 DỰ BÁO CHI TIÊU ${command.period.uppercase()}:
            
            💰 ƯỚC TÍNH:
            • Chi tiêu dự kiến: ${formatCurrency(forecast.estimatedSpending)}
            • Khoảng dao động: ${formatCurrency(forecast.lowerBound)} - ${formatCurrency(forecast.upperBound)}
            • Độ tin cậy: ${forecast.confidenceLevel}%
            
            💡 KIẾN NGHỊ:
            ${forecast.recommendations.joinToString("\n") { "• $it" }}
            
            ${if (forecast.warning.isNotEmpty()) "⚠️ ${forecast.warning}" else ""}
        """.trimIndent()

            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error getting spending forecast: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy dự báo chi tiêu: ${e.message}")
        }
    }

    private suspend fun getBudgetRecommendations(command: AICommand.GetBudgetRecommendations): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Getting budget recommendations: $command")

            val income = command.income ?: transactionViewModel.getTotalIncome()
            val pattern = analyzeSpendingPatterns()
            val recommendations = generateBudgetRecommendations(income, pattern)

            val message = """
            🎯 GỢI Ý NGÂN SÁCH:
            
            📊 PHÂN BỔ LÝ TƯỚNG:
            ${recommendations.allocation.joinToString("\n") { "• $it" }}
            
            🎯 MỤC TIÊU:
            ${recommendations.goals.joinToString("\n") { "• $it" }}
            
            💡 LỜI KHUYÊN:
            ${recommendations.advice.joinToString("\n") { "• $it" }}
        """.trimIndent()

            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error getting budget recommendations: ${e.message}", e)
            AICommandResult(false, "Lỗi lấy gợi ý ngân sách: ${e.message}")
        }
    }

    private suspend fun analyzeSpendingTrend(command: AICommand.AnalyzeSpendingTrend): AICommandResult {
        return try {
            Log.d("AICommandExecutor", "Analyzing spending trend: $command")

            val currentData = getTransactionsForPeriod(command.period)
            val previousData = getPreviousPeriodData(command.period)

            val trendAnalysis = performTrendAnalysis(currentData, previousData, command.compareWithPrevious)

            val message = """
            📈 PHÂN TÍCH XU HƯỚNG CHI TIÊU
            
            🔍 XU HƯỚNG CHÍNH:
            ${trendAnalysis.mainTrends.joinToString("\n") { "• $it" }}
            
            📊 BIẾN ĐỘNG:
            ${trendAnalysis.changes.joinToString("\n") { "• $it" }}
            
            🎯 DẤU HIỆU:
            ${trendAnalysis.signals.joinToString("\n") { "• $it" }}
            
            💡 HÀNH ĐỘNG:
            ${trendAnalysis.actions.joinToString("\n") { "• $it" }}
        """.trimIndent()

            AICommandResult(success = true, message = message)

        } catch (e: Exception) {
            Log.e("AICommandExecutor", "Error analyzing spending trend: ${e.message}", e)
            AICommandResult(false, "Lỗi phân tích xu hướng: ${e.message}")
        }
    }

    // 🔥 CÁC PHƯƠNG THỨC HỖ TRỢ MỚI
    private fun buildExportData(transactions: List<Transaction>, format: String): String {
        return when (format.lowercase()) {
            "csv" -> buildCSVExport(transactions)
            "json" -> buildJSONExport(transactions)
            else -> buildTextExport(transactions)
        }
    }

    private fun buildCSVExport(transactions: List<Transaction>): String {
        val header = "Ngày,Loại,Danh mục,Số tiền,Mô tả,Ví"
        val rows = transactions.joinToString("\n") { transaction ->
            "${transaction.date},${if (transaction.isIncome) "Thu" else "Chi"},${transaction.category},${transaction.amount},${transaction.description ?: ""},${transaction.wallet}"
        }
        return "$header\n$rows"
    }

    private fun buildJSONExport(transactions: List<Transaction>): String {
        val jsonArray = transactions.joinToString(",\n    ") { transaction ->
            """
        {
            "date": "${transaction.date}",
            "type": "${if (transaction.isIncome) "income" else "expense"}",
            "category": "${transaction.category}",
            "amount": ${transaction.amount},
            "description": "${transaction.description ?: ""}",
            "wallet": "${transaction.wallet}"
        }
        """.trimIndent()
        }
        return "[\n    $jsonArray\n]"
    }

    private fun buildTextExport(transactions: List<Transaction>): String {
        return transactions.joinToString("\n\n") { transaction ->
            """
            ${if (transaction.isIncome) "📥 THU" else "📤 CHI"} ${transaction.title}
            • Số tiền: ${formatCurrency(transaction.amount)}
            • Danh mục: ${transaction.category}
            • Ngày: ${transaction.date}
            • Ví: ${transaction.wallet}
            ${if (transaction.description != null) "• Mô tả: ${transaction.description}" else ""}
        """.trimIndent()
        }
    }

    private fun buildSearchResultsMessage(transactions: List<Transaction>, keyword: String, period: String?): String {
        val totalAmount = transactions.sumOf { it.amount }
        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = transactions.filter { !it.isIncome }.sumOf { it.amount }

        val periodInfo = if (period != null) " trong $period" else ""

        val header = """
        🔍 KẾT QUẢ TÌM KIẾM: '$keyword'$periodInfo
        📊 Tổng số: ${transactions.size} giao dịch
        💰 Tổng tiền: ${formatCurrency(totalAmount)}
        📥 Thu: ${formatCurrency(income)} • 📤 Chi: ${formatCurrency(expense)}
        
    """.trimIndent()

        val transactionsText = transactions.take(10).joinToString("\n\n") { transaction ->
            buildTransactionItemText(transaction)
        }

        val footer = if (transactions.size > 10) {
            "\n\n... và ${transactions.size - 10} giao dịch khác"
        } else ""

        return header + "\n\n" + transactionsText + footer
    }

    private fun buildBudgetStatusMessage(budgets: List<Budget>): String {
        val activeBudgets = budgets.filter { it.isActive }
        val overBudget = activeBudgets.count { it.isOverBudget }
        val nearBudget = activeBudgets.count { it.spentAmount / it.amount >= 0.8 && !it.isOverBudget }
        val safeBudgets = activeBudgets.count { it.spentAmount / it.amount < 0.8 }

        val criticalBudgets = activeBudgets
            .filter { it.isOverBudget }
            .joinToString("\n") { budget ->
                val category = categoryViewModel.getCategoryById(budget.categoryId)
                "• ${category?.name ?: budget.categoryId}: Vượt ${formatCurrency(budget.spentAmount - budget.amount)}"
            }

        return """
        TRẠNG THÁI NGÂN SÁCH
        
        TỔNG QUAN:
        • Tổng số: ${activeBudgets.size} ngân sách đang hoạt động
        • Vượt ngân sách: $overBudget
        • Sắp vượt: $nearBudget
        • An toàn: $safeBudgets
        
        ${if (criticalBudgets.isNotEmpty()) "CẢNH BÁO VƯỢT NGÂN SÁCH:\n$criticalBudgets" else "✅ Tất cả ngân sách đang trong tầm kiểm soát!"}
        
        KIẾN NGHỊ:
        ${if (overBudget > 0) "• Xem xét điều chỉnh ngân sách cho các danh mục vượt" else ""}
        ${if (nearBudget > 0) "• Theo dõi sát các danh mục sắp vượt ngân sách" else ""}
        ${if (safeBudgets == activeBudgets.size) "• Tiếp tục duy trì thói quen chi tiêu tốt!" else ""}
    """.trimIndent()
    }

    private fun getChangeSymbol(change: Double): String {
        return when {
            change > 0 -> "↗️"
            change < 0 -> "↘️"
            else -> "➡️"
        }
    }

    private fun getComparisonInsight(incomeChange: Double, expenseChange: Double, balanceChange: Double): String {
        return when {
            incomeChange > 10 && expenseChange < 5 -> "Xuất sắc! Thu nhập tăng mạnh trong khi chi tiêu được kiểm soát"
            incomeChange > 0 && expenseChange < 0 -> "Tốt! Thu nhập tăng, chi tiêu giảm"
            incomeChange < 0 && expenseChange > 0 -> "Cảnh báo! Thu nhập giảm, chi tiêu tăng"
            balanceChange > 0 -> "Số dư được cải thiện"
            balanceChange < 0 -> "Số dư giảm, cần xem xét"
            else -> "➖ Tình hình ổn định"
        }
    }

    private fun getHistoricalSpendingData(period: String): List<Double> {
        val periods = when (period) {
            "week" -> 8
            "month" -> 6
            "year" -> 3
            else -> 4
        }

        return List(periods) { index ->
            // Giả lập dữ liệu lịch sử - trong thực tế cần lấy từ database
            Random.nextDouble(1000000.0, 5000000.0)
        }
    }

    private fun calculateSpendingForecast(historicalData: List<Double>): SpendingForecast {
        val avg = historicalData.average()
        val stdDev = calculateStandardDeviation(historicalData)

        return SpendingForecast(
            estimatedSpending = avg,
            lowerBound = avg - stdDev,
            upperBound = avg + stdDev,
            confidenceLevel = 75,
            recommendations = listOf(
                "Dự trữ thêm 10-15% cho chi phí phát sinh",
                "Theo dõi các danh mục chi tiêu lớn",
                "Xem xét cắt giảm chi phí không cần thiết"
            ),
            warning = if (stdDev / avg > 0.3) "Chi tiêu có biến động lớn, cần thận trọng" else ""
        )
    }

    private fun analyzeSpendingPatterns(): SpendingPattern {
        val transactions = transactionViewModel.transactions.value
        val monthlySpending = transactions
            .filter { !it.isIncome }
            .groupBy { it.date.substring(3) } // Nhóm theo tháng
            .mapValues { (_, trans) -> trans.sumOf { it.amount } }

        val categoryPattern = transactions
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, trans) -> trans.size to trans.sumOf { it.amount } }

        return SpendingPattern(
            monthlyAverages = monthlySpending.values.average(),
            seasonalTrend = detectSeasonalTrend(monthlySpending),
            topCategories = categoryPattern.toList().sortedByDescending { it.second.second }.take(5),
            consistencyScore = calculateConsistencyScore(monthlySpending.values.toList())
        )
    }

    private fun generateBudgetRecommendations(income: Double, pattern: SpendingPattern): BudgetRecommendations {
        val essentialPercent = 0.5
        val wantsPercent = 0.3
        val savingsPercent = 0.2

        return BudgetRecommendations(
            allocation = listOf(
                "Nhu cầu thiết yếu (50%): ${formatCurrency(income * essentialPercent)}",
                "Mong muốn cá nhân (30%): ${formatCurrency(income * wantsPercent)}",
                "Tiết kiệm & Đầu tư (20%): ${formatCurrency(income * savingsPercent)}"
            ),
            goals = listOf(
                "Xây dựng quỹ khẩn cấp 3-6 tháng",
                "Tối ưu hóa chi tiêu cho ${pattern.topCategories.first().first}",
                "Tăng tỷ lệ tiết kiệm lên 25%"
            ),
            advice = listOf(
                "Tập trung vào các danh mục chi tiêu lớn nhất",
                "Tự động hóa tiết kiệm mỗi tháng",
                "Đánh giá lại ngân sách hàng quý"
            )
        )
    }

    private fun performTrendAnalysis(currentData: List<Transaction>, previousData: List<Transaction>, compare: Boolean): TrendAnalysis {
        val currentSpending = currentData.filter { !it.isIncome }.sumOf { it.amount }
        val previousSpending = previousData.filter { !it.isIncome }.sumOf { it.amount }

        val change = if (previousSpending > 0) (currentSpending - previousSpending) / previousSpending * 100 else 0.0

        return TrendAnalysis(
            mainTrends = listOf(
                if (change > 0) "Chi tiêu tăng ${"%.1f".format(change)}%" else "Chi tiêu giảm ${"%.1f".format(abs(change))}%",
                "Xu hướng ${if (change > 5) "tăng mạnh" else if (change < -5) "giảm rõ rệt" else "ổn định"}"
            ),
            changes = listOf(
                "Chi tiêu hiện tại: ${formatCurrency(currentSpending)}",
                if (compare) "Chi tiêu trước: ${formatCurrency(previousSpending)}" else "Không có dữ liệu so sánh"
            ),
            signals = listOf(
                if (change > 10) "Cần kiểm soát chi tiêu" else "Chi tiêu trong tầm kiểm soát",
                if (currentData.size > previousData.size * 1.2) "Số giao dịch tăng đáng kể" else "Tần suất giao dịch ổn định"
            ),
            actions = listOf(
                "Theo dõi các danh mục có xu hướng tăng",
                "Điều chỉnh ngân sách nếu cần",
                "Duy trì thói quen chi tiêu tốt"
            )
        )
    }

    // 🔥 CÁC PHƯƠNG THỨC HỖ TRỢ THỐNG KÊ
    private fun calculateStandardDeviation(data: List<Double>): Double {
        if (data.isEmpty()) return 0.0
        val mean = data.average()
        val variance = data.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    private fun detectSeasonalTrend(monthlySpending: Map<String, Double>): String {
        return "Ổn định" // Đơn giản hóa cho phiên bản này
    }

    private fun calculateConsistencyScore(monthlyValues: List<Double>): Int {
        if (monthlyValues.size < 2) return 100
        val avg = monthlyValues.average()
        val variance = monthlyValues.map { abs(it - avg) }.average()
        return (100 - (variance / avg * 100).toInt()).coerceIn(0, 100)
    }

    private suspend fun getPreviousPeriodData(period: String): List<Transaction> {
        return when (period) {
            "week" -> getTransactionsForPeriod("previous_week")
            "month" -> getTransactionsForPeriod("previous_month")
            else -> emptyList()
        }
    }

    private fun calculateBudgetEndDate(startDate: LocalDate, periodType: BudgetPeriodType): LocalDate {
        return when (periodType) {
            BudgetPeriodType.WEEK -> startDate.plusWeeks(1)
            BudgetPeriodType.MONTH -> startDate.plusMonths(1)
            BudgetPeriodType.QUARTER -> startDate.plusMonths(3)
            BudgetPeriodType.YEAR -> startDate.plusYears(1)
        }
    }

    // 🔥 CÁC PHƯƠNG THỨC CHƯA TRIỂN KHAI
    private suspend fun createCategory(command: AICommand.CreateCategory): AICommandResult {
        return AICommandResult(false, "Tạo danh mục đang phát triển")
    }

    private suspend fun deleteCategory(command: AICommand.DeleteCategory): AICommandResult {
        return AICommandResult(false, "Xóa danh mục đang phát triển")
    }

    private suspend fun listCategories(command: AICommand.ListCategories): AICommandResult {
        return AICommandResult(false, "Danh sách danh mục đang phát triển")
    }

    private suspend fun createRecurringExpense(command: AICommand.CreateRecurringExpense): AICommandResult {
        return AICommandResult(false, "Tạo chi tiêu định kỳ đang phát triển")
    }

    private suspend fun deleteRecurringExpense(command: AICommand.DeleteRecurringExpense): AICommandResult {
        return AICommandResult(false, "Xóa chi tiêu định kỳ đang phát triển")
    }

    private suspend fun listRecurringExpenses(command: AICommand.ListRecurringExpenses): AICommandResult {
        return AICommandResult(false, "Danh sách chi tiêu định kỳ đang phát triển")
    }

    private suspend fun toggleRecurringExpense(command: AICommand.ToggleRecurringExpense): AICommandResult {
        return AICommandResult(false, "Bật/tắt chi tiêu định kỳ đang phát triển")
    }

    private suspend fun transferMoney(command: AICommand.TransferMoney): AICommandResult {
        return AICommandResult(false, "Chuyển tiền đang phát triển")
    }
}

// ==================== AI VIEWMODEL CHÍNH - ĐÃ SỬA LỖI HOÀN CHỈNH ====================
class AIViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AIViewModel"
        private const val MAX_CALLS_PER_MINUTE = 30
        private const val MAX_CONVERSATION_HISTORY = 50
        private const val CACHE_DURATION_MS = 500000

        // 🔥 CẤU HÌNH HỆ THỐNG THÔNG BÁO
        private const val PROACTIVE_CHECK_INTERVAL = 60 * 1000L // 1 phút
        private const val MIN_TIME_BETWEEN_PROACTIVE = 2 * 60 * 1000L // 2 phút
        private const val INACTIVITY_THRESHOLD = 30 * 1000L // 30 giây
    }

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

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _aiState = MutableStateFlow(AIState.IDLE)
    val aiState: StateFlow<AIState> = _aiState

    val isAITyping = mutableStateOf(false)
    val lastError = mutableStateOf<String?>(null)

    private val generativeModel: GenerativeModel by lazy {
        try {
            GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo Gemini: ${e.message}")
            throw e
        }
    }

    private val commandExecutor by lazy {
        AICommandExecutor(
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

    // 🔥 BỘ NÃO AI NÂNG CAO
    private var lastUserActivityTime = System.currentTimeMillis()
    private var lastProactiveMessageTime = 0L
    private var userBehaviorProfile = UserBehaviorProfile()
    private var lastAnalysisTime = 0L
    private val analysisInterval = 10 * 60 * 1000L
    private var proactiveMessageJob: Job? = null
    private var brainJob: Job? = null

    // 🔥 HỆ THỐNG THEO DÕI SỰ KIỆN
    private val sentEvents = mutableSetOf<String>()
    private val eventCooldowns = mutableMapOf<String, Long>()

    init {
        Log.d(TAG, "🤖 AIViewModel khởi tạo với hệ thống học hỏi thông minh")
        initializeAIChat()

        viewModelScope.launch {
            connectDataSources()
            loadInitialInsights()
            startAIBrain()
        }
    }

    // 🔥 HỆ THỐNG BỘ NÃO AI - ĐÃ SỬA LỖI
    private fun startAIBrain() {
        brainJob = viewModelScope.launch {
            delay(5000) // Đợi 5 giây sau khi khởi động

            Log.d(TAG, "🧠 AI Brain đã khởi động!")

            // Gửi lời chào ban đầu
            pushProactiveMessage("🤖 Chào bạn! Tôi là WendyAI. Tôi sẽ giúp bạn quản lý tài chính thông minh hơn!")

            while (isActive) {
                try {
                    Log.d(TAG, "🧠 AI Brain: Đang kiểm tra điều kiện gửi tin nhắn...")

                    // 1. Tính thời gian không hoạt động
                    val timeSinceLastActivity = System.currentTimeMillis() - lastUserActivityTime

                    // 2. Kiểm tra điều kiện gửi tin nhắn chủ động
                    if (shouldSendProactiveMessage(timeSinceLastActivity)) {
                        Log.d(TAG, "🎯 Đủ điều kiện, bắt đầu gửi tin nhắn chủ động...")
                        sendProactiveMessage()
                    } else {
                        Log.d(TAG, "⏸️ Chưa đủ điều kiện gửi tin nhắn chủ động")
                    }

                    // 3. Phân tích tài chính định kỳ
                    if (System.currentTimeMillis() - lastAnalysisTime > analysisInterval) {
                        analyzeFinancialSituation()
                        lastAnalysisTime = System.currentTimeMillis()
                    }

                    // 4. Kiểm tra sự kiện đặc biệt
                    checkForSpecialEvents()

                    // 5. Đợi trước khi kiểm tra lại
                    Log.d(TAG, "⏳ Đợi 1 phút trước khi kiểm tra lại...")
                    delay(PROACTIVE_CHECK_INTERVAL)

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Lỗi AI Brain: ${e.message}")
                    delay(30 * 1000L) // Đợi 30s nếu có lỗi
                }
            }
        }
    }

    // 🔥 KIỂM TRA ĐIỀU KIỆN GỬI TIN NHẮN CHỦ ĐỘNG
    private fun shouldSendProactiveMessage(timeSinceLastActivity: Long): Boolean {
        Log.d(TAG, "📊 Kiểm tra điều kiện proactive...")

        // 1. AI không bận xử lý
        if (_aiState.value == AIState.PROCESSING) {
            Log.d(TAG, "❌ AI đang bận")
            return false
        }

        // 2. Có ít nhất 1 tin nhắn trong lịch sử (trừ tin nhắn chào)
        if (_messages.size <= 1) {
            Log.d(TAG, "❌ Chưa đủ tin nhắn: ${_messages.size}")
            return false
        }

        // 3. Người dùng không hoạt động ít nhất 30 giây
        if (timeSinceLastActivity < INACTIVITY_THRESHOLD) {
            Log.d(TAG, "❌ Người dùng vừa hoạt động: ${timeSinceLastActivity/1000}s trước")
            return false
        }

        // 4. Không gửi quá thường xuyên (ít nhất 2 phút giữa các lần)
        val timeSinceLastProactive = System.currentTimeMillis() - lastProactiveMessageTime
        if (timeSinceLastProactive < MIN_TIME_BETWEEN_PROACTIVE) {
            Log.d(TAG, "❌ Vừa gửi tin nhắn: ${timeSinceLastProactive/1000}s trước")
            return false
        }

        // 5. Tin nhắn cuối cùng không phải là proactive của AI
        val lastMessage = _messages.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser && lastMessage.isProactive) {
            Log.d(TAG, "❌ Tin nhắn cuối đã là proactive")
            return false
        }

        // 6. Thêm yếu tố ngẫu nhiên để không đoán trước được (50% cơ hội)
        val randomChance = Random.nextInt(100)
        if (randomChance < 50) {
            Log.d(TAG, "✅ Random check passed: $randomChance >= 50")
            Log.d(TAG, "✅ Đủ tất cả điều kiện gửi tin nhắn chủ động!")
            return true
        }

        Log.d(TAG, "❌ Random check failed: $randomChance < 50")
        return false
    }

    // 🔥 GỬI TIN NHẮN CHỦ ĐỘNG
    private suspend fun sendProactiveMessage() {
        try {
            Log.d(TAG, "🎯 Bắt đầu gửi tin nhắn chủ động...")

            // 1. Phân tích context người dùng
            val context = analyzeUserContext()
            Log.d(TAG, "📊 Context: balance=${formatCurrency(context.balance)}, hasOverBudget=${context.hasOverBudget}")

            // 2. Tạo tin nhắn phù hợp
            val message = generateProactiveMessageByPriority(context)

            if (message != null) {
                Log.d(TAG, "📝 Đã tạo tin nhắn: ${message.take(50)}...")

                // 3. Delay tự nhiên (1-3 giây)
                val randomDelay = Random.nextLong(1000, 3000)
                Log.d(TAG, "⏳ Đợi ${randomDelay}ms trước khi gửi...")
                delay(randomDelay)

                // 4. Gửi tin nhắn
                pushProactiveMessage(message)

                // 5. Cập nhật thời gian
                lastProactiveMessageTime = System.currentTimeMillis()
                userBehaviorProfile.totalInteractions++

                Log.d(TAG, "✅ Đã gửi tin nhắn chủ động thành công!")
            } else {
                Log.d(TAG, "❌ Không tạo được tin nhắn phù hợp")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi gửi tin nhắn chủ động: ${e.message}", e)
        }
    }

    private suspend fun generateProactiveMessageByPriority(context: ProactiveContext): String? {
        val messages = mutableListOf<Pair<Int, suspend () -> String?>>() // Thay đổi đây

        // Ưu tiên 1: Cảnh báo tài chính (40%)
        messages.add(40 to { generateFinancialAlertMessage(context) })

        // Ưu tiên 2: Theo thời gian (20%)
        messages.add(20 to { generateTimeBasedMessage(context) })

        // Ưu tiên 3: Theo hành vi (15%)
        messages.add(15 to { generateBehaviorBasedMessage(context) })

        // Ưu tiên 4: Giáo dục (10%)
        messages.add(10 to { generateEducationalMessage() })

        // Ưu tiên 5: Ngẫu nhiên (15%)
        messages.add(15 to { generateRandomTip() })

        // Sắp xếp và chọn
        for ((weight, generator) in messages.sortedByDescending { it.first }) {
            if (Random.nextInt(100) < weight) {
                val message = generator() // Bây giờ có thể gọi suspend function
                if (message != null) {
                    Log.d(TAG, "🎲 Chọn tin nhắn với weight: $weight")
                    return message
                }
            }
        }
        return null
    }

    // 🔥 PHÂN TÍCH CONTEXT NGƯỜI DÙNG
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
            val recentMessages = _messages.takeLast(10)

            val lastUserMessage = recentMessages.findLast { it.isUser }?.text?.lowercase() ?: ""

            // Tính toán dữ liệu tài chính
            val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
            val balance = totalIncome - totalExpense

            val currentMonthTransactions = getCurrentMonthTransactions(transactions)
            val monthExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val monthIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }

            val activeBudgets = budgets.filter { it.isActive }
            val overBudgetCategories = activeBudgets.filter { it.isOverBudget }

            // Lấy thông tin sở thích
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

    // 🔥 CÁC LOẠI TIN NHẮN CHỦ ĐỘNG
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
                return "🔴 CHÚ Ý: Chi tiêu của bạn đang vượt quá thu nhập. Cần xem xét lại ngân sách!"
            }

            if (context.monthExpense > context.monthIncome * 0.8 && context.monthIncome > 0) {
                return "🟡 LƯU Ý: Bạn đang chi tiêu ${(context.monthExpense/context.monthIncome*100).toInt()}% thu nhập. Mục tiêu lý tưởng là dưới 80%!"
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tạo thông báo tài chính: ${e.message}")
            null
        }
    }

    private fun generateTimeBasedMessage(context: ProactiveContext): String? {
        return when (context.currentHour) {
            in 6..9 -> "🌞 Chào buổi sáng! Bạn đã sẵn sàng cho một ngày tài chính thông minh chưa?"
            in 11..13 -> "🍽️ Đến giờ ăn trưa! Đây là thời điểm tốt để kiểm tra ngân sách ăn uống."
            in 17..19 -> "🌆 Cuối ngày rồi! Bạn có muốn xem tổng kết chi tiêu hôm nay không?"
            in 20..23 -> "🌙 Buổi tối yên tĩnh là thời điểm hoàn hảo để lên kế hoạch tài chính!"
            else -> null
        }
    }

    private fun generateBehaviorBasedMessage(context: ProactiveContext): String? {
        return when {
            context.lastUserMessage.contains("chi tiêu") ->
                "💡 Tôi thấy bạn quan tâm đến chi tiêu. Bạn có muốn phân tích chi tiêu theo danh mục không?"

            context.lastUserMessage.contains("ngân sách") ->
                "🎯 Dựa trên thu nhập của bạn, tôi có thể gợi ý ngân sách phù hợp. Muốn thử không?"

            context.lastUserMessage.contains("tiết kiệm") ->
                "💰 Tôi có một số mẹo tiết kiệm hiệu quả. Bạn có muốn nghe không?"

            context.userEngagementLevel > 7 ->
                "👏 Tôi thấy bạn rất tích cực quản lý tài chính! Hãy tiếp tục phát huy!"

            else -> null
        }
    }

    private fun generateEducationalMessage(): String? {
        val tips = listOf(
            "📊 **Mẹo hay**: Luôn theo dõi chi tiêu nhỏ - chúng có thể chiếm tới 30% ngân sách!",
            "💎 **Nguyên tắc 50/30/20**: 50% cho nhu cầu, 30% cho muốn, 20% cho tiết kiệm!",
            "🔔 **Nhắc nhở**: Đặt ngân sách cho từng danh mục giúp kiểm soát chi tiêu tốt hơn!",
            "🎯 **Chiến lược**: Xem lại chi tiêu cuối tuần giúp bạn điều chỉnh kịp thời!",
            "💡 **Bí quyết**: Sử dụng tính năng phân tích để hiểu rõ thói quen chi tiêu!"
        )
        return tips.random()
    }

    private fun generateRandomTip(): String? {
        val tips = listOf(
            "Bạn có biết: Ghi chép chi tiêu hàng ngày giúp tiết kiệm thêm 15-20% ngân sách?",
            "Mẹo hay: Đặt ngân sách riêng cho từng danh mục giúp kiểm soát chi tiêu tốt hơn!",
            "Hãy thử: Xem lại chi tiêu cuối tuần để điều chỉnh kịp thời!",
            "Bí quyết: Tự động hóa tiết kiệm giúp bạn không quên mục tiêu tài chính!",
            "Nguyên tắc 50/30/20: 50% nhu cầu, 30% mong muốn, 20% tiết kiệm!"
        )
        return tips.random()
    }

    // 🔥 PHÂN TÍCH TÌNH HÌNH TÀI CHÍNH
    private suspend fun analyzeFinancialSituation() {
        try {
            Log.d(TAG, "🔥 AI Brain: Đang phân tích tình hình tài chính...")

            val transactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
            }
            if (transactions.isEmpty()) return

            val currentMonthTransactions = getCurrentMonthTransactions(transactions)
            val monthExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val monthIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }

            val lastMonthTransactions = getLastMonthTransactions(transactions)
            val lastMonthExpense = lastMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }

            // Phát hiện xu hướng
            if (monthExpense > lastMonthExpense * 1.2 && lastMonthExpense > 0) {
                val increasePercent = ((monthExpense - lastMonthExpense) / lastMonthExpense * 100).toInt()
                pushProactiveMessage("📈 TÔI NHẬN THẤY: Chi tiêu tháng này tăng $increasePercent% so với tháng trước. Có điều gì đặc biệt không?")
            }

            if (monthIncome > 0 && monthExpense / monthIncome < 0.5) {
                pushProactiveMessage("💰 TUYỆT VỜI! Bạn đang tiết kiệm được hơn 50% thu nhập. Đây là mức rất tốt!")
            }

            // Phân tích danh mục chi tiêu
            val categoryAnalysis = currentMonthTransactions
                .filter { !it.isIncome }
                .groupBy { it.category }
                .mapValues { (_, trans) -> trans.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            if (categoryAnalysis.isNotEmpty()) {
                val topCategory = categoryAnalysis.first()
                trackUserPreference("favorite_category", topCategory.first)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích tài chính: ${e.message}")
        }
    }

    // 🔥 KIỂM TRA SỰ KIỆN ĐẶC BIỆT
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

    // 🔥 QUẢN LÝ SỰ KIỆN
    private fun hasSentEventToday(eventId: String): Boolean = sentEvents.contains(eventId)
    private fun markEventSent(eventId: String) { sentEvents.add(eventId) }

    // 🔥 CẬP NHẬT PROFILE NGƯỜI DÙNG
    private fun updateUserBehaviorProfile() {
        userBehaviorProfile.lastActiveTime = System.currentTimeMillis()
        userBehaviorProfile.totalInteractions++

        val recentActivity = _messages.count {
            System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000
        }
        userBehaviorProfile.engagementScore = when {
            recentActivity > 15 -> 10
            recentActivity > 10 -> 8
            recentActivity > 5 -> 6
            recentActivity > 2 -> 4
            else -> 2
        }
    }

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

    // 🔥 HỌC HỎI VÀ GHI NHỚ
    private fun trackUserPreference(type: String, value: String) {
        when (type) {
            "favorite_category" -> {
                userBehaviorProfile.preferredCategories.add(value)
                Log.d(TAG, "📝 Đã ghi nhận danh mục yêu thích: $value")
            }
            "common_command" -> {
                userBehaviorProfile.commonCommands[value] =
                    userBehaviorProfile.commonCommands.getOrDefault(value, 0) + 1
                Log.d(TAG, "📝 Đã ghi nhận lệnh thường dùng: $value")
            }
        }
    }

    // 🔥 ĐẨY TIN NHẮN TỨC THÌ
    private fun pushProactiveMessage(text: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📤 Đang đẩy tin nhắn: ${text.take(50)}...")

                if (_aiState.value == AIState.PROCESSING) {
                    Log.w(TAG, "⚠️ Bỏ qua vì AI đang xử lý")
                    return@launch
                }

                val message = ChatMessage(
                    text = text,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isProactive = true
                )

                _messages.add(message)
                lastProactiveMessageTime = System.currentTimeMillis()

                Log.d(TAG, "✅ Đã thêm tin nhắn chủ động vào danh sách")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi pushProactiveMessage: ${e.message}")
            }
        }
    }

    // 🔥 THÔNG BÁO TỪ SỰ KIỆN BÊN NGOÀI
    fun triggerProactiveMessage(trigger: String) {
        viewModelScope.launch {
            Log.d(TAG, "🔔 Trigger proactive message: $trigger")

            val message = when (trigger) {
                "new_transaction" -> "📥 Bạn vừa thêm giao dịch mới. Muốn xem tổng quan không?"
                "budget_warning" -> "⚠️ Có ngân sách sắp vượt. Cần kiểm tra ngay!"
                "low_balance" -> "💰 Số dư đang thấp. Hãy cẩn thận chi tiêu!"
                "weekend" -> "🎉 Cuối tuần rồi! Đã lên kế hoạch chi tiêu chưa?"
                else -> null
            }

            if (message != null && shouldSendProactiveMessage(Long.MAX_VALUE)) {
                pushProactiveMessage(message)
            }
        }
    }

    // ==================== CÁC PHƯƠNG THỨC CHÍNH CỦA AI ====================

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        currentJob?.cancel()

        lastUserActivityTime = System.currentTimeMillis()

        val userMessage = ChatMessage(
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.add(userMessage)
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

    private fun processWithAI(userText: String): Job {
        return viewModelScope.launch {
            try {
                _aiState.value = AIState.PROCESSING
                isAITyping.value = true
                lastError.value = null

                Log.d(TAG, "🔥 Bắt đầu xử lý AI: '$userText'")

                if (isCommand(userText)) {
                    Log.d(TAG, "🎯 Nhận diện là COMMAND")
                    val command = naturalLanguageParser.parseCommand(userText)
                    Log.d(TAG, "✅ Command parsed: ${command::class.simpleName}")

                    // Học hỏi từ lệnh
                    learnFromUserResponse(
                        ChatMessage(text = userText, isUser = true),
                        command
                    )

                    when (command) {
                        is AICommand.AddTransaction -> {
                            Log.d(TAG, "💰 Xử lý AddTransaction command")
                            val result = commandExecutor.executeCommand(command)
                            handleCommandResult(result, userText)
                        }
                        is AICommand.ListTransactions -> {
                            Log.d(TAG, "📋 Xử lý ListTransactions command")
                            val result = commandExecutor.executeCommand(command)
                            handleCommandResult(result, userText)
                        }
                        is AICommand.ShowSummary -> {
                            Log.d(TAG, "📊 Xử lý ShowSummary command")
                            val result = commandExecutor.executeCommand(command)
                            handleCommandResult(result, userText)
                        }
                        is AICommand.GetQuickTips -> {
                            Log.d(TAG, "💡 Xử lý GetQuickTips command")
                            val result = commandExecutor.executeCommand(command)
                            handleCommandResult(result, userText)
                        }
                        is AICommand.AnalyzeSpending -> {
                            Log.d(TAG, "📈 Xử lý AnalyzeSpending command")
                            val result = commandExecutor.executeCommand(command)
                            handleCommandResult(result, userText)
                        }
                        is AICommand.GetFinancialHealthScore -> {
                            Log.d(TAG, "🏥 Xử lý GetFinancialHealthScore command")
                            val result = commandExecutor.executeCommand(command)
                            handleCommandResult(result, userText)
                        }
                        else -> {
                            Log.w(TAG, "⚠️ Command chưa được hỗ trợ")
                            handleAIResponse("🤖 Tôi hiểu bạn muốn thực hiện lệnh này, nhưng tính năng đang được phát triển. Hãy thử các lệnh khác như:\n\n• Thêm chi tiêu/thu nhập\n• Xem giao dịch\n• Phân tích chi tiêu\n• Xem tổng quan tài chính")
                        }
                    }
                } else {
                    Log.d(TAG, "💬 Nhận diện là QUESTION/CONVERSATION")
                    processWithGeminiAPI(userText)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi trong processWithAI: ${e.message}", e)
                handleAIResponse("❌ Có lỗi xảy ra: ${e.message ?: "Vui lòng thử lại sau!"}")
            } finally {
                _aiState.value = AIState.IDLE
                isAITyping.value = false
            }
        }
    }

    private fun handleCommandResult(result: AICommandResult, userCommand: String) {
        if (result.success) {
            handleAIResponse(result.message)
            Log.d(TAG, "✅ Command executed successfully")
        } else {
            val errorMessage = buildErrorMessage(result.message, userCommand)
            handleAIResponse(errorMessage)
            Log.w(TAG, "❌ Command failed: ${result.message}")
        }
    }

    private suspend fun processWithGeminiAPI(userText: String) {
        try {
            Log.d(TAG, "🚀 Gọi Gemini API với prompt: ${userText.take(50)}...")

            val prompt = buildSmartPrompt(userText)

            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }

            val aiResponse = response.text ?: "Xin lỗi, tôi chưa thể trả lời câu hỏi này ngay lúc này."

            handleAIResponse(aiResponse)
            Log.d(TAG, "✅ Gemini API response received")

        } catch (e: CancellationException) {
            Log.d(TAG, "Gemini API call cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gemini API error: ${e.message}", e)
            handleAIResponse("🤖 Hiện tại tôi không thể kết nối đến AI. Bạn có thể thử các lệnh quản lý tài chính như:\n\n• 'Thêm chi tiêu 50k cho ăn uống'\n• 'Xem giao dịch hôm nay'\n• 'Phân tích chi tiêu tháng này'\n• 'Xem tổng quan tài chính'")
        }
    }

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
            - Dùng emoji phù hợp 💰📈💡
            - Cá nhân hóa dựa trên thông tin hành vi nếu có

            Hãy trả lời như một người bạn am hiểu tài chính!
        """.trimIndent()
    }

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

    private fun getUserProfileContext(): String {
        return """
            • Điểm engagement: ${userBehaviorProfile.engagementScore}/10
            • Danh mục yêu thích: ${userBehaviorProfile.preferredCategories.take(3).joinToString()}
            • Lệnh thường dùng: ${userBehaviorProfile.commonCommands.toList().sortedByDescending { it.second }.take(3).joinToString { it.first }}
            • Tổng tương tác: ${userBehaviorProfile.totalInteractions}
        """.trimIndent()
    }

    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }

    private fun isCommand(message: String): Boolean {
        val lowerMessage = message.lowercase().trim()

        val commandKeywords = listOf(
            "thêm", "tạo", "add", "create", "tao", "them",
            "chi tiêu", "chi", "mua", "thanh toán", "trả", "tốn", "tiêu",
            "thu nhập", "thu thập", "income", "lương", "thưởng", "nhận",
            "phân tích", "analytics", "thống kê", "xem", "tổng quan", "summary",
            "xem giao dịch", "xem giao dich", "liệt kê", "liet ke",
            "ngân sách", "ngan sach", "budget", "đặt ngân sách", "dat ngan sach", "set budget",
            "điểm sức khỏe", "diem suc khoe", "health score", "financial health"
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

    private fun initializeAIChat() {
        _messages.clear()
        conversationHistory.clear()

        _messages.add(
            ChatMessage(
                text = """
                🤖 WENDY AI - TRỢ LÝ TÀI CHÍNH THÔNG MINH
                
                Chào bạn! Tôi là WendyAI, trợ lý tài chính thông minh của bạn. 
                Tôi có thể giúp bạn:
                
                💰 Quản lý chi tiêu & thu nhập
                📊 Phân tích tài chính
                🎯 Đặt ngân sách
                💡 Đưa ra lời khuyên tài chính
                🧠 Học hỏi từ thói quen của bạn
                
                Hãy thử nói: "Thêm chi tiêu 50k cho ăn uống" hoặc "Xem giao dịch hôm nay"
                """.trimIndent(),
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun clearChat() {
        currentJob?.cancel()
        brainJob?.cancel()
        _messages.clear()
        conversationHistory.clear()
        lastError.value = null
        financialInsightsCache.clear()
        initializeAIChat()

        viewModelScope.launch {
            startAIBrain()
        }
    }

    fun getQuickFinancialTips(): List<String> {
        return listOf(
            "Chi tiêu ít hơn 50% thu nhập cho nhu cầu thiết yếu",
            "Tiết kiệm ít nhất 20% thu nhập mỗi tháng",
            "Theo dõi chi tiêu hàng ngày để kiểm soát ngân sách",
            "Đặt mục tiêu tài chính ngắn hạn và dài hạn"
        )
    }

    private fun handleAIResponse(response: String) {
        _messages.add(
            ChatMessage(
                text = response,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        )
        conversationHistory.add("AI: $response")
    }

    private fun buildErrorMessage(errorMessage: String, userCommand: String): String {
        val lowerCommand = userCommand.lowercase()

        val suggestion = when {
            lowerCommand.contains("ví") && errorMessage.contains("không tìm thấy") ->
                "\n💡 Gợi ý: Tính năng ví đã được đơn giản hóa trong phiên bản này"
            lowerCommand.contains("danh mục") && errorMessage.contains("không tìm thấy") ->
                "\n💡 Gợi ý: Hãy tạo danh mục trước bằng lệnh 'Tạo danh mục Ăn uống'"
            lowerCommand.contains("ngân sách") && errorMessage.contains("không tìm thấy") ->
                "\n💡 Gợi ý: Hãy tạo ngân sách bằng lệnh 'Đặt ngân sách 1 triệu cho Ăn uống'"
            errorMessage.contains("số tiền") || errorMessage.contains("amount") ->
                "\n💡 Gợi ý: Hãy nói rõ số tiền, ví dụ: 'Thêm chi tiêu 50 nghìn cho ăn uống'"
            else -> ""
        }

        return "❌ $errorMessage$suggestion"
    }

    private fun canMakeApiCall(): Boolean {
        val now = System.currentTimeMillis()
        apiCallTimes.removeAll { it < now - TimeUnit.MINUTES.toMillis(1) }
        return apiCallTimes.size < MAX_CALLS_PER_MINUTE
    }

    private fun showRateLimitMessage() {
        pushProactiveMessage("⏳ Bạn đang gửi tin nhắn hơi nhanh đó! Đợi tôi xíu rồi tiếp tục nhé!")
    }

    private fun showAIBusyMessage() {
        pushProactiveMessage("🤔 Tôi đang suy nghĩ về câu hỏi trước của bạn... Đợi xíu nhé!")
    }

    // 🔥 CÁC PHƯƠNG THỨC HỖ TRỢ DỮ LIỆU
    private suspend fun connectDataSources() {
        try {
            coroutineScope {
                launch {
                    transactionViewModel.transactions.collect { transactions ->
                        updateFinanceSummary(transactions)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kết nối dữ liệu: ${e.message}")
        }
    }

    private fun updateFinanceSummary(transactions: List<Transaction>) {
        try {
            lastFinanceSummary = null
            financialInsightsCache.clear()
            Log.d(TAG, "Dữ liệu cập nhật: ${transactions.size} giao dịch")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi cập nhật dữ liệu: ${e.message}")
        }
    }

    private suspend fun loadInitialInsights() {
        delay(1000)
        if (messages.size == 1) {
            val quickTips = getQuickFinancialTips().random()
            _messages.add(
                ChatMessage(
                    text = "💡 Mẹo nhanh: $quickTips\n\nHãy thử nhập: 'Thêm chi tiêu 50k cho ăn uống' hoặc 'Xem giao dịch hôm nay'",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // 🔥 PHƯƠNG THỨC TIỆN ÍCH
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

    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun learnFromUserResponse(message: ChatMessage, command: AICommand?) {
        if (message.isUser) {
            userBehaviorProfile.responseTimes.add(System.currentTimeMillis())

            // Ghi nhận lệnh thường dùng
            command?.let {
                trackUserPreference("common_command", it::class.simpleName ?: "unknown")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
        brainJob?.cancel()
        Log.d(TAG, "AIViewModel đã được giải phóng")
    }
}