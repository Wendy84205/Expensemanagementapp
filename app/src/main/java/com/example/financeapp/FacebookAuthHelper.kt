package com.example.financeapp

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.facebook.*

object FacebookAuthHelper {
    private var callbackManager: CallbackManager = CallbackManager.Factory.create()

    fun loginWithFacebook(activity: Activity, callback: (com.facebook.AccessToken?, String?) -> Unit) {
        val loginManager = com.facebook.login.LoginManager.getInstance()
        loginManager.logInWithReadPermissions(activity, listOf("email", "public_profile"))
        loginManager.registerCallback(callbackManager, object : FacebookCallback<com.facebook.login.LoginResult> {
            override fun onSuccess(result: com.facebook.login.LoginResult) {
                callback(result.accessToken, null)
            }
            override fun onCancel() { callback(null, "cancel") }
            override fun onError(error: FacebookException) { callback(null, error.localizedMessage) }
        })
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
