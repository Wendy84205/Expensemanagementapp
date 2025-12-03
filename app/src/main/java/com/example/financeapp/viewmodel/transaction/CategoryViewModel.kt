package com.example.financeapp.viewmodel.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Data class đại diện cho danh mục chi tiêu/thu nhập
 * @param id ID duy nhất của danh mục
 * @param name Tên danh mục
 * @param type Loại danh mục: "expense" (chi tiêu) hoặc "income" (thu nhập)
 * @param isMainCategory Có phải danh mục chính không
 * @param parentCategoryId ID danh mục cha (nếu là danh mục con)
 * @param icon Icon hiển thị
 * @param color Màu sắc hiển thị
 */
data class Category(
    val id: String,
    val name: String,
    val type: String, // "expense" hoặc "income"
    val isMainCategory: Boolean = false,
    val parentCategoryId: String? = null,
    val icon: String = "🍹",
    val color: String = "#FF69B4"
)

/**
 * ViewModel quản lý danh mục chi tiêu/thu nhập
 * Xử lý việc tạo, đọc, cập nhật danh mục và cung cấp dữ liệu cho UI
 */
class CategoryViewModel : ViewModel() {

    companion object {
        private const val TAG = "CategoryViewModel"

        @Volatile
        private var instance: CategoryViewModel? = null

        /**
         * Lấy singleton instance của CategoryViewModel
         */
        fun getInstance(): CategoryViewModel {
            return instance ?: synchronized(this) {
                instance ?: CategoryViewModel().also { instance = it }
            }
        }
    }

    // ==================== STATE FLOWS ====================

    /** Flow danh sách tất cả danh mục */
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    /** Flow danh sách danh mục có thể chọn theo loại */
    private val _selectableCategories = MutableStateFlow<Map<String, List<Category>>>(emptyMap())
    val selectableCategories: StateFlow<Map<String, List<Category>>> = _selectableCategories

    /** Trạng thái khởi tạo */
    private var isInitialized = false

    // ==================== INITIALIZATION ====================

    init {
        Log.d(TAG, "CategoryViewModel khởi tạo")
        initializeDefaultCategories()
        updateSelectableCategories()
    }

    /**
     * Khởi tạo danh mục mặc định khi ứng dụng chạy lần đầu
     */
    private fun initializeDefaultCategories() {
        if (isInitialized) return

        val defaultCategories = mutableListOf<Category>()

        // DANH MỤC LỚN - CHI TIÊU (EXPENSE)
        val expenseMainCategories = listOf(
            Category("1", "Chi tiêu - sinh hoạt", "expense", true, icon = "🛒"),
            Category("2", "Chi phí phát sinh", "expense", true, icon = "🎯"),
            Category("3", "Chi phí cố định", "expense", true, icon = "🏠"),
            Category("4", "Đầu tư - tiết kiệm", "expense", true, icon = "📈"),
            Category("999", "Khác", "expense", true, icon = "❓")
        )

        // DANH MỤC LỚN - THU NHẬP (INCOME)
        val incomeMainCategories = listOf(
            Category("5", "Thu nhập", "income", true, icon = "💰"),
            Category("1000", "Khác", "income", true, icon = "❓")
        )

        defaultCategories.addAll(expenseMainCategories)
        defaultCategories.addAll(incomeMainCategories)

        // DANH MỤC CON CHO "Chi tiêu - sinh hoạt"
        val chiTieuSubCategories = listOf(
            Category("101", "Chợ, siêu thị", "expense", false, "1", icon = "🛍️"),
            Category("102", "Ăn uống", "expense", false, "1", icon = "🍽️"),
            Category("103", "Di chuyển", "expense", false, "1", icon = "🚗")
        )

        // DANH MỤC CON CHO "Chi phí phát sinh"
        val chiPhiPhatSinhSubCategories = listOf(
            Category("201", "Mua sắm", "expense", false, "2", icon = "🛒"),
            Category("202", "Giải trí", "expense", false, "2", icon = "🎮"),
            Category("203", "Làm đẹp", "expense", false, "2", icon = "💄"),
            Category("204", "Sức khỏe", "expense", false, "2", icon = "🏥"),
            Category("205", "Từ thiện", "expense", false, "2", icon = "❤️")
        )

        // DANH MỤC CON CHO "Chi phí cố định"
        val chiPhiCoDinhSubCategories = listOf(
            Category("301", "Hóa đơn", "expense", false, "3", icon = "🧾"),
            Category("302", "Nhà cửa", "expense", false, "3", icon = "🏠"),
            Category("303", "Người thân", "expense", false, "3", icon = "👨‍👩‍👧‍👦")
        )

        // DANH MỤC CON CHO "Đầu tư - tiết kiệm"
        val dauTuTietKiemSubCategories = listOf(
            Category("401", "Đầu tư", "expense", false, "4", icon = "📊"),
            Category("402", "Học tập", "expense", false, "4", icon = "🎓")
        )

        // DANH MỤC CON CHO "Thu nhập"
        val thuNhapSubCategories = listOf(
            Category("501", "Lương", "income", false, "5", icon = "💵"),
            Category("502", "Thưởng", "income", false, "5", icon = "🎁"),
            Category("503", "Đầu tư", "income", false, "5", icon = "📈"),
            Category("504", "Kinh doanh", "income", false, "5", icon = "💼")
        )

        defaultCategories.addAll(chiTieuSubCategories)
        defaultCategories.addAll(chiPhiPhatSinhSubCategories)
        defaultCategories.addAll(chiPhiCoDinhSubCategories)
        defaultCategories.addAll(dauTuTietKiemSubCategories)
        defaultCategories.addAll(thuNhapSubCategories)

        _categories.value = defaultCategories
        isInitialized = true

        Log.d(TAG, "Đã khởi tạo ${defaultCategories.size} danh mục mặc định")
    }

