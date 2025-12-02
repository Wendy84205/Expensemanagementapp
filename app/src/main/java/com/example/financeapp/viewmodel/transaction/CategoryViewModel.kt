package com.example.financeapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Category(
    val id: String,
    val name: String,
    val type: String, // "expense" hoặc "income"
    val isMainCategory: Boolean = false,
    val parentCategoryId: String? = null,
    val icon: String = "🍹",
    val color: String = "#FF69B4"
)

class CategoryViewModel : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    // THÊM: StateFlow cho selectable categories
    private val _selectableCategories = MutableStateFlow<Map<String, List<Category>>>(emptyMap())
    val selectableCategories: StateFlow<Map<String, List<Category>>> = _selectableCategories

    private var isInitialized = false

    val categories: StateFlow<List<Category>> = _categories

    // THÊM: Companion object để shared instance
    companion object {
        @Volatile private var instance: CategoryViewModel? = null

        fun getInstance(): CategoryViewModel {
            return instance ?: synchronized(this) {
                instance ?: CategoryViewModel().also { instance = it }
            }
        }
    }

    init {
        initializeDefaultCategories()
        updateSelectableCategories()
    }

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
    }

    private fun updateSelectableCategories() {
        val expenseCategories = getSelectableCategoriesInternal("expense")
        val incomeCategories = getSelectableCategoriesInternal("income")
        _selectableCategories.value = mapOf(
            "expense" to expenseCategories,
            "income" to incomeCategories
        )
    }

    private fun getSelectableCategoriesInternal(type: String): List<Category> {
        ensureDefaultCategories()

        // THAY ĐỔI: Chỉ lấy danh mục con (không phải danh mục lớn)
        val subCategories = _categories.value.filter {
            !it.isMainCategory && it.type == type
        }

        val otherCategory = _categories.value.find {
            it.isMainCategory && it.name == "Khác" && it.type == type
        }

        return if (otherCategory != null) {
            subCategories + otherCategory
        } else {
            subCategories
        }
    }

    fun ensureDefaultCategories() {
        if (_categories.value.isEmpty()) {
            initializeDefaultCategories()
        }
    }

    fun getMainCategories(type: String? = null): List<Category> {
        ensureDefaultCategories()
        return if (type != null) {
            _categories.value.filter { it.isMainCategory && it.type == type }
        } else {
            _categories.value.filter { it.isMainCategory }
        }
    }

    fun getSubCategories(parentCategoryId: String): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter { it.parentCategoryId == parentCategoryId }
    }

    // THÊM: Hàm mới để lấy TẤT CẢ danh mục con theo type
    fun getAllSubCategories(type: String): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter {
            !it.isMainCategory && it.type == type
        }
    }

    // THÊM: Hàm lấy danh mục con cho recurring expense (chỉ subcategories)
    fun getSubCategoriesForRecurringExpense(type: String): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter {
            !it.isMainCategory && it.type == type
        }
    }

    // Giữ nguyên hàm này cho các component khác sử dụng
    fun getSelectableCategories(type: String): List<Category> {
        return getSelectableCategoriesInternal(type)
    }

    // Hàm để lấy danh mục hiển thị theo nhóm (cho màn hình quản lý danh mục)
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

    fun addCategory(
        name: String,
        type: String,
        isMainCategory: Boolean = false,
        parentCategoryId: String? = null,
        icon: String = "🍹"
    ) {
        viewModelScope.launch {
            ensureDefaultCategories()

            val newCategory = Category(
                id = System.currentTimeMillis().toString(),
                name = name,
                type = type,
                isMainCategory = isMainCategory,
                parentCategoryId = parentCategoryId,
                icon = icon
            )
            _categories.value = _categories.value + newCategory

            // CẬP NHẬT: Refresh selectable categories
            updateSelectableCategories()
        }
    }

    fun canAddSubCategory(parentCategoryId: String): Boolean {
        return getSubCategories(parentCategoryId).size < 20
    }

    fun getCategoryById(categoryId: String): Category? {
        ensureDefaultCategories()
        return _categories.value.find { it.id == categoryId }
    }

    fun isCategoryNameExists(name: String, parentCategoryId: String? = null): Boolean {
        ensureDefaultCategories()
        return _categories.value.any {
            it.name.equals(name, ignoreCase = true) && it.parentCategoryId == parentCategoryId
        }
    }

    fun getCurrentSubCategoryCount(parentCategoryId: String): Int {
        return getSubCategories(parentCategoryId).size
    }

    fun getIncomeCategories(): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter { it.type == "income" }
    }

    fun getExpenseCategories(): List<Category> {
        ensureDefaultCategories()
        return _categories.value.filter { it.type == "expense" }
    }

    // =====================
    // CÁC HÀM MỚI ĐỂ KẾT NỐI VỚI RECURRING EXPENSE
    // =====================

    // THÊM: Hàm mới để hỗ trợ RecurringExpenseViewModel
    fun validateCategoryForRecurringExpense(categoryId: String, expectedType: String): Boolean {
        ensureDefaultCategories()
        val category = getCategoryById(categoryId)
        return category != null && category.type == expectedType
    }

    // THÊM: Lấy category info cho recurring expense
    fun getCategoryInfoForRecurringExpense(categoryId: String): Pair<String, String>? {
        ensureDefaultCategories()
        val category = getCategoryById(categoryId)
        return if (category != null) {
            Pair(category.icon, category.color)
        } else {
            null
        }
    }

    // THÊM: Kiểm tra category có tồn tại không
    fun doesCategoryExist(categoryId: String): Boolean {
        ensureDefaultCategories()
        return getCategoryById(categoryId) != null
    }

    // THÊM: Lấy categories cho recurring expense selection (CHỈ DANH MỤC CON)
    fun getCategoriesForRecurringExpense(type: String): List<Category> {
        return getSubCategoriesForRecurringExpense(type)
    }

    // THÊM: Tìm category bằng name (hỗ trợ backward compatibility)
    fun findCategoryByName(categoryName: String): Category? {
        ensureDefaultCategories()
        return _categories.value.find { it.name == categoryName }
    }

    // THÊM: Lấy tất cả categories (cho các tính toán tổng hợp)
    fun getAllCategories(): List<Category> {
        ensureDefaultCategories()
        return _categories.value
    }

    // THÊM: Refresh categories (khi có thay đổi từ bên ngoài)
    fun refreshCategories() {
        updateSelectableCategories()
    }
}