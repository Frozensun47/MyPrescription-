package com.example.myprescription.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.myprescription.MyPrescriptionApplication
import com.example.myprescription.data.repository.AppRepository
import com.example.myprescription.model.Prescription
import com.example.myprescription.model.Report
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date

class MemberDetailsViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    private val _currentMemberId = MutableStateFlow<String?>(null)

    val prescriptions: StateFlow<List<Prescription>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) {
            repository.getPrescriptionsForMember(memberId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<Report>> = _currentMemberId.flatMapLatest { memberId ->
        if (memberId != null) {
            repository.getReportsForMember(memberId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddPrescriptionDialog = MutableStateFlow(false)
    val showAddPrescriptionDialog: StateFlow<Boolean> = _showAddPrescriptionDialog.asStateFlow()

    private val _showAddReportDialog = MutableStateFlow(false)
    val showAddReportDialog: StateFlow<Boolean> = _showAddReportDialog.asStateFlow()

    private val _targetPrescriptionIdForUpload = MutableStateFlow<String?>(null)
    private val _targetReportIdForUpload = MutableStateFlow<String?>(null)

    fun loadMemberData(memberId: String) {
        _currentMemberId.value = memberId
    }

    private suspend fun saveFileToInternalStorage(context: Context, uri: Uri, type: String, itemId: String): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "file"
            val fileName = "${type}_${itemId}_${System.currentTimeMillis()}.$extension"
            val file = File(context.filesDir, fileName) // Save to app's private files directory
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun addPrescription(prescriptionData: Prescription) { // Removed imageUri from params, will be handled by updatePrescriptionWithImage
        viewModelScope.launch {
            val memberId = _currentMemberId.value ?: return@launch
            val finalPrescription = prescriptionData.copy(memberId = memberId, imageUri = null) // Initially no image
            repository.insertPrescription(finalPrescription)
        }
    }

    fun addReport(reportData: Report) { // Removed fileUri from params
        viewModelScope.launch {
            val memberId = _currentMemberId.value ?: return@launch
            val finalReport = reportData.copy(memberId = memberId, fileUri = null) // Initially no file
            repository.insertReport(finalReport)
        }
    }

    fun setTargetPrescriptionForUpload(prescriptionId: String) {
        _targetPrescriptionIdForUpload.value = prescriptionId
    }

    fun setTargetReportForUpload(reportId: String) {
        _targetReportIdForUpload.value = reportId
    }

    fun updatePrescriptionWithImage(pickedImageUri: Uri) {
        viewModelScope.launch {
            val targetId = _targetPrescriptionIdForUpload.value ?: return@launch
            val memberId = _currentMemberId.value ?: return@launch
            val currentPrescription = prescriptions.value.find { it.id == targetId && it.memberId == memberId } ?: return@launch

            val imagePath = saveFileToInternalStorage(getApplication(), pickedImageUri, "prescription", targetId)
            if (imagePath != null) {
                if (currentPrescription.imageUri != null && currentPrescription.imageUri != imagePath) {
                    try { File(currentPrescription.imageUri).delete() } catch (e: Exception) { e.printStackTrace() }
                }
                val updatedPrescription = currentPrescription.copy(imageUri = imagePath)
                repository.updatePrescription(updatedPrescription)
            }
            _targetPrescriptionIdForUpload.value = null
        }
    }

    fun updateReportWithFile(pickedFileUri: Uri) {
        viewModelScope.launch {
            val targetId = _targetReportIdForUpload.value ?: return@launch
            val memberId = _currentMemberId.value ?: return@launch
            val currentReport = reports.value.find { it.id == targetId && it.memberId == memberId } ?: return@launch

            val filePath = saveFileToInternalStorage(getApplication(), pickedFileUri, "report", targetId)
            if (filePath != null) {
                if (currentReport.fileUri != null && currentReport.fileUri != filePath) {
                    try { File(currentReport.fileUri).delete() } catch (e: Exception) { e.printStackTrace() }
                }
                val updatedReport = currentReport.copy(fileUri = filePath)
                repository.updateReport(updatedReport)
            }
            _targetReportIdForUpload.value = null
        }
    }

    fun updatePrescriptionNotes(prescriptionId: String, newNotes: String) {
        viewModelScope.launch {
            val memberId = _currentMemberId.value ?: return@launch
            val prescription = prescriptions.value.find { it.id == prescriptionId && it.memberId == memberId }
            prescription?.let {
                repository.updatePrescription(it.copy(notes = newNotes.ifBlank { null }))
            }
        }
    }

    fun updateReportNotes(reportId: String, newNotes: String) {
        viewModelScope.launch {
            val memberId = _currentMemberId.value ?: return@launch
            val report = reports.value.find { it.id == reportId && it.memberId == memberId }
            report?.let {
                repository.updateReport(it.copy(notes = newNotes.ifBlank { null }))
            }
        }
    }

    fun onAddPrescriptionClicked() { _showAddPrescriptionDialog.value = true }
    fun onDismissPrescriptionDialog() { _showAddPrescriptionDialog.value = false }
    fun onAddReportClicked() { _showAddReportDialog.value = true }
    fun onDismissReportDialog() { _showAddReportDialog.value = false }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return MemberDetailsViewModel(
                    application,
                    (application as MyPrescriptionApplication).repository
                ) as T
            }
        }
    }
}