    /**
     * Cập nhật danh sách danh mục có thể chọn
     */
    private fun updateSelectableCategories() {
        val expenseCategories = getSelectableCategoriesInternal("expense")
        val incomeCategories = getSelectableCategoriesInternal("income")

        _selectableCategories.value = mapOf(
            "expense" to expenseCategories,
            "income" to incomeCategories
        )

        Log.d(TAG, "Đã cập nhật selectable categories: Expense=${expenseCategories.size}, Income=${incomeCategories.size}")
    }

    /**
     * Lấy danh sách danh mục có thể chọn nội bộ
     */
    private fun getSelectableCategoriesInternal(type: String): List<Category> {
        ensureDefaultCategories()

        // Chỉ lấy danh mục con (không phải danh mục lớn)
        val subCategories = _categories.value.filter {
            !it.isMainCategory && it.type == type
        }

        // Thêm danh mục "Khác" nếu có
        val otherCategory = _categories.value.find {
            it.isMainCategory && it.name == "Khác" && it.type == type
        }

        return if (otherCategory != null) {
            subCategories + otherCategory
        } else {
            subCategories
        }
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Đảm bảo danh mục mặc định đã được khởi tạo
     */
    fun ensureDefaultCategories() {
        if (_categories.value.isEmpty()) {
            initializeDefaultCategories()
        }
    }

    /**
     * Lấy danh sách danh mục chính theo loại
     * @param type Loại danh mục ("expense" hoặc "income"), null để lấy tất cả
     */
    fun getMainCategories(type: String? = null): List<Category> {
        ensureDefaultCategories()

        return if (type != null) {
            _categories.value.filter { it.isMainCategory && it.type == type }
        } else {
            _categories.value.filter { it.isMainCategory }
        }
    }

    /**
     * Lấy danh sách danh mục con của một danh mục chính
     * @param parentCategoryId ID danh mục cha
     */
    fun getSubCategories(parentCategoryId: String): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter { it.parentCategoryId == parentCategoryId }
    }

    /**
     * Lấy tất cả danh mục con theo loại
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun getAllSubCategories(type: String): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter {
            !it.isMainCategory && it.type == type
        }
    }

    /**
     * Lấy danh sách danh mục có thể chọn
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun getSelectableCategories(type: String): List<Category> {
        return getSelectableCategoriesInternal(type)
    }

    /**
     * Lấy danh mục con cho recurring expense (chỉ subcategories)
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun getSubCategoriesForRecurringExpense(type: String): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter {
            !it.isMainCategory && it.type == type
        }
    }

    /**
     * Lấy danh mục theo ID
     * @param categoryId ID danh mục cần tìm
     */
    fun getCategoryById(categoryId: String): Category? {
        ensureDefaultCategories()
        return _categories.value.find { it.id == categoryId }
    }

