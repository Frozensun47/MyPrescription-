package com.MyApps.myprescription.model

import com.MyApps.myprescription.model.Doctor
import com.MyApps.myprescription.model.Member
import com.MyApps.myprescription.model.Prescription
import com.MyApps.myprescription.model.Report
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val members: List<Member>,
    val doctors: List<Doctor>,
    val prescriptions: List<Prescription>,
    val reports: List<Report>
)