package com.example.myprescription.ViewModel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {
    private val _user = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _user.value = null
    }
}