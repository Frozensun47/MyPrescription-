package com.MyApps.myprescription

import android.app.Application
import androidx.room.Room
import com.MyApps.myprescription.data.database.AppDatabase
import com.MyApps.myprescription.data.repository.AppRepository

class MyPrescriptionApplication : Application() {
    var database: AppDatabase? = null
        private set
    var repository: AppRepository? = null
        private set

    private var currentUserId: String? = null

    @Synchronized
    fun initializeDependenciesForUser(userId: String) {
        // Only initialize if the database is not already set up for the current user
        if (currentUserId == userId && database?.isOpen == true) {
            return
        }

        // Close any existing database connection before creating a new one
        database?.close()

        currentUserId = userId

        val userDb = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "my_prescription_database_$userId"
        )
            // This helps prevent crashes if you change the database schema in the future
            .fallbackToDestructiveMigration()
            .build()

        database = userDb

        repository = AppRepository(
            db = userDb,
            memberDao = userDb.memberDao(),
            prescriptionDao = userDb.prescriptionDao(),
            reportDao = userDb.reportDao(),
            doctorDao = userDb.doctorDao()
        )
    }

    @Synchronized
    fun onUserLogout() {
        database?.close()
        database = null
        repository = null
        currentUserId = null
    }
}