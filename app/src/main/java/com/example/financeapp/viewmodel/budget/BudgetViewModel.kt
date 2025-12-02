package com.example.financeapp.viewmodel.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.Budget
import com.example.financeapp.data.BudgetPeriodType
import com.example.financeapp.viewmodel.CategoryViewModel
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

class BudgetViewModel : ViewModel() {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        println("🔥 BudgetViewModel init")
        loadBudgetsFromFirebase()
    }

    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: "default_user"

    private fun getBudgetsCollection() =
        db.collection("users").document(getCurrentUserId()).collection("budgets")

    private fun loadBudgetsFromFirebase() {
        viewModelScope.launch {
            try {
                println("🔥 Loading budgets from Firebase...")
                val querySnapshot = getBudgetsCollection().get().await()
                val budgetsList = querySnapshot.documents.mapNotNull { documentToBudget(it) }
                _budgets.value = budgetsList
                println("🔥 Loaded ${_budgets.value.size} budgets from Firebase")
            } catch (e: Exception) {
                println("❌ Error loading budgets: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun documentToBudget(document: DocumentSnapshot): Budget? {
        return try {
            val data = document.data ?: return null

            val periodType = when (data["periodType"] as? String) {
                "WEEK" -> BudgetPeriodType.WEEK
                "MONTH" -> BudgetPeriodType.MONTH
                "QUARTER" -> BudgetPeriodType.QUARTER
                "YEAR" -> BudgetPeriodType.YEAR
                else -> BudgetPeriodType.MONTH
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
            println("❌ Error converting document to Budget: ${e.message}")
            null
        }
    }

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

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                getBudgetsCollection().document(budget.id).set(budgetToMap(budget)).await()
                _budgets.value = _budgets.value + budget
            } catch (e: Exception) {
                println("❌ Error adding budget: ${e.message}")
            }
        }
    }

    fun updateFullBudget(updatedBudget: Budget) {
        viewModelScope.launch {
            try {
                getBudgetsCollection().document(updatedBudget.id).set(budgetToMap(updatedBudget)).await()
                _budgets.value = _budgets.value.map { if (it.id == updatedBudget.id) updatedBudget else it }
            } catch (e: Exception) {
                println("❌ Error updating budget: ${e.message}")
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                getBudgetsCollection().document(budgetId).delete().await()
                _budgets.value = _budgets.value.filter { it.id != budgetId }
            } catch (e: Exception) {
                println("❌ Error deleting budget: ${e.message}")
            }
        }
    }

    fun startRealTimeUpdates() {
        getBudgetsCollection().addSnapshotListener { snapshot, error ->
            if (error != null) {
                println("❌ Listen failed: $error")
                return@addSnapshotListener
            }
            snapshot?.let {
                _budgets.value = it.documents.mapNotNull { doc -> documentToBudget(doc) }
            }
        }
    }

    fun updateBudgetAfterTransaction(categoryId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val budgets = _budgets.value.toMutableList()
                val index = budgets.indexOfFirst { it.categoryId == categoryId && it.isActive }
                if (index == -1) {
                    println("⚠️ Không tìm thấy ngân sách cho categoryId: $categoryId")
                    return@launch
                }

                val budget = budgets[index]
                val newSpent = budget.spent + abs(amount)
                val updated = budget.copy(spent = newSpent, spentAmount = newSpent)

                // ✅ tạo list mới để trigger UI recompose
                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList

                // ✅ đồng bộ lên Firestore
                getBudgetsCollection().document(updated.id).update(
                    mapOf(
                        "spent" to updated.spent,
                        "spentAmount" to updated.spentAmount
                    )
                ).await()

                println("✅ Đã cập nhật ngân sách ${updated.categoryId}: spent=${updated.spentAmount}")
            } catch (e: Exception) {
                println("❌ Lỗi khi cập nhật ngân sách: ${e.message}")
            }
        }
    }


    fun calculateBudgetEndDate(startDate: LocalDate, periodType: BudgetPeriodType): LocalDate {
        return when (periodType) {
            BudgetPeriodType.WEEK -> startDate.plusWeeks(1)
            BudgetPeriodType.MONTH -> startDate.plusMonths(1)
            BudgetPeriodType.QUARTER -> startDate.plusMonths(3)
            BudgetPeriodType.YEAR -> startDate.plusYears(1)
        }
    }

    fun getCategoryName(categoryId: String, categoryViewModel: CategoryViewModel): String {
        val category = categoryViewModel.categories.value.find { it.id == categoryId }
        return category?.name ?: "Không xác định"
    }

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
}