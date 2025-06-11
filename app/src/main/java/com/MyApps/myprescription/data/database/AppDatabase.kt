package com.MyApps.myprescription.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.MyApps.myprescription.data.dao.DoctorDao
import com.MyApps.myprescription.data.dao.MemberDao
import com.MyApps.myprescription.data.dao.PrescriptionDao
import com.MyApps.myprescription.data.dao.ReportDao
import com.MyApps.myprescription.model.Doctor
import com.MyApps.myprescription.model.Member
import com.MyApps.myprescription.model.Prescription
import com.MyApps.myprescription.model.Report

@Database(entities = [Member::class, Prescription::class, Report::class, Doctor::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun reportDao(): ReportDao
    abstract fun doctorDao(): DoctorDao
}