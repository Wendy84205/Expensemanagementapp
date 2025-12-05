package com.example.financeapp.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.local.datastore.UserPreferencesDataStore
import com.example.financeapp.screen.main.dashboard.UserSession
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()
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

    // 🔹 Lưu user sau khi login thành công
    private fun saveFirebaseUser(user: FirebaseUser?) {
        if (user == null) return
        viewModelScope.launch {
            userPrefs.saveUser(
                id = user.uid,
                email = user.email,
                name = user.displayName,
                avatar = user.photoUrl?.toString()
            )
        }
    }

    // 🔹 Đăng ký bằng Email
    fun createUserWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Register success: $email")
                    saveFirebaseUser(firebaseAuth.currentUser)
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng ký thất bại"
                    Firebase.crashlytics.log("Register failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Đăng nhập bằng Email
    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Login success: $email")
                    saveFirebaseUser(firebaseAuth.currentUser)
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập thất bại"
                    Firebase.crashlytics.log("Login failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Đăng nhập bằng Google
    fun firebaseAuthWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Google login success")
                    saveFirebaseUser(firebaseAuth.currentUser)
                    onResult(true, null)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Đăng nhập Google thất bại"
                    Firebase.crashlytics.log("Google login failed: $msg")
                    onResult(false, msg)
                }
            }
    }

    // 🔹 Đăng nhập bằng Facebook
    fun firebaseAuthWithFacebook(token: AccessToken, onResult: (Boolean, String?) -> Unit) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.crashlytics.log("Facebook login success")
                    saveFirebaseUser(firebaseAuth.currentUser)
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
        viewModelScope.launch { userPrefs.clearUser() }
    }
}