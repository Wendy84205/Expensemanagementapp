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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // ==================== DEPENDENCIES ====================

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // ==================== INITIALIZATION ====================

    init {
        Log.d(TAG, "BudgetViewModel khởi tạo")
        loadBudgetsFromFirebase()
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
     */
    fun updateBudgetAfterTransaction(categoryId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val budgets = _budgets.value.toMutableList()
                val index = budgets.indexOfFirst { it.categoryId == categoryId && it.isActive }
                if (index == -1) {
                    Log.w(TAG, "Không tìm thấy ngân sách cho categoryId: $categoryId")
                    return@launch
                }

                val budget = budgets[index]
                val newSpent = budget.spent + abs(amount)
                val updated = budget.copy(spent = newSpent, spentAmount = newSpent)

                // Tạo list mới để trigger UI recompose
                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList

                // Đồng bộ lên Firestore
                getBudgetsCollection().document(updated.id).update(
                    mapOf(
                        "spent" to updated.spent,
                        "spentAmount" to updated.spentAmount
                    )
                ).await()

                Log.d(TAG, "Đã cập nhật ngân sách ${updated.categoryId}: spent=${updated.spentAmount}")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi cập nhật ngân sách: ${e.message}")
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
                Log.d(TAG, "Real-time update: ${_budgets.value.size} budgets")
            }
        }
    }
}