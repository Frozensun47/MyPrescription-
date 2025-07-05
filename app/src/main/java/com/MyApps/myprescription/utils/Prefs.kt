// frozensun47/myprescription-/MyPrescription--e4ea256193f6bab959107a3c7e7eea1813571356/app/src/main/java/com/MyApps/myprescription/util/Prefs.kt
package com.MyApps.myprescription.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.IOException
import java.security.GeneralSecurityException

class Prefs(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            "MyPrescriptionPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: GeneralSecurityException) {
        // This can happen if the master key is recreated, for example, after a reinstall.
        // In this case, we clear the old preferences and start fresh.
        context.getSharedPreferences("MyPrescriptionPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        EncryptedSharedPreferences.create(
            "MyPrescriptionPrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: IOException) {
        throw IOException("Could not create EncryptedSharedPreferences", e)
    }


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
    fun isAutoBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean("AUTO_BACKUP_ENABLED", false)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("AUTO_BACKUP_ENABLED", enabled).apply()
    }

    fun setFirstRun(isFirstRun: Boolean) {
        sharedPreferences.edit().putBoolean("IS_FIRST_RUN", isFirstRun).apply()
    }
}