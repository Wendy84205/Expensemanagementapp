package com.example.financeapp.viewmodel.features

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.RecurringFrequency
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

typealias FinanceCategory = com.example.financeapp.viewmodel.transaction.Category

class RecurringExpenseViewModel : ViewModel() {

    companion object {
        private const val TAG = "RecurringExpenseViewModel"
        private const val COLLECTION_NAME = "recurring_expenses"
        private const val PREF_NAME = "recurring_expense_prefs"
        private const val KEY_LAST_PROCESSED_DATE = "last_processed_date"
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var expensesListener: ListenerRegistration? = null
    private val categoryViewModel = CategoryViewModel.getInstance()

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpense>> = _recurringExpenses

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private val _availableSubCategories = MutableStateFlow<Map<String, List<FinanceCategory>>>(emptyMap())
    val availableSubCategories: StateFlow<Map<String, List<FinanceCategory>>> = _availableSubCategories

    private var isListenerSetup = false

    init {
        setupRealtimeListener()
        loadAvailableSubCategories()
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    private fun loadAvailableSubCategories() {
        viewModelScope.launch {
            try {
                val expenseCategories = categoryViewModel.getSubCategoriesForRecurringExpense("expense")
                val incomeCategories = categoryViewModel.getSubCategoriesForRecurringExpense("income")

                _availableSubCategories.value = mapOf(
                    "expense" to expenseCategories,
                    "income" to incomeCategories
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun setupRealtimeListener() {
        if (isListenerSetup) {
            _isLoading.value = false
            return
        }

        val userId = getCurrentUserId()
        if (userId == "anonymous") {
            _isLoading.value = false
            isListenerSetup = true
            _uiMessage.value = "Vui lòng đăng nhập để xem chi tiêu định kỳ"
            return
        }

        if (_recurringExpenses.value.isEmpty()) {
            _isLoading.value = true
        }

        try {
            expensesListener = db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    _isLoading.value = false
                    isListenerSetup = true

                    if (error != null) {
                        _uiMessage.value = "Lỗi tải chi tiêu định kỳ"
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        val expenses = mutableListOf<RecurringExpense>()
                        for (document in querySnapshot.documents) {
                            try {
                                val expense = document.toObject(RecurringExpense::class.java)
                                expense?.let {
                                    if (isValidExpenseCategory(expense)) {
                                        expenses.add(it)
                                    }
                                }
                            } catch (e: Exception) {
                            }
                        }
                        _recurringExpenses.value = expenses
                    }

                    if (snapshot == null) {
                        _isLoading.value = false
                    }
                }
        } catch (e: Exception) {
            _isLoading.value = false
            isListenerSetup = true
            _uiMessage.value = "Lỗi kết nối"
        }
    }

    fun loadRecurringExpenses() {
        if (!isListenerSetup || expensesListener == null) {
            isListenerSetup = false
            expensesListener?.remove()
            expensesListener = null
            setupRealtimeListener()
        } else {
            _isLoading.value = false
        }
    }

    private fun isValidExpenseCategory(expense: RecurringExpense): Boolean {
        return try {
            categoryViewModel.doesCategoryExist(expense.category)
        } catch (e: Exception) {
            true
        }
    }

    // ==================== CRUD OPERATIONS ====================

    fun addRecurringExpense(
        title: String,
        amount: Double,
        categoryId: String,
        wallet: String,
        description: String?,
        frequency: RecurringFrequency,
        startDate: String,
        endDate: String?
    ) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _uiMessage.value = "Vui lòng đăng nhập"
                    return@launch
                }

                val isValidCategory = try {
                    categoryViewModel.validateCategoryForRecurringExpense(categoryId, "expense")
                } catch (e: Exception) {
                    true
                }

                if (!isValidCategory) {
                    _uiMessage.value = "Lỗi: Danh mục không hợp lệ"
                    return@launch
                }

                val categoryInfo = try {
                    categoryViewModel.getCategoryInfoForRecurringExpense(categoryId)
                } catch (e: Exception) {
                    Pair("💰", "#0F4C75")
                }

                val categoryIcon = categoryInfo?.first ?: "💰"
                val categoryColor = categoryInfo?.second ?: "#0F4C75"

                val today = getCurrentDateInternal()
                val nextOccurrence = if (isDateBeforeOrEqual(startDate, today)) {
                    calculateNextOccurrence(today, frequency)
                } else {
                    startDate
                }

                val expense = RecurringExpense.Companion.fromEnum(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    amount = amount,
                    category = categoryId,
                    categoryIcon = categoryIcon,
                    categoryColor = categoryColor,
                    wallet = wallet,
                    description = description,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    nextOccurrence = nextOccurrence,
                    userId = userId
                )

                db.collection(COLLECTION_NAME)
                    .document(expense.id)
                    .set(expense)
                    .await()

                _uiMessage.value = "Đã thêm: $title"

            } catch (e: Exception) {
                _uiMessage.value = "Lỗi thêm"
            }
        }
    }

    fun addRecurringExpense(
        title: String,
        amount: Double,
        category: String,
        categoryIcon: String,
        categoryColor: String,
        wallet: String,
        description: String?,
        frequency: RecurringFrequency,
        startDate: String,
        endDate: String?
    ) {
        val categoryId = findCategoryIdByName(category) ?: category
        addRecurringExpense(title, amount, categoryId, wallet, description, frequency, startDate, endDate)
    }

    private fun findCategoryIdByName(categoryName: String): String? {
        return try {
            val allSubCategories = getAllSubCategories()
            allSubCategories.find { it.name == categoryName }?.id
        } catch (e: Exception) {
            null
        }
    }

    private fun getAllSubCategories(): List<FinanceCategory> {
        return try {
            categoryViewModel.getAllSubCategories("expense") + categoryViewModel.getAllSubCategories("income")
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateRecurringExpense(expense: RecurringExpense) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val updatedExpense = if (expense.userId.isBlank()) {
                    expense.copy(userId = userId)
                } else {
                    expense
                }

                db.collection(COLLECTION_NAME)
                    .document(updatedExpense.id)
                    .set(updatedExpense)
                    .await()

                _uiMessage.value = "Đã cập nhật: ${updatedExpense.title}"
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi cập nhật"
            }
        }
    }

