package com.example.financeapp.viewmodel.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.remote.FirestoreService
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.ai.AICommandResult
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel : ViewModel() {

    // State flows
    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _autoTransactionMessage = MutableStateFlow<String?>(null)
    val autoTransactionMessage: StateFlow<String?> = _autoTransactionMessage.asStateFlow()

    private val _transactionAdded = MutableSharedFlow<Transaction>()
    val transactionAdded: SharedFlow<Transaction> = _transactionAdded.asSharedFlow()

    private val _aiCommandResult = MutableStateFlow<AICommandResult?>(null)
    val aiCommandResult: StateFlow<AICommandResult?> = _aiCommandResult.asStateFlow()

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    // Savings integration
    private var _savingsViewModel: SavingsViewModel? = null

    fun connectToSavingsViewModel(savingsViewModel: SavingsViewModel) {
        _savingsViewModel = savingsViewModel
    }

    fun disconnectSavingsViewModel() {
        _savingsViewModel = null
    }

    // Dependencies
    private val firestoreService = FirestoreService()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    init {
        loadTransactionsFromFirestore()
    }

    // Private helper methods
    private fun setLoading(value: Boolean) {
        _loading.value = value
    }

    private fun parseDate(dateString: String): Date {
        return try {
            dateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatVND(amount: Double): String {
        return formatVND(amount.toFloat())
    }

    private fun formatVND(amount: Float): String {
        return if (amount >= 1000000000) {
            val ty = amount / 1000000000
            String.format("%,.1f tỷ", ty).replace(",", ".")
        } else if (amount >= 1000000) {
            val trieu = amount / 1000000
            String.format("%,.0f triệu", trieu).replace(",", ".")
        } else {
            String.format("%,.0f đ", amount).replace(",", ".")
        }
    }

    private fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: "anonymous"
    }

    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    private fun getDayOfWeek(date: String): String {
        return try {
            val parsedDate = dateFormat.parse(date) ?: Date()
            dayOfWeekFormat.format(parsedDate)
        } catch (e: Exception) {
            dayOfWeekFormat.format(Date())
        }
    }

    // Tạo transaction từ recurring expense
    fun createTransactionFromRecurringExpense(recurringExpense: RecurringExpense): Transaction {
        val today = getTodayDate()
        val dayOfWeek = getDayOfWeek(today)

        return Transaction(
            id = UUID.randomUUID().toString(),
            date = today,
            dayOfWeek = dayOfWeek,
            category = recurringExpense.category,
            categoryId = recurringExpense.category,
            amount = recurringExpense.amount,
            isIncome = false,
            group = "Chi tiêu định kỳ",
            wallet = recurringExpense.wallet,
            description = recurringExpense.description ?: "Tạo tự động từ chi tiêu định kỳ",
            categoryIcon = recurringExpense.categoryIcon,
            categoryColor = recurringExpense.categoryColor,
            createdAt = System.currentTimeMillis(),
            title = recurringExpense.title,
            isAutoGenerated = true,
            recurringSourceId = recurringExpense.id
        )
    }

    // Data loading
    private fun loadTransactionsFromFirestore() {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null

            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _errorMessage.value = "Vui lòng đăng nhập để xem giao dịch"
                    setLoading(false)
                    return@launch
                }

                val firestoreTransactions = firestoreService.getTransactionsByUser(userId)

                val sortedTransactions = firestoreTransactions.sortedByDescending {
                    try { dateFormat.parse(it.date) } catch (_: Exception) { Date(0) }
                }

                _transactions.value = sortedTransactions
                updateAnalyticsData()

            } catch (e: Exception) {
                _errorMessage.value = "Không thể tải danh sách giao dịch"
                _transactions.value = emptyList()

            } finally {
                setLoading(false)
            }
        }
    }

    // CRUD operations
    fun addTransaction(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            _successMessage.value = null

            try {
                if (transaction.amount <= 0) {
                    _errorMessage.value = "Số tiền phải lớn hơn 0"
                    return@launch
                }

                if (transaction.category.isBlank()) {
                    _errorMessage.value = "Vui lòng chọn danh mục"
                    return@launch
                }

                val userId = getCurrentUserId()
                val newTransaction = transaction.copy(
                    id = if (transaction.id.isBlank()) UUID.randomUUID().toString() else transaction.id
                )

                firestoreService.saveTransaction(newTransaction, userId)

                if (!newTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(newTransaction.category, newTransaction.amount)
                }

                val currentList = _transactions.value.toMutableList()
                currentList.add(0, newTransaction)
                _transactions.value = currentList
                updateAnalyticsData()

                notifySavingsViewModelAboutTransaction(newTransaction)
                _transactionAdded.emit(newTransaction)

                _successMessage.value = if (newTransaction.isIncome)
                    "Đã thêm thu nhập: ${newTransaction.title} - ${formatVND(newTransaction.amount)}"
                else
                    "Đã thêm chi tiêu: ${newTransaction.title} - ${formatVND(newTransaction.amount)}"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể thêm giao dịch"

                try {
                    val currentList = _transactions.value.toMutableList()
                    currentList.removeAll { it.id == transaction.id }
                    _transactions.value = currentList
                } catch (_: Exception) {
                }

            } finally {
                setLoading(false)
            }
        }
    }

    // Thông báo cho Savings ViewModel về giao dịch mới
    private fun notifySavingsViewModelAboutTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                _savingsViewModel?.let { savingsVM ->
                    if (transaction.isIncome) {
                        savingsVM.onIncomeAdded(
                            amount = transaction.amount.toLong(),
                            description = transaction.title,
                            category = transaction.category
                        )
                    } else {
                        savingsVM.onExpenseAdded(
                            amount = transaction.amount.toLong(),
                            category = transaction.category
                        )
                        checkAndSuggestSavings(transaction)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Kiểm tra và đề xuất tiết kiệm
    private fun checkAndSuggestSavings(transaction: Transaction) {
        viewModelScope.launch {
            try {
                val thirtyDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }.timeInMillis

                val similarExpenses = _transactions.value.filter {
                    !it.isIncome &&
                            it.category == transaction.category &&
                            it.createdAt > thirtyDaysAgo
                }

                if (similarExpenses.size >= 3) {
                    val avgExpense = similarExpenses.sumOf { it.amount } / similarExpenses.size

                    if (transaction.amount < avgExpense * 0.8) {
                        val savedAmount = avgExpense - transaction.amount

                        _savingsViewModel?.suggestSavingsFromExpenseReduction(
                            amount = savedAmount.toLong(),
                            category = transaction.category,
                            originalAverage = avgExpense.toLong()
                        )

                        _warningMessage.value = "Bạn đã tiết kiệm được ${formatVND(savedAmount)} " +
                                "từ '${transaction.category}'. Có muốn thêm vào mục tiêu tiết kiệm?"
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Thêm giao dịch từ recurring expense
    fun addTransactionFromRecurringExpense(
        recurringExpense: RecurringExpense,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            _autoTransactionMessage.value = null

            try {
                val transaction = createTransactionFromRecurringExpense(recurringExpense)

                val userId = getCurrentUserId()
                firestoreService.saveTransaction(transaction, userId)

                budgetViewModel?.updateBudgetAfterTransaction(transaction.category, transaction.amount)

                val currentList = _transactions.value.toMutableList()
                currentList.add(0, transaction)
                _transactions.value = currentList
                updateAnalyticsData()

                if (!transaction.isIncome) {
                    notifySavingsViewModelAboutTransaction(transaction)
                }

                _transactionAdded.emit(transaction)

                val successMsg = "Đã tạo giao dịch định kỳ: ${recurringExpense.title} - ${formatVND(recurringExpense.amount)}"
                _autoTransactionMessage.value = successMsg

            } catch (e: Exception) {
                val errorMsg = "Lỗi tạo giao dịch định kỳ"
                _autoTransactionMessage.value = errorMsg
            }
        }
    }

    // Update transaction
    fun updateTransaction(
        updatedTransaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val oldTransaction = _transactions.value.find { it.id == updatedTransaction.id }
                    ?: run {
                        _errorMessage.value = "Giao dịch không tồn tại"
                        return@launch
                    }

                if (!oldTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(oldTransaction.category, -oldTransaction.amount)
                }

                if (!updatedTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(updatedTransaction.category, updatedTransaction.amount)
                }

                val userId = getCurrentUserId()
                firestoreService.saveTransaction(updatedTransaction, userId)

                _transactions.value = _transactions.value.map {
                    if (it.id == updatedTransaction.id) updatedTransaction else it
                }.sortedByDescending {
                    try { dateFormat.parse(it.date) } catch (_: Exception) { Date(0) }
                }

                updateAnalyticsData()
                notifySavingsViewModelAboutTransactionUpdate(oldTransaction, updatedTransaction)
                _successMessage.value = "Đã cập nhật giao dịch thành công"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể cập nhật giao dịch"

            } finally {
                setLoading(false)
            }
        }
    }

    // Thông báo thay đổi giao dịch cho SavingsViewModel
    private fun notifySavingsViewModelAboutTransactionUpdate(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch {
            try {
                _savingsViewModel?.let { savingsVM ->
                    if (oldTx.amount != newTx.amount) {
                        val difference = newTx.amount - oldTx.amount

                        if (oldTx.isIncome) {
                            if (difference > 0) {
                                savingsVM.onIncomeAdded(
                                    amount = difference.toLong(),
                                    description = "Điều chỉnh thu nhập: ${newTx.title}",
                                    category = newTx.category
                                )
                            } else {
                                savingsVM.onIncomeReduced(
                                    amount = (-difference).toLong(),
                                    description = "Giảm thu nhập: ${newTx.title}"
                                )
                            }
                        } else {
                            if (difference > 0) {
                                savingsVM.onExpenseAdded(
                                    amount = difference.toLong(),
                                    category = newTx.category
                                )
                            } else {
                                val savedAmount = (-difference).toLong()
                                savingsVM.onExpenseReduced(
                                    amount = savedAmount,
                                    category = newTx.category
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Delete transaction
    fun deleteTransaction(
        transactionId: String,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val transactionToDelete = _transactions.value.find { it.id == transactionId }
                    ?: run {
                        _errorMessage.value = "Giao dịch không tồn tại"
                        return@launch
                    }

                if (!transactionToDelete.isIncome) {
                    budgetViewModel?.decreaseBudgetAfterDeletion(
                        categoryId = transactionToDelete.categoryId,
                        amount = transactionToDelete.amount
                    )
                }

                notifySavingsViewModelAboutTransactionDeletion(transactionToDelete)

                val userId = getCurrentUserId()
                firestoreService.deleteTransaction(transactionId, userId)

                _transactions.value = _transactions.value.filter { it.id != transactionId }
                updateAnalyticsData()

                _successMessage.value = "Đã xóa giao dịch thành công"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể xóa giao dịch"

            } finally {
                setLoading(false)
            }
        }
    }

    // Thông báo xóa giao dịch cho SavingsViewModel
    private fun notifySavingsViewModelAboutTransactionDeletion(transaction: Transaction) {
        viewModelScope.launch {
            try {
                _savingsViewModel?.let { savingsVM ->
                    if (transaction.isIncome) {
                        savingsVM.onIncomeReduced(
                            amount = transaction.amount.toLong(),
                            description = "Xóa thu nhập: ${transaction.title}"
                        )
                    } else {
                        savingsVM.onExpenseReduced(
                            amount = transaction.amount.toLong(),
                            category = transaction.category
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Analytics & reporting
    private fun updateAnalyticsData() {
        val currentMonthTx = getCurrentMonthTransactions()
        val income = currentMonthTx.filter { it.isIncome }.sumOf { it.amount }
        val expense = currentMonthTx.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        val topCategories = getTopSpendingCategories(3)
        val recentTransactions = _transactions.value.take(5)

        _analyticsData.value = AnalyticsData(
            monthlyIncome = income,
            monthlyExpense = expense,
            monthlyBalance = balance,
            totalTransactions = _transactions.value.size,
            topSpendingCategories = topCategories,
            recentTransactions = recentTransactions
        )

        updateSavingsViewModelAnalytics(income, expense)
    }

    // Cập nhật thống kê cho SavingsViewModel
    private fun updateSavingsViewModelAnalytics(income: Double, expense: Double) {
        viewModelScope.launch {
            try {
                _savingsViewModel?.updateFinancialStats(
                    monthlyIncome = income.toLong(),
                    monthlyExpense = expense.toLong()
                )
            } catch (_: Exception) {
            }
        }
    }

    fun getTopSpendingCategories(limit: Int = 3): List<Pair<String, Double>> {
        return _transactions.value
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getCurrentMonthTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return _transactions.value.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    fun addTransactionFromAI(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            _autoTransactionMessage.value = null
            _aiCommandResult.value = null

            try {
                val userId = getCurrentUserId()
                firestoreService.saveTransaction(transaction, userId)

                if (!transaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(transaction.category, transaction.amount)
                }

                val currentList = _transactions.value.toMutableList()
                currentList.add(0, transaction)
                _transactions.value = currentList
                updateAnalyticsData()

                notifySavingsViewModelAboutTransaction(transaction)
                _transactionAdded.emit(transaction)

                val successMsg = "Đã thêm ${if (transaction.isIncome) "thu nhập" else "chi tiêu"} ${formatVND(transaction.amount)} cho '${transaction.title}'"
                _autoTransactionMessage.value = successMsg
                _aiCommandResult.value = AICommandResult(true, successMsg, transaction)

            } catch (e: Exception) {
                val errorMsg = "Lỗi thêm giao dịch từ AI"
                _autoTransactionMessage.value = errorMsg
                _aiCommandResult.value = AICommandResult(false, errorMsg)

                val currentList = _transactions.value.toMutableList()
                currentList.removeAll { it.id == transaction.id }
                _transactions.value = currentList
            }
        }
    }

    // Utility methods
    fun getTotalIncome(): Double {
        return _transactions.value.filter { it.isIncome }.sumOf { it.amount }
    }

    fun getTotalExpense(): Double {
        return _transactions.value.filter { !it.isIncome }.sumOf { it.amount }
    }

    fun getCurrentBalance(): Double {
        return getTotalIncome() - getTotalExpense()
    }

    fun refreshTransactions() {
        loadTransactionsFromFirestore()
    }

    fun getUniqueCategories(): List<String> {
        return _transactions.value.map { it.category }.distinct()
    }

    fun getTransactionsByCategory(categoryId: String): List<Transaction> {
        return _transactions.value.filter { it.categoryId == categoryId }
    }

    fun getTransactionsByRecurringSource(recurringSourceId: String): List<Transaction> {
        return _transactions.value.filter { it.recurringSourceId == recurringSourceId }
    }

    fun getAutoGeneratedTransactions(): List<Transaction> {
        return _transactions.value.filter { it.isAutoGenerated }
    }

    // Lấy thu nhập tháng hiện tại (cho Savings)
    fun getCurrentMonthIncome(): Long {
        return getCurrentMonthTransactions()
            .filter { it.isIncome }
            .sumOf { it.amount }.toLong()
    }

    // Lấy chi tiêu tháng hiện tại (cho Savings)
    fun getCurrentMonthExpense(): Long {
        return getCurrentMonthTransactions()
            .filter { !it.isIncome }
            .sumOf { it.amount }.toLong()
    }

    // Tính số tiền có thể tiết kiệm (cho UI hiển thị)
    fun getPotentialSavingsAmount(): Long {
        val income = getCurrentMonthIncome()
        val expense = getCurrentMonthExpense()
        return (income - expense).coerceAtLeast(0)
    }

    // Đề xuất phân bổ tiết kiệm (gọi từ UI)
    fun suggestSavingsAllocation(percentage: Int = 20) {
        viewModelScope.launch {
            try {
                val potentialSavings = getPotentialSavingsAmount()
                if (potentialSavings > 0) {
                    val amountToSave = (potentialSavings * percentage) / 100

                    _savingsViewModel?.suggestManualAllocation(
                        amount = amountToSave,
                        percentage = percentage
                    )

                    _warningMessage.value = "Đề xuất: Tiết kiệm ${formatVND(amountToSave.toDouble())} " +
                            "($percentage% thu nhập dư) vào mục tiêu của bạn"
                } else {
                    _warningMessage.value = "Hiện không có thu nhập dư để tiết kiệm"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Không thể đề xuất tiết kiệm"
            }
        }
    }

    // Clear methods
    fun clearError() { _errorMessage.value = null }
    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearWarning() { _warningMessage.value = null }
    fun clearAutoMessage() { _autoTransactionMessage.value = null }
    fun clearAICommandResult() { _aiCommandResult.value = null }
}

// Data classes
data class AnalyticsData(
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val monthlyBalance: Double,
    val totalTransactions: Int,
    val topSpendingCategories: List<Pair<String, Double>>,
    val recentTransactions: List<Transaction>
)

data class MonthlyStats(
    val income: Double,
    val expense: Double,
    val balance: Double,
    val transactionCount: Int
)