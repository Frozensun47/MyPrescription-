package com.example.myprescription.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "reports",
    foreignKeys = [ForeignKey(
        entity = Member::class,
        parentColumns = ["id"],
        childColumns = ["memberId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["memberId"])]
)
data class Report(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memberId: String, // Foreign key to Member
    val reportName: String,
    val date: Date = Date(), // Will use TypeConverter
    val notes: String? = null,
    val fileUri: String? = null // Will store path to internal file
)