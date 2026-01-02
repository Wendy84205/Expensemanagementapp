package com.example.financeapp.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.local.datastore.UserPreferencesDataStore
import com.example.financeapp.screen.main.dashboard.UserSession
import com.facebook.AccessToken
import com.google.firebase.Timestamp
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val userPrefs = UserPreferencesDataStore(application)

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser

    val userSession: StateFlow<UserSession?> =
        userPrefs.userFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUser: StateFlow<UserSession?> = userSession
    val isAuthenticated: StateFlow<Boolean> =
        userSession.map { it != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _firebaseUser.value = auth.currentUser
        }
    }

    // Tạo hoặc lấy user từ Firestore dựa trên email
    private suspend fun createOrGetFirestoreUser(
        firebaseUser: FirebaseUser,
        provider: String
    ): String {
        return try {
            val userEmail = firebaseUser.email ?: ""
            val usersRef = firestore.collection("users")

            val querySnapshot = usersRef
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .await()

            val firestoreUserId = if (querySnapshot.isEmpty) {
                createNewFirestoreUser(firebaseUser, provider)
            } else {
                updateExistingFirestoreUser(querySnapshot.documents.first(), firebaseUser, provider)
            }

            firestoreUserId
        } catch (e: Exception) {
            firebaseUser.uid
        }
    }

    // Tạo user mới trong Firestore
    private suspend fun createNewFirestoreUser(
        firebaseUser: FirebaseUser,
        provider: String
    ): String {
        val usersRef = firestore.collection("users")
        val userId = firebaseUser.uid

        val newUser = hashMapOf(
            "id" to userId,
            "name" to (firebaseUser.displayName ?: "Người dùng"),
            "email" to (firebaseUser.email ?: ""),
            "profileImage" to (firebaseUser.photoUrl?.toString() ?: ""),
            "phoneNumber" to (firebaseUser.phoneNumber ?: ""),
            "isEmailVerified" to firebaseUser.isEmailVerified,
            "providerId" to provider,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now(),
            "lastLoginAt" to Timestamp.now()
        )

        usersRef.document(userId)
            .set(newUser)
            .await()

        return userId
    }

    // Cập nhật user đã tồn tại trong Firestore
    private suspend fun updateExistingFirestoreUser(
        existingDoc: com.google.firebase.firestore.DocumentSnapshot,
        firebaseUser: FirebaseUser,
        provider: String
    ): String {
        val userId = existingDoc.id
        val usersRef = firestore.collection("users")

        val updates = hashMapOf<String, Any>(
            "name" to (firebaseUser.displayName ?: existingDoc.getString("name") ?: ""),
            "profileImage" to (firebaseUser.photoUrl?.toString() ?: existingDoc.getString("profileImage") ?: ""),
            "phoneNumber" to (firebaseUser.phoneNumber ?: existingDoc.getString("phoneNumber") ?: ""),
            "isEmailVerified" to firebaseUser.isEmailVerified,
            "providerId" to provider,
            "updatedAt" to Timestamp.now(),
            "lastLoginAt" to Timestamp.now()
        )

        usersRef.document(userId)
            .update(updates)
            .await()

        return userId
    }

    // Lưu user sau khi login thành công
    private fun saveFirebaseUser(user: FirebaseUser?, provider: String) {
        if (user == null) return

        viewModelScope.launch {
            try {
                val firestoreUserId = createOrGetFirestoreUser(user, provider)

                userPrefs.saveUser(
                    id = firestoreUserId,
                    email = user.email,
                    name = user.displayName,
                    avatar = user.photoUrl?.toString()
                )

                migrateOldDataIfNeeded(firestoreUserId)
            } catch (e: Exception) {
                userPrefs.saveUser(
                    id = user.uid,
                    email = user.email,
                    name = user.displayName,
                    avatar = user.photoUrl?.toString()
                )
            }
        }
    }

    // Migrate dữ liệu cũ không có userId
    private suspend fun migrateOldDataIfNeeded(userId: String) {
        try {
            val transactionsRef = firestore.collection("transactions")

            val oldTransactions = transactionsRef
                .whereEqualTo("userId", "")
                .get()
                .await()

            for (doc in oldTransactions.documents) {
                doc.reference.update("userId", userId).await()
            }

            val budgetsRef = firestore.collection("budgets")
            val oldBudgets = budgetsRef
                .whereEqualTo("userId", "")
                .get()
                .await()

            for (doc in oldBudgets.documents) {
                doc.reference.update("userId", userId).await()
            }

            val categoriesRef = firestore.collection("categories")
            val oldCategories = categoriesRef
                .whereEqualTo("userId", "")
                .get()
                .await()

            for (doc in oldCategories.documents) {
                doc.reference.update("userId", userId).await()
            }

        } catch (e: Exception) {
        }
    }

    // Đăng ký bằng Email
    fun createUserWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveFirebaseUser(firebaseAuth.currentUser, "email")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng ký thất bại"
                    onResult(false, msg)
                }
            }
    }

    // Đăng nhập bằng Email
    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveFirebaseUser(firebaseAuth.currentUser, "email")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập thất bại"
                    onResult(false, msg)
                }
            }
    }

    // Đăng nhập bằng Google
    fun firebaseAuthWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveFirebaseUser(firebaseAuth.currentUser, "google")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập Google thất bại"
                    onResult(false, msg)
                }
            }
    }

    // Đăng nhập bằng Facebook
    fun firebaseAuthWithFacebook(token: AccessToken, onResult: (Boolean, String?) -> Unit) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveFirebaseUser(firebaseAuth.currentUser, "facebook")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập Facebook thất bại"
                    onResult(false, msg)
                }
            }
    }

    // Gửi email đặt lại mật khẩu
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Đã gửi liên kết khôi phục mật khẩu đến $email")
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Gửi email thất bại")
                }
            }
    }

    // Đăng xuất
    fun signOut() {
        firebaseAuth.signOut()
        viewModelScope.launch {
            userPrefs.clearUser()
        }
    }

    // Xóa tài khoản
    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            onResult(false, "Không tìm thấy user")
            return
        }

        viewModelScope.launch {
            try {
                val userId = user.uid

                deleteUserDataFromFirestore(userId)

                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            viewModelScope.launch {
                                userPrefs.clearUser()
                            }
                            onResult(true, null)
                        } else {
                            onResult(false, task.exception?.localizedMessage ?: "Xóa tài khoản thất bại")
                        }
                    }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Lỗi khi xóa tài khoản")
            }
        }
    }

    // Xóa dữ liệu user từ Firestore
    private suspend fun deleteUserDataFromFirestore(userId: String) {
        try {
            firestore.collection("users").document(userId).delete().await()

            val transactions = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            transactions.documents.forEach { it.reference.delete().await() }

            val budgets = firestore.collection("budgets")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            budgets.documents.forEach { it.reference.delete().await() }

            val categories = firestore.collection("categories")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            categories.documents.forEach { it.reference.delete().await() }

            val recurring = firestore.collection("recurring_expenses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            recurring.documents.forEach { it.reference.delete().await() }

            val savings = firestore.collection("savings_goals")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            savings.documents.forEach { it.reference.delete().await() }

        } catch (e: Exception) {
        }
    }

    // Lấy userId từ userSession
    suspend fun getCurrentUserId(): String {
        return userSession.value?.id ?: firebaseAuth.currentUser?.uid ?: "anonymous"
    }
}