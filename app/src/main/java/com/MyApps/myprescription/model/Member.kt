package com.MyApps.myprescription.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
@Serializable
@Entity(tableName = "members")
data class Member(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val age: Int,
    val relation: String,
    val gender: String,
    val profileImageUri: String? = null // Will store path to internal file
)