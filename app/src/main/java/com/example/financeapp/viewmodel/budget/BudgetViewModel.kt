package com.example.financeapp.viewmodel.budget

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.math.abs

/**
 * ViewModel quản lý ngân sách
 * Xử lý CRUD operations cho ngân sách và đồng bộ với Firestore
 */
class BudgetViewModel : ViewModel() {

    companion object {
        private const val TAG = "BudgetViewModel"
        private const val COLLECTION_NAME = "budgets"
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

    // ==================== DEPENDENCIES ====================

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // ==================== INITIALIZATION ====================

    init {
        Log.d(TAG, "BudgetViewModel khởi tạo")
        loadBudgetsFromFirebase()
        startRealTimeUpdates()

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
        return auth.currentUser?.uid ?: "default_user".also {
            Log.w(TAG, "User chưa đăng nhập, sử dụng default_user")
        }
    }

    /**
     * Lấy collection ngân sách của user hiện tại
     */
    private fun getBudgetsCollection() =
        db.collection("users").document(getCurrentUserId()).collection(COLLECTION_NAME)

    // ==================== DATA LOADING ====================

    /**
     * Tải danh sách ngân sách từ Firebase
     */
    private fun loadBudgetsFromFirebase() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Đang tải ngân sách từ Firebase...")
                val querySnapshot = getBudgetsCollection().get().await()
                val budgetsList = querySnapshot.documents.mapNotNull { documentToBudget(it) }
                _budgets.value = budgetsList
                updateExceededBudgetsList()
                Log.d(TAG, "Đã tải ${budgetsList.size} ngân sách từ Firebase")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi tải ngân sách: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Chuyển đổi DocumentSnapshot sang Budget object
     */
    private fun documentToBudget(document: DocumentSnapshot): Budget? {
        return try {
            val data = document.data ?: run {
                Log.w(TAG, "Document ${document.id} không có data")
                return null
            }

            val periodType = when (data["periodType"] as? String) {
                "WEEK" -> BudgetPeriodType.WEEK
                "MONTH" -> BudgetPeriodType.MONTH
                "QUARTER" -> BudgetPeriodType.QUARTER
                "YEAR" -> BudgetPeriodType.YEAR
                else -> {
                    Log.w(TAG, "Unknown periodType: ${data["periodType"]}, sử dụng MONTH")
                    BudgetPeriodType.MONTH
                }
            }

            Budget(
                id = document.id,
                categoryId = data["categoryId"] as? String ?: "",
                amount = (data["amount"] as? Double) ?: 0.0,
                periodType = periodType,
                startDate = LocalDate.parse(
                    data["startDate"] as? String ?: LocalDate.now().toString()
                ),
                endDate = LocalDate.parse(data["endDate"] as? String ?: LocalDate.now().toString()),
                note = data["note"] as? String,
                spentAmount = (data["spentAmount"] as? Double) ?: 0.0,
                isActive = data["isActive"] as? Boolean ?: true,
                spent = (data["spent"] as? Double) ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi converting document to Budget: ${e.message}")
            null
        }
    }

    /**
     * Chuyển đổi Budget object sang Map cho Firestore
     */
    private fun budgetToMap(budget: Budget): Map<String, Any> = mapOf(
        "id" to budget.id,
        "categoryId" to budget.categoryId,
        "amount" to budget.amount,
        "periodType" to budget.periodType.name,
        "startDate" to budget.startDate.toString(),
        "endDate" to budget.endDate.toString(),
        "note" to (budget.note ?: ""),
        "spentAmount" to budget.spentAmount,
        "isActive" to budget.isActive,
        "spent" to budget.spent
    )

    // ==================== CRUD OPERATIONS ====================

    /**
     * Thêm ngân sách mới
     * @param budget Ngân sách cần thêm
     */
    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                getBudgetsCollection().document(budget.id).set(budgetToMap(budget)).await()
                _budgets.value = _budgets.value + budget
                updateExceededBudgetsList()
                Log.d(TAG, "Đã thêm ngân sách mới: ${budget.categoryId}")
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
                getBudgetsCollection().document(updatedBudget.id).set(budgetToMap(updatedBudget)).await()
                _budgets.value = _budgets.value.map { if (it.id == updatedBudget.id) updatedBudget else it }
                updateExceededBudgetsList()
                Log.d(TAG, "Đã cập nhật ngân sách: ${updatedBudget.categoryId}")
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
                getBudgetsCollection().document(budgetId).delete().await()
                _budgets.value = _budgets.value.filter { it.id != budgetId }
                updateExceededBudgetsList()
                Log.d(TAG, "Đã xóa ngân sách: $budgetId")
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

                // Tạo list mới để trigger UI recompose
                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList
                updateExceededBudgetsList()

                // Đồng bộ lên Firestore
                getBudgetsCollection().document(updated.id).update(
                    mapOf(
                        "spent" to updated.spent,
                        "spentAmount" to updated.spentAmount
                    )
                ).await()

                Log.d(TAG, "Đã cập nhật ngân sách ${updated.categoryId}: spent=${updated.spentAmount}, vượt quá: $isExceeded")

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

                        // Lưu ngân sách mới lên Firebase
                        getBudgetsCollection().document(renewedBudget.id)
                            .set(budgetToMap(renewedBudget)).await()

                        // Cập nhật local list
                        budgets[i] = renewedBudget
                        hasChanges = true

                        Log.d(TAG, "Đã reset ngân sách ${budget.categoryId} cho chu kỳ mới")
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
        Log.d(TAG, "Cập nhật danh sách vượt quá: ${exceeded.size} ngân sách")
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

                // Tạo list mới để trigger UI recompose
                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList
                updateExceededBudgetsList()

                // Đồng bộ lên Firestore
                getBudgetsCollection().document(updated.id).update(
                    mapOf(
                        "spent" to updated.spent,
                        "spentAmount" to updated.spentAmount
                    )
                ).await()

                Log.d(TAG, "Đã giảm ngân sách ${updated.categoryId}: spent=${updated.spentAmount} (giảm ${abs(amount)})")

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi giảm ngân sách: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật lại toàn bộ ngân sách (khi import/cập nhật hàng loạt)
     */
    fun recalculateAllBudgets() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Đang tính toán lại toàn bộ ngân sách...")

                // Cập nhật từ dữ liệu giao dịch thực tế
                // (Cần tích hợp với TransactionViewModel)
                val budgets = _budgets.value.toMutableList()
                var hasChanges = false

                // Ở đây bạn có thể tính toán lại từ transaction data
                // Tạm thời chỉ log để debug
                budgets.forEach { budget ->
                    Log.d(TAG, "Budget ${budget.categoryId}: amount=${budget.amount}, spent=${budget.spent}")
                }

                Log.d(TAG, "Đã tính toán lại ${budgets.size} ngân sách")

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi tính toán lại ngân sách: ${e.message}")
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
     * Bắt đầu real-time updates từ Firestore
     */
    fun startRealTimeUpdates() {
        getBudgetsCollection().addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen failed: $error")
                return@addSnapshotListener
            }
            snapshot?.let {
                _budgets.value = it.documents.mapNotNull { doc -> documentToBudget(doc) }
                updateExceededBudgetsList()
                Log.d(TAG, "Real-time update: ${_budgets.value.size} budgets")
            }
        }
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
}