    /**
     * Lấy danh sách danh mục theo nhóm (cho màn hình quản lý danh mục)
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun getCategoriesGroupedByParent(type: String): Map<Category, List<Category>> {
        ensureDefaultCategories()
        val mainCategories = getMainCategories(type)
        val grouped = mutableMapOf<Category, List<Category>>()

        mainCategories.forEach { mainCategory ->
            if (mainCategory.name != "Khác") {
                val subCategories = getSubCategories(mainCategory.id)
                grouped[mainCategory] = subCategories
            } else {
                // Danh mục "Khác" hiển thị riêng
                grouped[mainCategory] = emptyList()
            }
        }

        return grouped
    }

    /**
     * Thêm danh mục mới
     * @param name Tên danh mục
     * @param type Loại danh mục ("expense" hoặc "income")
     * @param isMainCategory Có phải danh mục chính không
     * @param parentCategoryId ID danh mục cha (nếu là danh mục con)
     * @param icon Icon hiển thị
     */
    fun addCategory(
        name: String,
        type: String,
        isMainCategory: Boolean = false,
        parentCategoryId: String? = null,
        icon: String = "🍹"
    ) {
        viewModelScope.launch {
            ensureDefaultCategories()

            // Validation
            if (name.isBlank()) {
                Log.e(TAG, "Tên danh mục không được để trống")
                return@launch
            }

            // Kiểm tra tên đã tồn tại chưa
            if (isCategoryNameExists(name, parentCategoryId)) {
                Log.e(TAG, "Tên danh mục '$name' đã tồn tại")
                return@launch
            }

            val newCategory = Category(
                id = System.currentTimeMillis().toString(),
                name = name.trim(),
                type = type,
                isMainCategory = isMainCategory,
                parentCategoryId = parentCategoryId,
                icon = icon
            )

            _categories.value = _categories.value + newCategory
            updateSelectableCategories()

            Log.d(TAG, "Đã thêm danh mục mới: $name ($type)")
        }
    }

    /**
     * Kiểm tra có thể thêm danh mục con không
     * @param parentCategoryId ID danh mục cha
     */
    fun canAddSubCategory(parentCategoryId: String): Boolean {
        return getSubCategories(parentCategoryId).size < 20
    }

    /**
     * Kiểm tra tên danh mục đã tồn tại chưa
     * @param name Tên danh mục cần kiểm tra
     * @param parentCategoryId ID danh mục cha (nếu kiểm tra danh mục con)
     */
    fun isCategoryNameExists(name: String, parentCategoryId: String? = null): Boolean {
        ensureDefaultCategories()
        return _categories.value.any {
            it.name.equals(name, ignoreCase = true) && it.parentCategoryId == parentCategoryId
        }
    }

    /**
     * Lấy số lượng danh mục con hiện tại
     * @param parentCategoryId ID danh mục cha
     */
    fun getCurrentSubCategoryCount(parentCategoryId: String): Int {
        return getSubCategories(parentCategoryId).size
    }

