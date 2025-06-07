package com.example.myprescription.data.dao

import androidx.room.*
import com.example.myprescription.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    @Update
    suspend fun updateReport(report: Report)

    @Delete
    suspend fun deleteReport(report: Report)

    @Query("SELECT * FROM reports WHERE memberId = :memberId ORDER BY date DESC")
    fun getReportsForMember(memberId: String): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE id = :reportId")
    fun getReportById(reportId: String): Flow<Report?>
}