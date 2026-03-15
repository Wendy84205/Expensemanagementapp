package com.example.financeapp.viewmodel.budget

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.data.remote.FirestoreService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

class BudgetViewModel : ViewModel() {

    // State flows
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    private val _exceededBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val exceededBudgets: StateFlow<List<Budget>> = _exceededBudgets

    private val _budgetExceededEvent = MutableStateFlow<Pair<Budget, Double>?>(null)
    val budgetExceededEvent: StateFlow<Pair<Budget, Double>?> = _budgetExceededEvent

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // Dependencies
    private val firestoreService = FirestoreService()
    private val auth = Firebase.auth
    private var budgetsListener: ListenerRegistration? = null

    // Theo dõi user hiện tại để reload ngân sách khi đổi user
    private var lastUserId: String? = null

    init {
        // Lần đầu khởi tạo: tải ngân sách cho user hiện tại (nếu có)
        lastUserId = getCurrentUserId()
        loadBudgetsFromFirestore()

        // Lắng nghe thay đổi đăng nhập để đảm bảo mỗi user có dữ liệu riêng
        auth.addAuthStateListener { firebaseAuth ->
            val newUserId = firebaseAuth.currentUser?.uid ?: "anonymous"
            if (newUserId != lastUserId) {
                lastUserId = newUserId
                stopRealTimeUpdates()
                // Xóa dữ liệu cũ trong ViewModel để tránh hiển thị nhầm user
                _budgets.value = emptyList()
                _exceededBudgets.value = emptyList()
                _budgetExceededEvent.value = null
                loadBudgetsFromFirestore()
            }
        }

        viewModelScope.launch {
            delay(1000)
            checkAndResetExpiredBudgets()
            updateExceededBudgetsList()
        }
    }

    // ==================== CORE METHODS ====================

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    /**
     * Khởi động real-time updates
     */
    fun startRealTimeUpdates() {
        val userId = getCurrentUserId()
        if (userId == "anonymous") return

        try {
            _isLoading.value = true
            budgetsListener = firestoreService.setupBudgetsListener(
                userId = userId,
                onBudgetsUpdated = { budgetsList ->
                    _budgets.value = budgetsList
                    updateExceededBudgetsList()
                    _isLoading.value = false
                },
                onError = { error ->
                    _isLoading.value = false
                    _errorMessage.value = "Không thể kết nối real-time: $error"
                }
            )
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = "Lỗi khởi động real-time: ${e.message}"
        }
    }

    /**
     * Dừng real-time updates
     */
    fun stopRealTimeUpdates() {
        budgetsListener?.remove()
        budgetsListener = null
    }