    /**
     * Lấy danh sách danh mục thu nhập
     */
    fun getIncomeCategories(): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter { it.type == "income" }
    }

    /**
     * Lấy danh sách danh mục chi tiêu
     */
    fun getExpenseCategories(): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter { it.type == "expense" }
    }

    // ==================== METHODS FOR RECURRING EXPENSE ====================

    /**
     * Validate danh mục cho recurring expense
     * @param categoryId ID danh mục cần validate
     * @param expectedType Loại mong đợi ("expense" hoặc "income")
     */
    fun validateCategoryForRecurringExpense(categoryId: String, expectedType: String): Boolean {
        ensureDefaultCategories()
        val category = getCategoryById(categoryId)
        return category != null && category.type == expectedType
    }

    /**
     * Lấy thông tin category cho recurring expense
     * @param categoryId ID danh mục
     * @return Pair<icon, color> hoặc null nếu không tìm thấy
     */
    fun getCategoryInfoForRecurringExpense(categoryId: String): Pair<String, String>? {
        ensureDefaultCategories()
        val category = getCategoryById(categoryId)
        return if (category != null) {
            Pair(category.icon, category.color)
        } else {
            null
        }
    }

    /**
     * Kiểm tra danh mục có tồn tại không
     * @param categoryId ID danh mục cần kiểm tra
     */
    fun doesCategoryExist(categoryId: String): Boolean {
        ensureDefaultCategories()
        return getCategoryById(categoryId) != null
    }

    /**
     * Lấy danh sách danh mục cho recurring expense selection (chỉ danh mục con)
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun getCategoriesForRecurringExpense(type: String): List<Category> {
        return getSubCategoriesForRecurringExpense(type)
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Tìm danh mục bằng tên (hỗ trợ backward compatibility)
     * @param categoryName Tên danh mục cần tìm
     */
    fun findCategoryByName(categoryName: String): Category? {
        ensureDefaultCategories()
        return _categories.value.find { it.name == categoryName }
    }

    /**
     * Lấy tất cả danh mục (cho các tính toán tổng hợp)
     */
    fun getAllCategories(): List<Category> {
        ensureDefaultCategories()
        return _categories.value
    }

    /**
     * Refresh danh sách danh mục (khi có thay đổi từ bên ngoài)
     */
    fun refreshCategories() {
        updateSelectableCategories()
        Log.d(TAG, "Đã refresh danh sách danh mục")
    }

    // ==================== METHODS FOR AI INTEGRATION ====================

    /**
     * Lấy danh mục mặc định theo loại
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun getDefaultCategory(type: String): Category? {
        ensureDefaultCategories()
        return when (type) {
            "income" -> _categories.value.find { it.name == "Khác" && it.type == "income" }
            "expense" -> _categories.value.find { it.name == "Khác" && it.type == "expense" }
            else -> null
        }
    }

    /**
     * Tìm danh mục phù hợp với từ khóa
     * @param keyword Từ khóa tìm kiếm
     * @param type Loại danh mục ("expense" hoặc "income")
     */
    fun findMatchingCategory(keyword: String, type: String): Category? {
        ensureDefaultCategories()

        val keywordLower = keyword.lowercase()

        // Tìm theo tên chính xác
        val exactMatch = _categories.value.find {
            it.type == type && it.name.lowercase() == keywordLower
        }
        if (exactMatch != null) return exactMatch

        // Tìm theo từ khóa trong tên
        val containsMatch = _categories.value.find {
            it.type == type && it.name.lowercase().contains(keywordLower)
        }
        if (containsMatch != null) return containsMatch

        // Trả về danh mục mặc định nếu không tìm thấy
        return getDefaultCategory(type)
    }

    /**
     * Lấy danh sách từ khóa liên quan đến danh mục
     * @param categoryName Tên danh mục
     */
    fun getCategoryKeywords(categoryName: String): List<String> {
        return when (categoryName.lowercase()) {
            "ăn uống" -> listOf("ăn", "uống", "cafe", "nhà hàng", "food", "restaurant", "cơm", "cháo", "phở", "bún", "buffet")
            "mua sắm" -> listOf("mua sắm", "shopping", "mua quần áo", "trung tâm thương mại", "mall", "mua đồ")
            "giải trí" -> listOf("xem phim", "game", "giải trí", "entertainment", "cafe", "cà phê", "karaoke", "pub", "bar")
            "y tế" -> listOf("bệnh viện", "phòng khám", "thuốc", "sức khỏe", "health", "hospital", "khám bệnh")
            "giáo dục" -> listOf("học", "trường", "sách", "giáo dục", "education", "khóa học", "đào tạo")
            "nhà ở" -> listOf("tiền nhà", "thuê nhà", "mortgage", "nhà cửa", "sửa nhà", "điện", "nước")
            "đi lại" -> listOf("xe", "xăng", "dầu", "taxi", "grab", "transport", "đi lại", "di chuyển", "bus", "máy bay")
            "lương" -> listOf("lương", "salary", "tiền lương", "lương tháng", "payroll")
            "thưởng" -> listOf("thưởng", "bonus", "tiền thưởng", "thưởng tết")
            else -> emptyList()
        }
    }
}