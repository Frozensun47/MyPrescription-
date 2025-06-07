package com.example.myprescription.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myprescription.data.dao.MemberDao
import com.example.myprescription.data.dao.PrescriptionDao
import com.example.myprescription.data.dao.ReportDao
import com.example.myprescription.model.Member
import com.example.myprescription.model.Prescription
import com.example.myprescription.model.Report

@Database(entities = [Member::class, Prescription::class, Report::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_prescription_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}