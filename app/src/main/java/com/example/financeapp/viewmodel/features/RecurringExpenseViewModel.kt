package com.example.financeapp.viewmodel.features

import android.content.Context
import android.util.Log
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Định nghĩa typealias để tránh confusion
typealias FinanceCategory = com.example.financeapp.viewmodel.transaction.Category

/**
 * ViewModel quản lý chi tiêu định kỳ
 * Xử lý các chi tiêu được lặp lại theo tần suất (hàng ngày, tuần, tháng, v.v.)
 */
class RecurringExpenseViewModel : ViewModel() {

    companion object {
        private const val TAG = "RecurringExpenseViewModel"
        private const val COLLECTION_NAME = "recurring_expenses"
        private const val PREF_NAME = "recurring_expense_prefs"
        private const val KEY_LAST_PROCESSED_DATE = "last_processed_date"
    }

    // ==================== DEPENDENCIES ====================

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var expensesListener: ListenerRegistration? = null

    // Sử dụng singleton CategoryViewModel
    private val categoryViewModel = CategoryViewModel.getInstance()

    // ==================== STATE FLOWS ====================

    /** Flow danh sách chi tiêu định kỳ */
    private val _recurringExpenses = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpense>> = _recurringExpenses

    /** Flow trạng thái loading */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Flow thông báo UI */
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    /** Flow danh sách danh mục con có sẵn theo loại */
    private val _availableSubCategories = MutableStateFlow<Map<String, List<FinanceCategory>>>(emptyMap())
    val availableSubCategories: StateFlow<Map<String, List<FinanceCategory>>> = _availableSubCategories

    /** Trạng thái listener đã được thiết lập */
    private var isListenerSetup = false

    // ==================== INITIALIZATION ====================

    init {
        Log.d(TAG, "RecurringExpenseViewModel khởi tạo")
        setupRealtimeListener()
        loadAvailableSubCategories()
    }

    /**
     * Lấy ID user hiện tại (tương tự BudgetViewModel)
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous".also {
            Log.w(TAG, "User chưa đăng nhập, sử dụng anonymous")
        }
    }

    /**
     * Load danh sách danh mục con có sẵn
     */
    private fun loadAvailableSubCategories() {
        viewModelScope.launch {
            try {
                val expenseCategories = categoryViewModel.getSubCategoriesForRecurringExpense("expense")
                val incomeCategories = categoryViewModel.getSubCategoriesForRecurringExpense("income")

                _availableSubCategories.value = mapOf(
                    "expense" to expenseCategories,
                    "income" to incomeCategories
                )

                Log.d(TAG, "Đã load danh mục con: Expense=${expenseCategories.size}, Income=${incomeCategories.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi load subcategories: ${e.message}")
            }
        }
    }

    /**
     * Thiết lập real-time listener cho Firestore với user filtering
     */
    private fun setupRealtimeListener() {
        if (isListenerSetup) {
            _isLoading.value = false
            return
        }

        val userId = getCurrentUserId()
        if (userId == "anonymous") {
            _isLoading.value = false
            isListenerSetup = true
            Log.w(TAG, "User chưa đăng nhập, không thể setup listener")
            _uiMessage.value = "Vui lòng đăng nhập để xem chi tiêu định kỳ"
            return
        }

        if (_recurringExpenses.value.isEmpty()) {
            _isLoading.value = true
        }

        try {
            expensesListener = db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId) // Filter theo userId
                .addSnapshotListener { snapshot, error ->
                    _isLoading.value = false
                    isListenerSetup = true

                    if (error != null) {
                        _uiMessage.value = "Lỗi tải chi tiêu định kỳ: ${error.message}"
                        Log.e(TAG, "Firestore error: ${error.message}")
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
                                    } else {
                                        Log.w(TAG, "Recurring expense có category không hợp lệ: ${it.category}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Lỗi parse document: ${e.message}")
                            }
                        }
                        _recurringExpenses.value = expenses
                        Log.d(TAG, "Real-time update: ${expenses.size} recurring expenses cho user: $userId")
                    }

