package com.example.myprescription.data.database

import androidx.room.Database
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

    // The Companion Object singleton has been removed.
}