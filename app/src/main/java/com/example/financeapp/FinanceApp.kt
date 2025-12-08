package com.example.financeapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.utils.work.AIButlerWorker
import com.example.financeapp.viewmodel.ai.AIButlerService
import com.example.financeapp.viewmodel.ai.AIViewModel
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
/**
 * Application class chính của ứng dụng Finance App
 */
class FinanceApp : Application() {

    companion object {
        private const val TAG = "WendyAI"

        @JvmStatic
        fun getInstance(context: Context): FinanceApp {
            return context.applicationContext as FinanceApp
        }
    }

    // ==================== VIEWMODELS ====================

    /** Category ViewModel - quản lý danh mục thu/chi */
    private var _categoryViewModel: CategoryViewModel? = null
    val categoryViewModel: CategoryViewModel
        get() {
            if (_categoryViewModel == null) {
                _categoryViewModel = CategoryViewModel()
            }
            return _categoryViewModel!!
        }

    /** Transaction ViewModel - quản lý giao dịch */
    lateinit var transactionViewModel: TransactionViewModel
        private set

    /** Budget ViewModel - quản lý ngân sách */
    lateinit var budgetViewModel: BudgetViewModel
        private set

    /** Recurring Expense ViewModel - quản lý chi phí định kỳ */
    lateinit var recurringExpenseViewModel: RecurringExpenseViewModel
        private set

    /** AI ViewModel - quản lý AI assistant */
    lateinit var aiViewModel: AIViewModel
        private set

    // ==================== SERVICES ====================

    /** AI Butler Service - chạy background checks */
    private var _aiButlerService: AIButlerService? = null
    private val aiButlerService: AIButlerService
        get() = _aiButlerService ?: run {
            AIButlerService(this).also {
                _aiButlerService = it
                it.start()
            }
        }

