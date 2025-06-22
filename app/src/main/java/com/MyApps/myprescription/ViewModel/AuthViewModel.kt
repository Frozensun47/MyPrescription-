package com.MyApps.myprescription.ViewModel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.MyApps.myprescription.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val gso: GoogleSignInOptions
    private val googleSignInClient: GoogleSignInClient

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _user.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authStateListener)
        // Configure Google Sign-In Options
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Create a GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(application, gso)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    /**
     * Provides the Intent needed to start the Google Sign-In flow.
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Logs the user out from Firebase and the Google Sign-In client.
     * Signing out of the Google client is crucial to force the account chooser dialog.
     */
    fun logout() {
        // Sign out from Firebase
        auth.signOut()
        // The AuthStateListener will automatically update the _user.value to null

        // Sign out from Google Sign-In Client
        googleSignInClient.signOut()
    }
}