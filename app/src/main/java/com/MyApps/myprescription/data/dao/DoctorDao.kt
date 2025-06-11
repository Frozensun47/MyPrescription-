package com.MyApps.myprescription.data.dao

import androidx.room.*
import com.MyApps.myprescription.model.Doctor
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doctors: List<Doctor>)

    @Query("SELECT * FROM doctors")
    fun getAll(): Flow<List<Doctor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor)

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Delete
    suspend fun deleteDoctor(doctor: Doctor)

    @Query("SELECT * FROM doctors WHERE memberId = :memberId ORDER BY name ASC")
    fun getDoctorsForMember(memberId: String): Flow<List<Doctor>>
}