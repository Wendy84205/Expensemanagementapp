package com.example.financeapp.viewmodel.transaction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.remote.FirestoreService
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.ai.AICommandResult
import com.example.financeapp.viewmodel.budget.BudgetViewModel
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

/**
 * ViewModel quản lý giao dịch (thu nhập và chi tiêu)
 * Xử lý CRUD operations và cung cấp dữ liệu cho UI
 */
class TransactionViewModel : ViewModel() {

    companion object {
        private const val TAG = "TransactionViewModel"
    }

    // ==================== STATE FLOWS ====================

    /** Flow thông báo cảnh báo */
    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    /** Flow danh sách giao dịch */
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    /** Flow trạng thái loading */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Flow thông báo lỗi */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Flow thông báo thành công */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** Flow thông báo giao dịch tự động (AI/Recurring) */
    private val _autoTransactionMessage = MutableStateFlow<String?>(null)
    val autoTransactionMessage: StateFlow<String?> = _autoTransactionMessage.asStateFlow()

    /** Flow giao dịch mới được thêm */
    private val _transactionAdded = MutableSharedFlow<Transaction>()
    val transactionAdded: SharedFlow<Transaction> = _transactionAdded.asSharedFlow()

    /** Flow kết quả lệnh AI */
    private val _aiCommandResult = MutableStateFlow<AICommandResult?>(null)
    val aiCommandResult: StateFlow<AICommandResult?> = _aiCommandResult.asStateFlow()

    /** Flow dữ liệu analytics */
    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    // ==================== DEPENDENCIES ====================

    private val firestoreService = FirestoreService()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ==================== INITIALIZATION ====================

