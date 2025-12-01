package com.example.financeapp

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val application: Application
) {
    private val db: FirebaseFirestore = Firebase.firestore

    // ==================== TRANSACTIONS ====================
    suspend fun getAllTransactions(): List<Transaction> {
        return try {
            Log.d("FinanceRepository", "📊 Đang lấy TOÀN BỘ transactions...")
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
            Log.d("FinanceRepository", "✅ Lấy được ${transactions.size} transactions")
            transactions
        } catch (e: Exception) {
            Log.e("FinanceRepository", "❌ Lỗi lấy transactions: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRecentTransactions(days: Int = 30): List<Transaction> {
        return try {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
            val startDate = calendar.timeInMillis

            val querySnapshot = db.collection("transactions")
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                document.toObject(Transaction::class.java)?.copy(id = document.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTransactionsByCategory(category: String): List<Transaction> {
        return try {
            val querySnapshot = db.collection("transactions")
                .whereEqualTo("category", category)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                document.toObject(Transaction::class.java)?.copy(id = document.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== ACCOUNTS ====================
    suspend fun getAllAccounts(): List<User> {
        return try {
            Log.d("FinanceRepository", "🏦 Đang lấy TOÀN BỘ accounts...")
            val querySnapshot = db.collection("accounts")
                .get()
                .await()

            val accounts = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(User::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            }
            Log.d("FinanceRepository", "✅ Lấy được ${accounts.size} accounts")
            accounts
        } catch (e: Exception) {
            Log.e("FinanceRepository", "❌ Lỗi lấy accounts: ${e.message}")
            emptyList()
        }
    }

    // ==================== TỔNG HỢP DỮ LIỆU ĐƠN GIẢN ====================
    suspend fun getCompleteFinancialData(): FinancialData {
        return try {
            Log.d("FinanceRepository", "🚀 Bắt đầu lấy dữ liệu tài chính...")

            // Chỉ load transactions và accounts
            val transactions = getAllTransactions()
            val accounts = getAllAccounts()

            Log.d("FinanceRepository", "🎉 ĐÃ LOAD DỮ LIỆU: " +
                    "\n• ${transactions.size} giao dịch" +
                    "\n• ${accounts.size} tài khoản")

            FinancialData(
                transactions = transactions,
                accounts = accounts,
                budgets = emptyList(), // Bỏ qua
                savingsGoals = emptyList(), // Bỏ qua
                userProfile = null // Bỏ qua
            )
        } catch (e: Exception) {
            Log.e("FinanceRepository", "💥 Lỗi load dữ liệu: ${e.message}")
            FinancialData()
        }
    }
}