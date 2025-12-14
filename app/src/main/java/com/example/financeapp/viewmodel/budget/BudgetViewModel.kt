package com.example.financeapp.viewmodel.budget

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.data.remote.FirestoreService
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

/**
 * ViewModel quản lý ngân sách
 * Xử lý CRUD operations cho ngân sách và đồng bộ với Firestore
 */
class BudgetViewModel : ViewModel() {

    companion object {
        private const val TAG = "BudgetViewModel"
    }

    // ==================== STATE FLOWS ====================

    /** Flow danh sách ngân sách */
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    /** Flow danh sách ngân sách đã vượt quá */
    private val _exceededBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val exceededBudgets: StateFlow<List<Budget>> = _exceededBudgets

    /** Flow sự kiện vượt quá ngân sách */
    private val _budgetExceededEvent = MutableStateFlow<Pair<Budget, Double>?>(null)
    val budgetExceededEvent: StateFlow<Pair<Budget, Double>?> = _budgetExceededEvent

    /** Flow trạng thái loading */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ==================== DEPENDENCIES ====================

    private val firestoreService = FirestoreService()
    private val auth = Firebase.auth
    private var budgetsListener: ListenerRegistration? = null
    private var isListenerSetup = false

    // ==================== INITIALIZATION ====================

    init {
        Log.d(TAG, "BudgetViewModel khởi tạo")

        // Load dữ liệu ban đầu
        loadBudgetsFromFirestore()

        // Kiểm tra và reset ngân sách hết hạn khi khởi động
        viewModelScope.launch {
            delay(2000) // Đợi load dữ liệu xong
            checkAndResetExpiredBudgets()
            updateExceededBudgetsList()
        }
    }

    // ==================== FIREBASE HELPERS ====================

