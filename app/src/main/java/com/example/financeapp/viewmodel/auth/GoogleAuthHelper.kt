package com.example.financeapp

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

object GoogleAuthHelper {
    private var launcher: ActivityResultLauncher<Intent>? = null

    fun startSignIn(activity: Activity, callback: (idToken: String?, error: String?) -> Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("<397779842229-dqp2k3qtiaupus7jule3p472gltmmfe3.apps.googleusercontent.com>")
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(activity, gso)
        val signInIntent = client.signInIntent
        // We cannot create launcher here without Composition; simplest is to startActivityForResult (deprecated) for clarity
        activity.startActivityForResult(signInIntent, 1001)
    }

    fun handleSignInResult(data: Intent?, activity: Activity, callback: (idToken: String?, error: String?) -> Unit) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            callback(idToken, null)
        } catch (e: ApiException) {
            callback(null, e.localizedMessage)
        }
    }
}
