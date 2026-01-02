package com.example.financeapp

import android.app.Application
import android.content.Context
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

        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
            WorkManager.initialize(this, config)

            initializeViewModels()
            initializeNotificationSystem()
            initializeServices()
            scheduleBackgroundWorkers()

        } catch (e: Exception) {
        }
    }

    /**
     * Khởi tạo tất cả ViewModels
     */
    private fun initializeViewModels() {
        try {
            val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(this)

            transactionViewModel = factory.create(TransactionViewModel::class.java)

            budgetViewModel = factory.create(BudgetViewModel::class.java)

            recurringExpenseViewModel = factory.create(RecurringExpenseViewModel::class.java)

            if (_categoryViewModel == null) {
                _categoryViewModel = CategoryViewModel()
            }

            CoroutineScope(Dispatchers.Main).launch {
                delay(1500)
                aiViewModel = AIViewModel(this@FinanceApp)
            }

        } catch (e: Exception) {
        }
    }

    /**
     * Khởi tạo notification system
     */
    private fun initializeNotificationSystem() {
        try {
            NotificationHelper.createChannel(this)
        } catch (e: Exception) {
        }
    }

    /**
     * Khởi tạo và khởi động các services
     */
    private fun initializeServices() {
        try {
            aiButlerService.start()
        } catch (e: Exception) {
        }
    }

    /**
     * Lên lịch các background workers
     */
    private fun scheduleBackgroundWorkers() {
        try {
            AIButlerWorker.schedule(this)
        } catch (e: Exception) {
        }
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
            _aiButlerService?.stop()
            _aiButlerService = AIButlerService(this)
            _aiButlerService?.start() ?: false
        } catch (e: Exception) {
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
            "Không thể lấy trạng thái workers"
        }
    }

    /**
     * Debug information về trạng thái ứng dụng
     */
    fun getDebugInfo(): String {
        return try {
            """
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
            """.trimIndent()

        } catch (e: Exception) {
            "Error getting debug info"
        }
    }

    /**
     * Buộc kiểm tra điều kiện thông báo ngay lập tức
     */
    fun forceCheckNotifications() {
        try {
            _aiButlerService?.forceCheckNow()
        } catch (e: Exception) {
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    override fun onTerminate() {
        try {
            _aiButlerService?.stop()
            AIButlerWorker.cancel(this)
        } catch (e: Exception) {
        } finally {
            super.onTerminate()
        }
    }
}