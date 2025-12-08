package com.example.financeapp.viewmodel.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.SavingsGoal
import com.example.financeapp.data.remote.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavingsViewModel : ViewModel() {
    private val firestoreService = FirestoreService()
    private val auth: FirebaseAuth = Firebase.auth

    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoal>> = _savingsGoals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Lấy userId từ Firebase Auth
    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun loadSavingsGoals() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = currentUserId
                if (userId != null) {
                    val goals = firestoreService.getSavingsGoals(userId)
                    _savingsGoals.value = goals
                    println("DEBUG: Đã tải ${goals.size} mục tiêu") // Debug log
                } else {
                    _error.value = "Vui lòng đăng nhập để xem mục tiêu tiết kiệm"
                }
            } catch (e: Exception) {
                _error.value = "Không thể tải mục tiêu tiết kiệm: ${e.message}"
                println("DEBUG: Lỗi tải mục tiêu: ${e.message}") // Debug log
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Đảm bảo goal có userId từ current user
                val userId = currentUserId
                if (userId != null) {
                    val goalWithUserId = goal.copy(userId = userId)
                    println("DEBUG: Thêm mục tiêu: $goalWithUserId") // Debug log
                    firestoreService.addSavingsGoal(goalWithUserId)
                    // Load lại dữ liệu ngay lập tức
                    loadSavingsGoals()
                } else {
                    _error.value = "Vui lòng đăng nhập để thêm mục tiêu"
                }
            } catch (e: Exception) {
                _error.value = "Không thể thêm mục tiêu: ${e.message}"
                println("DEBUG: Lỗi thêm mục tiêu: ${e.message}") // Debug log
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun calculateProgress(goal: SavingsGoal): Float {
        return if (goal.targetAmount > 0) {
            (goal.currentAmount.toFloat() / goal.targetAmount.toFloat() * 100).coerceAtMost(100f)
        } else 0f
    }

    fun calculateRemainingDays(deadline: Long): Long {
        val currentTime = System.currentTimeMillis()
        val diff = deadline - currentTime
        return (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    }

    fun calculateMonthlyContribution(goal: SavingsGoal): Long {
        if (goal.deadline <= 0 || goal.targetAmount <= goal.currentAmount) return 0

        val remainingAmount = goal.targetAmount - goal.currentAmount
        val remainingDays = calculateRemainingDays(goal.deadline)
        val remainingMonths = (remainingDays / 30).coerceAtLeast(1)

        return (remainingAmount / remainingMonths).coerceAtLeast(1)
    }
}