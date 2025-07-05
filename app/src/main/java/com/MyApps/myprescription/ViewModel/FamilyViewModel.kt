package com.MyApps.myprescription.ViewModel

import android.app.Application
import android.net.Uri
import android.util.Log // Added for logging security alerts
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.MyApps.myprescription.MyPrescriptionApplication
import com.MyApps.myprescription.data.repository.AppRepository
import com.MyApps.myprescription.model.BackupData
import com.MyApps.myprescription.model.Member
import com.MyApps.myprescription.util.saveFileToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.work.*
import com.MyApps.myprescription.utils.BackupWorker
import java.util.concurrent.TimeUnit
import com.MyApps.myprescription.utils.GoogleDriveService

class FamilyViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val members: StateFlow<List<Member>> = repository.getAllMembers()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) // MODIFIED LINE

    private val _showAddMemberDialog = MutableStateFlow(false)
    val showAddMemberDialog: StateFlow<Boolean> = _showAddMemberDialog.asStateFlow()

    private val _editingMember = MutableStateFlow<Member?>(null)
    val editingMember: StateFlow<Member?> = _editingMember.asStateFlow()

    private val _backupStatus = MutableStateFlow("Not backed up yet")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    // --- Original Functions ---
    fun addMember(memberData: Member, profilePhotoUri: Uri?) {
        viewModelScope.launch {
            var finalMember = memberData
            if (profilePhotoUri != null) {
                val imagePath = saveFileToInternalStorage(getApplication(), profilePhotoUri, "profile")
                if (imagePath.isNotBlank()) {
                    finalMember = memberData.copy(profileImageUri = imagePath)
                }
            }
            repository.insertMember(finalMember)
            _showAddMemberDialog.value = false
        }
    }
    fun setAutoBackupEnabled(enabled: Boolean) {
        val workManager = WorkManager.getInstance(getApplication())
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_backup",
                ExistingPeriodicWorkPolicy.REPLACE,
                backupRequest
            )
        } else {
            workManager.cancelUniqueWork("daily_backup")
        }
    }

    fun backupNow() {
        val workManager = WorkManager.getInstance(getApplication())
        val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>().build()
        workManager.enqueue(backupRequest)

        workManager.getWorkInfoByIdLiveData(backupRequest.id)
            .observeForever { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> _backupStatus.value = "Backing up..."
                    WorkInfo.State.SUCCEEDED -> _backupStatus.value = "Last backup: Just now"
                    WorkInfo.State.FAILED -> _backupStatus.value = "Backup failed"
                    else -> {}
                }
            }
    }
    fun updateMember(memberData: Member, profilePhotoUri: Uri?) {
        viewModelScope.launch {
            var finalMember = memberData
            if (profilePhotoUri != null && profilePhotoUri.toString() != memberData.profileImageUri) {
                val imagePath = saveFileToInternalStorage(getApplication(), profilePhotoUri, "profile")
                if (imagePath.isNotBlank()) {
                    finalMember = memberData.copy(profileImageUri = imagePath)
                }
            } else if (profilePhotoUri == null && memberData.profileImageUri != null) {
                finalMember = memberData.copy(profileImageUri = null)
            }
            repository.updateMember(finalMember)
            _editingMember.value = null
            _showAddMemberDialog.value = false
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            if (member.profileImageUri != null) {
                try { File(member.profileImageUri).delete() } catch (e: Exception) { e.printStackTrace() }
            }
            repository.deleteMember(member)
        }
    }

    fun onAddMemberClicked() {
        _editingMember.value = null
        _showAddMemberDialog.value = true
    }

    fun onEditMemberClicked(member: Member) {
        _editingMember.value = member
        _showAddMemberDialog.value = true
    }

    fun onDismissDialog() {
        _showAddMemberDialog.value = false
        _editingMember.value = null
    }

    // --- Backup and Restore Functions ---
    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val members = repository.getAllMembers().first()
                val doctors = repository.getAllDoctorsOnce()
                val prescriptions = repository.getAllPrescriptionsOnce()
                val reports = repository.getAllReportsOnce()

                val backupData = BackupData(members, doctors, prescriptions, reports)
                val jsonString = Json.encodeToString(BackupData.serializer(), backupData)

                getApplication<Application>().contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                    ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zipOut ->
                        zipOut.putNextEntry(java.util.zip.ZipEntry("backup_data.json"))
                        zipOut.write(jsonString.toByteArray())
                        zipOut.closeEntry()

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
                    }
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup created successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear existing data before import (aggressive, but consistent with current design)
                repository.clearAllDatabaseTables()
                getApplication<Application>().filesDir.listFiles()?.forEach { file -> if (file.isFile) file.delete() }


                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        val filesDir = getApplication<Application>().filesDir
                        val canonicalFilesDir = filesDir.canonicalPath + File.separator // Ensure trailing separator

                        while (entry != null) {
                            if (entry.name == "backup_data.json") {
                                val jsonString = zipIn.bufferedReader().use { it.readText() }
                                val backupData = Json.decodeFromString(BackupData.serializer(), jsonString)

                                repository.insertAllMembers(backupData.members)
                                repository.insertAllDoctors(backupData.doctors)
                                repository.insertAllPrescriptions(backupData.prescriptions)
                                repository.insertAllReports(backupData.reports)

                            } else if (entry.name.startsWith("files/")) {
                                val entryName = entry.name.substringAfter("files/")
                                val targetFile = File(filesDir, entryName)

                                // CRITICAL: Validate that the canonical path of the target file is still within the app's files directory.
                                val canonicalFilePath = targetFile.canonicalPath

                                if (!canonicalFilePath.startsWith(canonicalFilesDir)) {
                                    Log.w("FamilyViewModel", "Security Alert: Attempted directory traversal detected. Skipping file: ${entry.name}")
                                    entry = zipIn.nextEntry
                                    continue // Skip this potentially malicious entry
                                }

                                // Create parent directories if they don't exist (important for sub-folders)
                                targetFile.parentFile?.mkdirs()

                                // If the path is valid, proceed with copying the file
                                FileOutputStream(targetFile).use { fos -> zipIn.copyTo(fos) }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Restore successful! Please restart the app.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FamilyViewModel", "Restore error", e) // Log the full exception for debugging
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MyPrescriptionApplication
                val repository = application.repository ?: throw IllegalStateException("Repository not initialized")
                return FamilyViewModel(application, repository) as T
            }
        }
    }
    private val googleDriveService = GoogleDriveService(application)

    fun exportBackupToDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = createBackupFile() // You'll need to create a temporary backup file
                googleDriveService.createBackup(backupFile)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup successful!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restoreBackupFromDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileId = googleDriveService.findBackup()
                if (fileId != null) {
                    val restoreFile = File(getApplication<Application>().cacheDir, "restore.zip")
                    val outputStream = FileOutputStream(restoreFile)
                    googleDriveService.restoreBackup(fileId, outputStream)
                    // Now, you can use the 'restoreFile' with your existing import logic
                    importBackup(Uri.fromFile(restoreFile))
                } else {
                    launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "No backup found.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun createBackupFile(): File {
        // This is a simplified version of your existing exportBackup logic
        val backupFile = File(getApplication<Application>().cacheDir, "backup.zip")
        // ... create the zip file here ...
        return backupFile
    }
}