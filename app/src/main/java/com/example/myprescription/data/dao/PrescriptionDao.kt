package com.example.myprescription.data.dao

import androidx.room.*
import com.example.myprescription.model.Prescription
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription)

    @Update
    suspend fun updatePrescription(prescription: Prescription)

    @Delete
    suspend fun deletePrescription(prescription: Prescription)

    @Query("SELECT * FROM prescriptions WHERE memberId = :memberId ORDER BY date DESC")
    fun getPrescriptionsForMember(memberId: String): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE id = :prescriptionId")
    fun getPrescriptionById(prescriptionId: String): Flow<Prescription?>
}