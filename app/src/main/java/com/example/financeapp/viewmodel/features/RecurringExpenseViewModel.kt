package com.example.financeapp.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.model.RecurringExpense
import com.example.financeapp.model.RecurringFrequency
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Định nghĩa typealias để tránh confusion
typealias FinanceCategory = com.example.financeapp.viewmodel.transaction.Category

class RecurringExpenseViewModel : ViewModel() {

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
                println("❌ Lỗi load subcategories: ${e.message}")
            }
        }
    }

    private fun setupRealtimeListener() {
        if (isListenerSetup) {
            _isLoading.value = false
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _isLoading.value = false
            isListenerSetup = true
            return
        }

        if (_recurringExpenses.value.isEmpty()) {
            _isLoading.value = true
        }

        try {
            expensesListener = db.collection("recurring_expenses")
                .whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    _isLoading.value = false
                    isListenerSetup = true

                    if (error != null) {
                        _uiMessage.value = "Lỗi tải chi tiêu định kỳ: ${error.message}"
                        println("❌ Firebase error: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        val expenses = mutableListOf<RecurringExpense>()
                        for (document in querySnapshot.documents) {
                            try {
                                val expense = document.toObject(RecurringExpense::class.java)
                                expense?.let {
                                    if (isValidExpenseCategory(expense)) {
                                        expenses.add(expense)
                                    } else {
                                        println("⚠️ Recurring expense có category không hợp lệ: ${expense.category}")
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Lỗi parse document: ${e.message}")
                            }
                        }
                        _recurringExpenses.value = expenses
                        println("✅ Real-time update: ${expenses.size} recurring expenses")
                    }

                    if (snapshot == null) {
                        _isLoading.value = false
                    }
                }
        } catch (e: Exception) {
            _isLoading.value = false
            isListenerSetup = true
            _uiMessage.value = "Lỗi kết nối: ${e.message}"
            println("❌ Listener setup error: ${e.message}")
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

    fun setCategoryViewModel(categoryViewModel: CategoryViewModel) {
        // Giữ lại cho tương thích
    }

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
                val currentUser = auth.currentUser
                if (currentUser == null) {
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

                val nextOccurrence = calculateNextOccurrence(startDate, frequency)

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
                    userId = currentUser.uid
                )

                db.collection("recurring_expenses")
                    .document(expense.id)
                    .set(expense)
                    .await()

                _uiMessage.value = "Đã thêm: $title"
                println("✅ Đã thêm recurring expense: ${expense.title}")
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi thêm: ${e.message}"
                println("❌ Lỗi thêm recurring expense: ${e.message}")
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

    fun getCategoryName(categoryId: String): String {
        return try {
            val category = categoryViewModel.getCategoryById(categoryId)
            category?.name ?: "Unknown Category"
        } catch (e: Exception) {
            "Unknown Category"
        }
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
        return _recurringExpenses.value
            .filter {
                it.isActive &&
                        it.getFrequencyEnum() == RecurringFrequency.MONTHLY &&
                        it.category == categoryId
            }
            .sumOf { it.amount }
    }

    fun updateRecurringExpense(expense: RecurringExpense) {
        viewModelScope.launch {
            try {
                db.collection("recurring_expenses")
                    .document(expense.id)
                    .set(expense)
                    .await()

                _uiMessage.value = "Đã cập nhật: ${expense.title}"
                println("✅ Đã cập nhật recurring expense: ${expense.title}")
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi cập nhật: ${e.message}"
                println("❌ Lỗi cập nhật recurring expense: ${e.message}")
            }
        }
    }

    fun deleteRecurringExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                val expense = _recurringExpenses.value.find { it.id == expenseId }
                val expenseName = expense?.title ?: "Chi tiêu định kỳ"

                db.collection("recurring_expenses")
                    .document(expenseId)
                    .delete()
                    .await()

                _uiMessage.value = "Đã xóa: $expenseName"
                println("✅ Đã xóa recurring expense: $expenseId")
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi xóa: ${e.message}"
                println("❌ Lỗi xóa recurring expense: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiMessage.value = null
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
                _uiMessage.value = "Lỗi cập nhật trạng thái: ${e.message}"
                println("❌ Lỗi toggle recurring expense: ${e.message}")
            }
        }
    }

    fun processDueRecurringExpenses(
        onTransactionCreated: (RecurringExpense) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = getTodayDate()
                val dueExpenses = _recurringExpenses.value.filter { expense ->
                    expense.isActive &&
                            expense.nextOccurrence == today &&
                            (expense.endDate == null || expense.endDate >= today)
                }

                println("🔍 Tìm thấy ${dueExpenses.size} chi tiêu cần xử lý")

                dueExpenses.forEach { expense ->
                    // 1. Gọi callback để tạo transaction
                    onTransactionCreated(expense)

                    // 2. Cập nhật next occurrence
                    val nextDate = calculateNextOccurrence(expense.nextOccurrence, expense.getFrequencyEnum())
                    val updatedExpense = expense.copy(
                        nextOccurrence = nextDate,
                        totalGenerated = expense.totalGenerated + 1,
                        lastGenerated = today
                    )

                    updateRecurringExpense(updatedExpense)
                    println("✅ Đã xử lý: ${expense.title} - ${formatCurrency(expense.amount)}")
                }

                if (dueExpenses.isNotEmpty()) {
                    _uiMessage.value = "Đã xử lý ${dueExpenses.size} chi tiêu định kỳ"
                }
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi xử lý chi tiêu định kỳ: ${e.message}"
                println("❌ Lỗi xử lý chi tiêu định kỳ: ${e.message}")
            }
        }
    }

    fun getActiveExpenses(): List<RecurringExpense> {
        return _recurringExpenses.value.filter { it.isActive }
    }

    fun getInactiveExpenses(): List<RecurringExpense> {
        return _recurringExpenses.value.filter { !it.isActive }
    }

    fun getMonthlyRecurringTotal(): Double {
        return _recurringExpenses.value
            .filter { it.isActive && it.getFrequencyEnum() == RecurringFrequency.MONTHLY }
            .sumOf { it.amount }
    }

    // 🗓️ DATE UTILITIES
    private fun calculateNextOccurrence(currentDate: String, frequency: RecurringFrequency): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(currentDate) ?: return currentDate

            val calendar = Calendar.getInstance()
            calendar.time = date

            when (frequency) {
                RecurringFrequency.DAILY -> calendar.add(Calendar.DATE, 1)
                RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
                RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
            }

            sdf.format(calendar.time)
        } catch (e: Exception) {
            currentDate
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

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

    override fun onCleared() {
        super.onCleared()
        expensesListener?.remove()
        isListenerSetup = false
    }
}

// Nếu bạn cần interface để phân biệt, có thể tạo một class wrapper
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