                    if (snapshot == null) {
                        _isLoading.value = false
                    }
                }

            Log.d(TAG, "Đã thiết lập real-time listener thành công cho user: $userId")
        } catch (e: Exception) {
            _isLoading.value = false
            isListenerSetup = true
            _uiMessage.value = "Lỗi kết nối: ${e.message}"
            Log.e(TAG, "Listener setup error: ${e.message}")
        }
    }

    /**
     * Load lại danh sách chi tiêu định kỳ
     */
    fun loadRecurringExpenses() {
        if (!isListenerSetup || expensesListener == null) {
            isListenerSetup = false
            expensesListener?.remove()
            expensesListener = null
            setupRealtimeListener()
            Log.d(TAG, "Reload recurring expenses listener")
        } else {
            _isLoading.value = false
        }
    }

    /**
     * Kiểm tra category của expense có hợp lệ không
     */
    private fun isValidExpenseCategory(expense: RecurringExpense): Boolean {
        return try {
            categoryViewModel.doesCategoryExist(expense.category)
        } catch (e: Exception) {
            // Nếu có lỗi, vẫn chấp nhận để không block data flow
            true
        }
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Thêm chi tiêu định kỳ mới (sử dụng categoryId)
     * @param title Tiêu đề
     * @param amount Số tiền
     * @param categoryId ID danh mục
     * @param wallet Ví
     * @param description Mô tả (optional)
     * @param frequency Tần suất
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc (optional)
     */
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
                    Log.w(TAG, "User chưa đăng nhập khi thêm recurring expense")
                    return@launch
                }

                // Validate category
                val isValidCategory = try {
                    categoryViewModel.validateCategoryForRecurringExpense(categoryId, "expense")
                } catch (e: Exception) {
                    Log.w(TAG, "Không thể validate category, vẫn tiếp tục: ${e.message}")
                    true
                }

                if (!isValidCategory) {
                    _uiMessage.value = "Lỗi: Danh mục không hợp lệ"
                    return@launch
                }

                // Lấy thông tin category
                val categoryInfo = try {
                    categoryViewModel.getCategoryInfoForRecurringExpense(categoryId)
                } catch (e: Exception) {
                    Log.w(TAG, "Không thể lấy category info, sử dụng giá trị mặc định: ${e.message}")
                    Pair("💰", "#0F4C75")
                }

                val categoryIcon = categoryInfo?.first ?: "💰"
                val categoryColor = categoryInfo?.second ?: "#0F4C75"

                // Tính ngày xảy ra tiếp theo (nếu startDate là hôm nay hoặc trước đó, tính ngay lập tức)
                val today = getTodayDate()
                val nextOccurrence = if (isDateBeforeOrEqual(startDate, today)) {
                    calculateNextOccurrence(today, frequency)
                } else {
                    startDate // Chưa đến ngày bắt đầu
                }

                // Tạo object RecurringExpense
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
                    userId = userId // Thêm userId
                )

                // Lưu vào Firestore
                db.collection(COLLECTION_NAME)
                    .document(expense.id)
                    .set(expense)
                    .await()

                _uiMessage.value = "Đã thêm: $title"
                Log.d(TAG, "✅ Đã thêm recurring expense: ${expense.title} cho user: $userId")

            } catch (e: Exception) {
                _uiMessage.value = "Lỗi thêm: ${e.message}"
                Log.e(TAG, "Lỗi thêm recurring expense: ${e.message}")
            }
        }
    }

    /**
     * Thêm chi tiêu định kỳ (sử dụng category name - overload cho backward compatibility)
     */
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

    /**
     * Tìm category ID bằng tên
     */
    private fun findCategoryIdByName(categoryName: String): String? {
        return try {
            val allSubCategories = getAllSubCategories()
            allSubCategories.find { it.name == categoryName }?.id
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tìm category by name: ${e.message}")
            null
        }
    }

    /**
     * Lấy tất cả danh mục con
     */
    private fun getAllSubCategories(): List<FinanceCategory> {
        return try {
            categoryViewModel.getAllSubCategories("expense") + categoryViewModel.getAllSubCategories("income")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy all subcategories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Cập nhật chi tiêu định kỳ
     * @param expense RecurringExpense đã cập nhật
     */
    fun updateRecurringExpense(expense: RecurringExpense) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                // Đảm bảo expense có userId của user hiện tại
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
                Log.d(TAG, "✅ Đã cập nhật recurring expense: ${updatedExpense.title} cho user: $userId")
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi cập nhật: ${e.message}"
                Log.e(TAG, "Lỗi cập nhật recurring expense: ${e.message}")
            }
        }
    }

    /**
     * Xóa chi tiêu định kỳ
     * @param expenseId ID chi tiêu cần xóa
     */
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
                Log.d(TAG, "✅ Đã xóa recurring expense: $expenseId của user: $userId")
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi xóa: ${e.message}"
                Log.e(TAG, "Lỗi xóa recurring expense: ${e.message}")
            }
        }
    }

    /**
     * Bật/tắt trạng thái active của chi tiêu định kỳ
     * @param expenseId ID chi tiêu
     */
    fun toggleRecurringExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                val expense = _recurringExpenses.value.find { it.id == expenseId }
                expense?.let {
                    val updated = it.copy(isActive = !it.isActive)
                    updateRecurringExpense(updated)
                    Log.d(TAG, "Đã toggle trạng thái expense: ${it.title} -> ${!it.isActive}")
                }
            } catch (e: Exception) {
                _uiMessage.value = "Lỗi cập nhật trạng thái: ${e.message}"
                Log.e(TAG, "Lỗi toggle recurring expense: ${e.message}")
            }
        }
    }

    // ==================== DATA QUERY METHODS ====================

    /**
     * Lấy tên category từ ID
     */
    fun getCategoryName(categoryId: String): String {
        return try {
            val category = categoryViewModel.getCategoryById(categoryId)
            category?.name ?: "Unknown Category"
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy category name: ${e.message}")
            "Unknown Category"
        }
    }

    /**
     * Lấy danh sách chi tiêu định kỳ theo user ID
     */
    fun getRecurringExpensesForUser(userId: String): List<RecurringExpense> {
        return _recurringExpenses.value.filter { it.userId == userId }
    }

    /**
     * Lấy danh sách chi tiêu định kỳ cho user hiện tại
     */
    fun getCurrentUserRecurringExpenses(): List<RecurringExpense> {
        val userId = getCurrentUserId()
        return _recurringExpenses.value.filter { it.userId == userId }
    }

    /**
     * Lấy danh sách danh mục con cho chi tiêu
     */
    fun getExpenseSubCategoriesForSelection(): List<FinanceCategory> {
        return try {
            categoryViewModel.getSubCategoriesForRecurringExpense("expense")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy expense subcategories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Lấy danh sách danh mục con cho thu nhập
     */
    fun getIncomeSubCategoriesForSelection(): List<FinanceCategory> {
        return try {
            categoryViewModel.getSubCategoriesForRecurringExpense("income")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy income subcategories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Lấy tổng chi tiêu định kỳ hàng tháng theo category cho user hiện tại
     */
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

    /**
     * Lấy danh sách chi tiêu đang active cho user hiện tại
     */
    fun getActiveExpenses(): List<RecurringExpense> {
        val userId = getCurrentUserId()
        return _recurringExpenses.value.filter { it.userId == userId && it.isActive }
    }

    /**
     * Lấy danh sách chi tiêu không active cho user hiện tại
     */
    fun getInactiveExpenses(): List<RecurringExpense> {
        val userId = getCurrentUserId()
        return _recurringExpenses.value.filter { it.userId == userId && !it.isActive }
    }

    /**
     * Lấy tổng chi tiêu định kỳ hàng tháng cho user hiện tại
     */
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

    // ==================== PROCESSING METHODS (CẢI THIỆN) ====================

    /**
     * Xử lý các chi tiêu định kỳ đến hạn cho user hiện tại (Phiên bản cải tiến)
     * @param onTransactionCreated Callback khi tạo transaction mới
     * @param context Context để lưu SharedPreferences
     */
    fun processDueRecurringExpenses(
        context: Context,
        onTransactionCreated: (RecurringExpense) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    Log.w(TAG, "User chưa đăng nhập, không xử lý recurring expenses")
                    return@launch
                }

                val today = getTodayDate()
                Log.d(TAG, "Bắt đầu xử lý recurring expenses vào ngày: $today")

                // Kiểm tra xem đã xử lý hôm nay chưa
                val lastProcessedDate = getLastProcessedDate(context)
                if (lastProcessedDate == today) {
                    Log.d(TAG, "Đã xử lý recurring expenses hôm nay rồi: $today")
                    return@launch
                }

                // Lấy danh sách expense cần xử lý
                val dueExpenses = _recurringExpenses.value.filter { expense ->
                    isExpenseDueToday(expense, today) &&
                            expense.userId == userId &&
                            expense.isActive
                }

                Log.d(TAG, "Tìm thấy ${dueExpenses.size} chi tiêu cần xử lý cho user: $userId")

                if (dueExpenses.isEmpty()) {
                    Log.d(TAG, "Không có chi tiêu định kỳ nào cần xử lý hôm nay")
                    // Vẫn lưu ngày xử lý để không kiểm tra lại
                    saveLastProcessedDate(context, today)
                    return@launch
                }

                var processedCount = 0
                dueExpenses.forEach { expense ->
                    try {
                        Log.d(TAG, "Bắt đầu xử lý expense: ${expense.title}, next: ${expense.nextOccurrence}")

                        // 1. Gọi callback để tạo transaction
                        onTransactionCreated(expense)

                        // 2. Cập nhật next occurrence
                        val nextDate = calculateNextOccurrence(today, expense.getFrequencyEnum())
                        val updatedExpense = expense.copy(
                            nextOccurrence = nextDate,
                            totalGenerated = expense.totalGenerated + 1,
                            lastGenerated = today
                        )

                        // 3. Cập nhật vào Firestore
                        updateRecurringExpense(updatedExpense)

                        processedCount++
                        Log.d(TAG, "✅ Đã xử lý: ${expense.title} - ${formatCurrency(expense.amount)} -> Next: $nextDate")

                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý expense ${expense.title}: ${e.message}")
                    }
                }

                // Lưu ngày đã xử lý
                saveLastProcessedDate(context, today)

                if (processedCount > 0) {
                    _uiMessage.value = "Đã xử lý $processedCount chi tiêu định kỳ"
                    Log.d(TAG, "✅ Đã xử lý thành công $processedCount/$dueExpenses.size chi tiêu định kỳ")
                }

            } catch (e: Exception) {
                _uiMessage.value = "Lỗi xử lý chi tiêu định kỳ: ${e.message}"
                Log.e(TAG, "Lỗi xử lý chi tiêu định kỳ: ${e.message}")
            }
        }
    }

    /**
     * Kiểm tra xem expense có đến hạn hôm nay không
     */
    private fun isExpenseDueToday(expense: RecurringExpense, today: String): Boolean {
        return try {
            // 1. Kiểm tra đã đến ngày bắt đầu chưa
            if (isDateAfter(expense.startDate, today)) {
                return false // Chưa đến ngày bắt đầu
            }

            // 2. Kiểm tra đã quá end date chưa (nếu có)
            if (expense.endDate != null && expense.endDate.isNotEmpty()) {
                if (isDateAfter(today, expense.endDate)) {
                    return false // Đã quá ngày kết thúc
                }
            }

            // 3. So sánh nextOccurrence với ngày hôm nay
            val isDue = expense.nextOccurrence == today

            // 4. Nếu nextOccurrence đã qua ngày hôm nay (do bỏ lỡ), cũng tính là đến hạn
            val isMissed = isDateAfter(today, expense.nextOccurrence)

            isDue || isMissed
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra ngày: ${e.message}")
            false
        }
    }

    /**
     * Kiểm tra nếu date1 là sau date2
     */
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

    /**
     * Kiểm tra nếu date1 là trước hoặc bằng date2
     */
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

    /**
     * Thiết lập cơ chế tự động xử lý khi app mở
     * Gọi method này khi app khởi động (trong MainActivity hoặc SplashScreen)
     */
    fun setupAutoProcessing(
        context: Context,
        onTransactionCreated: (RecurringExpense) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Thiết lập auto-processing recurring expenses")

                // Đợi một chút để đảm bảo dữ liệu đã load
                kotlinx.coroutines.delay(2000)

                // Kiểm tra và xử lý các expense đến hạn
                processDueRecurringExpenses(context, onTransactionCreated)

                // Kiểm tra và reset các expense bị bỏ lỡ
                checkAndResetMissedExpenses()

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi thiết lập auto processing: ${e.message}")
            }
        }
    }

    /**
     * Kiểm tra và reset các expense bị bỏ lỡ (nextOccurrence đã qua)
     */
    private fun checkAndResetMissedExpenses() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") return@launch

                val today = getTodayDate()

                _recurringExpenses.value.forEach { expense ->
                    if (expense.userId == userId && expense.isActive) {
                        // Nếu nextOccurrence đã qua mà chưa xử lý
                        if (isDateAfter(today, expense.nextOccurrence)) {
                            // Tính lại next occurrence từ ngày hôm nay
                            val newNextDate = calculateNextOccurrence(today, expense.getFrequencyEnum())
                            val updatedExpense = expense.copy(
                                nextOccurrence = newNextDate
                            )
                            updateRecurringExpense(updatedExpense)
                            Log.d(TAG, "Reset next occurrence cho ${expense.title}: ${expense.nextOccurrence} -> $newNextDate")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi checkAndResetMissedExpenses: ${e.message}")
            }
        }
    }

    // ==================== SHARED PREFERENCES ====================

    /**
     * Lưu ngày đã xử lý
     */
    private fun saveLastProcessedDate(context: Context, date: String) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_PROCESSED_DATE, date).apply()
            Log.d(TAG, "Đã lưu ngày xử lý: $date")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lưu ngày xử lý: ${e.message}")
        }
    }

    /**
     * Lấy ngày đã xử lý lần cuối
     */
    private fun getLastProcessedDate(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_LAST_PROCESSED_DATE, null)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy ngày xử lý: ${e.message}")
            null
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Tính ngày xảy ra tiếp theo từ ngày hiện tại
     */
    private fun calculateNextOccurrence(fromDate: String, frequency: RecurringFrequency): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(fromDate) ?: return fromDate

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
            Log.e(TAG, "Lỗi tính next occurrence: ${e.message}")
            fromDate
        }
    }

    /**
     * Lấy ngày hiện tại định dạng yyyy-MM-dd
     */
    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Định dạng tiền tệ
     */
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

    // ==================== CLEANUP & COMPATIBILITY ====================

    /**
     * Set CategoryViewModel (cho backward compatibility)
     */
    fun setCategoryViewModel(categoryViewModel: CategoryViewModel) {
        // Giữ lại cho tương thích
        Log.d(TAG, "setCategoryViewModel được gọi (backward compatibility)")
    }

    /**
     * Clear message
     */
    fun clearMessage() {
        _uiMessage.value = null
    }

    /**
     * Cleanup khi ViewModel bị hủy
     */
    override fun onCleared() {
        super.onCleared()
        expensesListener?.remove()
        isListenerSetup = false
        Log.d(TAG, "RecurringExpenseViewModel đã được giải phóng")
    }
}

// ==================== SUPPORTING DATA CLASS ====================

/**
 * Data class wrapper cho Category để phân biệt
 */
data class CategoryItem(
    val id: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String
) {
    companion object {
        /**
         * Chuyển đổi từ FinanceCategory sang CategoryItem
         */
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