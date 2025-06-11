package com.MyApps.myprescription.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memberId: String,
    val name: String,
    val specialization: String? = null
)