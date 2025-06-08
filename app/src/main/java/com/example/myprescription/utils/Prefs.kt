package com.example.myprescription.util

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

    var pin: String?
        get() = sharedPreferences.getString("USER_PIN", null)
        set(value) {
            sharedPreferences.edit().putString("USER_PIN", value).apply()
        }

    // New property to control PIN screen visibility
    var isPinEnabled: Boolean
        get() = sharedPreferences.getBoolean("IS_PIN_ENABLED", true) // Default to true
        set(value) {
            sharedPreferences.edit().putBoolean("IS_PIN_ENABLED", value).apply()
        }

    var isUserLoggedIn: Boolean
        get() = sharedPreferences.getBoolean("IS_LOGGED_IN", false)
        set(value) {
            sharedPreferences.edit().putBoolean("IS_LOGGED_IN", value).apply()
        }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}