    init {
        Log.d(TAG, "TransactionViewModel khởi tạo")
        loadTransactionsFromFirestore()
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Cập nhật trạng thái loading
     */
    private fun setLoading(value: Boolean) {
        _loading.value = value
    }

    /**
     * Parse ngày từ string
     */
    private fun parseDate(dateString: String): Date {
        return try {
            dateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * Định dạng tiền tệ
     */
    private fun formatCurrency(amount: Double): String {
        return "%,.0fđ".format(amount)
    }

    /**
     * Lấy userId hiện tại
     */
    private fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: "anonymous"
    }

    // ==================== DATA LOADING ====================

    /**
     * Tải danh sách giao dịch từ Firestore
     */
    private fun loadTransactionsFromFirestore() {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null

            try {
                val userId = getCurrentUserId()
                val firestoreTransactions = firestoreService.getTransactionsByUser(userId)

                // Sắp xếp theo ngày mới nhất
                val sortedTransactions = firestoreTransactions.sortedByDescending {
                    try { dateFormat.parse(it.date) } catch (_: Exception) { Date(0) }
                }

                _transactions.value = sortedTransactions
                updateAnalyticsData()

                Log.d(TAG, "Đã tải ${sortedTransactions.size} giao dịch từ Firestore cho user: $userId")

            } catch (e: Exception) {
                _errorMessage.value = "Không thể tải danh sách giao dịch: ${e.message}"
                _transactions.value = emptyList()
                Log.e(TAG, "Lỗi tải giao dịch: ${e.message}")

            } finally {
                setLoading(false)
            }
        }
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Thêm giao dịch mới
     * @param transaction Giao dịch cần thêm
     * @param budgetViewModel ViewModel ngân sách để cập nhật (optional)
     */
    fun addTransaction(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Validation
                if (transaction.amount <= 0) {
                    _errorMessage.value = "Số tiền phải lớn hơn 0"
                    return@launch
                }

                // Tạo tiêu đề từ các nguồn có sẵn
                val finalTitle = transaction.title.ifBlank {
                    transaction.description.ifBlank { transaction.category }
                }

                if (finalTitle.isBlank() && transaction.category.isBlank()) {
                    _errorMessage.value = "Vui lòng chọn danh mục"
                    return@launch
                }

                // Tạo transaction với ID mới nếu cần
                val newTransaction = transaction.copy(
                    id = if (transaction.id.isBlank()) UUID.randomUUID().toString() else transaction.id,
                    title = finalTitle
                )

                // Lưu vào Firestore với userId
                val userId = getCurrentUserId()
                firestoreService.saveTransaction(newTransaction, userId)

                // Cập nhật ngân sách nếu là chi tiêu
                if (!newTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(newTransaction.category, newTransaction.amount)
                }

                // Cập nhật local state
                val currentList = _transactions.value.toMutableList()
                currentList.add(0, newTransaction)
                _transactions.value = currentList
                updateAnalyticsData()

                // Thông báo sự kiện
                _transactionAdded.emit(newTransaction)

                // Thông báo thành công
                _successMessage.value = if (newTransaction.isIncome)
                    "Đã thêm thu nhập: ${newTransaction.title}"
                else
                    "Đã thêm chi tiêu: ${newTransaction.title}"

                Log.d(TAG, "✅ Đã thêm giao dịch: ${newTransaction.title}, Số tiền: ${formatCurrency(newTransaction.amount)}")
                Log.d(TAG, "📊 Tổng số giao dịch: ${_transactions.value.size}, Tổng chi tiêu: ${formatCurrency(getTotalExpense())}")

            } catch (e: Exception) {
                _errorMessage.value = "Không thể thêm giao dịch: ${e.message}"
                Log.e(TAG, "Lỗi thêm giao dịch: ${e.message}")

                // Rollback local state nếu có lỗi
                try {
                    val currentList = _transactions.value.toMutableList()
                    currentList.removeAll { it.id == transaction.id }
                    _transactions.value = currentList
                } catch (rollbackError: Exception) {
                    Log.e(TAG, "Lỗi rollback: ${rollbackError.message}")
                }

            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Thêm giao dịch từ AI Assistant
     * @param transaction Giao dịch từ AI
     * @param budgetViewModel ViewModel ngân sách (optional)
     */
    fun addTransactionFromAI(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            _autoTransactionMessage.value = null
            _aiCommandResult.value = null

            try {
                // Lưu vào Firestore với userId
                val userId = getCurrentUserId()
                firestoreService.saveTransaction(transaction, userId)

                // Cập nhật ngân sách nếu là chi tiêu
                if (!transaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(transaction.category, transaction.amount)
                }

                // Cập nhật local state
                val currentList = _transactions.value.toMutableList()
                currentList.add(0, transaction)
                _transactions.value = currentList
                updateAnalyticsData()

                // Thông báo sự kiện
                _transactionAdded.emit(transaction)

                // Thông báo thành công
                val successMsg = "Đã thêm ${if (transaction.isIncome) "thu nhập" else "chi tiêu"} ${formatCurrency(transaction.amount)} cho '${transaction.title}'"
                _autoTransactionMessage.value = successMsg
                _aiCommandResult.value = AICommandResult(true, successMsg, transaction)

                Log.d(TAG, "🤖 AI: $successMsg")

            } catch (e: Exception) {
                val errorMsg = "Lỗi thêm giao dịch từ AI: ${e.message}"
                _autoTransactionMessage.value = errorMsg
                _aiCommandResult.value = AICommandResult(false, errorMsg)
                Log.e(TAG, "Lỗi thêm giao dịch từ AI: ${e.message}")

                // Rollback local state
                val currentList = _transactions.value.toMutableList()
                currentList.removeAll { it.id == transaction.id }
                _transactions.value = currentList
            }
        }
    }

    /**
     * Thêm giao dịch từ recurring expense
     * @param transaction Giao dịch định kỳ
     * @param budgetViewModel ViewModel ngân sách (optional)
     */
    fun addTransactionFromRecurring(
        transaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            _autoTransactionMessage.value = null

            try {
                // Lưu vào Firestore với userId
                val userId = getCurrentUserId()
                firestoreService.saveTransaction(transaction, userId)

                // Cập nhật ngân sách nếu là chi tiêu
                if (!transaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(transaction.category, transaction.amount)
                }

                // Cập nhật local state
                val currentList = _transactions.value.toMutableList()
                currentList.add(0, transaction)
                _transactions.value = currentList
                updateAnalyticsData()

                // Thông báo sự kiện
                _transactionAdded.emit(transaction)
                _autoTransactionMessage.value = "Đã thêm giao dịch định kỳ: ${transaction.title}"

                Log.d(TAG, "🔄 Đã thêm giao dịch định kỳ: ${transaction.title}")

            } catch (e: Exception) {
                _autoTransactionMessage.value = "Lỗi thêm giao dịch định kỳ: ${e.message}"
                Log.e(TAG, "Lỗi thêm giao dịch định kỳ: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật giao dịch
     * @param updatedTransaction Giao dịch đã cập nhật
     * @param budgetViewModel ViewModel ngân sách (optional)
     */
    fun updateTransaction(
        updatedTransaction: Transaction,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Tìm giao dịch cũ
                val oldTransaction = _transactions.value.find { it.id == updatedTransaction.id }
                    ?: run {
                        _errorMessage.value = "Giao dịch không tồn tại"
                        return@launch
                    }

                // Revert budget nếu là chi tiêu cũ
                if (!oldTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(oldTransaction.category, -oldTransaction.amount)
                }

                // Cập nhật budget nếu là chi tiêu mới
                if (!updatedTransaction.isIncome) {
                    budgetViewModel?.updateBudgetAfterTransaction(updatedTransaction.category, updatedTransaction.amount)
                }

                // Lưu vào Firestore với userId
                val userId = getCurrentUserId()
                firestoreService.saveTransaction(updatedTransaction, userId)

                // Cập nhật local state
                _transactions.value = _transactions.value.map {
                    if (it.id == updatedTransaction.id) updatedTransaction else it
                }.sortedByDescending {
                    try { dateFormat.parse(it.date) } catch (_: Exception) { Date(0) }
                }

                updateAnalyticsData()

                _successMessage.value = "Đã cập nhật giao dịch thành công"
                Log.d(TAG, "🔄 Đã cập nhật giao dịch: ${updatedTransaction.title}")

            } catch (e: Exception) {
                _errorMessage.value = "Không thể cập nhật giao dịch: ${e.message}"
                Log.e(TAG, "Lỗi cập nhật giao dịch: ${e.message}")

            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Xóa giao dịch
     * @param transactionId ID giao dịch cần xóa
     * @param budgetViewModel ViewModel ngân sách (optional)
     */
    fun deleteTransaction(
        transactionId: String,
        budgetViewModel: BudgetViewModel? = null
    ) {
        viewModelScope.launch {
            setLoading(true)
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Tìm giao dịch cần xóa
                val transactionToDelete = _transactions.value.find { it.id == transactionId }
                    ?: run {
                        _errorMessage.value = "Giao dịch không tồn tại"
                        return@launch
                    }

                // Sửa: Revert budget nếu là chi tiêu - sử dụng categoryId thay vì category
                if (!transactionToDelete.isIncome) {
                    budgetViewModel?.decreaseBudgetAfterDeletion(
                        categoryId = transactionToDelete.categoryId ?: transactionToDelete.category,
                        amount = transactionToDelete.amount
                    )
                }

                // Xóa từ Firestore với userId
                val userId = getCurrentUserId()
                firestoreService.deleteTransaction(transactionId, userId)

                // Cập nhật local state
                _transactions.value = _transactions.value.filter { it.id != transactionId }
                updateAnalyticsData()

                _successMessage.value = "Đã xóa giao dịch thành công"
                Log.d(TAG, "🗑️ Đã xóa giao dịch: ${transactionToDelete.title}")

            } catch (e: Exception) {
                _errorMessage.value = "Không thể xóa giao dịch: ${e.message}"
                Log.e(TAG, "Lỗi xóa giao dịch: ${e.message}")

            } finally {
                setLoading(false)
            }
        }
    }

    // ==================== ANALYTICS & REPORTING ====================

    /**
     * Cập nhật dữ liệu analytics
     */
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

        Log.d(TAG, "📊 Analytics updated: Income=${formatCurrency(income)}, Expense=${formatCurrency(expense)}")
    }

    /**
     * Lấy danh sách top danh mục chi tiêu
     * @param limit Số lượng danh mục (mặc định 3)
     * @return List cặp (category, totalAmount)
     */
    fun getTopSpendingCategories(limit: Int = 3): List<Pair<String, Double>> {
        return _transactions.value
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Lấy giao dịch tháng hiện tại
     */
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

    /**
     * Lấy giao dịch tuần trước
     */
    fun getLastWeekTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeek = calendar.time

        return _transactions.value.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            transactionDate.after(lastWeek) || transactionDate == lastWeek
        }
    }

    /**
     * Lấy giao dịch năm hiện tại
     */
    fun getCurrentYearTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        return _transactions.value.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    /**
     * Tìm kiếm giao dịch với các filter
     * @param query Từ khóa tìm kiếm (optional)
     * @param period Khoảng thời gian (week, month, year, all_time)
     * @param category Danh mục (optional)
     */
    fun searchTransactions(
        query: String? = null,
        period: String = "all_time",
        category: String? = null
    ): List<Transaction> {
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

    // ==================== UTILITY METHODS ====================

    /**
     * Lấy tổng thu nhập
     */
    fun getTotalIncome(): Double {
        return _transactions.value.filter { it.isIncome }.sumOf { it.amount }
    }

    /**
     * Lấy tổng chi tiêu
     */
    fun getTotalExpense(): Double {
        return _transactions.value.filter { !it.isIncome }.sumOf { it.amount }
    }

    /**
     * Lấy số dư hiện tại
     */
    fun getCurrentBalance(): Double {
        return getTotalIncome() - getTotalExpense()
    }

    /**
     * Refresh danh sách giao dịch
     */
    fun refreshTransactions() {
        loadTransactionsFromFirestore()
    }

    /**
     * Lấy danh sách danh mục duy nhất
     */
    fun getUniqueCategories(): List<String> {
        return _transactions.value.map { it.category }.distinct()
    }

    /**
     * Lấy giao dịch theo danh mục
     */
    fun getTransactionsByCategory(categoryId: String): List<Transaction> {
        return _transactions.value.filter { it.category == categoryId }
    }

    /**
     * Lấy giao dịch theo ví
     */
    fun getTransactionsByWallet(walletName: String): List<Transaction> {
        return _transactions.value.filter { it.wallet.equals(walletName, ignoreCase = true) }
    }

    /**
     * Lấy giao dịch theo tháng và năm
     */
    fun getTransactionsByMonth(month: Int, year: Int): List<Transaction> {
        return _transactions.value.filter {
            val cal = Calendar.getInstance().apply {
                time = parseDate(it.date)
            }
            (cal.get(Calendar.MONTH) + 1 == month) && (cal.get(Calendar.YEAR) == year)
        }
    }

    /**
     * Lấy thống kê theo tháng
     */
    fun getMonthlyStats(month: Int, year: Int): MonthlyStats {
        val monthTx = getTransactionsByMonth(month, year)
        val income = monthTx.filter { it.isIncome }.sumOf { it.amount }
        val expense = monthTx.filter { !it.isIncome }.sumOf { it.amount }
        return MonthlyStats(income, expense, income - expense, monthTx.size)
    }

    /**
     * Xóa tất cả giao dịch của một ví
     */
    fun deleteTransactionsByWallet(walletName: String) {
        viewModelScope.launch {
            val toDelete = _transactions.value.filter {
                it.wallet.equals(walletName, true)
            }

            // Xóa từ Firestore
            val userId = getCurrentUserId()
            toDelete.forEach {
                try {
                    firestoreService.deleteTransaction(it.id, userId)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi xóa transaction ${it.id}: ${e.message}")
                }
            }

            // Cập nhật local state
            _transactions.value = _transactions.value.filter {
                !it.wallet.equals(walletName, true)
            }

            updateAnalyticsData()
            Log.d(TAG, "Đã xóa ${toDelete.size} giao dịch của ví: $walletName")
        }
    }

    // ==================== CLEAR METHODS ====================

    fun clearError() { _errorMessage.value = null }
    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearWarning() { _warningMessage.value = null }
    fun clearAutoMessage() { _autoTransactionMessage.value = null }
    fun clearAICommandResult() { _aiCommandResult.value = null }
}

// ==================== DATA CLASSES ====================

/**
 * Dữ liệu analytics cho dashboard
 */
data class AnalyticsData(
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val monthlyBalance: Double,
    val totalTransactions: Int,
    val topSpendingCategories: List<Pair<String, Double>>,
    val recentTransactions: List<Transaction>
)

/**
 * Thống kê theo tháng
 */
data class MonthlyStats(
    val income: Double,
    val expense: Double,
    val balance: Double,
    val transactionCount: Int
)

/**
 * Thống kê theo tuần
 */
data class WeeklyStats(
    val income: Double,
    val expense: Double,
    val balance: Double,
    val transactionCount: Int
)