    /**
     * Tải ngân sách từ Firestore
     */
    private fun loadBudgetsFromFirestore() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _errorMessage.value = "Vui lòng đăng nhập để xem ngân sách"
                    _isLoading.value = false
                    return@launch
                }

                val budgetsList = firestoreService.getBudgets(userId)
                _budgets.value = budgetsList
                updateExceededBudgetsList()

                _successMessage.value = "Đã tải ${budgetsList.size} ngân sách"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể tải ngân sách: ${e.message}"
                _budgets.value = emptyList()

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Thêm ngân sách mới
     */
    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _errorMessage.value = "Vui lòng đăng nhập để thêm ngân sách"
                    return@launch
                }

                // Kiểm tra ngân sách trùng
                val existingBudget = _budgets.value.find {
                    it.categoryId == budget.categoryId && it.isActive
                }

                if (existingBudget != null) {
                    _errorMessage.value = "Đã có ngân sách cho danh mục này. Vui lòng cập nhật thay vì tạo mới."
                    return@launch
                }

                // Tạo budget với ID mới
                val budgetWithId = budget.copy(
                    id = if (budget.id.isBlank()) {
                        "budget_${System.currentTimeMillis()}"
                    } else budget.id,
                    userId = userId
                )

                // Lưu lên Firestore
                firestoreService.saveBudget(budgetWithId, userId)

                // Cập nhật local state
                val currentList = _budgets.value.toMutableList()
                currentList.add(budgetWithId)
                _budgets.value = currentList
                updateExceededBudgetsList()

                _successMessage.value = "Đã thêm ngân sách ${formatCurrency(budget.amount)} cho danh mục ${budget.categoryId}"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể thêm ngân sách: ${e.message}"

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cập nhật toàn bộ ngân sách
     */
    fun updateFullBudget(updatedBudget: Budget) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _errorMessage.value = "Vui lòng đăng nhập để cập nhật ngân sách"
                    return@launch
                }

                // Kiểm tra tồn tại
                val existingBudget = _budgets.value.find { it.id == updatedBudget.id }
                if (existingBudget == null) {
                    _errorMessage.value = "Ngân sách không tồn tại"
                    return@launch
                }

                val budgetWithUserId = updatedBudget.copy(userId = userId)

                // Lưu lên Firestore
                firestoreService.saveBudget(budgetWithUserId, userId)

                // Cập nhật local state
                _budgets.value = _budgets.value.map {
                    if (it.id == updatedBudget.id) budgetWithUserId else it
                }
                updateExceededBudgetsList()

                _successMessage.value = "Đã cập nhật ngân sách thành công"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể cập nhật ngân sách: ${e.message}"

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Xóa ngân sách
     */
    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _errorMessage.value = "Vui lòng đăng nhập để xóa ngân sách"
                    return@launch
                }

                // Kiểm tra tồn tại
                val existingBudget = _budgets.value.find { it.id == budgetId }
                if (existingBudget == null) {
                    _errorMessage.value = "Ngân sách không tồn tại"
                    return@launch
                }

                // Xóa khỏi Firestore
                firestoreService.deleteBudget(budgetId, userId)

                // Cập nhật local state
                _budgets.value = _budgets.value.filter { it.id != budgetId }
                updateExceededBudgetsList()

                _successMessage.value = "Đã xóa ngân sách thành công"

            } catch (e: Exception) {
                _errorMessage.value = "Không thể xóa ngân sách: ${e.message}"

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cập nhật ngân sách sau khi thêm giao dịch
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
                val now = LocalDate.now()

                // Tìm ngân sách active cho category
                val index = budgets.indexOfFirst {
                    it.categoryId == categoryId &&
                            it.isActive &&
                            !now.isBefore(it.startDate) &&
                            !now.isAfter(it.endDate)
                }

                if (index == -1) return@launch

                val budget = budgets[index]
                val newSpentAmount = budget.spentAmount + abs(amount)
                val updated = budget.copy(spentAmount = newSpentAmount)

                // Kiểm tra vượt ngân sách
                val exceededAmount = newSpentAmount - budget.amount
                val isExceeded = exceededAmount > 0

                // Trigger event nếu vượt
                if (isExceeded && triggerNotification) {
                    _budgetExceededEvent.value = updated to exceededAmount
                }

                // Cập nhật local state
                budgets[index] = updated
                _budgets.value = budgets
                updateExceededBudgetsList()

                // Lưu lên Firestore
                firestoreService.saveBudget(updated, userId)

            } catch (e: Exception) {
                // Silent fail - không hiển thị lỗi cho người dùng
                Log.e("BudgetViewModel", "Error updating budget: ${e.message}")
            }
        }
    }

    /**
     * Giảm ngân sách sau khi xóa giao dịch
     */
    fun decreaseBudgetAfterDeletion(categoryId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val budgets = _budgets.value.toMutableList()
                val now = LocalDate.now()

                // Tìm ngân sách active
                val index = budgets.indexOfFirst {
                    it.categoryId == categoryId &&
                            it.isActive &&
                            !now.isBefore(it.startDate) &&
                            !now.isAfter(it.endDate)
                }

                if (index == -1) return@launch

                val budget = budgets[index]
                val newSpentAmount = (budget.spentAmount - abs(amount)).coerceAtLeast(0.0)
                val updated = budget.copy(spentAmount = newSpentAmount)

                // Cập nhật local state
                budgets[index] = updated
                _budgets.value = budgets
                updateExceededBudgetsList()

                // Lưu lên Firestore
                firestoreService.saveBudget(updated, userId)

            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error decreasing budget: ${e.message}")
            }
        }
    }

    /**
     * Kiểm tra và reset ngân sách hết hạn
     */
    private fun checkAndResetExpiredBudgets() {
        viewModelScope.launch {
            try {
                val currentDate = LocalDate.now()
                val budgets = _budgets.value.toMutableList()
                var hasChanges = false

                for (i in budgets.indices) {
                    val budget = budgets[i]

                    // Kiểm tra nếu đã hết hạn nhưng vẫn active
                    if (currentDate.isAfter(budget.endDate) && budget.isActive) {
                        // Tạo ngân sách mới cho chu kỳ tiếp theo
                        val renewedBudget = budget.copy(
                            id = "budget_${System.currentTimeMillis()}_${i}",
                            startDate = currentDate,
                            endDate = calculateBudgetEndDate(currentDate, budget.periodType),
                            spentAmount = 0.0,
                            isActive = true
                        )

                        // Deactivate ngân sách cũ
                        val oldBudget = budgets[i].copy(isActive = false)

                        val userId = getCurrentUserId()
                        firestoreService.saveBudget(oldBudget.copy(userId = userId), userId)
                        firestoreService.saveBudget(renewedBudget.copy(userId = userId), userId)

                        budgets[i] = oldBudget
                        budgets.add(renewedBudget)
                        hasChanges = true
                    }
                }

                if (hasChanges) {
                    _budgets.value = budgets
                    updateExceededBudgetsList()
                }

            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error resetting expired budgets: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật danh sách ngân sách vượt
     */
    private fun updateExceededBudgetsList() {
        val exceeded = _budgets.value.filter { it.isOverBudget && it.isActive }
        _exceededBudgets.value = exceeded
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Tạo ngân sách mới
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
            id = "budget_${System.currentTimeMillis()}",
            categoryId = categoryId,
            amount = amount,
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            note = note,
            spentAmount = 0.0,
            isActive = true
        )
    }

    /**
     * Lấy ngân sách cho category
     */
    fun getBudgetForCategory(categoryId: String): Budget? {
        val now = LocalDate.now()
        return _budgets.value.find {
            it.categoryId == categoryId &&
                    it.isActive &&
                    !now.isBefore(it.startDate) &&
                    !now.isAfter(it.endDate)
        }
    }

    /**
     * Lấy danh sách ngân sách active
     */
    fun getActiveBudgets(): List<Budget> {
        val now = LocalDate.now()
        return _budgets.value.filter {
            it.isActive && !now.isBefore(it.startDate) && !now.isAfter(it.endDate)
        }
    }

    /**
     * Lấy danh sách ngân sách hết hạn
     */
    fun getExpiredBudgets(): List<Budget> {
        return _budgets.value.filter {
            it.isActive && LocalDate.now().isAfter(it.endDate)
        }
    }

    /**
     * Lấy tổng số tiền vượt ngân sách
     */
    fun getTotalExceededAmount(): Double {
        return _budgets.value.sumOf { budget ->
            if (budget.isOverBudget && budget.isActive) {
                (budget.spentAmount - budget.amount).coerceAtLeast(0.0)
            } else 0.0
        }
    }

    /**
     * Lấy tổng ngân sách
     */
    fun getTotalBudgetAmount(): Double {
        val now = LocalDate.now()
        return _budgets.value
            .filter { it.isActive && !now.isBefore(it.startDate) && !now.isAfter(it.endDate) }
            .sumOf { it.amount }
    }

    /**
     * Lấy tổng đã chi tiêu
     */
    fun getTotalSpentAmount(): Double {
        val now = LocalDate.now()
        return _budgets.value
            .filter { it.isActive && !now.isBefore(it.startDate) && !now.isAfter(it.endDate) }
            .sumOf { it.spentAmount }
    }

    /**
     * Lấy tỷ lệ sử dụng ngân sách
     */
    fun getBudgetUsageRate(): Double {
        val totalBudget = getTotalBudgetAmount()
        if (totalBudget == 0.0) return 0.0
        return (getTotalSpentAmount() / totalBudget) * 100
    }

    /**
     * Lấy số lượng ngân sách đang hoạt động
     */
    fun getActiveBudgetCount(): Int {
        return getActiveBudgets().size
    }

    /**
     * Lấy số lượng ngân sách đã vượt
     */
    fun getExceededBudgetCount(): Int {
        return _exceededBudgets.value.size
    }

    /**
     * Lấy số lượng ngân sách sắp vượt (>80%)
     */
    fun getNearExceededBudgets(): List<Budget> {
        val now = LocalDate.now()
        return _budgets.value.filter {
            it.isActive &&
                    !now.isBefore(it.startDate) &&
                    !now.isAfter(it.endDate) &&
                    it.amount > 0 &&
                    (it.spentAmount / it.amount) >= 0.8 &&
                    (it.spentAmount / it.amount) < 1.0
        }
    }

    /**
     * Lấy ngân sách theo ID
     */
    fun getBudgetById(budgetId: String): Budget? {
        return _budgets.value.find { it.id == budgetId }
    }

    /**
     * Lấy ngân sách theo category với thời gian
     */
    fun getBudgetForCategoryAndPeriod(
        categoryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Budget? {
        return _budgets.value.find {
            it.categoryId == categoryId &&
                    it.isActive &&
                    !startDate.isBefore(it.startDate) &&
                    !endDate.isAfter(it.endDate)
        }
    }

    /**
     * Kiểm tra category có ngân sách không
     */
    fun hasBudgetForCategory(categoryId: String): Boolean {
        return getBudgetForCategory(categoryId) != null
    }

    /**
     * Lấy tất cả ngân sách cho category (bao gồm cả đã hết hạn)
     */
    fun getAllBudgetsForCategory(categoryId: String): List<Budget> {
        return _budgets.value.filter { it.categoryId == categoryId }
    }

    /**
     * Refresh dữ liệu
     */
    fun refreshBudgets() {
        loadBudgetsFromFirestore()
    }

    /**
     * Clear events
     */
    fun clearBudgetExceededEvent() {
        _budgetExceededEvent.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // ==================== HELPER METHODS ====================

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
     * Format currency
     */
    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }

    /**
     * Format percentage
     */
    private fun formatPercentage(value: Double): String {
        return "%.1f".format(value) + "%"
    }

    /**
     * Lấy thông tin chi tiết về ngân sách
     */
    fun getBudgetDetails(budgetId: String): String? {
        val budget = getBudgetById(budgetId) ?: return null

        val remaining = budget.amount - budget.spentAmount
        val percentage = if (budget.amount > 0) (budget.spentAmount / budget.amount * 100) else 0.0
        val daysLeft = calculateDaysLeft(budget.endDate)

        return """
            📊 Ngân sách: ${formatCurrency(budget.amount)}
            💸 Đã chi: ${formatCurrency(budget.spentAmount)} (${formatPercentage(percentage)})
            💰 Còn lại: ${formatCurrency(remaining)}
            📅 Thời gian: ${budget.startDate} → ${budget.endDate}
            ⏳ Còn ${daysLeft} ngày
            ${if (budget.isOverBudget) "⚠️ ĐÃ VƯỢT NGÂN SÁCH!" else "✅ Đang trong ngân sách"}
        """.trimIndent()
    }

    /**
     * Tính số ngày còn lại
     */
    private fun calculateDaysLeft(endDate: LocalDate): Long {
        val today = LocalDate.now()
        return if (today.isAfter(endDate)) 0 else
            endDate.toEpochDay() - today.toEpochDay()
    }

    /**
     * Get budget status summary
     */
    fun getBudgetStatusSummary(): String {
        val active = getActiveBudgetCount()
        val exceeded = getExceededBudgetCount()
        val nearExceeded = getNearExceededBudgets().size
        val totalBudget = getTotalBudgetAmount()
        val totalSpent = getTotalSpentAmount()
        val usageRate = getBudgetUsageRate()

        return """
            📊 TỔNG QUAN NGÂN SÁCH
            
            • Số lượng: $active ngân sách đang hoạt động
            • Tổng ngân sách: ${formatCurrency(totalBudget)}
            • Đã chi: ${formatCurrency(totalSpent)} (${formatPercentage(usageRate)})
            • Vượt: $exceeded ngân sách
            • Sắp vượt: $nearExceeded ngân sách (>80%)
            
            ${if (exceeded > 0) "⚠️ Có $exceeded ngân sách đang vượt!" else "✅ Tất cả ngân sách đang trong tầm kiểm soát"}
        """.trimIndent()
    }

    /**
     * Dọn dẹp khi ViewModel bị hủy
     */
    override fun onCleared() {
        super.onCleared()
        stopRealTimeUpdates()
    }
}