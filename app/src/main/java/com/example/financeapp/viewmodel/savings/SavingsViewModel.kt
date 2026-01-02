package com.example.financeapp.viewmodel.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.SavingsGoal
import com.example.financeapp.data.remote.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

class SavingsViewModel : ViewModel() {
    private val firestoreService = FirestoreService()
    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    // ========== STATE FLOWS ==========
    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoal>> = _savingsGoals.asStateFlow()

    private val _selectedGoal = MutableStateFlow<SavingsGoal?>(null)
    val selectedGoal: StateFlow<SavingsGoal?> = _selectedGoal.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _addSuccess = MutableStateFlow(false)
    val addSuccess: StateFlow<Boolean> = _addSuccess.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    private val _withdrawSuccess = MutableStateFlow(false)
    val withdrawSuccess: StateFlow<Boolean> = _withdrawSuccess.asStateFlow()

    private val _totalIncome = MutableStateFlow(0L)
    val totalIncome: StateFlow<Long> = _totalIncome.asStateFlow()

    private val _totalExpense = MutableStateFlow(0L)
    val totalExpense: StateFlow<Long> = _totalExpense.asStateFlow()

    private val _availableSavings = MutableStateFlow(0L)
    val availableSavings: StateFlow<Long> = _availableSavings.asStateFlow()

    private val _monthlyAnalysis = MutableStateFlow(MonthlyAnalysis())
    val monthlyAnalysis: StateFlow<MonthlyAnalysis> = _monthlyAnalysis.asStateFlow()

    private val _transactionHistory = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactionHistory: StateFlow<List<TransactionRecord>> = _transactionHistory.asStateFlow()

    private val _suggestedAllocation = MutableStateFlow<SuggestedAllocation?>(null)
    val suggestedAllocation: StateFlow<SuggestedAllocation?> = _suggestedAllocation.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    // ========== SUPPORTING DATA CLASSES ==========
    data class MonthlyAnalysis(
        val income: Long = 0,
        val expense: Long = 0,
        val savings: Long = 0,
        val savingsRate: Float = 0f
    )

    data class TransactionRecord(
        val id: String = "",
        val goalId: String = "",
        val goalName: String = "",
        val amount: Long = 0,
        val type: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val userId: String = "",
        val description: String = "",
        val balanceAfter: Long = 0
    )

    data class SuggestedAllocation(
        val amount: Long,
        val source: String,
        val category: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val percentage: Int = 0,
        val suggestedGoals: List<String> = emptyList(),
        val originalAverage: Long = 0L,
        val description: String = ""
    )

    // ========== PUBLIC PROPERTIES ==========
    val currentUserId: String?
        get() = auth.currentUser?.uid

    // ========== CRUD OPERATIONS ==========

