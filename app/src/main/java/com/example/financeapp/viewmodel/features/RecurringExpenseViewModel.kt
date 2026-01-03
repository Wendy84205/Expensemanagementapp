package com.example.financeapp.viewmodel.features

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.RecurringFrequency
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

typealias FinanceCategory = com.example.financeapp.viewmodel.transaction.Category

// ==================== DATA CLASSES ====================

data class RecurringAICommandResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)

data class ParsedAddCommand(
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: RecurringFrequency,
    val wallet: String = "Ví chính",
    val description: String = "Tạo tự động bởi AI Assistant",
    val startDate: String,
    val endDate: String? = null
)

// ==================== RECURRING EXPENSE VIEWMODEL ====================

class RecurringExpenseViewModel : ViewModel() {

    companion object {
        private const val COLLECTION_NAME = "recurring_expenses"
        private const val PREF_NAME = "recurring_expense_prefs"
        private const val KEY_LAST_PROCESSED_DATE = "last_processed_date"
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var expensesListener: ListenerRegistration? = null

    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var transactionViewModel: TransactionViewModel

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpense>> = _recurringExpenses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _availableCategories = MutableStateFlow<Map<String, List<FinanceCategory>>>(emptyMap())
    val availableCategories: StateFlow<Map<String, List<FinanceCategory>>> = _availableCategories.asStateFlow()

    private val _aiCommandResult = MutableStateFlow<RecurringAICommandResult?>(null)
    val aiCommandResult: StateFlow<RecurringAICommandResult?> = _aiCommandResult.asStateFlow()

    private var isListenerSetup = false

    init {
        setupRealtimeListener()
        loadAvailableCategories()
    }

    fun setCategoryViewModel(categoryViewModel: CategoryViewModel) {
        this.categoryViewModel = categoryViewModel
        loadAvailableCategories()
    }

    fun setTransactionViewModel(transactionViewModel: TransactionViewModel) {
        this.transactionViewModel = transactionViewModel
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
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _uiMessage.value = "Vui lòng đăng nhập"
                    return@launch
                }

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
                    category = category,
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

                val categoryInfo = try {
                    categoryViewModel.getCategoryInfoForRecurringExpense(categoryId)
                } catch (e: Exception) {
                    null
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

                        db.collection(COLLECTION_NAME)
                            .document(updatedExpense.id)
                            .set(updatedExpense)
                            .await()

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

    fun executeAICommand(command: String, context: Context) {
        viewModelScope.launch {
            try {
                val result = when {
                    isAddCommand(command) -> executeAddCommand(command)
                    isListCommand(command) -> executeListCommand(command)
                    isDeleteCommand(command) -> executeDeleteCommand(command)
                    isUpdateCommand(command) -> executeUpdateCommand(command)
                    isProcessCommand(command) -> executeProcessCommand(context)
                    isSummaryCommand(command) -> executeSummaryCommand()
                    isUpcomingCommand(command) -> executeUpcomingCommand()
                    isToggleCommand(command) -> executeToggleCommand(command)
                    else -> RecurringAICommandResult(
                        success = false,
                        message = "Không hiểu lệnh. Các lệnh hỗ trợ:\n" +
                                "• Thêm chi tiêu định kỳ [số tiền] cho [danh mục]\n" +
                                "• Xem chi tiêu định kỳ\n" +
                                "• Xóa chi tiêu định kỳ [tên]\n" +
                                "• Xử lý chi tiêu đến hạn\n" +
                                "• Tổng quan định kỳ\n" +
                                "• Chi tiêu sắp tới\n" +
                                "• Tắt/Bật chi tiêu [tên]"
                    )
                }

                _aiCommandResult.value = result

            } catch (e: Exception) {
                _aiCommandResult.value = RecurringAICommandResult(
                    success = false,
                    message = "Lỗi thực thi lệnh"
                )
            }
        }
    }

    private fun isAddCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("thêm chi tiêu định kỳ") ||
                lower.contains("tạo chi tiêu định kỳ") ||
                lower.contains("add recurring") ||
                lower.contains("tạo định kỳ") ||
                (lower.contains("thêm") && lower.contains("định kỳ"))
    }

    private fun isListCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("xem chi tiêu định kỳ") ||
                lower.contains("danh sách định kỳ") ||
                lower.contains("list recurring") ||
                lower.contains("hiển thị định kỳ") ||
                (lower.contains("xem") && lower.contains("định kỳ"))
    }

