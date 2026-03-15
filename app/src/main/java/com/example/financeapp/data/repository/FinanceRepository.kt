package com.example.financeapp.data.repository

import android.app.Application
import android.util.Log
import com.example.financeapp.data.local.database.entities.FinanceDatabase
import com.example.financeapp.data.local.dao.*
import com.example.financeapp.data.local.database.entities.BudgetEntity
import com.example.financeapp.data.local.database.entities.CategoryEntity
import com.example.financeapp.data.local.database.entities.TransactionEntity
import com.example.financeapp.data.local.database.entities.UserProfileEntity
import com.example.financeapp.data.models.*
import com.example.financeapp.data.remote.FirestoreService
import com.example.financeapp.viewmodel.transaction.Category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val application: Application,
    private val firestoreService: FirestoreService
) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Room Database
    private val database = FinanceDatabase.getInstance(application)
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val recurringExpenseDao = database.recurringExpenseDao()
    private val categoryDao = database.categoryDao()
    private val userDao = database.userDao()
    private val userProfileDao = database.userProfileDao()

    // ==================== USER MANAGEMENT ====================
    // 🔹 SỬA: Lấy userId từ auth
    suspend fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    // 🔹 THÊM: Hàm không suspend để dùng trong Flow
    fun getCurrentUserIdSync(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    private fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // 🔹 HÀM MỚI: Tạo hoặc cập nhật user trong Firestore
    suspend fun createOrUpdateUserInFirestore(
        userId: String,
        name: String?,
        email: String?,
        profileImage: String?,
        providerId: String,
        phoneNumber: String? = null
    ): String {
        return firestoreService.createOrUpdateUser(userId, name, email, profileImage, providerId, phoneNumber)
    }

    // 🔹 HÀM MỚI: Lấy user từ Firestore theo email
    suspend fun getUserByEmail(email: String): User? {
        return firestoreService.getUserByEmail(email)
    }

    // 🔹 HÀM MỚI: Kiểm tra user tồn tại theo email
    suspend fun checkUserExistsByEmail(email: String): Boolean {
        return firestoreService.checkUserExistsByEmail(email)
    }

    // 🔹 HÀM MỚI: Migrate dữ liệu cũ
    suspend fun migrateUserData(oldUserId: String, newUserId: String) {
        firestoreService.migrateUserData(oldUserId, newUserId)
    }

    // ==================== SYNC METHODS ====================
    suspend fun syncAllData() {
        if (!isUserLoggedIn()) return

        val userId = getCurrentUserId()
        Log.d("FinanceRepository", "Bắt đầu đồng bộ dữ liệu cho user: $userId")

        syncTransactions(userId)
        syncBudgets(userId)
        syncCategories(userId)
        syncRecurringExpenses(userId)
        syncUserProfile(userId)
    }

    private suspend fun syncBudgets(userId: String) {
        try {
            // SỬA: Sử dụng FirestoreService để lấy budgets
            val remoteBudgets = firestoreService.getBudgets(userId)

            // Lưu vào Room
            remoteBudgets.forEach { budget ->
                // SỬA: Chỉ truyền budget, không truyền userId nữa
                val entity = BudgetEntity.fromBudget(budget.copy(userId = userId))
                budgetDao.insert(entity)
            }

            Log.d("FinanceRepository", "Đã đồng bộ ${remoteBudgets.size} budgets")
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi đồng bộ budgets: ${e.message}")
        }
    }

    private suspend fun syncRecurringExpenses(userId: String) {
        try {
            // SỬA: Sử dụng FirestoreService để lấy recurring expenses
            val remoteExpenses = firestoreService.getRecurringExpenses(userId)

            // TODO: Tạo entity và lưu vào Room nếu cần
            // remoteExpenses.forEach { expense ->
            //     val entity = RecurringExpenseEntity.fromRecurringExpense(expense, userId)
            //     recurringExpenseDao.insert(entity)
            // }

            Log.d("FinanceRepository", "Đã đồng bộ ${remoteExpenses.size} recurring expenses")
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi đồng bộ recurring expenses: ${e.message}")
        }
    }

    private suspend fun syncUserProfile(userId: String) {
        try {
            // Lấy từ Firestore
            val remoteProfile = getRemoteUserProfile(userId)

            // Lưu vào Room nếu có
            remoteProfile?.let { profile ->
                val entity = UserProfileEntity.fromUserProfile(profile)
                userProfileDao.insert(entity)
                Log.d("FinanceRepository", "Đã đồng bộ user profile")
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi đồng bộ user profile: ${e.message}")
        }
    }

    // ==================== ADDITIONAL FIREBASE METHODS ====================
    private suspend fun getRemoteBudgets(userId: String): List<Budget> {
        return try {
            val querySnapshot = db.collection("users").document(userId)
                .collection("budgets")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Budget::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getRemoteUserProfile(userId: String): UserProfile? {
        return try {
            val document = db.collection("users").document(userId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(UserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun syncTransactions(userId: String) {
        try {
            // SỬA: Sử dụng FirestoreService để lấy transactions theo userId
            val remoteTransactions = firestoreService.getTransactionsByUser(userId)

            // Lưu vào Room
            remoteTransactions.forEach { transaction ->
                val entity = TransactionEntity.fromTransaction(transaction, userId)
                transactionDao.insert(entity)
            }

            Log.d("FinanceRepository", "Đã đồng bộ ${remoteTransactions.size} transactions")
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi đồng bộ transactions: ${e.message}")
        }
    }

    private suspend fun syncCategories(userId: String) {
        try {
            // Lưu danh mục mặc định vào Room
            val defaultCategories = getDefaultCategories()
            defaultCategories.forEach { category ->
                val entity = CategoryEntity.fromCategory(category, "system")
                categoryDao.insert(entity)
            }

            // SỬA: Sử dụng FirestoreService để lấy categories theo userId
            val remoteCategories = firestoreService.getCategories(userId)
            remoteCategories.forEach { category ->
                val entity = CategoryEntity.fromCategory(category, userId)
                categoryDao.insert(entity)
            }

            Log.d("FinanceRepository", "Đã đồng bộ ${defaultCategories.size + remoteCategories.size} categories")
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi đồng bộ categories: ${e.message}")
        }
    }

    // ==================== TRANSACTIONS (LOCAL + REMOTE) ====================
    suspend fun addTransaction(transaction: Transaction) {
        val userId = getCurrentUserId()

        // 1. Lưu vào Room (offline)
        val entity = TransactionEntity.fromTransaction(transaction, userId)
        transactionDao.insert(entity)

        // 2. Đồng bộ lên Firestore nếu có mạng
        if (isUserLoggedIn()) {
            try {
                // SỬA: Sử dụng FirestoreService với userId
                firestoreService.saveTransaction(transaction, userId)
                // Cập nhật trạng thái đã đồng bộ
                transactionDao.update(entity.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("FinanceRepository", "Transaction saved offline, will sync later")
            }
        }
    }

    // 🔹 SỬA: Dùng getCurrentUserIdSync() cho Flow
    fun getTransactions(): Flow<List<Transaction>> {
        val userId = getCurrentUserIdSync()
        return transactionDao.getTransactionsByUser(userId)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    // 🔹 HÀM MỚI: Lấy transactions từ Firestore (online)
    suspend fun getTransactionsFromFirestore(userId: String): List<Transaction> {
        return firestoreService.getTransactionsByUser(userId)
    }

    fun getRecentTransactions(days: Int = 30): Flow<List<Transaction>> {
        val userId = getCurrentUserIdSync()
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val startDate = formatDateForRoom(calendar.time)

        val endDate = formatDateForRoom(java.util.Date())

        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        val userId = getCurrentUserIdSync()
        return transactionDao.getTransactionsByCategory(userId, categoryId)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    suspend fun getTotalIncome(startDate: String, endDate: String): Double {
        val userId = getCurrentUserId()
        return transactionDao.getTotalByType(userId, true, startDate, endDate) ?: 0.0
    }

    suspend fun getTotalExpense(startDate: String, endDate: String): Double {
        val userId = getCurrentUserId()
        return transactionDao.getTotalByType(userId, false, startDate, endDate) ?: 0.0
    }

    // ==================== TRANSACTIONS (LOCAL + REMOTE) ====================
    suspend fun deleteTransaction(transactionId: String) {
        val userId = getCurrentUserId()

        // 1. Xóa từ Firestore trước (online)
        if (isUserLoggedIn()) {
            try {
                firestoreService.deleteTransaction(transactionId, userId)
            } catch (e: Exception) {
                Log.e("FinanceRepository", "Lỗi xóa transaction từ Firestore: ${e.message}")
                // Vẫn tiếp tục xóa từ Room dù Firestore có lỗi
            }
        }

        // 2. Xóa từ Room (offline)
        try {
            // Sử dụng @Query để xóa trực tiếp
            transactionDao.deleteTransactionById(transactionId)
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi xóa transaction từ Room: ${e.message}")
        }
    }

    // ==================== CATEGORIES ====================
    suspend fun addCategory(category: Category) {
        val userId = getCurrentUserId()
        val entity = CategoryEntity.fromCategory(category, userId)
        categoryDao.insert(entity)

        if (isUserLoggedIn()) {
            try {
                // SỬA: Sử dụng FirestoreService với userId
                firestoreService.saveCategory(category, userId)
                categoryDao.update(entity.copy(isSynced = true))
            } catch (e: Exception) {
                // Saved offline
            }
        }
    }

    // 🔹 SỬA: Dùng getCurrentUserIdSync() cho Flow
    fun getCategories(): Flow<List<Category>> {
        val userId = getCurrentUserIdSync()
        return categoryDao.getCategoriesByUser(userId)
            .map { entities -> entities.map { it.toCategory() } }
    }

    // 🔹 HÀM MỚI: Lấy categories từ Firestore (online)
    suspend fun getCategoriesFromFirestore(userId: String): List<Category> {
        return firestoreService.getCategories(userId)
    }

    fun getExpenseCategories(): Flow<List<Category>> {
        val userId = getCurrentUserIdSync()
        return categoryDao.getCategoriesByType(userId, "expense")
            .map { entities -> entities.map { it.toCategory() } }
    }

    fun getIncomeCategories(): Flow<List<Category>> {
        val userId = getCurrentUserIdSync()
        return categoryDao.getCategoriesByType(userId, "income")
            .map { entities -> entities.map { it.toCategory() } }
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)?.toCategory()
    }

    suspend fun addBudget(budget: Budget) {
        val userId = getCurrentUserId()
        val budgetWithUserId = budget.copy(userId = userId)
        val entity = BudgetEntity.fromBudget(budgetWithUserId)
        budgetDao.insert(entity)

        if (isUserLoggedIn()) {
            try {
                firestoreService.saveBudget(budgetWithUserId, userId)
                budgetDao.update(entity.copy(isSynced = true))
            } catch (e: Exception) {
            }
        }
    }

    // 🔹 SỬA: Dùng getCurrentUserIdSync() cho Flow
    fun getBudgets(): Flow<List<Budget>> {
        val userId = getCurrentUserIdSync()
        return budgetDao.getBudgetsByUser(userId)
            .map { entities -> entities.map { it.toBudget() } }
    }

    // 🔹 HÀM MỚI: Lấy budgets từ Firestore (online)
    suspend fun getBudgetsFromFirestore(userId: String): List<Budget> {
        return firestoreService.getBudgets(userId)
    }

    fun getActiveBudgets(): Flow<List<Budget>> {
        val userId = getCurrentUserIdSync()
        return budgetDao.getActiveBudgets(userId)
            .map { entities -> entities.map { it.toBudget() } }
    }

    // ==================== RECURRING EXPENSES ====================
    suspend fun addRecurringExpense(recurringExpense: RecurringExpense) {
        val userId = getCurrentUserId()
        // TODO: Tạo entity cho recurring expense nếu cần

        if (isUserLoggedIn()) {
            try {
                // SỬA: Sử dụng FirestoreService với userId
                firestoreService.saveRecurringExpense(recurringExpense, userId)
            } catch (e: Exception) {
                // Saved offline
            }
        }
    }

    suspend fun getRecurringExpensesFromFirestore(userId: String): List<RecurringExpense> {
        return firestoreService.getRecurringExpenses(userId)
    }

    suspend fun deleteRecurringExpense(expenseId: String) {
        val userId = getCurrentUserId()

        if (isUserLoggedIn()) {
            try {
                firestoreService.deleteRecurringExpense(expenseId, userId)
            } catch (e: Exception) {
                Log.e("FinanceRepository", "Lỗi xóa recurring expense từ Firestore")
            }
        }
    }

    // ==================== USER PROFILE ====================
    suspend fun saveUserProfile(profile: UserProfile) {
        val entity = UserProfileEntity.fromUserProfile(profile)
        userProfileDao.insert(entity)

        if (isUserLoggedIn()) {
            try {
                saveUserProfileToFirestore(profile)
                userProfileDao.update(entity.copy(isSynced = true))
            } catch (e: Exception) {
                // Saved offline
            }
        }
    }

    suspend fun getUserProfile(): UserProfile? {
        val userId = getCurrentUserId()
        return userProfileDao.getProfileByUserId(userId)?.toUserProfile()
    }

    // ==================== FIREBASE METHODS ====================
    private suspend fun getRemoteTransactions(userId: String): List<Transaction> {
        return try {
            val querySnapshot = db.collection("users").document(userId)
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Transaction::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun addTransactionToFirestore(transaction: Transaction, userId: String) {
        db.collection("users").document(userId)
            .collection("transactions")
            .add(transaction.toMap())
            .await()
    }

    private suspend fun getRemoteCategories(userId: String): List<Category> {
        return try {
            val querySnapshot = db.collection("users").document(userId)
                .collection("categories")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Category::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun addCategoryToFirestore(category: Category, userId: String) {
        db.collection("users").document(userId)
            .collection("categories")
            .document(category.id)
            .set(category)
            .await()
    }

    private suspend fun addBudgetToFirestore(budget: Budget, userId: String) {
        db.collection("users").document(userId)
            .collection("budgets")
            .document(budget.id)
            .set(budget)
            .await()
    }

    private suspend fun saveUserProfileToFirestore(profile: UserProfile) {
        db.collection("users").document(profile.userId)
            .set(profile)
            .await()
    }

    // ==================== UTILITY METHODS ====================
    private fun getDefaultCategories(): List<Category> {
        // Danh mục mặc định tương tự CategoryViewModel
        return listOf(
            // Expense main categories
            Category("1", "Chi tiêu - sinh hoạt", "expense", true, icon = "🛒"),
            Category("2", "Chi phí phát sinh", "expense", true, icon = "🎯"),
            Category("3", "Chi phí cố định", "expense", true, icon = "🏠"),
            Category("4", "Đầu tư - tiết kiệm", "expense", true, icon = "📈"),
            Category("999", "Khác", "expense", true, icon = "❓"),

            // Income main categories
            Category("5", "Thu nhập", "income", true, icon = "💰"),
            Category("1000", "Khác", "income", true, icon = "❓"),

            // Expense subcategories
            Category("101", "Chợ, siêu thị", "expense", false, "1", icon = "🛍️"),
            Category("102", "Ăn uống", "expense", false, "1", icon = "🍽️"),
            Category("103", "Di chuyển", "expense", false, "1", icon = "🚗"),

            Category("201", "Mua sắm", "expense", false, "2", icon = "🛒"),
            Category("202", "Giải trí", "expense", false, "2", icon = "🎮"),
            Category("203", "Làm đẹp", "expense", false, "2", icon = "💄"),
            Category("204", "Sức khỏe", "expense", false, "2", icon = "🏥"),
            Category("205", "Từ thiện", "expense", false, "2", icon = "❤️"),

            Category("301", "Hóa đơn", "expense", false, "3", icon = "🧾"),
            Category("302", "Nhà cửa", "expense", false, "3", icon = "🏠"),
            Category("303", "Người thân", "expense", false, "3", icon = "👨‍👩‍👧‍👦"),

            Category("401", "Đầu tư", "expense", false, "4", icon = "📊"),
            Category("402", "Học tập", "expense", false, "4", icon = "🎓"),

            // Income subcategories
            Category("501", "Lương", "income", false, "5", icon = "💵"),
            Category("502", "Thưởng", "income", false, "5", icon = "🎁"),
            Category("503", "Đầu tư", "income", false, "5", icon = "📈"),
            Category("504", "Kinh doanh", "income", false, "5", icon = "💼")
        )
    }

    private fun formatDateForRoom(date: java.util.Date): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return format.format(date)
    }

    // ==================== BACKWARD COMPATIBILITY ====================
    suspend fun getAllTransactionsLegacy(): List<Transaction> {
        return try {
            Log.d("FinanceRepository", "Đang lấy TOÀN BỘ transactions...")
            val querySnapshot = db.collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val transactions = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Transaction::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            }
            Log.d("FinanceRepository", "Lấy được ${transactions.size} transactions")
            transactions
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi lấy transactions: ${e.message}")
            emptyList()
        }
    }

    suspend fun getCompleteFinancialData(): FinancialData {
        return try {
            Log.d("FinanceRepository", "Bắt đầu lấy dữ liệu tài chính...")

            // Lấy từ Room (ưu tiên)
            val transactions = if (isUserLoggedIn()) {
                val userId = getCurrentUserId()
                transactionDao.getTransactionsByUser(userId)
                    .map { it.map { entity -> entity.toTransaction() } }
                    .first() // Extract snapshot list from flow
            } else {
                // Fallback to legacy if not logged in
                getAllTransactionsLegacy()
            }

            FinancialData(
                transactions = transactions,
                accounts = emptyList(),
                budgets = emptyList(),
                userProfile = null
            )
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Lỗi load dữ liệu: ${e.message}")
            FinancialData()
        }
    }

    // ==================== CLEANUP ====================
    suspend fun clearLocalData() {
        val userId = getCurrentUserId()
        transactionDao.deleteAllByUser(userId)
        budgetDao.deleteAllByUser(userId)
        categoryDao.deleteAllByUser(userId)
        userProfileDao.deleteProfile(userId)
    }

    // ==================== SAVINGS GOALS ====================
    suspend fun getSavingsGoals(userId: String): List<SavingsGoal> {
        return try {
            firestoreService.getSavingsGoals(userId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSavingsGoal(goal: SavingsGoal): String {
        return try {
            firestoreService.addSavingsGoal(goal)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) {
        try {
            firestoreService.updateSavingsGoal(goal)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteSavingsGoal(goalId: String) {
        try {
            firestoreService.deleteSavingsGoal(goalId)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun addToSavings(goalId: String, amount: Long) {
        try {
            firestoreService.addToSavings(goalId, amount)
        } catch (e: Exception) {
            throw e
        }
    }

    // ==================== MONTHLY SUMMARY ====================
    suspend fun getMonthlySummary(userId: String, month: Int, year: Int): Pair<Long, Long> {
        return try {
            firestoreService.getMonthlySummary(userId, month, year)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    suspend fun autoUpdateSavingsFromRemainingIncome(userId: String) {
        try {
            firestoreService.autoUpdateSavingsFromRemainingIncome(userId)
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error auto updating savings: ${e.message}")
        }
    }
}