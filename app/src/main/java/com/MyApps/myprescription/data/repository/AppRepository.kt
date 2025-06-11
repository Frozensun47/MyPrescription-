package com.MyApps.myprescription.data.repository

import com.MyApps.myprescription.data.dao.DoctorDao
import com.MyApps.myprescription.data.dao.MemberDao
import com.MyApps.myprescription.data.dao.PrescriptionDao
import com.MyApps.myprescription.data.dao.ReportDao
import com.MyApps.myprescription.data.database.AppDatabase
import com.MyApps.myprescription.model.Doctor
import com.MyApps.myprescription.model.Member
import com.MyApps.myprescription.model.Prescription
import com.MyApps.myprescription.model.Report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(
    private val db: AppDatabase,
    private val memberDao: MemberDao,
    private val prescriptionDao: PrescriptionDao,
    private val reportDao: ReportDao,
    private val doctorDao: DoctorDao
) {
    // Member operations
    fun getAllMembers(): Flow<List<Member>> = memberDao.getAllMembers()
    suspend fun insertMember(member: Member) = memberDao.insertMember(member)
    suspend fun updateMember(member: Member) = memberDao.updateMember(member)
    suspend fun deleteMember(member: Member) = memberDao.deleteMember(member)

    // Prescription operations
    fun getPrescriptionsForMember(memberId: String): Flow<List<Prescription>> =
        prescriptionDao.getPrescriptionsForMember(memberId)
    suspend fun insertPrescription(prescription: Prescription) = prescriptionDao.insertPrescription(prescription)
    suspend fun updatePrescription(prescription: Prescription) = prescriptionDao.updatePrescription(prescription)
    suspend fun deletePrescription(prescription: Prescription) = prescriptionDao.deletePrescription(prescription)

    // Report operations
    fun getReportsForMember(memberId: String): Flow<List<Report>> =
        reportDao.getReportsForMember(memberId)
    suspend fun insertReport(report: Report) = reportDao.insertReport(report)
    suspend fun updateReport(report: Report) = reportDao.updateReport(report)
    suspend fun deleteReport(report: Report) = reportDao.deleteReport(report)

    // Doctor operations
    fun getDoctorsForMember(memberId: String): Flow<List<Doctor>> = doctorDao.getDoctorsForMember(memberId)
    suspend fun insertDoctor(doctor: Doctor) = doctorDao.insertDoctor(doctor)
    suspend fun updateDoctor(doctor: Doctor) = doctorDao.updateDoctor(doctor)
    suspend fun deleteDoctor(doctor: Doctor) = doctorDao.deleteDoctor(doctor)

    suspend fun clearAllDatabaseTables() {
        db.clearAllTables()
    }

    suspend fun getAllMembersOnce(): List<Member> = memberDao.getAllMembers().first()
}