    private fun isDeleteCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("xóa chi tiêu định kỳ") ||
                lower.contains("delete recurring") ||
                lower.contains("remove recurring") ||
                lower.contains("hủy chi tiêu") ||
                (lower.contains("xóa") && lower.contains("định kỳ"))
    }

    private fun isUpdateCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("cập nhật chi tiêu") ||
                lower.contains("update recurring") ||
                lower.contains("sửa chi tiêu") ||
                lower.contains("chỉnh sửa") ||
                (lower.contains("cập nhật") && lower.contains("định kỳ"))
    }

    private fun isProcessCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("xử lý chi tiêu") ||
                lower.contains("process recurring") ||
                lower.contains("đến hạn") ||
                lower.contains("tạo giao dịch")
    }

    private fun isSummaryCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("tổng quan định kỳ") ||
                lower.contains("summary recurring") ||
                lower.contains("thống kê định kỳ") ||
                lower.contains("tổng hợp định kỳ")
    }

    private fun isUpcomingCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("sắp tới") ||
                lower.contains("upcoming") ||
                lower.contains("sắp đến hạn") ||
                lower.contains("sắp đáo hạn")
    }

    private fun isToggleCommand(command: String): Boolean {
        val lower = command.lowercase()
        return lower.contains("tắt chi tiêu") ||
                lower.contains("bật chi tiêu") ||
                lower.contains("toggle recurring") ||
                lower.contains("ngừng") || lower.contains("kích hoạt")
    }

    private suspend fun executeAddCommand(command: String): RecurringAICommandResult {
        return try {
            val parsed = parseAddCommand(command)

            if (parsed.amount <= 0) {
                return RecurringAICommandResult(
                    success = false,
                    message = "Số tiền không hợp lệ. Vui lòng nhập số tiền lớn hơn 0"
                )
            }

            if (parsed.category.isEmpty()) {
                return RecurringAICommandResult(
                    success = false,
                    message = "Không tìm thấy danh mục. Vui lòng thử: 'Thêm chi tiêu định kỳ 1 triệu cho ăn uống hàng tháng'"
                )
            }

            val categoryObj = _availableCategories.value.values.flatten()
                .firstOrNull { it.name.equals(parsed.category, ignoreCase = true) }

            val categoryIcon = categoryObj?.icon ?: "💰"
            val categoryColor = categoryObj?.color ?: "#2196F3"

            addRecurringExpense(
                title = parsed.title,
                amount = parsed.amount,
                category = parsed.category,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                wallet = parsed.wallet,
                description = parsed.description,
                frequency = parsed.frequency,
                startDate = parsed.startDate,
                endDate = parsed.endDate
            )

            RecurringAICommandResult(
                success = true,
                message = "Đã thêm chi tiêu định kỳ thành công:\n" +
                        "• Tên: ${parsed.title}\n" +
                        "• Số tiền: ${formatCurrency(parsed.amount)}\n" +
                        "• Danh mục: ${parsed.category}\n" +
                        "• Tần suất: ${getFrequencyName(parsed.frequency)}",
                data = null
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi thêm chi tiêu định kỳ"
            )
        }
    }

    private fun parseAddCommand(command: String): ParsedAddCommand {
        val lowerCommand = command.lowercase()

        val amount = extractAmount(lowerCommand)
        val category = extractCategory(lowerCommand)

        val frequency = when {
            lowerCommand.contains("hàng ngày") || lowerCommand.contains("daily") -> RecurringFrequency.DAILY
            lowerCommand.contains("hàng tuần") || lowerCommand.contains("weekly") -> RecurringFrequency.WEEKLY
            lowerCommand.contains("hàng tháng") || lowerCommand.contains("monthly") -> RecurringFrequency.MONTHLY
            lowerCommand.contains("hàng quý") || lowerCommand.contains("quarterly") -> RecurringFrequency.QUARTERLY
            lowerCommand.contains("hàng năm") || lowerCommand.contains("yearly") -> RecurringFrequency.YEARLY
            else -> RecurringFrequency.MONTHLY
        }

        val title = when {
            category.contains("ăn uống", ignoreCase = true) -> "Chi phí ăn uống định kỳ"
            category.contains("tiền nhà", ignoreCase = true) || category.contains("thuê nhà", ignoreCase = true) -> "Tiền thuê nhà"
            category.contains("điện nước", ignoreCase = true) -> "Tiền điện nước"
            category.contains("internet", ignoreCase = true) -> "Tiền internet"
            category.contains("điện thoại", ignoreCase = true) -> "Tiền điện thoại"
            category.contains("bảo hiểm", ignoreCase = true) -> "Bảo hiểm"
            category.contains("học phí", ignoreCase = true) -> "Học phí"
            category.contains("xăng xe", ignoreCase = true) -> "Chi phí xăng xe"
            category.contains("gym", ignoreCase = true) -> "Phí tập gym"
            category.contains("netflix", ignoreCase = true) -> "Phí Netflix"
            category.contains("spotify", ignoreCase = true) -> "Phí Spotify"
            else -> "Chi tiêu định kỳ"
        }

        val wallet = "Ví chính"
        val description = "Tạo tự động bởi AI Assistant"
        val startDate = getTodayDateForUI()
        val endDate: String? = null

        return ParsedAddCommand(
            title = title,
            amount = amount,
            category = category,
            frequency = frequency,
            wallet = wallet,
            description = description,
            startDate = startDate,
            endDate = endDate
        )
    }

    private suspend fun executeListCommand(command: String): RecurringAICommandResult {
        return try {
            val lowerCommand = command.lowercase()
            val filterActive = when {
                lowerCommand.contains("đang hoạt động") -> true
                lowerCommand.contains("đã tắt") -> false
                else -> null
            }

            val userId = getCurrentUserId()
            var filtered = _recurringExpenses.value.filter { it.userId == userId }

            filterActive?.let { active ->
                filtered = filtered.filter { it.isActive == active }
            }

            val categoryFilter = extractCategory(lowerCommand)
            if (categoryFilter.isNotEmpty()) {
                filtered = filtered.filter { it.category.contains(categoryFilter, true) }
            }

            val frequency = when {
                lowerCommand.contains("hàng ngày") -> RecurringFrequency.DAILY
                lowerCommand.contains("hàng tuần") -> RecurringFrequency.WEEKLY
                lowerCommand.contains("hàng tháng") -> RecurringFrequency.MONTHLY
                lowerCommand.contains("hàng quý") -> RecurringFrequency.QUARTERLY
                lowerCommand.contains("hàng năm") -> RecurringFrequency.YEARLY
                else -> null
            }
            frequency?.let { freq ->
                filtered = filtered.filter { it.getFrequencyEnum() == freq }
            }

            if (filtered.isEmpty()) {
                return RecurringAICommandResult(
                    success = true,
                    message = "Không có chi tiêu định kỳ nào${filterActive?.let { if (it) " đang hoạt động" else " đã tắt" } ?: ""}"
                )
            }

            RecurringAICommandResult(
                success = true,
                message = buildRecurringListMessage(filtered),
                data = filtered
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi lấy danh sách"
            )
        }
    }

    private suspend fun executeDeleteCommand(command: String): RecurringAICommandResult {
        return try {
            val lowerCommand = command.lowercase()
            val expenseToDelete = findExpenseByCommand(lowerCommand)

            if (expenseToDelete == null) {
                return RecurringAICommandResult(
                    success = false,
                    message = "Không tìm thấy chi tiêu định kỳ để xóa. Vui lòng thử: 'Xóa chi tiêu định kỳ tiền nhà'"
                )
            }

            deleteRecurringExpense(expenseToDelete.id)

            RecurringAICommandResult(
                success = true,
                message = "Đã xóa chi tiêu định kỳ: ${expenseToDelete.title}"
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi xóa chi tiêu"
            )
        }
    }

    private suspend fun executeUpdateCommand(command: String): RecurringAICommandResult {
        return try {
            val lowerCommand = command.lowercase()
            val expenseToUpdate = findExpenseByCommand(lowerCommand)

            if (expenseToUpdate == null) {
                return RecurringAICommandResult(
                    success = false,
                    message = "Không tìm thấy chi tiêu định kỳ để cập nhật"
                )
            }

            val newAmount = extractAmount(lowerCommand)
            val newCategory = extractCategory(lowerCommand)

            val newFrequency = when {
                lowerCommand.contains("hàng ngày") -> RecurringFrequency.DAILY
                lowerCommand.contains("hàng tuần") -> RecurringFrequency.WEEKLY
                lowerCommand.contains("hàng tháng") -> RecurringFrequency.MONTHLY
                lowerCommand.contains("hàng quý") -> RecurringFrequency.QUARTERLY
                lowerCommand.contains("hàng năm") -> RecurringFrequency.YEARLY
                else -> null
            }

            val updatedExpense = if (newFrequency != null) {
                RecurringExpense.Companion.fromEnum(
                    id = expenseToUpdate.id,
                    title = expenseToUpdate.title,
                    amount = if (newAmount > 0) newAmount else expenseToUpdate.amount,
                    category = if (newCategory.isNotEmpty()) newCategory else expenseToUpdate.category,
                    categoryIcon = expenseToUpdate.categoryIcon,
                    categoryColor = expenseToUpdate.categoryColor,
                    wallet = expenseToUpdate.wallet,
                    description = expenseToUpdate.description,
                    frequency = newFrequency,
                    startDate = expenseToUpdate.startDate,
                    endDate = expenseToUpdate.endDate,
                    nextOccurrence = expenseToUpdate.nextOccurrence,
                    isActive = expenseToUpdate.isActive,
                    userId = expenseToUpdate.userId,
                    totalGenerated = expenseToUpdate.totalGenerated,
                    lastGenerated = expenseToUpdate.lastGenerated
                )
            } else {
                expenseToUpdate.copy(
                    amount = if (newAmount > 0) newAmount else expenseToUpdate.amount,
                    category = if (newCategory.isNotEmpty()) newCategory else expenseToUpdate.category
                )
            }

            updateRecurringExpense(updatedExpense)

            RecurringAICommandResult(
                success = true,
                message = "Đã cập nhật chi tiêu định kỳ: ${expenseToUpdate.title}",
                data = updatedExpense
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi cập nhật"
            )
        }
    }

    private suspend fun executeProcessCommand(context: Context): RecurringAICommandResult {
        return try {
            val userId = getCurrentUserId()
            if (userId == "anonymous") {
                return RecurringAICommandResult(
                    success = false,
                    message = "Vui lòng đăng nhập để xử lý chi tiêu định kỳ"
                )
            }

            val today = getCurrentDateInternal()
            val lastProcessedDate = getLastProcessedDate(context)

            if (lastProcessedDate == today) {
                return RecurringAICommandResult(
                    success = true,
                    message = "Chi tiêu định kỳ đã được xử lý hôm nay rồi!"
                )
            }

            val dueExpenses = _recurringExpenses.value.filter { expense ->
                isExpenseDueToday(expense, today) &&
                        expense.userId == userId &&
                        expense.isActive
            }

            if (dueExpenses.isEmpty()) {
                saveLastProcessedDate(context, today)
                return RecurringAICommandResult(
                    success = true,
                    message = "Không có chi tiêu định kỳ nào đến hạn hôm nay"
                )
            }

            var processedCount = 0
            val processedExpenses = mutableListOf<RecurringExpense>()

            dueExpenses.forEach { expense ->
                try {
                    transactionViewModel.addTransactionFromRecurringExpense(expense, null)

                    val nextDate = calculateNextOccurrence(today, expense.getFrequencyEnum())
                    val updatedExpense = expense.copy(
                        nextOccurrence = nextDate,
                        totalGenerated = expense.totalGenerated + 1,
                        lastGenerated = today
                    )

                    db.collection(COLLECTION_NAME)
                        .document(updatedExpense.id)
                        .set(updatedExpense)
                        .await()

                    processedCount++
                    processedExpenses.add(updatedExpense)

                } catch (e: Exception) {
                }
            }

            saveLastProcessedDate(context, today)

            RecurringAICommandResult(
                success = true,
                message = "Đã xử lý $processedCount chi tiêu định kỳ đến hạn:\n" +
                        processedExpenses.joinToString("\n") {
                            "• ${it.title}: ${formatCurrency(it.amount)}"
                        },
                data = processedExpenses
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi xử lý chi tiêu đến hạn"
            )
        }
    }

    private suspend fun executeSummaryCommand(): RecurringAICommandResult {
        return try {
            val userId = getCurrentUserId()
            val userExpenses = _recurringExpenses.value.filter { it.userId == userId }

            if (userExpenses.isEmpty()) {
                return RecurringAICommandResult(
                    success = true,
                    message = "Chưa có chi tiêu định kỳ nào"
                )
            }

            RecurringAICommandResult(
                success = true,
                message = buildSummaryMessage(userExpenses),
                data = userExpenses
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi lấy tổng quan"
            )
        }
    }

    private suspend fun executeUpcomingCommand(): RecurringAICommandResult {
        return try {
            val upcoming = getUpcomingExpenses(7)

            if (upcoming.isEmpty()) {
                return RecurringAICommandResult(
                    success = true,
                    message = "Không có chi tiêu định kỳ nào sắp đến hạn trong 7 ngày tới"
                )
            }

            RecurringAICommandResult(
                success = true,
                message = buildUpcomingMessage(upcoming),
                data = upcoming
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi lấy chi tiêu sắp tới"
            )
        }
    }

    private suspend fun executeToggleCommand(command: String): RecurringAICommandResult {
        return try {
            val lowerCommand = command.lowercase()
            val enable = !lowerCommand.contains("tắt")

            val expenseToToggle = findExpenseByCommand(lowerCommand)

            if (expenseToToggle == null) {
                return RecurringAICommandResult(
                    success = false,
                    message = "Không tìm thấy chi tiêu định kỳ để ${if (enable) "bật" else "tắt"}"
                )
            }

            toggleRecurringExpense(expenseToToggle.id)

            RecurringAICommandResult(
                success = true,
                message = "${if (enable) "Bật" else "Tắt"} chi tiêu định kỳ: ${expenseToToggle.title}",
                data = expenseToToggle
            )

        } catch (e: Exception) {
            RecurringAICommandResult(
                success = false,
                message = "Lỗi ${if (command.contains("tắt")) "tắt" else "bật"} chi tiêu"
            )
        }
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    private fun loadAvailableCategories() {
        viewModelScope.launch {
            try {
                val expenseCategories = categoryViewModel.getSubCategoriesForRecurringExpense("expense")
                val incomeCategories = categoryViewModel.getSubCategoriesForRecurringExpense("income")

                _availableCategories.value = mapOf(
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
                                    if (isValidExpenseCategory(it)) {
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

    private fun isValidExpenseCategory(expense: RecurringExpense): Boolean {
        return try {
            categoryViewModel.doesCategoryExist(expense.category)
        } catch (e: Exception) {
            true
        }
    }

    private fun findExpenseByCommand(command: String): RecurringExpense? {
        val userId = getCurrentUserId()
        val userExpenses = _recurringExpenses.value.filter { it.userId == userId }

        userExpenses.forEach { expense ->
            if (command.contains(expense.title.lowercase())) {
                return expense
            }
        }

        val category = extractCategory(command)
        if (category.isNotEmpty()) {
            return userExpenses.find {
                it.category.contains(category, true)
            }
        }

        return null
    }

    private fun buildRecurringListMessage(expenses: List<RecurringExpense>): String {
        val totalAmount = expenses.sumOf { it.amount }
        val monthlyTotal = expenses
            .filter { it.getFrequencyEnum() == RecurringFrequency.MONTHLY }
            .sumOf { it.amount }

        val header = """
            DANH SÁCH CHI TIÊU ĐỊNH KỲ
            
            • Tổng số: ${expenses.size} chi tiêu
            • Tổng tiền: ${formatCurrency(totalAmount)}
            • Ước tính hàng tháng: ${formatCurrency(monthlyTotal)}
            
        """.trimIndent()

        val items = expenses.take(10).joinToString("\n\n") { expense ->
            """
            • ${expense.title}
              Số tiền: ${formatCurrency(expense.amount)}
              Danh mục: ${expense.category}
              Tần suất: ${getFrequencyName(expense.getFrequencyEnum())}
              Đến hạn: ${RecurringExpense.formatDateForUI(expense.nextOccurrence)}
              Trạng thái: ${if (expense.isActive) "Đang hoạt động" else "Đã tắt"}
            """.trimIndent()
        }

        val footer = if (expenses.size > 10) {
            "\n\n... và ${expenses.size - 10} chi tiêu khác"
        } else ""

        return header + "\n\n" + items + footer
    }

    private fun buildSummaryMessage(expenses: List<RecurringExpense>): String {
        val activeExpenses = expenses.filter { it.isActive }
        val inactiveExpenses = expenses.filter { !it.isActive }

        val totalMonthly = activeExpenses
            .filter { it.getFrequencyEnum() == RecurringFrequency.MONTHLY }
            .sumOf { it.amount }

        val today = getCurrentDateInternal()
        val dueToday = activeExpenses.count { it.nextOccurrence == today }
        val dueThisWeek = activeExpenses.count { expense ->
            getDaysBetween(today, expense.nextOccurrence) in 0..7
        }

        val topCategories = activeExpenses
            .groupBy { it.category }
            .mapValues { (_, expList) ->
                expList.sumOf { expense ->
                    when (expense.getFrequencyEnum()) {
                        RecurringFrequency.DAILY -> expense.amount * 30
                        RecurringFrequency.WEEKLY -> expense.amount * 4
                        RecurringFrequency.MONTHLY -> expense.amount
                        RecurringFrequency.QUARTERLY -> expense.amount / 3
                        RecurringFrequency.YEARLY -> expense.amount / 12
                    }
                }
            }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        return """
            TỔNG QUAN CHI TIÊU ĐỊNH KỲ
            
            THỐNG KÊ:
            • Đang hoạt động: ${activeExpenses.size} chi tiêu
            • Đã tắt: ${inactiveExpenses.size} chi tiêu
            • Tổng hàng tháng: ${formatCurrency(totalMonthly)}
            • Đến hạn hôm nay: $dueToday chi tiêu
            • Sắp đến hạn (7 ngày): $dueThisWeek chi tiêu
            
            TOP DANH MỤC:
            ${if (topCategories.isNotEmpty()) {
            topCategories.joinToString("\n") { (cat, amount) ->
                "• $cat: ${formatCurrency(amount)}/tháng"
            }
        } else "Chưa có dữ liệu"}
            
            KIẾN NGHỊ:
            ${getRecommendations(expenses)}
        """.trimIndent()
    }

    private fun buildUpcomingMessage(expenses: List<RecurringExpense>): String {
        return """
            CHI TIÊU ĐỊNH KỲ SẮP ĐẾN HẠN
            
            ${expenses.take(5).joinToString("\n\n") { expense ->
            """
                • ${expense.title}
                  Số tiền: ${formatCurrency(expense.amount)}
                  Danh mục: ${expense.category}
                  Đến hạn: ${RecurringExpense.formatDateForUI(expense.nextOccurrence)} (còn ${getDaysBetween(getCurrentDateInternal(), expense.nextOccurrence)} ngày)
                  Tần suất: ${getFrequencyName(expense.getFrequencyEnum())}
                """.trimIndent()
        }}
            
            ${if (expenses.size > 5) "\n... và ${expenses.size - 5} chi tiêu khác" else ""}
        """.trimIndent()
    }

    private fun getUpcomingExpenses(daysAhead: Int): List<RecurringExpense> {
        val userId = getCurrentUserId()
        val today = getCurrentDateInternal()

        return _recurringExpenses.value.filter { expense ->
            expense.userId == userId &&
                    expense.isActive &&
                    getDaysBetween(today, expense.nextOccurrence) in 0..daysAhead
        }.sortedBy { it.nextOccurrence }
    }

    private fun getRecommendations(expenses: List<RecurringExpense>): String {
        val activeExpenses = expenses.filter { it.isActive }

        if (activeExpenses.isEmpty()) {
            return "Hãy thêm chi tiêu định kỳ đầu tiên!"
        }

        val recommendations = mutableListOf<String>()

        activeExpenses
            .filter { it.amount > 5000000 }
            .take(2)
            .forEach { expense ->
                recommendations.add("Xem xét lại '${expense.title}' (${formatCurrency(expense.amount)}) - có thể cắt giảm?")
            }

        val today = getCurrentDateInternal()
        val overdue = activeExpenses.count {
            isDateBefore(it.nextOccurrence, today)
        }
        if (overdue > 0) {
            recommendations.add("Có $overdue chi tiêu đã quá hạn. Hãy xử lý ngay!")
        }

        val dailyCount = activeExpenses.count {
            it.getFrequencyEnum() == RecurringFrequency.DAILY
        }
        if (dailyCount > 2) {
            recommendations.add("Có $dailyCount chi tiêu hàng ngày. Cân nhắc chuyển sang hàng tuần để dễ quản lý")
        }

        if (recommendations.size < 3) {
            recommendations.addAll(listOf(
                "Đánh giá lại các chi tiêu định kỳ mỗi 3 tháng",
                "Cân nhắc kết hợp các chi tiêu nhỏ thành một khoản lớn hơn",
                "Sử dụng tính năng nhắc nhở cho các chi tiêu quan trọng"
            ))
        }

        return recommendations.take(3).joinToString("\n") { "• $it" }
    }

    private fun extractAmount(text: String): Double {
        val patterns = listOf(
            Regex("""(\d+([.,]\d+)?)\s*(triệu|tr|million|m)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+([.,]\d+)?)\s*(nghìn|nghin|ngàn|ngan|k|thousand)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(triệu|tr)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(nghìn|nghin|ngàn|ngan|k)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+([.,]\d+)?)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text.lowercase())
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", ".")
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val unit = match.groupValues.getOrNull(3)?.lowercase() ?: ""

                return when {
                    unit.contains("triệu") || unit.contains("tr") || unit.contains("million") || unit.contains("m") ->
                        amount * 1000000
                    unit.contains("nghìn") || unit.contains("nghin") || unit.contains("ngàn") ||
                            unit.contains("ngan") || unit.contains("k") || unit.contains("thousand") ->
                        amount * 1000
                    amount > 1000 && unit.isEmpty() -> amount
                    amount < 1000 && unit.isEmpty() -> amount * 1000
                    else -> amount
                }
            }
        }
        return 0.0
    }

    private fun extractCategory(text: String): String {
        val categories = _availableCategories.value.values.flatten()
        val lowerText = text.lowercase()

        categories.forEach { category ->
            if (lowerText.contains(category.name.lowercase())) {
                return category.name
            }

            when (category.name.lowercase()) {
                "ăn uống" -> if (lowerText.contains("ăn") || lowerText.contains("uống") ||
                    lowerText.contains("cafe") || lowerText.contains("food") || lowerText.contains("restaurant")) return category.name
                "mua sắm" -> if (lowerText.contains("mua sắm") || lowerText.contains("shopping") ||
                    lowerText.contains("quần áo")) return category.name
                "giải trí" -> if (lowerText.contains("giải trí") || lowerText.contains("xem phim") ||
                    lowerText.contains("game") || lowerText.contains("netflix")) return category.name
                "y tế" -> if (lowerText.contains("y tế") || lowerText.contains("bệnh viện") ||
                    lowerText.contains("thuốc") || lowerText.contains("phòng khám")) return category.name
                "giáo dục" -> if (lowerText.contains("giáo dục") || lowerText.contains("học") ||
                    lowerText.contains("sách") || lowerText.contains("học phí")) return category.name
                "nhà ở" -> if (lowerText.contains("nhà") || lowerText.contains("tiền nhà") ||
                    lowerText.contains("thuê nhà") || lowerText.contains("mortgage")) return category.name
                "đi lại" -> if (lowerText.contains("đi lại") || lowerText.contains("xăng") ||
                    lowerText.contains("xe") || lowerText.contains("grab")) return category.name
                "tiện ích" -> if (lowerText.contains("điện") || lowerText.contains("nước") ||
                    lowerText.contains("internet") || lowerText.contains("điện thoại")) return category.name
            }
        }

        return ""
    }

    private fun getFrequencyName(frequency: RecurringFrequency): String {
        return when (frequency) {
            RecurringFrequency.DAILY -> "Hàng ngày"
            RecurringFrequency.WEEKLY -> "Hàng tuần"
            RecurringFrequency.MONTHLY -> "Hàng tháng"
            RecurringFrequency.QUARTERLY -> "Hàng quý"
            RecurringFrequency.YEARLY -> "Hàng năm"
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            NumberFormat.getNumberInstance(Locale.getDefault()).format(amount) + "đ"
        } catch (e: Exception) {
            amount.toString() + "đ"
        }
    }

    private fun getTodayDateForUI(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentDateInternal(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    private fun getDaysBetween(date1: String, date2: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1 = sdf.parse(date1)
            val d2 = sdf.parse(date2)

            if (d1 != null && d2 != null) {
                val diff = d2.time - d1.time
                (diff / (1000 * 60 * 60 * 24)).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
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

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun clearAICommandResult() {
        _aiCommandResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        expensesListener?.remove()
        isListenerSetup = false
    }
}