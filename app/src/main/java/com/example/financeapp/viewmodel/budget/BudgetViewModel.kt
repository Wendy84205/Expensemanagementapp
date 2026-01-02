package com.example.financeapp.viewmodel.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.data.models.calculateBudgetEndDate
import com.example.financeapp.data.models.isOverBudget
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

class BudgetViewModel : ViewModel() {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets

    private val _exceededBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val exceededBudgets: StateFlow<List<Budget>> = _exceededBudgets

    private val _budgetExceededEvent = MutableStateFlow<Pair<Budget, Double>?>(null)
    val budgetExceededEvent: StateFlow<Pair<Budget, Double>?> = _budgetExceededEvent

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val firestoreService = FirestoreService()
    private val auth = Firebase.auth
    private var budgetsListener: ListenerRegistration? = null

    init {
        loadBudgetsFromFirestore()
        viewModelScope.launch {
            delay(1000)
            checkAndResetExpiredBudgets()
            updateExceededBudgetsList()
        }
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

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
                }
            )
        } catch (e: Exception) {
            _isLoading.value = false
        }
    }

    fun stopRealTimeUpdates() {
        budgetsListener?.remove()
        budgetsListener = null
    }

    private fun loadBudgetsFromFirestore() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = getCurrentUserId()
                val budgetsList = firestoreService.getBudgets(userId)
                _budgets.value = budgetsList
                updateExceededBudgetsList()
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val budgetWithId = budget.copy(
                    id = if (budget.id.isBlank()) System.currentTimeMillis().toString() else budget.id,
                    userId = userId
                )
                firestoreService.saveBudget(budgetWithId, userId)
                _budgets.value = _budgets.value + budgetWithId
                updateExceededBudgetsList()
            } catch (e: Exception) {}
        }
    }

    fun updateFullBudget(updatedBudget: Budget) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val budgetWithUserId = updatedBudget.copy(userId = userId)
                firestoreService.saveBudget(budgetWithUserId, userId)
                _budgets.value = _budgets.value.map {
                    if (it.id == updatedBudget.id) budgetWithUserId else it
                }
                updateExceededBudgetsList()
            } catch (e: Exception) {}
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                firestoreService.deleteBudget(budgetId, userId)
                _budgets.value = _budgets.value.filter { it.id != budgetId }
                updateExceededBudgetsList()
            } catch (e: Exception) {}
        }
    }

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

                val exceededAmount = newSpentAmount - budget.amount
                val isExceeded = exceededAmount > 0

                if (isExceeded && triggerNotification) {
                    _budgetExceededEvent.value = updated to exceededAmount
                }

                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList
                updateExceededBudgetsList()
                firestoreService.saveBudget(updated, userId)
            } catch (e: Exception) {}
        }
    }

    private fun checkAndResetExpiredBudgets() {
        viewModelScope.launch {
            try {
                val currentDate = LocalDate.now()
                val budgets = _budgets.value.toMutableList()
                var hasChanges = false

                for (i in budgets.indices) {
                    val budget = budgets[i]
                    if (currentDate.isAfter(budget.endDate) && budget.isActive) {
                        val renewedBudget = budget.copy(
                            id = System.currentTimeMillis().toString(),
                            startDate = currentDate,
                            endDate = calculateBudgetEndDate(currentDate, budget.periodType),
                            spentAmount = 0.0,
                            isActive = true
                        )

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
            } catch (e: Exception) {}
        }
    }

    private fun updateExceededBudgetsList() {
        val exceeded = _budgets.value.filter { it.isOverBudget && it.isActive }
        _exceededBudgets.value = exceeded
    }

    fun clearBudgetExceededEvent() {
        _budgetExceededEvent.value = null
    }

    fun decreaseBudgetAfterDeletion(categoryId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val budgets = _budgets.value.toMutableList()
                val now = LocalDate.now()

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

                val newList = budgets.toMutableList().apply { set(index, updated) }.toList()
                _budgets.value = newList
                updateExceededBudgetsList()
                firestoreService.saveBudget(updated, userId)
            } catch (e: Exception) {}
        }
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
            isActive = true
        )
    }

    fun getBudgetForCategory(categoryId: String): Budget? {
        val now = LocalDate.now()
        return _budgets.value.find {
            it.categoryId == categoryId &&
                    it.isActive &&
                    !now.isBefore(it.startDate) &&
                    !now.isAfter(it.endDate)
        }
    }

    fun getTotalExceededAmount(): Double {
        return _budgets.value.sumOf { budget ->
            if (budget.isOverBudget && budget.isActive) (budget.spentAmount - budget.amount) else 0.0
        }
    }

    fun getTotalBudgetAmount(): Double {
        val now = LocalDate.now()
        return _budgets.value
            .filter { it.isActive && !now.isBefore(it.startDate) && !now.isAfter(it.endDate) }
            .sumOf { it.amount }
    }

    fun getTotalSpentAmount(): Double {
        val now = LocalDate.now()
        return _budgets.value
            .filter { it.isActive && !now.isBefore(it.startDate) && !now.isAfter(it.endDate) }
            .sumOf { it.spentAmount }
    }

    fun getActiveBudgets(): List<Budget> {
        val now = LocalDate.now()
        return _budgets.value.filter {
            it.isActive && !now.isBefore(it.startDate) && !now.isAfter(it.endDate)
        }
    }

    fun getExpiredBudgets(): List<Budget> {
        return _budgets.value.filter {
            it.isActive && LocalDate.now().isAfter(it.endDate)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealTimeUpdates()
    }
}