package com.example.myprescription.data.repository

import com.example.myprescription.data.dao.MemberDao
import com.example.myprescription.data.dao.PrescriptionDao
import com.example.myprescription.data.dao.ReportDao
import com.example.myprescription.model.Member
import com.example.myprescription.model.Prescription
import com.example.myprescription.model.Report
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val memberDao: MemberDao,
    private val prescriptionDao: PrescriptionDao,
    private val reportDao: ReportDao
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
    suspend fun deletePrescription(prescription: Prescription) = prescriptionDao.deletePrescription(prescription) // Add this line

    // Report operations
    fun getReportsForMember(memberId: String): Flow<List<Report>> =
        reportDao.getReportsForMember(memberId)
    suspend fun insertReport(report: Report) = reportDao.insertReport(report)
    suspend fun updateReport(report: Report) = reportDao.updateReport(report)
    suspend fun deleteReport(report: Report) = reportDao.deleteReport(report) // Add this line
}