    // 1. TẢI DANH SÁCH MỤC TIÊU
    fun loadSavingsGoals() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserId
                if (userId != null) {
                    val goals = firestoreService.getSavingsGoals(userId)
                    _savingsGoals.value = goals

                    goals.filter { it.isActive }.forEach { goal ->
                        calculateAutoSavings(goal)
                    }

                    updateMonthlyAnalysis(userId)
                } else {
                    _error.value = "Vui lòng đăng nhập để xem mục tiêu tiết kiệm"
                }
            } catch (e: Exception) {
                _error.value = "Không thể tải mục tiêu tiết kiệm"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 2. THÊM MỤC TIÊU MỚI
    fun addSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _addSuccess.value = false
            try {
                val userId = currentUserId
                if (userId != null) {
                    val newGoal = goal.copy(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        progress = goal.calculateProgress(),
                        startDate = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    firestoreService.addSavingsGoal(newGoal)

                    val currentList = _savingsGoals.value.toMutableList()
                    currentList.add(newGoal)
                    _savingsGoals.value = currentList

                    _addSuccess.value = true
                } else {
                    _error.value = "Vui lòng đăng nhập để thêm mục tiêu"
                }
            } catch (e: Exception) {
                _error.value = "Không thể thêm mục tiêu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 3. THÊM MỤC TIÊU TỰ ĐỘNG VỚI TÍNH TOÁN %
    fun addAutoSavingsGoal(
        name: String,
        targetAmount: Long,
        allocationPercentage: Int,
        category: String = "Personal"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _addSuccess.value = false

            try {
                val userId = currentUserId
                if (userId != null) {
                    val newGoal = SavingsGoal(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        targetAmount = targetAmount,
                        currentAmount = 0L,
                        deadline = 0L,
                        category = category,
                        userId = userId,
                        color = 0,
                        icon = 0,
                        description = "Mục tiêu tự động tính từ thu nhập dư",
                        progress = 0f,
                        isCompleted = false,
                        monthlyContribution = 0L,
                        startDate = System.currentTimeMillis(),
                        isActive = true,
                        autoCalculate = true,
                        allocationPercentage = allocationPercentage
                    )

                    firestoreService.addSavingsGoal(newGoal)

                    val currentList = _savingsGoals.value.toMutableList()
                    currentList.add(newGoal)
                    _savingsGoals.value = currentList

                    calculateAutoSavings(newGoal)

                    _addSuccess.value = true
                } else {
                    _error.value = "Vui lòng đăng nhập"
                }
            } catch (e: Exception) {
                _error.value = "Không thể thêm mục tiêu tự động"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 4. CẬP NHẬT MỤC TIÊU
    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false
            try {
                val userId = currentUserId
                if (userId != null && goal.userId == userId) {
                    val updatedGoal = goal.copy(
                        progress = goal.calculateProgress(),
                        updatedAt = System.currentTimeMillis()
                    )

                    firestoreService.updateSavingsGoal(updatedGoal)

                    val currentList = _savingsGoals.value.toMutableList()
                    val index = currentList.indexOfFirst { it.id == goal.id }
                    if (index != -1) {
                        currentList[index] = updatedGoal
                        _savingsGoals.value = currentList
                    }

                    if (_selectedGoal.value?.id == goal.id) {
                        _selectedGoal.value = updatedGoal
                    }

                    _updateSuccess.value = true
                } else {
                    _error.value = "Không thể cập nhật mục tiêu"
                }
            } catch (e: Exception) {
                _error.value = "Không thể cập nhật mục tiêu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 5. XOÁ MỤC TIÊU
    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _deleteSuccess.value = false
            try {
                val userId = currentUserId
                val goal = _savingsGoals.value.find { it.id == goalId }

                if (userId != null && goal != null && goal.userId == userId) {
                    firestoreService.deleteSavingsGoal(goalId)

                    val currentList = _savingsGoals.value.toMutableList()
                    currentList.removeAll { it.id == goalId }
                    _savingsGoals.value = currentList

                    if (_selectedGoal.value?.id == goalId) {
                        _selectedGoal.value = null
                    }

                    _deleteSuccess.value = true
                } else {
                    _error.value = "Không thể xoá mục tiêu"
                }
            } catch (e: Exception) {
                _error.value = "Không thể xoá mục tiêu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 6. THÊM TIỀN VÀO MỤC TIÊU (GỬI TIỀN)
    fun addToSavingsGoal(goalId: String, amount: Long) {
        if (amount <= 0) {
            _error.value = "Số tiền phải lớn hơn 0"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserId
                val goal = _savingsGoals.value.find { it.id == goalId }

                if (userId != null && goal != null && goal.userId == userId) {
                    firestoreService.addToSavings(goalId, amount)

                    val updatedGoal = goal.copy(
                        currentAmount = goal.currentAmount + amount,
                        progress = goal.calculateProgress(),
                        isCompleted = goal.currentAmount + amount >= goal.targetAmount,
                        updatedAt = System.currentTimeMillis()
                    )

                    val currentList = _savingsGoals.value.toMutableList()
                    val index = currentList.indexOfFirst { it.id == goalId }
                    if (index != -1) {
                        currentList[index] = updatedGoal
                        _savingsGoals.value = currentList
                    }

                    if (_selectedGoal.value?.id == goalId) {
                        _selectedGoal.value = updatedGoal
                    }

                    createTransactionRecord(
                        TransactionRecord(
                            goalId = goalId,
                            goalName = goal.name,
                            amount = amount,
                            type = "deposit",
                            userId = userId,
                            description = "Thêm tiền thủ công",
                            balanceAfter = goal.currentAmount + amount
                        )
                    )
                } else {
                    _error.value = "Không thể thêm tiền vào mục tiêu"
                }
            } catch (e: Exception) {
                _error.value = "Không thể thêm tiền vào mục tiêu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 8. TẠM DỪNG/KÍCH HOẠT MỤC TIÊU
    fun toggleGoalActive(goalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserId
                val goal = _savingsGoals.value.find { it.id == goalId }

                if (userId != null && goal != null && goal.userId == userId) {
                    val updatedGoal = goal.copy(
                        isActive = !goal.isActive,
                        updatedAt = System.currentTimeMillis()
                    )

                    firestoreService.updateSavingsGoal(updatedGoal)

                    val currentList = _savingsGoals.value.toMutableList()
                    val index = currentList.indexOfFirst { it.id == goalId }
                    if (index != -1) {
                        currentList[index] = updatedGoal
                        _savingsGoals.value = currentList
                    }

                    if (_selectedGoal.value?.id == goalId) {
                        _selectedGoal.value = updatedGoal
                    }

                    val status = if (updatedGoal.isActive) "kích hoạt" else "tạm dừng"
                    _error.value = "Đã $status mục tiêu '${goal.name}'"
                }
            } catch (e: Exception) {
                _error.value = "Không thể thay đổi trạng thái mục tiêu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 9. ĐÁNH DẤU HOÀN THÀNH/CHƯA HOÀN THÀNH
    fun toggleGoalCompletion(goalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserId
                val goal = _savingsGoals.value.find { it.id == goalId }

                if (userId != null && goal != null && goal.userId == userId) {
                    val updatedGoal = goal.copy(
                        isCompleted = !goal.isCompleted,
                        updatedAt = System.currentTimeMillis()
                    )

                    firestoreService.updateSavingsGoal(updatedGoal)

                    val currentList = _savingsGoals.value.toMutableList()
                    val index = currentList.indexOfFirst { it.id == goalId }
                    if (index != -1) {
                        currentList[index] = updatedGoal
                        _savingsGoals.value = currentList
                    }

                    if (_selectedGoal.value?.id == goalId) {
                        _selectedGoal.value = updatedGoal
                    }

                    val status = if (updatedGoal.isCompleted) "hoàn thành" else "chưa hoàn thành"
                    _error.value = "Đã đánh dấu mục tiêu '${goal.name}' là $status"
                }
            } catch (e: Exception) {
                _error.value = "Không thể thay đổi trạng thái hoàn thành"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 10. TẢI LỊCH SỬ GIAO DỊCH
    fun loadTransactionHistory(goalId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserId ?: return@launch

                val query = db.collection("savings_transactions")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

                if (goalId != null) {
                    query.whereEqualTo("goalId", goalId)
                }

                val snapshot = query.get().await()
                val transactions = snapshot.documents.map { document ->
                    TransactionRecord(
                        id = document.id,
                        goalId = document.getString("goalId") ?: "",
                        goalName = document.getString("goalName") ?: "",
                        amount = document.getLong("amount") ?: 0L,
                        type = document.getString("type") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0L,
                        userId = document.getString("userId") ?: "",
                        description = document.getString("description") ?: "",
                        balanceAfter = document.getLong("balanceAfter") ?: 0L
                    )
                }

                _transactionHistory.value = transactions
            } catch (e: Exception) {
                _error.value = "Không thể tải lịch sử giao dịch"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== TÍNH TOÁN TỰ ĐỘNG ==========

    // Tính toán tiền tiết kiệm tự động dựa trên thu nhập và chi tiêu
    fun calculateAutoSavings(goal: SavingsGoal? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userId = currentUserId ?: run {
                    _error.value = "Vui lòng đăng nhập"
                    return@launch
                }

                val startDate = goal?.startDate ?: getStartOfCurrentMonth()
                val endDate = if (goal?.deadline ?: 0L > 0) {
                    goal?.deadline ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }

                val incomeInPeriod = getTotalIncomeByPeriod(
                    userId = userId,
                    startDate = startDate,
                    endDate = endDate
                )

                val expenseInPeriod = getTotalExpenseByPeriod(
                    userId = userId,
                    startDate = startDate,
                    endDate = endDate
                )

                val potentialSavings = incomeInPeriod - expenseInPeriod

                _totalIncome.value = incomeInPeriod
                _totalExpense.value = expenseInPeriod
                _availableSavings.value = potentialSavings.coerceAtLeast(0)

                goal?.let {
                    if (potentialSavings > 0 && it.isActive && it.autoCalculate) {
                        autoAllocateToGoal(it, potentialSavings)
                    }
                }

                updateMonthlyAnalysis(userId)

            } catch (e: Exception) {
                _error.value = "Không thể tính toán tiết kiệm tự động"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tính toán tiền tiết kiệm theo % thu nhập
    fun calculateSavingsByPercentage(percentage: Int) {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch

                val monthlyIncome = _monthlyAnalysis.value.income
                val amountByPercentage = (monthlyIncome * percentage) / 100

                if (amountByPercentage > 0) {
                    val activeGoals = getActiveGoals()
                    if (activeGoals.isNotEmpty()) {
                        val amountPerGoal = amountByPercentage / activeGoals.size
                        activeGoals.forEach { goal ->
                            addToSavingsGoal(goal.id, amountPerGoal)
                        }
                        _error.value = "Đã phân bổ ${formatCurrency(amountByPercentage)} vào ${activeGoals.size} mục tiêu"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Không thể tính theo %"
            }
        }
    }

    // ========== SERVICE FUNCTIONS ==========
    private suspend fun getTotalIncomeByPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Long {
        return try {
            val snapshot = db.collection("incomes")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            snapshot.documents.sumOf { document ->
                document.getLong("amount") ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun getTotalExpenseByPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Long {
        return try {
            val snapshot = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "expense")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            snapshot.documents.sumOf { document ->
                document.getLong("amount") ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun createTransactionRecord(record: TransactionRecord) {
        try {
            db.collection("savings_transactions")
                .add(record)
                .await()

            loadTransactionHistory(record.goalId)
        } catch (e: Exception) {
        }
    }

    // ========== HELPER FUNCTIONS ==========

    private suspend fun updateMonthlyAnalysis(userId: String) {
        try {
            val startOfMonth = getStartOfCurrentMonth()
            val endOfMonth = getEndOfCurrentMonth()

            val monthlyIncome = getTotalIncomeByPeriod(
                userId = userId,
                startDate = startOfMonth,
                endDate = endOfMonth
            )

            val monthlyExpense = getTotalExpenseByPeriod(
                userId = userId,
                startDate = startOfMonth,
                endDate = endOfMonth
            )

            val monthlySavings = monthlyIncome - monthlyExpense
            val savingsRate = if (monthlyIncome > 0) {
                (monthlySavings.toFloat() / monthlyIncome.toFloat() * 100).coerceAtLeast(0f)
            } else 0f

            _monthlyAnalysis.value = MonthlyAnalysis(
                income = monthlyIncome,
                expense = monthlyExpense,
                savings = monthlySavings.coerceAtLeast(0),
                savingsRate = savingsRate
            )

        } catch (e: Exception) {
        }
    }

    private fun autoAllocateToGoal(goal: SavingsGoal, availableAmount: Long) {
        viewModelScope.launch {
            try {
                val remainingNeeded = goal.targetAmount - goal.currentAmount
                val amountToAdd = minOf(availableAmount, remainingNeeded).coerceAtLeast(0)

                if (amountToAdd > 0) {
                    addToSavingsGoal(goal.id, amountToAdd)

                    createTransactionRecord(
                        TransactionRecord(
                            goalId = goal.id,
                            goalName = goal.name,
                            amount = amountToAdd,
                            type = "auto_allocation",
                            userId = goal.userId,
                            description = "Phân bổ tự động từ thu nhập dư",
                            balanceAfter = goal.currentAmount + amountToAdd
                        )
                    )

                    _error.value = "Đã tự động thêm ${formatCurrency(amountToAdd)} vào '${goal.name}'"
                }
            } catch (e: Exception) {
                _error.value = "Không thể phân bổ tự động"
            }
        }
    }

    // ========== CÁC HÀM CƠ BẢN KHÁC ==========
    fun selectGoal(goal: SavingsGoal?) {
        _selectedGoal.value = goal
        goal?.id?.let { loadTransactionHistory(it) }
    }

    fun updateGoalFields(goalId: String, fields: Map<String, Any>) {
        viewModelScope.launch {
            try {
                getGoalById(goalId)?.let { goal ->
                    var updatedGoal = goal

                    fields.forEach { (key, value) ->
                        updatedGoal = when (key) {
                            "name" -> updatedGoal.copy(name = value as String)
                            "targetAmount" -> updatedGoal.copy(targetAmount = value as Long)
                            "deadline" -> updatedGoal.copy(deadline = value as Long)
                            "description" -> updatedGoal.copy(description = value as String)
                            "category" -> updatedGoal.copy(category = value as String)
                            "color" -> updatedGoal.copy(color = value as Int)
                            "icon" -> updatedGoal.copy(icon = value as Int)
                            "monthlyContribution" -> updatedGoal.copy(monthlyContribution = value as Long)
                            "isActive" -> updatedGoal.copy(isActive = value as Boolean)
                            "currentAmount" -> updatedGoal.copy(currentAmount = value as Long)
                            "isCompleted" -> updatedGoal.copy(isCompleted = value as Boolean)
                            "autoCalculate" -> updatedGoal.copy(autoCalculate = value as Boolean)
                            "allocationPercentage" -> updatedGoal.copy(allocationPercentage = value as Int)
                            else -> updatedGoal
                        }
                    }

                    updatedGoal = updatedGoal.copy(
                        progress = updatedGoal.calculateProgress(),
                        updatedAt = System.currentTimeMillis()
                    )

                    updateSavingsGoal(updatedGoal)
                } ?: run {
                    _error.value = "Không tìm thấy mục tiêu"
                }
            } catch (e: Exception) {
                _error.value = "Không thể cập nhật mục tiêu"
            }
        }
    }

    fun quickAddGoal(name: String, targetAmount: Long, category: String = "Personal") {
        val goal = SavingsGoal(
            id = "",
            name = name,
            targetAmount = targetAmount,
            currentAmount = 0L,
            deadline = 0L,
            category = category,
            userId = "",
            color = 0,
            icon = 0,
            description = "",
            progress = 0f,
            isCompleted = false,
            monthlyContribution = 0L,
            startDate = System.currentTimeMillis(),
            isActive = true,
            autoCalculate = false,
            allocationPercentage = 0
        )
        addSavingsGoal(goal)
    }

    // ========== HÀM XỬ LÝ TỪ TRANSACTIONVIEWMODEL ==========

    /**
     * Xử lý khi có thu nhập mới được thêm
     */
    fun onIncomeAdded(amount: Long, description: String, category: String) {
        viewModelScope.launch {
            try {
                val currentIncome = _totalIncome.value
                _totalIncome.value = currentIncome + amount

                autoAllocateFromIncome(amount, description)

                _availableSavings.value = _totalIncome.value - _totalExpense.value

                _error.value = "Đã nhận thu nhập ${formatCurrency(amount)}"

            } catch (e: Exception) {
                _error.value = "Lỗi xử lý thu nhập"
            }
        }
    }

    /**
     * Tự động phân bổ từ thu nhập vào các goal
     */
    private fun autoAllocateFromIncome(amount: Long, sourceDescription: String) {
        viewModelScope.launch {
            try {
                val autoGoals = _savingsGoals.value.filter {
                    it.isActive && it.autoCalculate && !it.isCompleted
                }

                if (autoGoals.isNotEmpty()) {
                    var totalAllocated = 0L

                    autoGoals.forEach { goal ->
                        val allocatedAmount = (amount * goal.allocationPercentage) / 100

                        if (allocatedAmount > 0) {
                            addToSavingsGoal(goal.id, allocatedAmount)

                            createTransactionRecord(
                                TransactionRecord(
                                    goalId = goal.id,
                                    goalName = goal.name,
                                    amount = allocatedAmount,
                                    type = "auto_income_allocation",
                                    userId = currentUserId ?: "",
                                    description = "Tự động từ $sourceDescription",
                                    balanceAfter = goal.currentAmount + allocatedAmount
                                )
                            )

                            totalAllocated += allocatedAmount
                        }
                    }

                    if (totalAllocated > 0) {
                        _error.value = "Đã tự động phân bổ ${formatCurrency(totalAllocated)} vào ${autoGoals.size} mục tiêu"
                    }
                }

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Xử lý khi có chi tiêu mới
     */
    fun onExpenseAdded(amount: Long, category: String) {
        viewModelScope.launch {
            try {
                val currentExpense = _totalExpense.value
                _totalExpense.value = currentExpense + amount

                _availableSavings.value = _totalIncome.value - _totalExpense.value

                val income = _totalIncome.value
                if (income > 0) {
                    val expenseRate = (_totalExpense.value.toFloat() / income.toFloat()) * 100
                    if (expenseRate > 80) {
                        _warningMessage.value = "Chi tiêu đang cao (${expenseRate.toInt()}% thu nhập)"
                    }
                }

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Xử lý khi chi tiêu giảm (xóa hoặc sửa giảm)
     */
    fun onExpenseReduced(amount: Long, category: String) {
        viewModelScope.launch {
            try {
                _totalExpense.value = _totalExpense.value - amount

                _availableSavings.value = _availableSavings.value + amount

                suggestAllocationFromSavings(amount, "Tiết kiệm từ giảm chi $category")

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Đề xuất phân bổ từ khoản tiết kiệm
     */
    private fun suggestAllocationFromSavings(amount: Long, reason: String) {
        viewModelScope.launch {
            try {
                val activeGoals = getActiveGoals()

                if (activeGoals.isNotEmpty() && amount > 0) {
                    _suggestedAllocation.value = SuggestedAllocation(
                        amount = amount,
                        source = "expense_reduction",
                        category = reason,
                        timestamp = System.currentTimeMillis(),
                        suggestedGoals = activeGoals.map { it.id }
                    )

                    _warningMessage.value = "Bạn có ${formatCurrency(amount)} có thể thêm vào tiết kiệm. " +
                            "Từ: $reason"
                }

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Xử lý khi thu nhập giảm (xóa hoặc sửa giảm)
     */
    fun onIncomeReduced(amount: Long, description: String) {
        viewModelScope.launch {
            try {
                _totalIncome.value = _totalIncome.value - amount

                _availableSavings.value = _totalIncome.value - _totalExpense.value

                _warningMessage.value = "Thu nhập giảm ${formatCurrency(amount)}"

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Cập nhật thống kê tài chính từ TransactionViewModel
     */
    fun updateFinancialStats(monthlyIncome: Long, monthlyExpense: Long) {
        viewModelScope.launch {
            try {
                _totalIncome.value = monthlyIncome
                _totalExpense.value = monthlyExpense
                _availableSavings.value = (monthlyIncome - monthlyExpense).coerceAtLeast(0)

                val savingsRate = if (monthlyIncome > 0) {
                    ((monthlyIncome - monthlyExpense).toFloat() / monthlyIncome.toFloat() * 100)
                        .coerceAtLeast(0f)
                } else 0f

                _monthlyAnalysis.value = MonthlyAnalysis(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    savings = (monthlyIncome - monthlyExpense).coerceAtLeast(0),
                    savingsRate = savingsRate
                )

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Đề xuất từ giảm chi tiêu (gọi từ TransactionViewModel)
     */
    fun suggestSavingsFromExpenseReduction(amount: Long, category: String, originalAverage: Long) {
        viewModelScope.launch {
            try {
                if (amount > 0) {
                    _suggestedAllocation.value = SuggestedAllocation(
                        amount = amount,
                        source = "expense_reduction",
                        category = category,
                        timestamp = System.currentTimeMillis(),
                        originalAverage = originalAverage,
                        description = "Bạn đã chi ít hơn ${formatCurrency(amount)} so với trung bình"
                    )

                    _warningMessage.value = "Đề xuất: Thêm ${formatCurrency(amount)} vào tiết kiệm " +
                            "(tiết kiệm từ $category)"
                }

            } catch (e: Exception) {
            }
        }
    }

    /**
     * Đề xuất phân bổ thủ công (gọi từ UI)
     */
    fun suggestManualAllocation(amount: Long, percentage: Int) {
        viewModelScope.launch {
            try {
                val activeGoals = getActiveGoals()

                if (activeGoals.isNotEmpty() && amount > 0) {
                    _suggestedAllocation.value = SuggestedAllocation(
                        amount = amount,
                        source = "manual_suggestion",
                        category = "Thu nhập dư",
                        timestamp = System.currentTimeMillis(),
                        percentage = percentage,
                        suggestedGoals = activeGoals.map { it.id }
                    )

                    _warningMessage.value = "Đề xuất phân bổ ${formatCurrency(amount)} " +
                            "($percentage% thu nhập dư) vào ${activeGoals.size} mục tiêu"
                }

            } catch (e: Exception) {
                _error.value = "Không thể tạo đề xuất"
            }
        }
    }

    // ========== HÀM CLEAR ==========
    fun clearSuggestedAllocation() {
        _suggestedAllocation.value = null
    }

    fun clearWarningMessage() {
        _warningMessage.value = null
    }

    // ========== UTILITY FUNCTIONS ==========
    fun getGoalById(goalId: String): SavingsGoal? {
        return _savingsGoals.value.find { it.id == goalId }
    }

    fun getTotalSaved(): Long {
        return _savingsGoals.value.sumOf { it.currentAmount }
    }

    fun getTotalTarget(): Long {
        return _savingsGoals.value.sumOf { it.targetAmount }
    }

    fun getCompletedGoals(): List<SavingsGoal> {
        return _savingsGoals.value.filter { it.isCompleted || it.progress >= 100f }
    }

    fun getActiveGoals(): List<SavingsGoal> {
        return _savingsGoals.value.filter { it.isActive && !it.isCompleted }
    }

    fun getInactiveGoals(): List<SavingsGoal> {
        return _savingsGoals.value.filter { !it.isActive }
    }

    fun resetAddSuccess() { _addSuccess.value = false }
    fun resetUpdateSuccess() { _updateSuccess.value = false }
    fun resetDeleteSuccess() { _deleteSuccess.value = false }
    fun resetWithdrawSuccess() { _withdrawSuccess.value = false }

    fun calculateProgress(goal: SavingsGoal): Float {
        return goal.calculateProgress()
    }

    fun calculateRemainingDays(deadline: Long): Long {
        if (deadline <= 0) return 0
        val currentTime = System.currentTimeMillis()
        val diff = deadline - currentTime
        return (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    }

    fun calculateMonthlyContribution(goal: SavingsGoal): Long {
        if (goal.deadline <= 0 || goal.targetAmount <= goal.currentAmount) return 0
        val remainingAmount = goal.targetAmount - goal.currentAmount
        val remainingDays = calculateRemainingDays(goal.deadline)
        val remainingMonths = (remainingDays / 30).coerceAtLeast(1)
        return (remainingAmount / remainingMonths).coerceAtLeast(1)
    }

    // ========== UTILITY FUNCTIONS ==========
    private fun getStartOfCurrentMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfCurrentMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun formatCurrency(amount: Long): String {
        return "%,d".format(amount).replace(",", ".")
    }
}