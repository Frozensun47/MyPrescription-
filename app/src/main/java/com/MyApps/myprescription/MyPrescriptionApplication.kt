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

    fun initializeDependenciesForUser(userId: String) {
        database?.close()

        val userDb = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "my_prescription_database_$userId"
        ).build()
        database = userDb

        repository = AppRepository(
            db = userDb,
            memberDao = userDb.memberDao(),
            prescriptionDao = userDb.prescriptionDao(),
            reportDao = userDb.reportDao(),
            doctorDao = userDb.doctorDao() // Add this line
        )
    }

    fun onUserLogout() {
        database?.close()
        database = null
        repository = null
    }
}