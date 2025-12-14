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
import com.google.firebase.crashlytics.ktx.crashlytics
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
        userPrefs.userFlow.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), null)

    val currentUser: StateFlow<UserSession?> = userSession
    val isAuthenticated: StateFlow<Boolean> =
        userSession.map { it != null }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), false)

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _firebaseUser.value = auth.currentUser
        }
    }

    // 🔹 Tạo hoặc lấy user từ Firestore dựa trên email
    private suspend fun createOrGetFirestoreUser(
        firebaseUser: FirebaseUser,
        provider: String
    ): String {
        return try {
            val userEmail = firebaseUser.email ?: ""
            val usersRef = firestore.collection("users")

            // Kiểm tra user đã tồn tại theo email
            val querySnapshot = usersRef
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .await()

            val firestoreUserId = if (querySnapshot.isEmpty) {
                // User chưa tồn tại → tạo mới
                createNewFirestoreUser(firebaseUser, provider)
            } else {
                // User đã tồn tại → cập nhật thông tin
                updateExistingFirestoreUser(querySnapshot.documents.first(), firebaseUser, provider)
            }

            firestoreUserId
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            // Fallback: sử dụng Firebase UID
            firebaseUser.uid
        }
    }

    // 🔹 Tạo user mới trong Firestore
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

    // 🔹 Cập nhật user đã tồn tại trong Firestore
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

    // 🔹 Lưu user sau khi login thành công (đã sửa)
    private fun saveFirebaseUser(user: FirebaseUser?, provider: String) {
        if (user == null) return

        viewModelScope.launch {
            try {
                // 1. Tạo/kiểm tra user trong Firestore
                val firestoreUserId = createOrGetFirestoreUser(user, provider)

                // 2. Lưu user vào DataStore
                userPrefs.saveUser(
                    id = firestoreUserId, // Sử dụng Firestore userId
                    email = user.email,
                    name = user.displayName,
                    avatar = user.photoUrl?.toString()
                )

                // 3. Đồng bộ dữ liệu cũ nếu cần
                migrateOldDataIfNeeded(firestoreUserId)

                Firebase.crashlytics.log("User saved: $firestoreUserId, provider: $provider")
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                // Fallback: lưu với Firebase UID
                userPrefs.saveUser(
                    id = user.uid,
                    email = user.email,
                    name = user.displayName,
                    avatar = user.photoUrl?.toString()
                )
            }
        }
    }

    // 🔹 Migrate dữ liệu cũ không có userId
    private suspend fun migrateOldDataIfNeeded(userId: String) {
        try {
            val transactionsRef = firestore.collection("transactions")

            // 1. Tìm các transaction không có userId
            val oldTransactions = transactionsRef
                .whereEqualTo("userId", "")
                .get()
                .await()

            // 2. Cập nhật userId cho các transaction này
            for (doc in oldTransactions.documents) {
                doc.reference.update("userId", userId).await()
            }

            // 3. Tương tự cho các collection khác
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

            if (oldTransactions.documents.isNotEmpty() ||
                oldBudgets.documents.isNotEmpty() ||
                oldCategories.documents.isNotEmpty()) {
                Firebase.crashlytics.log("Migrated old data for user: $userId")
            }
        } catch (e: Exception) {
            // Không crash nếu migration thất bại
            Firebase.crashlytics.recordException(e)
        }
    }

    // 🔹 Đăng ký bằng Email (đã sửa)
    fun createUserWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Register success: $email")
                    saveFirebaseUser(firebaseAuth.currentUser, "email")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng ký thất bại"
                    Firebase.crashlytics.log("Register failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Đăng nhập bằng Email (đã sửa)
    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Login success: $email")
                    saveFirebaseUser(firebaseAuth.currentUser, "email")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập thất bại"
                    Firebase.crashlytics.log("Login failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Đăng nhập bằng Google (đã sửa)
    fun firebaseAuthWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Google login success")
                    saveFirebaseUser(firebaseAuth.currentUser, "google")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập Google thất bại"
                    Firebase.crashlytics.log("Google login failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Đăng nhập bằng Facebook (đã sửa)
    fun firebaseAuthWithFacebook(token: AccessToken, onResult: (Boolean, String?) -> Unit) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Facebook login success")
                    saveFirebaseUser(firebaseAuth.currentUser, "facebook")
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập Facebook thất bại"
                    Firebase.crashlytics.log("Facebook login failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Gửi email đặt lại mật khẩu
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

    // 🔹 Đăng xuất
    fun signOut() {
        firebaseAuth.signOut()
        Firebase.crashlytics.log("User signed out")
        viewModelScope.launch {
            userPrefs.clearUser()
        }
    }

    // 🔹 Xóa tài khoản (SỬA: Loại bỏ phần lấy userId từ DataStore)
    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            onResult(false, "Không tìm thấy user")
            return
        }

        viewModelScope.launch {
            try {
                // SỬA: Sử dụng Firebase UID thay vì lấy từ DataStore
                val userId = user.uid

                // 1. Xóa dữ liệu trong Firestore
                deleteUserDataFromFirestore(userId)

                // 2. Xóa user khỏi Firebase Auth
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // 3. Xóa khỏi DataStore
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

    // 🔹 Xóa dữ liệu user từ Firestore
    private suspend fun deleteUserDataFromFirestore(userId: String) {
        try {
            // Xóa user document
            firestore.collection("users").document(userId).delete().await()

            // Xóa transactions của user
            val transactions = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            transactions.documents.forEach { it.reference.delete().await() }

            // Xóa budgets của user
            val budgets = firestore.collection("budgets")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            budgets.documents.forEach { it.reference.delete().await() }

            // Xóa categories của user
            val categories = firestore.collection("categories")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            categories.documents.forEach { it.reference.delete().await() }

            // Xóa recurring expenses của user
            val recurring = firestore.collection("recurring_expenses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            recurring.documents.forEach { it.reference.delete().await() }

            // Xóa savings goals của user
            val savings = firestore.collection("savings_goals")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            savings.documents.forEach { it.reference.delete().await() }

        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
        }
    }

    // 🔹 HÀM MỚI: Lấy userId từ userSession (nếu cần)
    suspend fun getCurrentUserId(): String {
        return userSession.value?.id ?: firebaseAuth.currentUser?.uid ?: "anonymous"
    }
}