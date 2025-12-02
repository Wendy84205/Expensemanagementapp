package com.example.financeapp

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

object PhoneAuthHelper {
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun startPhoneAuth(phoneNumber: String, activity: Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyWithCode(code: String, onComplete: (PhoneAuthCredential?) -> Unit) {
        storedVerificationId?.let {
            val credential = PhoneAuthProvider.getCredential(it, code)
            onComplete(credential)
        } ?: onComplete(null)
    }

    fun setStoredVerificationId(id: String) { storedVerificationId = id }
    fun setResendToken(token: PhoneAuthProvider.ForceResendingToken) { resendToken = token }
}
