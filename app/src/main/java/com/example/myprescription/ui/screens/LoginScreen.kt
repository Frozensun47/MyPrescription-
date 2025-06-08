package com.example.myprescription.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myprescription.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    // 1. Configure Google Sign-In Options
    // This object specifies the details we need from the user's Google account.
    // We request an ID token for Firebase and the user's email.
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Your web client ID
            .requestEmail()
            .build()
    }

    // 2. Create a GoogleSignInClient
    // This client is responsible for managing the Google Sign-In process.
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // 3. Create an ActivityResultLauncher
    // This launcher will handle the result from the Google Sign-In activity.
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign-In was successful, so we get the account
                val account = task.getResult(ApiException::class.java)!!
                // Now, we use the account's ID token to authenticate with Firebase
                firebaseAuthWithGoogle(account.idToken!!) {
                    onLoginSuccess()
                }
            } catch (e: ApiException) {
                isLoading = false
                Log.w("LoginScreen", "Google sign in failed", e)
                Toast.makeText(context, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            isLoading = false
            // Handle cases where the user cancels the sign-in flow
            Toast.makeText(context, "Google sign-in cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. Define the Sign-In Function
    // This function will be called when the user clicks the sign-in button.
    val launchSignIn: () -> Unit = {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // --- The UI ---
    // This is the user interface for your login screen. It remains largely unchanged.
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Signing in...")
        } else {
            Text(
                "Welcome to MyPrescription",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Sign in to securely store and manage your medical records.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = launchSignIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In with Google", modifier = Modifier.padding(6.dp))
            }
        }
    }
}

/**
 * Authenticates the user with Firebase using the Google ID token.
 *
 * @param idToken The Google ID token obtained from a successful Google Sign-In.
 * @param onLoginSuccess A callback to execute upon successful Firebase authentication.
 */
private fun firebaseAuthWithGoogle(idToken: String, onLoginSuccess: () -> Unit) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    Firebase.auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Firebase sign-in was successful.
                onLoginSuccess()
            } else {
                // If sign-in fails, log the error.
                Log.w("LoginScreen", "signInWithCredential;failure", task.exception)
            }
        }
}