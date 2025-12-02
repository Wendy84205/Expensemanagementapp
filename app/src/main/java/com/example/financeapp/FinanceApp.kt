package com.example.financeapp

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel

/**
 * Application toàn cục để chia sẻ ViewModel cho AI.
 */
class FinanceApp : Application() {
    val categoryViewModel: com.example.financeapp.viewmodel.CategoryViewModel by lazy { _root_ide_package_.com.example.financeapp.viewmodel.CategoryViewModel() }
    lateinit var transactionViewModel: TransactionViewModel
    lateinit var budgetViewModel: BudgetViewModel
    lateinit var recurringExpenseViewModel: RecurringExpenseViewModel

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo ViewModel toàn cục (nếu cần chia sẻ cho AI)
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        transactionViewModel = factory.create(TransactionViewModel::class.java)
        budgetViewModel = factory.create(BudgetViewModel::class.java)
        recurringExpenseViewModel = factory.create(RecurringExpenseViewModel::class.java)
    }
}
