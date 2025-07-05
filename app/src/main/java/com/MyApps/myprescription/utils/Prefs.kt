package com.MyApps.myprescription.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class Prefs(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "MyPrescriptionPrefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // --- Add these new functions ---
    fun hasSeenTutorial(userId: String): Boolean {
        return sharedPreferences.getBoolean("TUTORIAL_SEEN_$userId", false)
    }

    fun setTutorialSeen(userId: String) {
        sharedPreferences.edit().putBoolean("TUTORIAL_SEEN_$userId", true).apply()
    }
    // -----------------------------

    // Functions now take userId to store PINs per-account
    fun getPin(userId: String): String? {
        return sharedPreferences.getString("USER_PIN_$userId", null)
    }

    fun setPin(userId: String, pin: String) {
        sharedPreferences.edit().putString("USER_PIN_$userId", pin).apply()
    }

    fun isPinEnabled(userId: String): Boolean {
        // If a user has a PIN, the feature is implicitly enabled for them.
        // We can default to false if no pin is set.
        return sharedPreferences.contains("USER_PIN_$userId")
    }

    fun setPinEnabled(userId: String, isEnabled: Boolean) {
        // Instead of a separate flag, we remove the pin to disable it.
        if (!isEnabled) {
            sharedPreferences.edit().remove("USER_PIN_$userId").apply()
        }
        // Enabling is handled by setting a new PIN.
    }

    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean("IS_FIRST_RUN", true)
    }

    fun setFirstRun(isFirstRun: Boolean) {
        sharedPreferences.edit().putBoolean("IS_FIRST_RUN", isFirstRun).apply()
    }
}