    fun deleteRecurringExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val expense = _recurringExpenses.value.find { it.id == expenseId }
                val expenseName = expense?.title ?: "Chi tiêu định kỳ"

                db.collection(COLLECTION_NAME)
                    .document(expenseId)
                    .delete()
                    .await()

                _uiMessage.value = "Đã xóa: $expenseName"
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi xóa"
            }
        }
    }

    fun toggleRecurringExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                val expense = _recurringExpenses.value.find { it.id == expenseId }
                expense?.let {
                    val updated = it.copy(isActive = !it.isActive)
                    updateRecurringExpense(updated)
                }
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi cập nhật trạng thái"
            }
        }
    }

    // ==================== DATA QUERY METHODS ====================

    fun getCategoryName(categoryId: String): String {
        return try {
            val category = categoryViewModel.getCategoryById(categoryId)
            category?.name ?: "Unknown Category"
        } catch (e: Exception) {
            "Unknown Category"
        }
    }

    fun getRecurringExpensesForUser(userId: String): List<RecurringExpense> {
        return _recurringExpenses.value.filter { it.userId == userId }
    }

    fun getCurrentUserRecurringExpenses(): List<RecurringExpense> {
        val userId = getCurrentUserId()
        return _recurringExpenses.value.filter { it.userId == userId }
    }

    fun getExpenseSubCategoriesForSelection(): List<FinanceCategory> {
        return try {
            categoryViewModel.getSubCategoriesForRecurringExpense("expense")
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getIncomeSubCategoriesForSelection(): List<FinanceCategory> {
        return try {
            categoryViewModel.getSubCategoriesForRecurringExpense("income")
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getMonthlyRecurringTotalByCategory(categoryId: String): Double {
        val userId = getCurrentUserId()
        return _recurringExpenses.value
            .filter {
                it.userId == userId &&
                        it.isActive &&
                        it.getFrequencyEnum() == RecurringFrequency.MONTHLY &&
                        it.category == categoryId
            }
            .sumOf { it.amount }
    }

    fun getActiveExpenses(): List<RecurringExpense> {
        val userId = getCurrentUserId()
        return _recurringExpenses.value.filter { it.userId == userId && it.isActive }
    }

    fun getInactiveExpenses(): List<RecurringExpense> {
        val userId = getCurrentUserId()
        return _recurringExpenses.value.filter { it.userId == userId && !it.isActive }
    }

    fun getMonthlyRecurringTotal(): Double {
        val userId = getCurrentUserId()
        return _recurringExpenses.value
            .filter {
                it.userId == userId &&
                        it.isActive &&
                        it.getFrequencyEnum() == RecurringFrequency.MONTHLY
            }
            .sumOf { it.amount }
    }

    // ==================== PROCESSING METHODS ====================

    fun processDueRecurringExpenses(
        context: Context,
        onTransactionCreated: (RecurringExpense) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    return@launch
                }

                val today = getCurrentDateInternal()

                val lastProcessedDate = getLastProcessedDate(context)
                if (lastProcessedDate == today) {
                    return@launch
                }

                val dueExpenses = _recurringExpenses.value.filter { expense ->
                    isExpenseDueToday(expense, today) &&
                            expense.userId == userId &&
                            expense.isActive
                }

                if (dueExpenses.isEmpty()) {
                    saveLastProcessedDate(context, today)
                    return@launch
                }

                var processedCount = 0
                dueExpenses.forEach { expense ->
                    try {
                        onTransactionCreated(expense)

                        val nextDate = calculateNextOccurrence(today, expense.getFrequencyEnum())
                        val updatedExpense = expense.copy(
                            nextOccurrence = nextDate,
                            totalGenerated = expense.totalGenerated + 1,
                            lastGenerated = today
                        )

                        updateRecurringExpense(updatedExpense)

                        processedCount++

                    } catch (e: Exception) {
                    }
                }

                saveLastProcessedDate(context, today)

                if (processedCount > 0) {
                    _uiMessage.value = "Đã xử lý $processedCount chi tiêu định kỳ"
                }

            } catch (e: Exception) {
                _uiMessage.value = "Lỗi xử lý chi tiêu định kỳ"
            }
        }
    }

    private fun isExpenseDueToday(expense: RecurringExpense, today: String = getCurrentDateInternal()): Boolean {
        return try {
            if (!expense.isActive) return false

            if (isDateBefore(today, expense.startDate)) {
                return false
            }

            if (expense.endDate != null && expense.endDate.isNotEmpty()) {
                if (isDateAfter(today, expense.endDate)) {
                    return false
                }
            }

            !isDateBefore(today, expense.nextOccurrence)

        } catch (e: Exception) {
            false
        }
    }

    private fun isDateAfter(date1: String, date2: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)
            d1 != null && d2 != null && d1.after(d2)
        } catch (e: Exception) {
            false
        }
    }

    private fun isDateBeforeOrEqual(date1: String, date2: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)
            d1 != null && d2 != null && (d1.before(d2) || d1 == d2)
        } catch (e: Exception) {
            false
        }
    }

    // ==================== AUTO-PROCESSING MECHANISM ====================

    fun setupAutoProcessing(
        context: Context,
        onTransactionCreated: (RecurringExpense) -> Unit
    ) {
        viewModelScope.launch {
            try {
                processDueRecurringExpenses(context, onTransactionCreated)
                checkAndResetMissedExpenses()
            } catch (e: Exception) {
            }
        }
    }

    private fun checkAndResetMissedExpenses() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") return@launch

                val today = getCurrentDateInternal()

                _recurringExpenses.value.forEach { expense ->
                    if (expense.userId == userId && expense.isActive) {
                        if (isDateAfter(today, expense.nextOccurrence)) {
                            val newNextDate = calculateNextOccurrence(today, expense.getFrequencyEnum())
                            val updatedExpense = expense.copy(
                                nextOccurrence = newNextDate
                            )
                            updateRecurringExpense(updatedExpense)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    // ==================== DATE UTILITIES ====================

    private fun getCurrentDateInternal(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    private fun getTodayDateUI(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun isDateBefore(date1: String, date2: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)
            d1 != null && d2 != null && d1.before(d2)
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateNextOccurrence(fromDate: String, frequency: RecurringFrequency): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(fromDate) ?: return fromDate

            val calendar = Calendar.getInstance()
            calendar.time = date

            when (frequency) {
                RecurringFrequency.DAILY -> calendar.add(Calendar.DATE, 1)
                RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.MONTHLY -> {
                    calendar.add(Calendar.MONTH, 1)
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                    if (currentDay > maxDay) {
                        calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                    }
                }
                RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
                RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
            }

            sdf.format(calendar.time)
        } catch (e: Exception) {
            fromDate
        }
    }

    // ==================== SHARED PREFERENCES ====================

    private fun saveLastProcessedDate(context: Context, date: String) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_PROCESSED_DATE, date).apply()
        } catch (e: Exception) {
        }
    }

    private fun getLastProcessedDate(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_LAST_PROCESSED_DATE, null)
        } catch (e: Exception) {
            null
        }
    }

    // ==================== UTILITY METHODS ====================

    private fun formatCurrency(amount: Double): String {
        return try {
            val locale = Locale.Builder()
                .setLanguage("vi")
                .setRegion("VN")
                .build()
            NumberFormat.getCurrencyInstance(locale).format(amount)
        } catch (e: Exception) {
            NumberFormat.getCurrencyInstance().apply {
                maximumFractionDigits = 0
            }.format(amount)
        }
    }

    fun setCategoryViewModel(categoryViewModel: CategoryViewModel) {
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        expensesListener?.remove()
        isListenerSetup = false
    }
}

data class CategoryItem(
    val id: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String
) {
    companion object {
        fun fromFinanceCategory(category: FinanceCategory): CategoryItem {
            return CategoryItem(
                id = category.id,
                name = category.name,
                type = category.type,
                icon = category.icon,
                color = category.color
            )
        }
    }
}