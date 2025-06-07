package com.example.myprescription

import android.app.Application
import com.example.myprescription.data.database.AppDatabase
import com.example.myprescription.data.repository.AppRepository

class MyPrescriptionApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        AppRepository(
            database.memberDao(),
            database.prescriptionDao(),
            database.reportDao()
        )
    }
}