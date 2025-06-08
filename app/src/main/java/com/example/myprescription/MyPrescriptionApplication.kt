package com.example.myprescription

import android.app.Application
import androidx.room.Room
import com.example.myprescription.data.database.AppDatabase
import com.example.myprescription.data.repository.AppRepository

class MyPrescriptionApplication : Application() {
    // These are now nullable and will be initialized per-user
    var database: AppDatabase? = null
        private set
    var repository: AppRepository? = null
        private set

    /**
     * Creates and initializes the database and repository for a specific user.
     * This should be called upon successful login or app startup if a user is already logged in.
     * @param userId The unique ID of the current Firebase user.
     */
    fun initializeDependenciesForUser(userId: String) {
        // Close any existing database connection before creating a new one
        database?.close()

        // Create a new database instance with a user-specific name
        val userDb = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "my_prescription_database_$userId" // This ensures each user has a separate DB file
        ).build()
        database = userDb

        // Create a new repository instance tied to the user-specific database
        repository = AppRepository(
            db = userDb,
            memberDao = userDb.memberDao(),
            prescriptionDao = userDb.prescriptionDao(),
            reportDao = userDb.reportDao()
        )
    }

    /**
     * Clears the current database and repository instances.
     * This should be called on logout or when changing accounts.
     */
    fun onUserLogout() {
        database?.close()
        database = null
        repository = null
    }
}