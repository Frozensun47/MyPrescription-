package com.MyApps.myprescription.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myprescription.util.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
@Serializable
@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val memberId: String,
    val doctorId: String?, // Foreign key to Doctor
    val reportName: String,
    @Serializable(with = DateSerializer::class)
    val date: Date,
    val notes: String? = null,
    val fileUri: String? = null, // Path to the original file in internal storage
    val mimeType: String? = null, // The MIME type of the original file
    val previewImageUri: String? = null // Path to a generated image preview (for PDFs, etc.)
)