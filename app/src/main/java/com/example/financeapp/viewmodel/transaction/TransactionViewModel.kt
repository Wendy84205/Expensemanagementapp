package com.example.financeapp.viewmodel.transaction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.remote.FirestoreService
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.ai.AICommandResult
import com.example.financeapp.viewmodel.budget.BudgetViewModel
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

    // 🔥 THÊM: Flow cho AI integration
    private val _aiCommandResult = MutableStateFlow<AICommandResult?>(null)
    val aiCommandResult: StateFlow<AICommandResult?> = _aiCommandResult.asStateFlow()

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    private val firestoreService = FirestoreService()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    init {
        loadTransactionsFromFirestore()
    }

    private fun setLoading(value: Boolean) {
        _loading.value = value
    }

    private fun loadTransactionsFromFirestore() {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            try {
                val firestoreTransactions = firestoreService.getTransactions()

                val sortedTransactions = firestoreTransactions.sortedByDescending {
                    try { dateFormat.parse(it.date) } catch (_: Exception) { Date(0) }
                }

                _transactions.value = sortedTransactions
                updateAnalyticsData() // 🔥 Cập nhật analytics khi load transactions

            } catch (e: Exception) {
                _errorMessage.value = "Không thể tải danh sách giao dịch: ${e.message}"
                _transactions.value = emptyList()
            } finally {
                setLoading(false)
            }
        }
    }

    // ───────── Add Transaction ─────────
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

                // ✅ Sửa validation: title có thể lấy từ description hoặc category
                val finalTitle = transaction.title.ifBlank { 
                    transaction.description.ifBlank { transaction.category }
                }

                if (finalTitle.isBlank() && transaction.category.isBlank()) {
                    _errorMessage.value = "Vui lòng chọn danh mục"
                    return@launch
                }

                val newTransaction = transaction.copy(
                    id = if (transaction.id.isBlank()) UUID.randomUUID().toString() else transaction.id,
                    title = finalTitle
                )

                firestoreService.saveTransaction(newTransaction)

                // ✅ Bỏ cập nhật ví: chỉ cập nhật ngân sách nếu là chi tiêu

                if (!newTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(newTransaction.category, newTransaction.amount)
                }

                // ✅ Cập nhật local state ngay lập tức để UI phản hồi nhanh
                val currentList = _transactions.value.toMutableList()
                currentList.add(0, newTransaction)
                _transactions.value = currentList
                
                // ✅ Đảm bảo analytics được update ngay
                updateAnalyticsData() // 🔥 Cập nhật analytics
                

                _transactionAdded.emit(newTransaction)

                _successMessage.value = if (newTransaction.isIncome)
                    "Đã thêm thu nhập: ${newTransaction.title}"
                else
                    "Đã thêm chi tiêu: ${newTransaction.title}"
                
                Log.d("TransactionViewModel", "✅ Transaction added: ${newTransaction.title}, Amount: ${newTransaction.amount}, IsIncome: ${newTransaction.isIncome}")
                Log.d("TransactionViewModel", "✅ Total transactions: ${_transactions.value.size}, Total expense: ${getTotalExpense()}")
                
                // ✅ Không cần reload từ Firestore vì đã cập nhật local state ngay

            } catch (e: Exception) {
                _errorMessage.value = "Không thể thêm giao dịch: ${e.message}"

                try {
                    val currentList = _transactions.value.toMutableList()
                    currentList.removeAll { it.id == transaction.id }
                    _transactions.value = currentList
                } catch (_: Exception) { }

            } finally {
                setLoading(false)
            }
        }
    }

    // ───────── Add Transaction From AI ─────────
    fun addTransactionFromAI(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            _autoTransactionMessage.value = null
            _aiCommandResult.value = null

            try {
                // ✅ Bỏ kiểm tra ví cho AI: chỉ lưu giao dịch và cập nhật ngân sách

                firestoreService.saveTransaction(transaction)

                
                if (!transaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(transaction.category, transaction.amount)
                }

                val currentList = _transactions.value.toMutableList()
                currentList.add(0, transaction)
                _transactions.value = currentList
                updateAnalyticsData() // 🔥 Cập nhật analytics

                _transactionAdded.emit(transaction)

                val successMsg = "✅ Đã thêm ${if (transaction.isIncome) "thu nhập" else "chi tiêu"} ${formatCurrency(transaction.amount)} cho '${transaction.title}'"
                _autoTransactionMessage.value = successMsg
                _aiCommandResult.value = AICommandResult(true, successMsg, transaction)

            } catch (e: Exception) {
                val errorMsg = "❌ Lỗi thêm giao dịch từ AI: ${e.message}"
                _autoTransactionMessage.value = errorMsg
                _aiCommandResult.value = AICommandResult(false, errorMsg)

                val currentList = _transactions.value.toMutableList()
                currentList.removeAll { it.id == transaction.id }
                _transactions.value = currentList
            }
        }
    }

    // ───────── Add Transaction From Recurring ─────────
    fun addTransactionFromRecurring(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            _autoTransactionMessage.value = null
            try {
                firestoreService.saveTransaction(transaction)

                if (!transaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(transaction.category, transaction.amount)
                }

                val currentList = _transactions.value.toMutableList()
                currentList.add(0, transaction)
                _transactions.value = currentList
                updateAnalyticsData() // 🔥 Cập nhật analytics

                _transactionAdded.emit(transaction)
                _autoTransactionMessage.value = "Đã thêm giao dịch định kỳ: ${transaction.title}"

            } catch (e: Exception) {
                _autoTransactionMessage.value = "Lỗi thêm giao dịch định kỳ: ${e.message}"
            }
        }
    }

    // ───────── Update Transaction ─────────
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

                // ✅ Bỏ revert ví

                if (!oldTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(oldTransaction.category, -oldTransaction.amount)
                }

                // ✅ Bỏ cập nhật ví cho transaction mới

                if (!updatedTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(updatedTransaction.category, updatedTransaction.amount)
                }

                firestoreService.saveTransaction(updatedTransaction)

                _transactions.value = _transactions.value.map {
                    if (it.id == updatedTransaction.id) updatedTransaction else it
                }.sortedByDescending {
                    try { dateFormat.parse(it.date) } catch (_: Exception) { Date(0) }
                }
                updateAnalyticsData() // 🔥 Cập nhật analytics

                _successMessage.value = "Đã cập nhật giao dịch thành công"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể cập nhật giao dịch: ${e.message}"
            } finally {
                setLoading(false)
            }
        }
    }

    // ───────── Delete Transaction ─────────
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

                // ✅ Bỏ revert ví khi xóa giao dịch

                if (!transactionToDelete.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(transactionToDelete.category, -transactionToDelete.amount)
                }

                // ✅ Xóa transaction khỏi Firestore
                firestoreService.deleteTransaction(transactionId)

                // ✅ Cập nhật local state ngay lập tức
                _transactions.value = _transactions.value.filter { it.id != transactionId }
                updateAnalyticsData() // 🔥 Cập nhật analytics

                _successMessage.value = "Đã xóa giao dịch thành công"
                
                // ✅ Reload để đồng bộ (optional, local state đã được cập nhật)
                // loadTransactionsFromFirestore()

            } catch (e: Exception) {
                _errorMessage.value = "Không thể xóa giao dịch: ${e.message}"
                // ✅ Nếu lỗi, vẫn giữ transaction trong list để user có thể thử lại
            } finally {
                setLoading(false)
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

    fun getLastWeekTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeek = calendar.time

        return _transactions.value.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            transactionDate.after(lastWeek) || transactionDate == lastWeek
        }
    }

    fun getCurrentYearTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        return _transactions.value.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    fun searchTransactions(query: String? = null, period: String = "all_time", category: String? = null): List<Transaction> {
        var filtered = _transactions.value

        // Filter by period
        filtered = when (period) {
            "week" -> getLastWeekTransactions()
            "month" -> getCurrentMonthTransactions()
            "year" -> getCurrentYearTransactions()
            else -> filtered
        }

        // Filter by category
        if (!category.isNullOrBlank()) {
            filtered = filtered.filter { it.category.equals(category, true) }
        }

        // Filter by search query
        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { transaction ->
                transaction.title.contains(query, true) ||
                        transaction.category.contains(query, true) ||
                        transaction.description.contains(query, true)
            }
        }

        return filtered
    }

    // 🔥 THÊM: Cập nhật analytics data
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
    }

    // ───────── UTILITY FUNCTIONS ─────────
    fun getTotalIncome(): Double = _transactions.value.filter { it.isIncome }.sumOf { it.amount }

    fun getTotalExpense(): Double = _transactions.value.filter { !it.isIncome }.sumOf { it.amount }

    fun getCurrentBalance(): Double = getTotalIncome() - getTotalExpense()

    fun refreshTransactions() {
        loadTransactionsFromFirestore()
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearWarning() { _warningMessage.value = null }
    fun clearAutoMessage() { _autoTransactionMessage.value = null }
    fun clearAICommandResult() { _aiCommandResult.value = null }

    fun getUniqueCategories(): List<String> =
        _transactions.value.map { it.category }.distinct()

    fun getTransactionsByCategory(categoryId: String): List<Transaction> =
        _transactions.value.filter { it.category == categoryId }

    fun getTransactionsByWallet(walletName: String): List<Transaction> =
        _transactions.value.filter { it.wallet.equals(walletName, ignoreCase = true) }

    fun getTransactionsByMonth(month: Int, year: Int): List<Transaction> =
        _transactions.value.filter {
            val cal = Calendar.getInstance().apply {
                time = parseDate(it.date)
            }
            (cal.get(Calendar.MONTH) + 1 == month) && (cal.get(Calendar.YEAR) == year)
        }

    fun getMonthlyStats(month: Int, year: Int): MonthlyStats {
        val monthTx = getTransactionsByMonth(month, year)
        val income = monthTx.filter { it.isIncome }.sumOf { it.amount }
        val expense = monthTx.filter { !it.isIncome }.sumOf { it.amount }
        return MonthlyStats(income, expense, income - expense, monthTx.size)
    }

    fun deleteTransactionsByWallet(walletName: String) {
        viewModelScope.launch {
            val toDelete = _transactions.value.filter {
                it.wallet.equals(walletName, true)
            }
            _transactions.value = _transactions.value.filter {
                !it.wallet.equals(walletName, true)
            }
            toDelete.forEach {
                firestoreService.deleteTransaction(it.id)
            }
            updateAnalyticsData()
        }
    }

    private fun parseDate(dateString: String): Date {
        return try {
            dateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun formatCurrency(amount: Double): String =
        "%,.0fđ".format(amount)
}

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

data class WeeklyStats(
    val income: Double,
    val expense: Double,
    val balance: Double,
    val transactionCount: Int
)