    /**
     * Lấy ID user hiện tại
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous".also {
            Log.w(TAG, "User chưa đăng nhập, sử dụng anonymous")
        }
    }

    // ==================== REAL-TIME UPDATES ====================

    /**
     * Bắt đầu real-time updates từ Firestore
     */
    fun startRealTimeUpdates() {
        if (isListenerSetup && budgetsListener != null) {
            Log.d(TAG, "Real-time updates đã được thiết lập")
            return
        }

        val userId = getCurrentUserId()
        if (userId == "anonymous") {
            Log.w(TAG, "User chưa đăng nhập, không thể setup real-time updates")
            return
        }

        try {
            // Hiển thị loading state
            _isLoading.value = true

            // Thiết lập real-time listener
            budgetsListener = firestoreService.setupBudgetsListener(
                userId = userId,
                onBudgetsUpdated = { budgetsList ->
                    _budgets.value = budgetsList
                    updateExceededBudgetsList()
                    _isLoading.value = false
                    Log.d(TAG, "Real-time update: ${budgetsList.size} budgets")
                },
                onError = { error ->
                    _isLoading.value = false
                    Log.e(TAG, "Firestore real-time error: ${error.message}")
                }
            )

            isListenerSetup = true
            Log.d(TAG, "✅ Đã thiết lập real-time updates cho budgets")
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "Lỗi thiết lập real-time updates: ${e.message}")
        }
    }

    /**
     * Dừng real-time updates
     */
    fun stopRealTimeUpdates() {
        budgetsListener?.remove()
        budgetsListener = null
        isListenerSetup = false
        Log.d(TAG, "🛑 Đã dừng real-time updates")
    }

    // ==================== DATA LOADING ====================

    /**
     * Tải danh sách ngân sách từ Firestore (one-time load)
     */
    private fun loadBudgetsFromFirestore() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Đang tải ngân sách từ Firestore...")
                val userId = getCurrentUserId()
                val budgetsList = firestoreService.getBudgets(userId)
                _budgets.value = budgetsList
                updateExceededBudgetsList()
                _isLoading.value = false
                Log.d(TAG, "Đã tải ${budgetsList.size} ngân sách cho user: $userId")
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e(TAG, "Lỗi tải ngân sách: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Thêm ngân sách mới
     * @param budget Ngân sách cần thêm
     */
    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                // Tạo budget với ID mới nếu cần
                val budgetWithId = budget.copy(
                    id = if (budget.id.isBlank()) System.currentTimeMillis().toString() else budget.id
                )

                firestoreService.saveBudget(budgetWithId, userId)

                // Real-time listener sẽ tự động cập nhật
                // Hoặc cập nhật local để UI phản ứng ngay lập tức
                _budgets.value = _budgets.value + budgetWithId
                updateExceededBudgetsList()

                Log.d(TAG, "✅ Đã thêm ngân sách: ${budgetWithId.categoryId}")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi thêm ngân sách: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật toàn bộ ngân sách
     * @param updatedBudget Ngân sách đã cập nhật
     */
    fun updateFullBudget(updatedBudget: Budget) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                firestoreService.saveBudget(updatedBudget, userId)

                // Real-time listener sẽ tự động cập nhật
                // Hoặc cập nhật local để UI phản ứng ngay lập tức
                _budgets.value = _budgets.value.map {
                    if (it.id == updatedBudget.id) updatedBudget else it
                }
                updateExceededBudgetsList()
                Log.d(TAG, "🔄 Đã cập nhật ngân sách: ${updatedBudget.categoryId}")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi cập nhật ngân sách: ${e.message}")
            }
        }
    }

    /**
     * Xóa ngân sách
     * @param budgetId ID ngân sách cần xóa
     */
    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                firestoreService.deleteBudget(budgetId, userId)

                // Real-time listener sẽ tự động cập nhật
                // Hoặc cập nhật local để UI phản ứng ngay lập tức
                _budgets.value = _budgets.value.filter { it.id != budgetId }
                updateExceededBudgetsList()
                Log.d(TAG, "🗑️ Đã xóa ngân sách: $budgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xóa ngân sách: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật ngân sách sau khi có giao dịch mới
     * @param categoryId ID danh mục
     * @param amount Số tiền giao dịch
     * @param triggerNotification Có kích hoạt thông báo khi vượt quá không (mặc định: true)
     */
    fun updateBudgetAfterTransaction(
        categoryId: String,
        amount: Double,
        triggerNotification: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val budgets = _budgets.value.toMutableList()
                val index = budgets.indexOfFirst {
                    it.categoryId == categoryId &&
                            it.isActive &&
                            LocalDate.now().isAfter(it.startDate.minusDays(1)) &&
                            LocalDate.now().isBefore(it.endDate.plusDays(1))
                }

                if (index == -1) {
                    Log.w(TAG, "Không tìm thấy ngân sách active cho categoryId: $categoryId")
                    return@launch
                }

                val budget = budgets[index]
                val newSpent = budget.spent + abs(amount)
                val updated = budget.copy(spent = newSpent, spentAmount = newSpent)

                // KIỂM TRA VƯỢT QUÁ NGÂN SÁCH
                val exceededAmount = newSpent - budget.amount
                val isExceeded = exceededAmount > 0

                // Kích hoạt sự kiện nếu vượt quá và cần thông báo
                if (isExceeded && triggerNotification) {
                    _budgetExceededEvent.value = updated to exceededAmount
                }

                // Cập nhật local list để UI phản ứng ngay
                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList
                updateExceededBudgetsList()

                // Đồng bộ lên Firestore (real-time listener sẽ cập nhật lại)
                firestoreService.saveBudget(updated, userId)

                Log.d(TAG, "📊 Đã cập nhật ngân sách ${updated.categoryId}: spent=${updated.spentAmount}, vượt quá: $isExceeded")

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi cập nhật ngân sách: ${e.message}")
            }
        }
    }

    // ==================== BUDGET MONITORING METHODS ====================

    /**
     * Kiểm tra và reset ngân sách đã hết hạn
     */
    private fun checkAndResetExpiredBudgets() {
        viewModelScope.launch {
            try {
                val currentDate = LocalDate.now()
                val budgets = _budgets.value.toMutableList()
                var hasChanges = false

                for (i in budgets.indices) {
                    val budget = budgets[i]

                    // Nếu ngân sách đã hết hạn và đang active
                    if (currentDate.isAfter(budget.endDate) && budget.isActive) {
                        // Tạo ngân sách mới cho chu kỳ tiếp theo
                        val newStartDate = budget.endDate.plusDays(1)
                        val newEndDate = calculateBudgetEndDate(newStartDate, budget.periodType)

                        val renewedBudget = budget.copy(
                            id = System.currentTimeMillis().toString(),
                            startDate = newStartDate,
                            endDate = newEndDate,
                            spent = 0.0,
                            spentAmount = 0.0
                        )

                        // Lưu ngân sách mới lên Firestore
                        val userId = getCurrentUserId()
                        firestoreService.saveBudget(renewedBudget, userId)

                        // Cập nhật local list
                        budgets[i] = renewedBudget
                        hasChanges = true

                        Log.d(TAG, "🔄 Đã reset ngân sách ${budget.categoryId} cho chu kỳ mới")
                    }
                }

                if (hasChanges) {
                    _budgets.value = budgets
                    updateExceededBudgetsList()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi reset ngân sách: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật danh sách ngân sách vượt quá
     */
    private fun updateExceededBudgetsList() {
        val exceeded = _budgets.value.filter { checkBudgetExceeded(it).first }
        _exceededBudgets.value = exceeded
        Log.d(TAG, "📈 Cập nhật danh sách vượt quá: ${exceeded.size} ngân sách")
    }

    /**
     * Xóa sự kiện vượt quá ngân sách (sau khi đã xử lý)
     */
    fun clearBudgetExceededEvent() {
        _budgetExceededEvent.value = null
    }

    /**
     * Giảm ngân sách sau khi xóa giao dịch
     * @param categoryId ID danh mục
     * @param amount Số tiền giao dịch đã xóa
     */
    fun decreaseBudgetAfterDeletion(categoryId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val budgets = _budgets.value.toMutableList()
                val index = budgets.indexOfFirst {
                    it.categoryId == categoryId &&
                            it.isActive &&
                            LocalDate.now().isAfter(it.startDate.minusDays(1)) &&
                            LocalDate.now().isBefore(it.endDate.plusDays(1))
                }

                if (index == -1) {
                    Log.w(TAG, "Không tìm thấy ngân sách active cho categoryId: $categoryId")
                    return@launch
                }

                val budget = budgets[index]
                val newSpent = budget.spent - abs(amount)

                // Đảm bảo không âm
                val safeNewSpent = newSpent.coerceAtLeast(0.0)
                val updated = budget.copy(spent = safeNewSpent, spentAmount = safeNewSpent)

                // Cập nhật local list để UI phản ứng ngay
                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList
                updateExceededBudgetsList()

                // Đồng bộ lên Firestore
                firestoreService.saveBudget(updated, userId)

                Log.d(TAG, "📉 Đã giảm ngân sách ${updated.categoryId}: spent=${updated.spentAmount} (giảm ${abs(amount)})")

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi giảm ngân sách: ${e.message}")
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Tính ngày kết thúc ngân sách
     * @param startDate Ngày bắt đầu
     * @param periodType Loại chu kỳ
     */
    fun calculateBudgetEndDate(startDate: LocalDate, periodType: BudgetPeriodType): LocalDate {
        return when (periodType) {
            BudgetPeriodType.WEEK -> startDate.plusWeeks(1)
            BudgetPeriodType.MONTH -> startDate.plusMonths(1)
            BudgetPeriodType.QUARTER -> startDate.plusMonths(3)
            BudgetPeriodType.YEAR -> startDate.plusYears(1)
        }
    }

    /**
     * Lấy tên danh mục từ ID
     * @param categoryId ID danh mục
     * @param categoryViewModel CategoryViewModel để lấy thông tin danh mục
     */
    fun getCategoryName(categoryId: String, categoryViewModel: CategoryViewModel): String {
        val category = categoryViewModel.categories.value.find { it.id == categoryId }
        return category?.name ?: "Không xác định"
    }

    /**
     * Tạo ngân sách mới
     * @param categoryId ID danh mục
     * @param amount Số tiền ngân sách
     * @param periodType Loại chu kỳ
     * @param startDate Ngày bắt đầu
     * @param note Ghi chú
     */
    fun createNewBudget(
        categoryId: String,
        amount: Double,
        periodType: BudgetPeriodType,
        startDate: LocalDate = LocalDate.now(),
        note: String? = null
    ): Budget {
        val endDate = calculateBudgetEndDate(startDate, periodType)
        return Budget(
            id = System.currentTimeMillis().toString(),
            categoryId = categoryId,
            amount = amount,
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            note = note,
            spentAmount = 0.0,
            isActive = true,
            spent = 0.0
        )
    }

    /**
     * Refresh dữ liệu ngân sách
     */
    fun refreshBudgets() {
        stopRealTimeUpdates()
        loadBudgetsFromFirestore()
        startRealTimeUpdates()
    }

    // ==================== BUDGET STATUS METHODS ====================

    /**
     * Kiểm tra xem ngân sách có bị vượt quá không
     * @param budget Ngân sách cần kiểm tra
     * @return Pair<Boolean, Double> (isExceeded, exceededAmount)
     */
    fun checkBudgetExceeded(budget: Budget): Pair<Boolean, Double> {
        val exceededAmount = budget.spent - budget.amount
        return (exceededAmount > 0) to if (exceededAmount > 0) exceededAmount else 0.0
    }

    /**
     * Lấy tỷ lệ sử dụng ngân sách (0-100%)
     */
    fun getBudgetUsagePercentage(budget: Budget): Int {
        return if (budget.amount > 0) {
            (budget.spent / budget.amount * 100).toInt().coerceIn(0, Int.MAX_VALUE)
        } else {
            0
        }
    }

    /**
     * Lấy ngân sách cho một danh mục cụ thể
     * @param categoryId ID danh mục
     * @return Budget hoặc null nếu không tìm thấy
     */
    fun getBudgetForCategory(categoryId: String): Budget? {
        return _budgets.value.find {
            it.categoryId == categoryId &&
                    it.isActive &&
                    LocalDate.now().isAfter(it.startDate.minusDays(1)) &&
                    LocalDate.now().isBefore(it.endDate.plusDays(1))
        }
    }

    /**
     * Lấy tổng số tiền vượt quá
     */
    fun getTotalExceededAmount(): Double {
        return _budgets.value.sumOf { budget ->
            val (isExceeded, amount) = checkBudgetExceeded(budget)
            if (isExceeded) amount else 0.0
        }
    }

    /**
     * Tính tổng ngân sách đang active
     */
    fun getTotalBudgetAmount(): Double {
        return _budgets.value
            .filter { it.isActive &&
                    LocalDate.now().isAfter(it.startDate.minusDays(1)) &&
                    LocalDate.now().isBefore(it.endDate.plusDays(1)) }
            .sumOf { it.amount }
    }

    /**
     * Tính tổng đã chi
     */
    fun getTotalSpentAmount(): Double {
        return _budgets.value
            .filter { it.isActive &&
                    LocalDate.now().isAfter(it.startDate.minusDays(1)) &&
                    LocalDate.now().isBefore(it.endDate.plusDays(1)) }
            .sumOf { it.spent }
    }

    /**
     * Lấy ngân sách active (chưa hết hạn)
     */
    fun getActiveBudgets(): List<Budget> {
        return _budgets.value.filter {
            it.isActive &&
                    LocalDate.now().isAfter(it.startDate.minusDays(1)) &&
                    LocalDate.now().isBefore(it.endDate.plusDays(1))
        }
    }

    /**
     * Lấy ngân sách đã hết hạn
     */
    fun getExpiredBudgets(): List<Budget> {
        return _budgets.value.filter {
            it.isActive &&
                    LocalDate.now().isAfter(it.endDate)
        }
    }

    // ==================== CLEANUP ====================

    /**
     * Cleanup khi ViewModel bị hủy
     */
    override fun onCleared() {
        super.onCleared()
        stopRealTimeUpdates()
        Log.d(TAG, "BudgetViewModel đã được giải phóng")
    }
}