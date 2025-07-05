package com.MyApps.myprescription.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.MyApps.myprescription.MyPrescriptionApplication
import com.MyApps.myprescription.data.repository.AppRepository
import com.MyApps.myprescription.model.BackupData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

class BackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = applicationContext as MyPrescriptionApplication
        val currentUser = Firebase.auth.currentUser

        // If no user is signed in, we cannot perform a backup.
        // We return success so the worker doesn't keep retrying.
        if (currentUser == null) {
            return@withContext Result.success()
        }

        try {
            // Initialize dependencies for the logged-in user
            application.initializeDependenciesForUser(currentUser.uid)
            val repository = application.repository ?: return@withContext Result.failure()

            // Create the Drive service now that we have a user context
            val googleDriveService = GoogleDriveService(applicationContext)

            val backupFile = createBackupFile(repository)
            googleDriveService.createBackup(backupFile)

            // Clean up the temporary file after a successful backup
            backupFile.delete()

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace() // Log the error for easier debugging
            return@withContext Result.failure()
        }
    }

    private suspend fun createBackupFile(repository: AppRepository): File {
        val backupFile = File(applicationContext.cacheDir, "myprescription_temp_backup.zip")

        // Fetch all data required for the backup
        val members = repository.getAllMembersOnce()
        val doctors = repository.getAllDoctorsOnce()
        val prescriptions = repository.getAllPrescriptionsOnce()
        val reports = repository.getAllReportsOnce()

        val backupData = BackupData(members, doctors, prescriptions, reports)
        val jsonString = Json.encodeToString(BackupData.serializer(), backupData)

        // Create the zip archive with all data
        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            // 1. Add the main data file
            zipOut.putNextEntry(java.util.zip.ZipEntry("backup_data.json"))
            zipOut.write(jsonString.toByteArray())
            zipOut.closeEntry()

            // 2. Add all associated images and files
            val allFilePaths = (
                    prescriptions.flatMap { it.imageUri?.split(',') ?: emptyList() } +
                            reports.flatMap { it.fileUri?.split(',') ?: emptyList() } +
                            members.mapNotNull { it.profileImageUri }
                    ).filter { it.isNotBlank() }.toSet()

            allFilePaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    zipOut.putNextEntry(java.util.zip.ZipEntry("files/${file.name}"))
                    file.inputStream().copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }

            // 3. Add the settings file
            val prefsDir = File(applicationContext.applicationInfo.dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "MyPrescriptionPrefs.xml")
            if (prefsFile.exists()) {
                zipOut.putNextEntry(java.util.zip.ZipEntry("MyPrescriptionPrefs.xml"))
                prefsFile.inputStream().copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
        return backupFile
    }
}