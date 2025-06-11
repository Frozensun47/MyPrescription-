package com.MyApps.myprescription.data.dao

import androidx.room.*
import com.MyApps.myprescription.model.Prescription
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prescriptions: List<Prescription>)

    @Query("SELECT * FROM prescriptions")
    fun getAll(): Flow<List<Prescription>>

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