package com.MyApps.myprescription.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "prescriptions",
    foreignKeys = [ForeignKey(
        entity = Member::class,
        parentColumns = ["id"],
        childColumns = ["memberId"],
        onDelete = ForeignKey.CASCADE // If a member is deleted, their prescriptions are also deleted
    )],
    indices = [Index(value = ["memberId"])]
)
data class Prescription(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memberId: String, // Foreign key to Member
    val doctorId: String?, // Foreign key to Doctor
    val doctorName: String,
    val date: Date = Date(), // Will use TypeConverter
    val notes: String? = null,
    val imageUri: String? = null // Will store path to internal file
)