    // ==================== APPLICATION LIFECYCLE ====================

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "WendyAI đang khởi tạo...")

        try {
            // THÊM: Khởi tạo WorkManager trước
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
            WorkManager.initialize(this, config)

            // 1. Khởi tạo ViewModels
            initializeViewModels()

            // 2. Khởi tạo notification system
            initializeNotificationSystem()

            // 3. Khởi động services
            initializeServices()

            // 4. Lên lịch background workers
            scheduleBackgroundWorkers()

            // 5. Log khởi tạo thành công
            logInitializationSuccess()

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo ứng dụng", e)
        }
    }

    /**
     * Khởi tạo tất cả ViewModels
     */
    private fun initializeViewModels() {
        try {
            Log.d(TAG, "Khởi tạo ViewModels...")

            // Tạo ViewModel factory
            val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(this)

            // Khởi tạo TransactionViewModel
            transactionViewModel = factory.create(TransactionViewModel::class.java).apply {
                Log.d(TAG, "TransactionViewModel initialized")
            }

            // Khởi tạo BudgetViewModel
            budgetViewModel = factory.create(BudgetViewModel::class.java).apply {
                Log.d(TAG, "BudgetViewModel initialized")
            }

            // Khởi tạo RecurringExpenseViewModel
            recurringExpenseViewModel = factory.create(RecurringExpenseViewModel::class.java).apply {
                Log.d(TAG, "RecurringExpenseViewModel initialized")
            }

            // Khởi tạo CategoryViewModel (nếu chưa có)
            if (_categoryViewModel == null) {
                _categoryViewModel = CategoryViewModel()
                Log.d(TAG, "CategoryViewModel initialized")
            }
            CoroutineScope(Dispatchers.Main).launch {
                // Đợi một chút để các ViewModel khác load dữ liệu
                delay(1500) // Đợi 1.5 giây

                // Khởi tạo AIViewModel
                aiViewModel = AIViewModel(this@FinanceApp).apply {
                    Log.d(TAG, "AIViewModel initialized")
                }
            }

            Log.i(TAG, "Tất cả ViewModels đã được khởi tạo")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo ViewModels", e)
        }
    }

    /**
     * Khởi tạo notification system
     */
    private fun initializeNotificationSystem() {
        try {
            Log.d(TAG, "Khởi tạo notification system...")

            // Tạo notification channel (bắt buộc từ Android 8.0+)
            NotificationHelper.createChannel(this)

            Log.d(TAG, "Notification system initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo notification system", e)
        }
    }

    /**
     * Khởi tạo và khởi động các services
     */
    private fun initializeServices() {
        try {
            Log.d(TAG, "Khởi tạo services...")

            // Khởi động AI Butler Service
            val started = aiButlerService.start()
            if (started) {
                Log.i(TAG, "AI Butler Service đã khởi động thành công")
            } else {
                Log.w(TAG, "AI Butler Service không thể khởi động")
            }

            Log.d(TAG, "Services initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo services", e)
        }
    }

    /**
     * Lên lịch các background workers
     */
    private fun scheduleBackgroundWorkers() {
        try {
            Log.d(TAG, "Lên lịch background workers...")

            // Lên lịch AI Butler Worker
            val workerScheduled = AIButlerWorker.schedule(this)
            if (workerScheduled) {
                Log.i(TAG, "AI Butler Worker đã được lên lịch thành công")
            } else {
                Log.w(TAG, "Không thể lên lịch AI Butler Worker")
            }

            Log.d(TAG, "Background workers scheduled")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lên lịch background workers", e)
        }
    }

    /**
     * Log thông tin khởi tạo thành công
     */
    private fun logInitializationSuccess() {
        Log.i(TAG, """
            ========================================
            FINANCE APP KHỞI TẠO THÀNH CÔNG!
            
            Các thành phần đã khởi tạo:
            • CategoryViewModel: ${if (_categoryViewModel != null) "✓" else "✗"}
            • TransactionViewModel: ${if (::transactionViewModel.isInitialized) "✓" else "✗"}
            • BudgetViewModel: ${if (::budgetViewModel.isInitialized) "✓" else "✗"}
            • RecurringExpenseViewModel: ${if (::recurringExpenseViewModel.isInitialized) "✓" else "✗"}
            • AIViewModel: ${if (::aiViewModel.isInitialized) "✓" else "✗"}
            • Notification System: ✓
            • AI Butler Service: ${if (_aiButlerService != null) "✓" else "✗"}
            • Background Workers: ${if (AIButlerWorker.isScheduled(this)) "✓" else "✗"}
            ========================================
        """.trimIndent())
    }

    // ==================== PUBLIC API ====================

    /**
     * Lấy instance của AI Butler Service
     */
    fun getAIButlerService(): AIButlerService {
        return aiButlerService
    }

    /**
     * Kiểm tra AI Butler Service có đang chạy không
     */
    fun isAIButlerServiceRunning(): Boolean {
        return _aiButlerService?.isServiceRunning() ?: false
    }

    /**
     * Khởi động lại AI Butler Service
     */
    fun restartAIButlerService(): Boolean {
        return try {
            Log.i(TAG, "Khởi động lại AI Butler Service...")

            // Dừng service nếu đang chạy
            _aiButlerService?.stop()

            // Khởi tạo và khởi động lại
            _aiButlerService = AIButlerService(this)
            _aiButlerService?.start() ?: false

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khởi động lại AI Butler Service", e)
            false
        }
    }

    /**
     * Kiểm tra trạng thái background workers
     */
    fun getWorkerStatus(): String {
        return try {
            val isWorkerScheduled = AIButlerWorker.isScheduled(this)
            val serviceRunning = isAIButlerServiceRunning()

            """
            Worker Status:
            • AI Butler Worker: ${if (isWorkerScheduled) "Đã lên lịch" else "Chưa lên lịch"}
            • AI Butler Service: ${if (serviceRunning) "Đang chạy" else "Đã dừng"}
            """.trimIndent()

        } catch (e: Exception) {
            "Không thể lấy trạng thái workers: ${e.message}"
        }
    }

    /**
     * Debug information về trạng thái ứng dụng
     */
    fun getDebugInfo(): String {
        return try {
            """
            ===== FINANCE APP DEBUG INFO =====
            
            VIEWMODELS:
            • CategoryViewModel: ${_categoryViewModel != null}
            • TransactionViewModel: ${::transactionViewModel.isInitialized}
            • BudgetViewModel: ${::budgetViewModel.isInitialized}
            • RecurringExpenseViewModel: ${::recurringExpenseViewModel.isInitialized}
            • AIViewModel: ${::aiViewModel.isInitialized}
            
            SERVICES:
            • AI Butler Service: ${_aiButlerService != null}
            
            WORKERS:
            ${getWorkerStatus()}
            
            ===== END DEBUG INFO =====
            """.trimIndent()

        } catch (e: Exception) {
            "Error getting debug info: ${e.message}"
        }
    }

    /**
     * Buộc kiểm tra điều kiện thông báo ngay lập tức
     */
    fun forceCheckNotifications() {
        try {
            Log.i(TAG, "Buộc kiểm tra thông báo...")

            _aiButlerService?.forceCheckNow()
            Log.i(TAG, "Đã kích hoạt force check")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi force check notifications", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "onTrimMemory called with level: $level")
    }

    override fun onTerminate() {
        try {
            Log.i(TAG, "Ứng dụng đang terminate, dọn dẹp resources...")

            // Dừng AI Butler Service
            _aiButlerService?.stop()
            Log.d(TAG, "AI Butler Service đã dừng")

            // Hủy background workers
            AIButlerWorker.cancel(this)
            Log.d(TAG, "Background workers đã hủy")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi terminate ứng dụng", e)
        } finally {
            super.onTerminate()
        }
    }
}